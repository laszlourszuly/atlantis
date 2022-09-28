package com.echsylon.atlantis.message

import com.echsylon.atlantis.extension.toRandomDelay
import com.echsylon.atlantis.extension.toRandomSize
import com.echsylon.atlantis.extension.writeIfNotNull
import com.echsylon.atlantis.message.Reason.GOING_AWAY
import com.echsylon.atlantis.message.Reason.INTERNAL_ERROR
import com.echsylon.atlantis.message.Reason.MESSAGE_TO_BIG
import com.echsylon.atlantis.message.Reason.NORMAL_CLOSURE
import com.echsylon.atlantis.message.Reason.PROTOCOL_ERROR
import com.echsylon.atlantis.message.Type.CLOSE
import com.echsylon.atlantis.message.Type.PING
import com.echsylon.atlantis.message.Type.PONG
import com.echsylon.atlantis.message.exception.PayloadTooLargeException
import com.echsylon.atlantis.message.exception.UnmaskedFrameException
import com.echsylon.atlantis.response.Message
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import java.io.ByteArrayOutputStream
import java.util.concurrent.BlockingDeque
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This class is responsible for the sending and receiving messages over a
 * socket. It does so by observing the socket InputStream and aggregating
 * frames (and composing them to messages) sent by the client. Once a message
 * is fully read it is exposed through an injected callback mechanism.
 *
 * Any messages to send are similarly split up to frames, if necessary, and
 * written to the socket OutputStream.
 *
 * While the observing part is a blocking operation (blocks the calling thread)
 * the composing and splitting operations are done from spawned worker threads.
 */
class WebSocket(
    private val path: String,
    private val onMessage: (message: ByteArray, isText: Boolean) -> Unit
) {
    private val frames: FrameHelper = FrameHelper()
    private val started: AtomicBoolean = AtomicBoolean(false)
    private val closing: AtomicBoolean = AtomicBoolean(false)
    private val messageWriteQueue: BlockingDeque<Frame> = LinkedBlockingDeque()
    private val messageReadQueue: BlockingQueue<Frame> = LinkedBlockingQueue()
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(10) {
        Thread(it, "Atlantis Mock WebSocket").apply { isDaemon = true }
    }

    private lateinit var source: BufferedSource
    private lateinit var sink: BufferedSink

    /**
     * Whether this instance will accept more messages or not.
     */
    val isShutdown: Boolean
        get() = executor.isShutdown

    /**
     * Sends the given messages honoring any configured delays and frame sizes.
     *
     * @param messages The messages to send. This implementation will cap the
     * data at Int.MAX_VALUE bytes for practical reasons.
     */
    fun send(vararg messages: Message) {
        messages.forEach { sendMessage(it) }
    }

    /**
     * Starts the communication infrastructure. This includes spawning message
     * read and write worker threads as well as parsing incoming frames on the
     * WebSocket. The parsing of frames is done on the calling thread. As a
     * result the calling thread will be blocked by this function.
     *
     * @param socketInput The input stream to read peer frames from.
     * @param socketOutput The output stream to write messages to.
     */
    fun start(socketInput: BufferedSource, socketOutput: BufferedSink) {
        if (started.get()) return
        source = socketInput
        sink = socketOutput
        startReadingFrames()        // Will spawn worker thread
        startWritingFrames()        // Will spawn worker thread
        startObservingSocket()      // Will block current thread
    }

    /**
     * Initiates a controlled close sequence, awaiting a corresponding ack
     * close message from the peer client, rejecting any further messages.
     *
     * @param reason Optional close reason.
     */
    fun stop(reason: Reason = GOING_AWAY) {
        sendClose(reason)
    }

    /**
     * Enqueues a pong frame at the head of the send message queue.
     *
     * @param payload Optional data to send along the ping. Note that the
     * WebSocket specification only allows for 125 bytes of payload in
     * control frames.
     */
    private fun sendPong(payload: ByteArray?) {
        executor.runCatching {
            execute {
                val frame = frames.createPongFrame(payload)
                messageWriteQueue.putFirst(frame)
            }
        }
    }

    /**
     * Enqueues the frames from the given message after the configured amount
     * of time to the write-queue.
     *
     * @param message The message to split and send.
     */
    private fun sendMessage(message: Message) {
        val delay = message.delay.toRandomDelay()
        val fragments = splitMessage(message)
        if (fragments.isNotEmpty()) {
            executor.runCatching {
                schedule( // Will throw if closing (because in "shutdown" state)
                    {
                        println("Atlantis WebSocket Send:\n\t${path}\n\t$message")
                        messageWriteQueue
                            .runCatching { addAll(fragments) }
                            .onFailure { shutdown() }
                            .onSuccess {
                                // Re-schedule ping and pong messages.
                                if (message.delay.isZero()) return@onSuccess
                                if (message.type == PING) sendMessage(message)
                                if (message.type == PONG) sendMessage(message)
                            }
                    },
                    delay.toLong(),
                    MILLISECONDS
                )
            }.onFailure {
                shutdown()
            }
        }
    }

    /**
     * Enqueues a server induced close message at the head of the write queue.
     *
     * @param reason The reason for closing the connection.
     */
    private fun sendClose(reason: Reason) {
        executor.runCatching {
            execute {
                val frame = frames.createCloseFrame(reason)
                messageWriteQueue.putFirst(frame)
            }
        }
    }

    /**
     * Collects the successfully read frames from the read-queue and composes
     * messages from them, sending them back to the caller through a callback
     * interface.
     *
     * This function automatically acts on Close and Ping frames.
     */
    private fun startReadingFrames() {
        // We don't want to be blocked by, nor do we want to block the
        // handshake request thread while we're assembling frames into
        // messages, hence, we act in our own thread.
        executor.runCatching {
            execute {
                val message = ByteArrayOutputStream()
                var isText = false
                while (!executor.isShutdown || closing.get()) {
                    val frame = messageReadQueue.take()
                    val isClosing = closing.get()
                    when {
                        frame.isCloseFrame -> {
                            val code = frames.parseCloseReason(frame)
                            println("Atlantis WebSocket Receive:\n\t$path\n\tclose ($code)")
                            if (isClosing) shutdownNow()
                            else close(frame)
                            break
                        }
                        isClosing -> {
                            // Ignore all other frames while closing.
                            continue
                        }
                        frame.isPongFrame -> {
                            // No default behaviour on Pong messages.
                            println("Atlantis WebSocket Receive:\n\t$path\n\tpong")
                            continue
                        }
                        frame.isPingFrame -> {
                            println("Atlantis WebSocket Receive:\n\t$path\n\tping")
                            sendPong(frame.payload)
                            continue
                        }
                        frame.isTextFrame -> {
                            isText = true
                            message.reset()
                            message.writeIfNotNull(frame.payload)
                        }
                        frame.isBinaryFrame -> {
                            isText = false
                            message.reset()
                            message.writeIfNotNull(frame.payload)
                        }
                        frame.isContinuationFrame -> {
                            message.writeIfNotNull(frame.payload)
                        }
                    }
                    if (frame.isFinalFrame) {
                        val bytes = message.toByteArray()
                        println("Atlantis WebSocket Receive:\n\t$path\n\t${Buffer().write(bytes)}")
                        onMessage(bytes, isText)
                    }
                }
            }
        }.onFailure {
            shutdown()
        }
    }

    /**
     * Takes the message frames, one-by-one, from the write-queue and sends
     * them to the remote peer.
     */
    private fun startWritingFrames() {
        // We don't want to be blocked by, nor do we want to block the
        // handshake request thread while we're sending messages to the
        // remote peer, hence, we act in our own thread.
        executor.runCatching {
            execute {
                while (!executor.isShutdown) {
                    val frame = messageWriteQueue.take()
                    when {
                        frame.isCloseFrame -> close(frame)
                        else -> frames.writeFrame(frame, sink)
                    }
                }
            }
        }.onFailure { cause ->
            when (cause) {
                is PayloadTooLargeException -> close(frames.createCloseFrame(INTERNAL_ERROR))
                else -> shutdown()
            }
        }
    }

    /**
     * Starts reading the WebSocket input stream in a blocking fashion. When a
     * frame has successfully been decoded it's added to the read-queue. This
     * function aborts further execution when detecting an unmasked client
     * frame or a frame with a payload larger than Int.MAX_VALUE.
     */
    private fun startObservingSocket() {
        // This function must run in the handshake request thread. The caller
        // of this function is responsible to adjust the timeout of the socket
        // accordingly.
        runCatching {
            while (!executor.isShutdown) {
                val frame = frames.readFrame(source)
                messageReadQueue.add(frame)
            }
        }.onFailure { cause ->
            when (cause) {
                is UnmaskedFrameException -> close(frames.createCloseFrame(PROTOCOL_ERROR))
                is PayloadTooLargeException -> close(frames.createCloseFrame(MESSAGE_TO_BIG))
                else -> shutdown()
            }
        }
    }

    /**
     * Splits a message into frames of the configured size. Control messages,
     * like Close, Ping or Pong, are always returned as single-item lists.
     *
     * @param message The message to split. The message defines the range of
     * its desired frame sizes. Frame size [0..0] is interpreted as max frame
     * size.
     */
    private fun splitMessage(message: Message): List<Frame> {
        return message.takeIf { it.type == CLOSE }?.let { listOf(frames.createCloseFrame(message.code, message.text)) }
            ?: message.takeIf { it.type == PING }?.let { listOf(frames.createPingFrame(message.payload)) }
            ?: message.takeIf { it.type == PONG }?.let { listOf(frames.createPongFrame(message.payload)) }
            ?: message.text?.let { frames.createTextFrames(it, message.chunk.toRandomSize()) }
            ?: message.data?.let { frames.createDataFrames(it, message.chunk.toRandomSize()) }
            ?: emptyList()
    }

    /**
     * Initiates a controlled close sequence, including sending a close message
     * to the remote peer. The message is sent immediately from the current
     * thread.
     *
     * @param closeFrame The close frame to send to the remote peer. This is
     * typically the peers close frame being echoed if the peer initiated the
     * close sequence. Defaults to a NORMAL_CLOSURE close frame.
     */
    private fun close(closeFrame: Frame? = null) {
        val frame = closeFrame ?: frames.createCloseFrame(NORMAL_CLOSURE)
        frames.runCatching { writeFrame(frame, sink) }
        closing.set(true)
        shutdown()
    }

    /**
     * Shuts down spawned worker threads and cleans up any pending resources.
     */
    private fun shutdown() {
        messageWriteQueue.clear()
        messageReadQueue.clear()
        executor.shutdown()
    }
}

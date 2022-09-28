package com.echsylon.atlantis.demo

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.echsylon.atlantis.Atlantis
import com.echsylon.atlantis.demo.HttpClient.*
import com.echsylon.atlantis.demo.databinding.MainActivityBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val binding by lazy { MainActivityBinding.inflate(LayoutInflater.from(this)) }
    private val httpClient by lazy { HttpClient.create() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Atlantis is configured in DebugDemoApplication

        binding.actionJson.setOnClickListener { onJsonClick() }
        binding.actionSerial.setOnClickListener { onNextClick() }
        binding.actionRandom.setOnClickListener { onRandomClick() }
        binding.actionMessaging.setOnClickListener { startMessaging() }
    }

    private fun onJsonClick() {
        binding.responseJson.text = null
        binding.actionJson.isEnabled = false
        lifecycleScope.launch(IO) {
            httpClient
                .runCatching { getJsonString() }
                .onFailure { it.printStackTrace() }
                .onSuccess { withContext(Main) { binding.responseJson.text = it } }
                .also { withContext(Main) { binding.actionJson.isEnabled = true } }
        }
    }

    private fun onNextClick() {
        binding.responseSerial.text = null
        binding.actionSerial.isEnabled = false
        lifecycleScope.launch(IO) {
            httpClient
                .runCatching { getNextString() }
                .onFailure { it.printStackTrace() }
                .onSuccess { withContext(Main) { binding.responseSerial.text = it } }
                .also { withContext(Main) { binding.actionSerial.isEnabled = true } }
        }
    }

    private fun onRandomClick() {
        binding.responseRandom.text = null
        binding.actionRandom.isEnabled = false
        lifecycleScope.launch(IO) {
            httpClient
                .runCatching { getAnyString() }
                .onFailure { it.printStackTrace() }
                .onSuccess { withContext(Main) { binding.responseRandom.text = it } }
                .also { withContext(Main) { binding.actionRandom.isEnabled = true } }
        }
    }

    @ExperimentalCoroutinesApi
    private fun startMessaging() {
        binding.responseMessaging.text = null
        binding.actionMessaging.isEnabled = false
        lifecycleScope.launch(IO) {
            httpClient
                .runCatching { connectWebSocket().collect { withContext(Main) { handleEvent(it) } } }
                .onFailure { it.printStackTrace() }
                .onSuccess { /* nothing to do, the magic happens in handleEvent() */ }
        }
    }

    private fun stopMessaging() {
        httpClient.disconnectWebSocket()
    }

    @ExperimentalCoroutinesApi
    private fun handleEvent(event: Event) {
        when (event) {
            is Open -> {
                binding.actionMessaging.text = getString(R.string.stop)
                binding.actionMessaging.isEnabled = true
                binding.actionMessaging.setOnClickListener { stopMessaging() }
                binding.responseMessaging.append("---OPENED---\n")
            }
            is Close -> {
                binding.actionMessaging.text = getString(R.string.start)
                binding.actionMessaging.isEnabled = true
                binding.actionMessaging.setOnClickListener { startMessaging() }
                binding.responseMessaging.append("---CLOSED (${event.reason})---\n")
            }
            is Failure -> {
                binding.actionMessaging.text = getString(R.string.start)
                binding.actionMessaging.isEnabled = true
                binding.actionMessaging.setOnClickListener { startMessaging() }
                binding.responseMessaging.append("---ERROR---\n")
            }
            is Message -> {
                binding.responseMessaging.append("${event.text}\n")
            }
        }
    }
}
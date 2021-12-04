package com.echsylon.atlantis

import java.io.InputStream

/**
 * The entry point for configuring and starting and stopping the Atlantis
 * mock server implementation.
 */
class Atlantis {
    private val configurator = ConfigurationHelper()
    private val server = Server()

    /**
     * Returns the current started state of this Atlantis instance.
     * @return Whether it's running or not.
     */
    val isRunning: Boolean
        get() = server.isRunning

    /**
     * Replaces any existing request pattern configuration with the provided
     * configuration.
     *
     * @param stream The input stream to read the new configuration from. This
     *               can be a file or any other medium exposing the UTF-8 JSON
     *               notation of an Atlantis 4 configuration.
     */
    fun setConfiguration(stream: InputStream) {
        val config = Configuration()
        configurator.addFromJson(config, stream)
        server.configuration = config
    }

    /**
     * Replaces any existing request pattern configuration with the provided
     * configuration.
     *
     * @param configuration The new configuration.
     */
    fun setConfiguration(configuration: Configuration) {
        server.configuration = configuration
    }

    /**
     * Appends the provided configuration to the existing one.
     *
     * @param stream The input stream to read the additional configuration
     *               from. This can be a file or any other medium exposing the
     *               UTF-8 JSON notation of an Atlantis 4 configuration.
     */
    fun addConfiguration(stream: InputStream) {
        configurator.addFromJson(server.configuration, stream)
    }

    /**
     * Appends the provided configuration to the existing one.
     *
     * @param configuration The additional configuration.
     */
    fun addConfiguration(configuration: Configuration) {
        server.configuration.addConfiguration(configuration)
    }

    /**
     * Starts the Atlantis mock server. Optionally a PKCS12 trust store,
     * containing one X509 certificate and one RSA key, can be provided. This
     * will have Atlantis serve HTTPS responses using the provided certificate.
     *
     * @param port The port of the local host address to start listening on for
     *             any client HTTP requests.
     * @param trust The optional trust store byte array.
     * @param password The corresponding password, or null.
     */
    fun start(port: Int = 0, trust: InputStream? = null, password: String? = null) {
        server.start(port, trust?.readBytes(), password)
    }

    /**
     * Stops the Atlantis mock server.
     */
    fun stop() {
        server.stop()
    }
}
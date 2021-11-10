package com.echsylon.atlantis

import java.io.InputStream

class Atlantis {
    private val configurator = ConfigurationHelper()
    private val server = Server()

    val isRunning: Boolean
        get() = server.isRunning

    fun setConfiguration(stream: InputStream) {
        val config = Configuration()
        configurator.addFromJson(config, stream)
        server.configuration = config
    }

    fun setConfiguration(configuration: Configuration) {
        server.configuration = configuration
    }

    fun addConfiguration(stream: InputStream) {
        configurator.addFromJson(server.configuration, stream)
    }

    fun addConfiguration(configuration: Configuration) {
        server.configuration.addConfiguration(configuration)
    }

    fun start(port: Int = 0) {
        server.start(port)
    }

    fun stop() {
        server.stop()
    }
}
package com.echsylon.atlantis.demo

import com.echsylon.atlantis.Atlantis

class DebugDemoApplication : DemoApplication() {
    private val atlantis by lazy { Atlantis() }

    override fun onCreate() {
        super.onCreate()
        val configStream = assets.open("atlantis_config.json")
        val trustStore = assets.open("mock_trust.p12")
        atlantis.addConfiguration(configStream)
        atlantis.start(8080, trustStore, "password")
    }
}
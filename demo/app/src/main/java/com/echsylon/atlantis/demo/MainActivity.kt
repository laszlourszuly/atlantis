package com.echsylon.atlantis.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.echsylon.atlantis.Atlantis
import com.echsylon.atlantis.demo.databinding.MainActivityBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val binding by lazy { MainActivityBinding.inflate(LayoutInflater.from(this)) }
    private val httpClient by lazy { HttpClient.create() }
    private val atlantis by lazy { Atlantis() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val configStream = assets.open("atlantis_config.json")
        val trustStore = assets.open("mock_trust.p12")
        atlantis.addConfiguration(configStream)
        atlantis.start(8080, trustStore, "password")

        binding.actionJson.setOnClickListener { onJsonClick() }
        binding.actionSerial.setOnClickListener { onNextClick() }
        binding.actionRandom.setOnClickListener { onRandomClick() }
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
}
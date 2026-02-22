package com.jizhi.stream.core.config

import com.google.gson.Gson
import java.io.File

data class AppSettings(
    var deviceName: String = "JizhiStream",
    var password: String = "jizhi2024",
    var streamConfig: StreamConfig = StreamConfig(),
    var manualDevices: MutableList<ManualDevice> = mutableListOf()
) {
    data class ManualDevice(val name: String, val ip: String, val port: Int = 23333)

    companion object {
        private val gson = Gson()
        private fun getConfigFile(): File {
            val dir = File(System.getProperty("user.home"), ".jizhistream")
            dir.mkdirs()
            return File(dir, "settings.json")
        }

        fun load(): AppSettings {
            val file = getConfigFile()
            return if (file.exists()) {
                try { gson.fromJson(file.readText(), AppSettings::class.java) } catch (_: Exception) { AppSettings() }
            } else AppSettings()
        }

        fun save(settings: AppSettings) {
            getConfigFile().writeText(gson.toJson(settings))
        }
    }
}

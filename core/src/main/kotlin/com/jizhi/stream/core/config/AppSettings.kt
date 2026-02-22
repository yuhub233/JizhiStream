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
        private var customDir: File? = null

        fun init(dir: File) {
            customDir = dir
        }

        private fun getConfigFile(): File {
            val dir = customDir ?: File(System.getProperty("user.home"), ".jizhistream")
            dir.mkdirs()
            return File(dir, "settings.json")
        }

        fun load(): AppSettings {
            return try {
                val file = getConfigFile()
                if (file.exists()) {
                    gson.fromJson(file.readText(), AppSettings::class.java) ?: AppSettings()
                } else AppSettings()
            } catch (_: Exception) { AppSettings() }
        }

        fun save(settings: AppSettings) {
            try { getConfigFile().writeText(gson.toJson(settings)) } catch (_: Exception) {}
        }
    }
}

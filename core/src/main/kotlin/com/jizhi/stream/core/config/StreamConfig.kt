package com.jizhi.stream.core.config

data class StreamConfig(
    val width: Int = 1920,
    val height: Int = 1080,
    val maxBitrateMbps: Int = 100000,
    val fps: Int = 60,
    val codec: String = "h264"
) {
    fun encode(): ByteArray {
        return "$width|$height|$maxBitrateMbps|$fps|$codec".toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun decode(data: ByteArray): StreamConfig? {
            val str = String(data, Charsets.UTF_8)
            val parts = str.split("|")
            if (parts.size != 5) return null
            return try {
                StreamConfig(
                    width = parts[0].toInt(),
                    height = parts[1].toInt(),
                    maxBitrateMbps = parts[2].toInt(),
                    fps = parts[3].toInt(),
                    codec = parts[4]
                )
            } catch (_: Exception) { null }
        }
    }
}

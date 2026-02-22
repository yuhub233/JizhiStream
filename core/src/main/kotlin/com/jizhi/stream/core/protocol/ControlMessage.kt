package com.jizhi.stream.core.protocol

import java.nio.ByteBuffer

data class ControlMessage(
    val type: Byte,
    val payload: ByteArray = ByteArray(0)
) {
    fun encode(): ByteArray {
        val buf = ByteBuffer.allocate(8 + payload.size)
        buf.putInt(StreamProtocol.MAGIC.toInt())
        buf.put(StreamProtocol.VERSION)
        buf.put(type)
        buf.putShort(payload.size.toShort())
        buf.put(payload)
        return buf.array()
    }

    companion object {
        fun decode(data: ByteArray): ControlMessage? {
            if (data.size < 8) return null
            val buf = ByteBuffer.wrap(data)
            val magic = buf.int
            if (magic != StreamProtocol.MAGIC.toInt()) return null
            val version = buf.get()
            if (version != StreamProtocol.VERSION) return null
            val type = buf.get()
            val len = buf.short.toInt() and 0xFFFF
            if (data.size < 8 + len) return null
            val payload = ByteArray(len)
            buf.get(payload)
            return ControlMessage(type, payload)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ControlMessage) return false
        return type == other.type && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int = 31 * type.hashCode() + payload.contentHashCode()
}

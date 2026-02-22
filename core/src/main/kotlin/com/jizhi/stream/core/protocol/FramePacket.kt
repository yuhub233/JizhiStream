package com.jizhi.stream.core.protocol

import java.nio.ByteBuffer

data class FramePacket(
    val frameId: Int,
    val chunkIndex: Short,
    val totalChunks: Short,
    val timestamp: Long,
    val data: ByteArray
) {
    fun encode(): ByteArray {
        val buf = ByteBuffer.allocate(StreamProtocol.HEADER_SIZE + data.size)
        buf.putInt(frameId)
        buf.putShort(chunkIndex)
        buf.putShort(totalChunks)
        buf.putLong(timestamp)
        buf.put(data)
        return buf.array()
    }

    companion object {
        fun decode(raw: ByteArray): FramePacket? {
            if (raw.size < StreamProtocol.HEADER_SIZE) return null
            val buf = ByteBuffer.wrap(raw)
            val frameId = buf.int
            val chunkIndex = buf.short
            val totalChunks = buf.short
            val timestamp = buf.long
            val data = ByteArray(raw.size - StreamProtocol.HEADER_SIZE)
            buf.get(data)
            return FramePacket(frameId, chunkIndex, totalChunks, timestamp, data)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FramePacket) return false
        return frameId == other.frameId && chunkIndex == other.chunkIndex && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * frameId + chunkIndex.toInt()
}

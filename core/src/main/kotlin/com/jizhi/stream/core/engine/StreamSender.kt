package com.jizhi.stream.core.engine

import com.jizhi.stream.core.protocol.FramePacket
import com.jizhi.stream.core.protocol.StreamProtocol
import kotlinx.coroutines.*
import java.net.*
import java.util.concurrent.atomic.AtomicInteger

class StreamSender(private val targetHost: String) {
    private var socket: DatagramSocket? = null
    private var running = false
    private val frameCounter = AtomicInteger(0)
    private val targetAddr by lazy { InetAddress.getByName(targetHost) }

    fun start() {
        running = true
        socket = DatagramSocket().apply {
            sendBufferSize = 16 * 1024 * 1024
        }
    }

    fun stop() {
        running = false
        socket?.close()
    }

    fun sendFrame(frameData: ByteArray) {
        if (!running) return
        val frameId = frameCounter.getAndIncrement()
        val timestamp = System.nanoTime()
        val maxPayload = StreamProtocol.MAX_PAYLOAD
        val totalChunks = ((frameData.size + maxPayload - 1) / maxPayload).toShort()

        var offset = 0
        var chunkIdx: Short = 0
        while (offset < frameData.size) {
            val end = minOf(offset + maxPayload, frameData.size)
            val chunk = frameData.copyOfRange(offset, end)
            val packet = FramePacket(frameId, chunkIdx, totalChunks, timestamp, chunk)
            val encoded = packet.encode()
            try {
                socket?.send(DatagramPacket(encoded, encoded.size, targetAddr, StreamProtocol.STREAM_PORT))
            } catch (_: Exception) {}
            offset = end
            chunkIdx++
        }
    }
}

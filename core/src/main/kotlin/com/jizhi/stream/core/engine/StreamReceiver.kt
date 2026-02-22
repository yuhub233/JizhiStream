package com.jizhi.stream.core.engine

import com.jizhi.stream.core.protocol.FramePacket
import com.jizhi.stream.core.protocol.StreamProtocol
import kotlinx.coroutines.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap

class StreamReceiver {
    private var socket: DatagramSocket? = null
    private var running = false
    var onFrameReady: ((ByteArray, Long) -> Unit)? = null

    private val frameBuffers = ConcurrentHashMap<Int, FrameBuffer>()

    private class FrameBuffer(val totalChunks: Int, val timestamp: Long) {
        val chunks = arrayOfNulls<ByteArray>(totalChunks)
        var received = 0

        fun addChunk(index: Int, data: ByteArray): Boolean {
            if (index < 0 || index >= totalChunks) return false
            if (chunks[index] == null) {
                chunks[index] = data
                received++
            }
            return received == totalChunks
        }

        fun assemble(): ByteArray {
            var totalSize = 0
            for (c in chunks) totalSize += c?.size ?: 0
            val result = ByteArray(totalSize)
            var offset = 0
            for (c in chunks) {
                if (c != null) {
                    System.arraycopy(c, 0, result, offset, c.size)
                    offset += c.size
                }
            }
            return result
        }
    }

    fun start(scope: CoroutineScope) {
        running = true
        scope.launch(Dispatchers.IO) { receiveLoop() }
        scope.launch(Dispatchers.IO) { cleanupLoop() }
    }

    fun stop() {
        running = false
        socket?.close()
    }

    private fun receiveLoop() {
        socket = DatagramSocket(null).apply {
            reuseAddress = true
            receiveBufferSize = 16 * 1024 * 1024
            bind(InetSocketAddress(StreamProtocol.STREAM_PORT))
        }
        val buf = ByteArray(StreamProtocol.MAX_UDP_PACKET)
        while (running) {
            try {
                val packet = DatagramPacket(buf, buf.size)
                socket?.receive(packet)
                val fp = FramePacket.decode(buf.copyOf(packet.length)) ?: continue
                val buffer = frameBuffers.getOrPut(fp.frameId) {
                    FrameBuffer(fp.totalChunks.toInt(), fp.timestamp)
                }
                if (buffer.addChunk(fp.chunkIndex.toInt(), fp.data)) {
                    frameBuffers.remove(fp.frameId)
                    onFrameReady?.invoke(buffer.assemble(), buffer.timestamp)
                }
            } catch (_: Exception) {
                if (!running) break
            }
        }
    }

    private suspend fun cleanupLoop() {
        while (running) {
            delay(1000)
            val now = System.nanoTime()
            val staleIds = frameBuffers.entries
                .filter { now - it.value.timestamp > 2_000_000_000L }
                .map { it.key }
            staleIds.forEach { frameBuffers.remove(it) }
        }
    }
}

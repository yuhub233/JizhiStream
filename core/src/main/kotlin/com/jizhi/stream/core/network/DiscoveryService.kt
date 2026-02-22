package com.jizhi.stream.core.network

import com.jizhi.stream.core.protocol.ControlMessage
import com.jizhi.stream.core.protocol.StreamProtocol
import kotlinx.coroutines.*
import java.net.*

data class DeviceInfo(
    val name: String,
    val ip: String,
    val platform: String,
    val port: Int = StreamProtocol.CONTROL_PORT
)

class DiscoveryService(
    private val deviceName: String,
    private val platform: String
) {
    private var running = false
    private var socket: DatagramSocket? = null
    private val discovered = mutableMapOf<String, DeviceInfo>()
    var onDeviceFound: ((DeviceInfo) -> Unit)? = null

    fun getDevices(): List<DeviceInfo> = synchronized(discovered) { discovered.values.toList() }

    fun start(scope: CoroutineScope) {
        running = true
        scope.launch(Dispatchers.IO) { listenLoop() }
        scope.launch(Dispatchers.IO) { broadcastLoop() }
    }

    fun stop() {
        running = false
        socket?.close()
    }

    private fun buildPayload(): ByteArray {
        val info = "$deviceName|$platform"
        return info.toByteArray(Charsets.UTF_8)
    }

    private fun parsePayload(data: ByteArray): Pair<String, String>? {
        val str = String(data, Charsets.UTF_8)
        val parts = str.split("|", limit = 2)
        if (parts.size != 2) return null
        return parts[0] to parts[1]
    }

    private suspend fun broadcastLoop() {
        val sock = DatagramSocket()
        sock.broadcast = true
        while (running) {
            try {
                val msg = ControlMessage(StreamProtocol.MSG_DISCOVER, buildPayload()).encode()
                val packet = DatagramPacket(msg, msg.size, InetAddress.getByName("255.255.255.255"), StreamProtocol.DISCOVERY_PORT)
                sock.send(packet)
            } catch (_: Exception) {}
            delay(2000)
        }
        sock.close()
    }

    private fun listenLoop() {
        socket = DatagramSocket(null).apply {
            reuseAddress = true
            bind(InetSocketAddress(StreamProtocol.DISCOVERY_PORT))
        }
        val buf = ByteArray(1024)
        while (running) {
            try {
                val packet = DatagramPacket(buf, buf.size)
                socket?.receive(packet)
                val msg = ControlMessage.decode(buf.copyOf(packet.length)) ?: continue
                val ip = packet.address.hostAddress ?: continue
                if (msg.type == StreamProtocol.MSG_DISCOVER) {
                    val (name, plat) = parsePayload(msg.payload) ?: continue
                    val reply = ControlMessage(StreamProtocol.MSG_DISCOVER_REPLY, buildPayload()).encode()
                    socket?.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
                    addDevice(DeviceInfo(name, ip, plat))
                } else if (msg.type == StreamProtocol.MSG_DISCOVER_REPLY) {
                    val (name, plat) = parsePayload(msg.payload) ?: continue
                    addDevice(DeviceInfo(name, ip, plat))
                }
            } catch (_: Exception) {
                if (!running) break
            }
        }
    }

    private fun addDevice(device: DeviceInfo) {
        val isNew = synchronized(discovered) {
            val existing = discovered[device.ip]
            discovered[device.ip] = device
            existing == null
        }
        if (isNew) onDeviceFound?.invoke(device)
    }
}

package com.jizhi.stream.core.network

import com.jizhi.stream.core.config.StreamConfig
import com.jizhi.stream.core.protocol.ControlMessage
import com.jizhi.stream.core.protocol.StreamProtocol
import com.jizhi.stream.core.security.AuthManager
import java.io.*
import java.net.Socket

class ControlClient(private val authManager: AuthManager) {
    private var socket: Socket? = null
    private var output: DataOutputStream? = null
    private var input: DataInputStream? = null

    fun connect(host: String, port: Int = StreamProtocol.CONTROL_PORT): Boolean {
        return try {
            socket = Socket(host, port).apply { soTimeout = 10000 }
            output = DataOutputStream(socket!!.getOutputStream())
            input = DataInputStream(socket!!.getInputStream())

            val challengeMsg = readMsg() ?: return false
            if (challengeMsg.type != StreamProtocol.MSG_AUTH_REQUEST) return false

            val response = authManager.computeResponse(challengeMsg.payload)
            sendMsg(ControlMessage(StreamProtocol.MSG_AUTH_RESPONSE, response))

            val result = readMsg() ?: return false
            if (result.type != StreamProtocol.MSG_AUTH_RESPONSE) return false
            result.payload.isNotEmpty() && result.payload[0] == StreamProtocol.AUTH_OK
        } catch (_: Exception) { false }
    }

    fun requestStream(config: StreamConfig): Boolean {
        return try {
            sendMsg(ControlMessage(StreamProtocol.MSG_START_STREAM, config.encode()))
            true
        } catch (_: Exception) { false }
    }

    fun stopStream() {
        try { sendMsg(ControlMessage(StreamProtocol.MSG_STOP_STREAM)) } catch (_: Exception) {}
    }

    fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
    }

    private fun sendMsg(msg: ControlMessage) {
        val data = msg.encode()
        output?.writeInt(data.size)
        output?.write(data)
        output?.flush()
    }

    private fun readMsg(): ControlMessage? {
        return try {
            val len = input?.readInt() ?: return null
            if (len > 65536 || len < 0) return null
            val data = ByteArray(len)
            input?.readFully(data)
            ControlMessage.decode(data)
        } catch (_: Exception) { null }
    }
}

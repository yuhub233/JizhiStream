package com.jizhi.stream.core.network

import com.jizhi.stream.core.config.StreamConfig
import com.jizhi.stream.core.protocol.ControlMessage
import com.jizhi.stream.core.protocol.StreamProtocol
import com.jizhi.stream.core.security.AuthManager
import kotlinx.coroutines.*
import java.io.*
import java.net.*

class ControlServer(
    private val authManager: AuthManager,
    private val onStreamRequested: (StreamConfig, Socket) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var running = false
    private val challenges = mutableMapOf<String, ByteArray>()

    fun start(scope: CoroutineScope) {
        running = true
        scope.launch(Dispatchers.IO) { acceptLoop() }
    }

    fun stop() {
        running = false
        serverSocket?.close()
    }

    private fun acceptLoop() {
        serverSocket = ServerSocket(StreamProtocol.CONTROL_PORT)
        while (running) {
            try {
                val client = serverSocket?.accept() ?: break
                Thread { handleClient(client) }.start()
            } catch (_: Exception) {
                if (!running) break
            }
        }
    }

    private fun handleClient(socket: Socket) {
        val ip = socket.inetAddress.hostAddress ?: return
        try {
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            if (authManager.isLocked(ip)) {
                val resp = ControlMessage(StreamProtocol.MSG_AUTH_RESPONSE, byteArrayOf(StreamProtocol.AUTH_LOCKED))
                sendMsg(output, resp)
                socket.close()
                return
            }

            val challenge = authManager.generateChallenge()
            challenges[ip] = challenge
            sendMsg(output, ControlMessage(StreamProtocol.MSG_AUTH_REQUEST, challenge))

            val authReply = readMsg(input) ?: return
            if (authReply.type != StreamProtocol.MSG_AUTH_RESPONSE) return

            if (!authManager.verify(challenge, authReply.payload, ip)) {
                val locked = authManager.isLocked(ip)
                val code = if (locked) StreamProtocol.AUTH_LOCKED else StreamProtocol.AUTH_FAIL
                sendMsg(output, ControlMessage(StreamProtocol.MSG_AUTH_RESPONSE, byteArrayOf(code)))
                socket.close()
                return
            }

            sendMsg(output, ControlMessage(StreamProtocol.MSG_AUTH_RESPONSE, byteArrayOf(StreamProtocol.AUTH_OK)))

            while (running && !socket.isClosed) {
                val msg = readMsg(input) ?: break
                when (msg.type) {
                    StreamProtocol.MSG_START_STREAM -> {
                        val config = StreamConfig.decode(msg.payload) ?: StreamConfig()
                        onStreamRequested(config, socket)
                    }
                    StreamProtocol.MSG_STOP_STREAM -> break
                    StreamProtocol.MSG_HEARTBEAT -> sendMsg(output, ControlMessage(StreamProtocol.MSG_HEARTBEAT))
                }
            }
        } catch (_: Exception) {
        } finally {
            socket.close()
        }
    }

    private fun sendMsg(output: DataOutputStream, msg: ControlMessage) {
        val data = msg.encode()
        output.writeInt(data.size)
        output.write(data)
        output.flush()
    }

    private fun readMsg(input: DataInputStream): ControlMessage? {
        return try {
            val len = input.readInt()
            if (len > 65536 || len < 0) return null
            val data = ByteArray(len)
            input.readFully(data)
            ControlMessage.decode(data)
        } catch (_: Exception) { null }
    }
}

package com.jizhi.stream.core.protocol

object StreamProtocol {
    const val CONTROL_PORT = 23333
    const val STREAM_PORT = 23334
    const val DISCOVERY_PORT = 23335

    const val MAGIC = 0x4A5A_5354L // "JZST"
    const val VERSION: Byte = 1

    const val MSG_DISCOVER = 0x01.toByte()
    const val MSG_DISCOVER_REPLY = 0x02.toByte()
    const val MSG_AUTH_REQUEST = 0x10.toByte()
    const val MSG_AUTH_RESPONSE = 0x11.toByte()
    const val MSG_START_STREAM = 0x20.toByte()
    const val MSG_STOP_STREAM = 0x21.toByte()
    const val MSG_CONFIG = 0x30.toByte()
    const val MSG_HEARTBEAT = 0x40.toByte()
    const val MSG_INPUT_EVENT = 0x50.toByte()

    const val AUTH_OK: Byte = 0
    const val AUTH_FAIL: Byte = 1
    const val AUTH_LOCKED: Byte = 2

    const val MAX_UDP_PACKET = 65000
    const val HEADER_SIZE = 16
    const val MAX_PAYLOAD = MAX_UDP_PACKET - HEADER_SIZE
}

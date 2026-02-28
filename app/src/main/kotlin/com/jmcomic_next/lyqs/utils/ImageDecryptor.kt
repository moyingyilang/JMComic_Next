package com.jmcomic_next.lyqs.utils

import java.nio.ByteBuffer

object ImageDecryptor {
    fun decrypt(buffer: ByteArray, key: String): ByteArray {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val keyLength = keyBytes.size
        for (i in buffer.indices) {
            buffer[i] = (buffer[i].toInt() xor keyBytes[i % keyLength].toInt()).toByte()
        }
        return buffer
    }

    fun decrypt(buffer: ByteBuffer, key: String): ByteBuffer {
        val array = buffer.array()
        decrypt(array, key)
        return ByteBuffer.wrap(array)
    }
}

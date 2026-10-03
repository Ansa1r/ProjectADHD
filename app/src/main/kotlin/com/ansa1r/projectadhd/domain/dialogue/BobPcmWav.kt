package com.ansa1r.projectadhd.domain.dialogue

import java.nio.ByteBuffer
import java.nio.ByteOrder

object BobPcmWav {
    fun decode(bytes: ByteArray): BobPcmSample {
        require(bytes.size >= 12)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun tag(at: Int) = String(bytes, at, 4, Charsets.US_ASCII)
        require(tag(0) == "RIFF" && tag(8) == "WAVE")
        var sampleRate = 0
        var dataAt = -1
        var dataSize = 0
        var cursor = 12
        while (cursor <= bytes.size - 8) {
            val length = buffer.getInt(cursor + 4)
            require(length >= 0 && length <= bytes.size - cursor - 8)
            val body = cursor + 8
            when (tag(cursor)) {
                "fmt " -> {
                    require(length >= 16 && buffer.getShort(body).toInt() == 1)
                    require(buffer.getShort(body + 2).toInt() == 1 && buffer.getShort(body + 14).toInt() == 16)
                    require(buffer.getShort(body + 12).toInt() == 2)
                    sampleRate = buffer.getInt(body + 4)
                    require(sampleRate > 0)
                }
                "data" -> { dataAt = body; dataSize = length }
            }
            cursor = body + length + (length and 1)
        }
        require(sampleRate > 0 && dataAt >= 0 && dataSize > 0 && dataSize % 2 == 0)
        return BobPcmSample(sampleRate, ShortArray(dataSize / 2) { buffer.getShort(dataAt + it * 2) })
    }
}

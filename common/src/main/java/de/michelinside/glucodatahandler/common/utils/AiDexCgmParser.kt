package de.michelinside.glucodatahandler.common.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AiDexCgmParser {

    data class AiDexRecord(
        val timestamp: Long,
        val glucose: Float,
        val battery: Int,
        val serialNumber: String?
    )

    fun parse(rawData: ByteArray, serialNumber: String?): AiDexRecord? {
        try {
            if (rawData.size < 11) return null

            val buffer = ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN)

            // Header
            val battery = buffer.get(0).toUByte().toInt()
            
            // Timestamp calculation
            // Formula: 0x386CD300L + (rawData[1] & 0xFF) * 10L + (uint32 from rawData[2..5])
            val byte1 = rawData[1].toUByte().toLong()
            val time32 = buffer.getInt(2).toUInt().toLong()
            val baseTime = 0x386CD300L
            val timestampSec = baseTime + (byte1 * 10L) + time32
            val timestampMs = timestampSec * 1000L

            // State & Glucose Logic
            val byte9 = rawData[9].toInt()
            val state = when {
                (byte9 and (1 shl 5)) != 0 -> 2
                (byte9 and (1 shl 6)) != 0 -> 1
                (byte9 and (1 shl 7)) != 0 -> 3
                else -> 0
            }

            // Event Index (masked byte 9)
            val eventIndex = byte9 and 0x1F

            // Event Data (byte 10)
            val eventDataByte = rawData[10]

            // Valid Glucose Check
            // 1. Event Index < 0x1F
            // 2. Bitmask check
            // 3. State is 0 or 3
            
            if (eventIndex < 0x1F) {
                val mask = 0x40019D80
                // 1 << eventIndex
                if (((1 shl eventIndex) and mask) != 0) {
                    if (state == 0 || state == 3) {
                        var glucose: Float
                        if (eventIndex == 4) {
                            glucose = eventDataByte.toFloat()
                        } else {
                            glucose = (eventDataByte.toUByte().toInt()).toFloat() / 10.0f
                        }

                        if (glucose > 0) {
                            return AiDexRecord(timestampMs, glucose, battery, serialNumber)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GDH.AiDexParser", "Error parsing AiDex data: " + e.message)
        }
        return null
    }
}
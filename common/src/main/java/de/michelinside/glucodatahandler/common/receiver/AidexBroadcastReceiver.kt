package de.michelinside.glucodatahandler.common.receiver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.microtechmd.blecomm.entity.BleMessage
import de.michelinside.glucodatahandler.common.ReceiveData
import de.michelinside.glucodatahandler.common.notifier.DataSource
import de.michelinside.glucodatahandler.common.utils.AiDexCgmParser
import de.michelinside.glucodatahandler.common.utils.GlucoDataUtils

class AidexBroadcastReceiver : NamedBroadcastReceiver() {
    private val LOG_ID = "GDH.AidexBroadcastReceiver"
    override fun getName(): String {
        return "AidexBroadcastReceiver"
    }

    override fun onReceiveData(context: Context, intent: Intent) {
        Log.d(LOG_ID, "onReceiveData called for action ${intent.action}")
        try {
            if (intent.action == "com.microtechmd.cgms.NOTIFICATION") {
                val message = intent.getSerializableExtra("message")
                if (message is BleMessage) {
                    if (message.operation == 1 && message.isSuccess) {
                        val rawData = message.data
                        val serialNumber = intent.getStringExtra("sn")
                        val record = AiDexCgmParser.parse(rawData, serialNumber)
                        if (record != null) {
                            Log.d(LOG_ID, "Parsed record: $record")
                            val extras = Bundle()
                            extras.putLong(ReceiveData.TIME, record.timestamp)
                            extras.putFloat(ReceiveData.GLUCOSECUSTOM, record.glucose)
                            // Convert the native mmol/L back into mg/dL for GDH
                            extras.putInt(ReceiveData.MGDL, GlucoDataUtils.mmolToMg(record.glucose).toInt())
                            extras.putString(ReceiveData.SERIAL, record.serialNumber)
                            // Battery is not directly supported in ReceiveData bundle, but we have it.
                            // extras.putInt("battery", record.battery)
                            ReceiveData.handleIntent(context, DataSource.AIDEX, extras)
                        }
                    }
                }
            }
        } catch (exc: Exception) {
            Log.e(LOG_ID, "onReceiveData exception: " + exc.message.toString() )
        }
    }
}
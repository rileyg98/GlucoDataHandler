package de.michelinside.glucodatahandler.transfer

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import de.michelinside.glucodatahandler.common.ReceiveData
import de.michelinside.glucodatahandler.common.utils.Log
import de.michelinside.glucodatahandler.common.utils.Utils
import java.math.RoundingMode

abstract class TransferTask {
    abstract val LOG_ID: String
    abstract val enablePref: String
    abstract val intervalPref: String
    open val defaultInterval = 5
    var enabled = false
        protected set
    var interval = 0
        protected set
    protected var lastExecution = 0L

    abstract fun execute(context: Context): Boolean

    fun checkExecution(context: Context) {
        try {
            Log.d(LOG_ID, "checkExecution called - enable: $enabled, interval: $interval, lastExecution: ${Utils.getUiTimeStamp(lastExecution)} - elapsed: ${Utils.getElapsedTimeMinute(lastExecution, RoundingMode.HALF_UP)}")
            if(enabled && lastExecution < ReceiveData.time && Utils.getElapsedTimeMinute(lastExecution, RoundingMode.HALF_UP) >= interval) {
                Log.i(LOG_ID, "Executing task")
                if(execute(context))
                    lastExecution = ReceiveData.time
            }
        } catch (exc: Exception) {
            Log.e(LOG_ID, "checkExecution exception: " + exc.message.toString() )
        }
    }

    open fun checkPreferenceChanged(sharedPreferences: SharedPreferences, key: String?): Boolean {
        Log.d(LOG_ID, "checkPreferenceChanged called for key $key")
        if (key == null) {
            if(enablePref.isNotEmpty())
                enabled = sharedPreferences.getBoolean(enablePref, false)
            if(intervalPref.isNotEmpty())
                interval = sharedPreferences.getInt(intervalPref, defaultInterval)
            else
                interval = defaultInterval
            Log.i(LOG_ID, "checkPreferenceChanged: enable: $enabled, interval: $interval")
            return enabled
        }
        if (key == enablePref) {
            enabled = sharedPreferences.getBoolean(enablePref, false)
            Log.i(LOG_ID, "checkPreferenceChanged: enable: $enabled")
            return enabled
        }
        if (key == intervalPref) {
            interval = sharedPreferences.getInt(intervalPref, defaultInterval)
            Log.i(LOG_ID, "checkPreferenceChanged: interval: $interval")
        }
        return false
    }

}
package de.michelinside.glucodatahandler.preferences

import android.content.SharedPreferences
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import de.michelinside.glucodatahandler.R
import de.michelinside.glucodatahandler.common.Constants
import de.michelinside.glucodatahandler.common.Intents
import de.michelinside.glucodatahandler.common.utils.Log
import de.michelinside.glucodatahandler.healthconnect.HealthConnectManager
import kotlinx.coroutines.launch

class TransferSettingsFragment: SettingsFragmentBase(R.xml.pref_transfer) {
    override fun onResume() {
        Log.d(LOG_ID, "onResume called")
        try {
            super.onResume()
            updateEnableStates()
        } catch (exc: Exception) {
            Log.e(LOG_ID, "onResume exception: " + exc.toString())
        }
    }

    private fun updateEnableStates() {
        try {
            if(preferenceManager.sharedPreferences == null)
                return
            val prefHealthConnect = findPreference<Preference>("transfer_healthconnect")
            if(prefHealthConnect != null ) {
                val enabled = preferenceManager.sharedPreferences!!.getBoolean(Constants.SHARED_PREF_SEND_TO_HEALTH_CONNECT, false)
                setEnableState(prefHealthConnect, enabled)
            }
            val prefLocalApps = findPreference<Preference>("transfer_localapps")
            if(prefLocalApps != null ) {
                val xdripEnabled = preferenceManager.sharedPreferences!!.getBoolean(Constants.SHARED_PREF_SEND_XDRIP_BROADCAST, false)
                val toXdripEnabled = preferenceManager.sharedPreferences!!.getBoolean(Constants.SHARED_PREF_SEND_TO_XDRIP, false)
                val toGlucodataEnabled = preferenceManager.sharedPreferences!!.getBoolean(Constants.SHARED_PREF_SEND_TO_GLUCODATA_AOD, false)
                setEnableState(prefLocalApps, xdripEnabled || toXdripEnabled || toGlucodataEnabled)
            }
        } catch (exc: Exception) {
            Log.e(LOG_ID, "updateEnableStates exception: " + exc.toString())
        }
    }

    private fun setEnableState(pref: Preference, enable: Boolean) {
        Log.d(LOG_ID, "setEnableState called for ${pref.key}: $enable")
        if(enable) {
            pref.icon = ContextCompat.getDrawable(requireContext(), de.michelinside.glucodatahandler.common.R.drawable.switch_on)
        } else {
            pref.icon = ContextCompat.getDrawable(requireContext(), de.michelinside.glucodatahandler.common.R.drawable.switch_off)
        }
    }

}

class TransferHealthConnectFragment: SettingsFragmentBase(R.xml.pref_transfer_healthconnect) {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Set<String>>

    override fun initPreferences() {
        Log.v(LOG_ID, "initPreferences called")
        super.initPreferences()
        setupHealthConnect()
    }

    private fun setupHealthConnect() {
        val pref = findPreference<SwitchPreferenceCompat>(Constants.SHARED_PREF_SEND_TO_HEALTH_CONNECT)
        if (Build.VERSION.SDK_INT < 28 && !HealthConnectManager.isHealthConnectAvailable(requireContext().applicationContext)) {
            pref?.isVisible = false
        } else {
            requestPermissionLauncher = registerForActivityResult(HealthConnectManager.getPermissionRequestContract()) { grantedPermissions ->
                if (grantedPermissions.containsAll(HealthConnectManager.WRITE_GLUCOSE_PERMISSIONS)) {
                    Log.i(LOG_ID, "Health Connect permissions granted by user.")
                    // Berechtigungen erteilt, UI aktualisieren oder weitere Aktionen ausführen
                } else {
                    Log.w(LOG_ID, "Health Connect permissions were not fully granted.")
                    // Berechtigungen nicht (vollständig) erteilt
                    pref?.isChecked = false
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if(key == Constants.SHARED_PREF_SEND_TO_HEALTH_CONNECT) {
            if(sharedPreferences!!.getBoolean(Constants.SHARED_PREF_SEND_TO_HEALTH_CONNECT, false)) {
                lifecycleScope.launch { // Changed from GlobalScope
                    val isReady = HealthConnectManager.checkAndEnsureRequirements(requireContext().applicationContext, requestPermissionLauncher)
                    if (isReady) {
                        // Health Connect ist sofort bereit
                        Log.d(LOG_ID, "Health Connect ready to use.")
                    } else {
                        // Maßnahmen wurden eingeleitet (Play Store / Berechtigungsdialog)
                        // Die Activity wartet auf das Ergebnis des Launchers oder auf Nutzerinteraktion
                        Log.d(LOG_ID, "Health Connect requirements not met, actions initiated.")

                    }
                }
            }
        } else
            super.onSharedPreferenceChanged(sharedPreferences, key)
    }
}

class TransferBroadcastFragement: SettingsFragmentBase(R.xml.pref_transfer_localapps) {
    override fun initPreferences() {
        Log.v(LOG_ID, "initPreferences called")
        super.initPreferences()
        setupReceivers(Constants.GLUCODATA_BROADCAST_ACTION, Constants.SHARED_PREF_GLUCODATA_RECEIVERS)
        setupReceivers(Constants.XDRIP_ACTION_GLUCOSE_READING, Constants.SHARED_PREF_XDRIP_RECEIVERS)
        setupReceivers(Intents.XDRIP_BROADCAST_ACTION, Constants.SHARED_PREF_XDRIP_BROADCAST_RECEIVERS)
    }
}
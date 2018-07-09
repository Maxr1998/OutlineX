@file:Suppress("ConstantConditionIf")

package com.schnettler.outlinecolors

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.schnettler.outlinecolors.AdvancedConstants.ENFORCE_MINIMUM_SUBSTRATUM_VERSION
import com.schnettler.outlinecolors.AdvancedConstants.MINIMUM_SUBSTRATUM_VERSION
import com.schnettler.outlinecolors.AdvancedConstants.ORGANIZATION_THEME_SYSTEMS
import com.schnettler.outlinecolors.AdvancedConstants.OTHER_THEME_SYSTEMS
import com.schnettler.outlinecolors.AdvancedConstants.SHOW_DIALOG_REPEATEDLY
import com.schnettler.outlinecolors.AdvancedConstants.SHOW_LAUNCH_DIALOG
import com.schnettler.outlinecolors.AdvancedConstants.SUBSTRATUM_FILTER_CHECK
import com.schnettler.outlinecolors.ThemeFunctions.SUBSTRATUM_PACKAGE_NAME
import com.schnettler.outlinecolors.ThemeFunctions.checkSubstratumIntegrity
import com.schnettler.outlinecolors.ThemeFunctions.getSelfSignature
import com.schnettler.outlinecolors.ThemeFunctions.getSelfVerifiedIntentResponse
import com.schnettler.outlinecolors.ThemeFunctions.getSelfVerifiedPirateTools
import com.schnettler.outlinecolors.ThemeFunctions.getSelfVerifiedThemeEngines
import com.schnettler.outlinecolors.ThemeFunctions.getSubstratumFromPlayStore
import com.schnettler.outlinecolors.ThemeFunctions.getSubstratumUpdatedResponse
import com.schnettler.outlinecolors.ThemeFunctions.hasOtherThemeSystem
import com.schnettler.outlinecolors.ThemeFunctions.isCallingPackageAllowed
import com.schnettler.outlinecolors.ThemeFunctions.isPackageInstalled

/**
 * NOTE TO THEMERS
 *
 * This class is a TEMPLATE of how you should be launching themes. As long as you keep the structure
 * of launching themes the same, you can avoid easy script crackers by changing how
 * functions/methods are coded, as well as boolean variable placement.
 *
 * The more you play with this the harder it would be to decompile and crack!
 */

class SubstratumLauncher : Activity() {

    private var substratumIntentData = "projekt.substratum.THEME"
    private var getKeysIntent = "projekt.substratum.GET_KEYS"
    private var receiveKeysIntent = "projekt.substratum.RECEIVE_KEYS"
    private var tag = "SubstratumThemeReport"

    private fun calibrateSystem(certified: Boolean) {
        quitSelf(certified)
    }

    private fun quitSelf(certified: Boolean): Boolean {
        if (!hasOtherThemeSystem(this)) {
            if (!isPackageInstalled(applicationContext, SUBSTRATUM_PACKAGE_NAME)) {
                getSubstratumFromPlayStore(this)
                return false
            }
            if (ENFORCE_MINIMUM_SUBSTRATUM_VERSION
                    && !getSubstratumUpdatedResponse(applicationContext)) {
                val parse = String.format(
                        getString(R.string.outdated_substratum),
                        getString(R.string.ThemeName),
                        MINIMUM_SUBSTRATUM_VERSION.toString())
                Toast.makeText(this, parse, Toast.LENGTH_SHORT).show()
                return false
            }
        } else if (!BuildConfig.SUPPORTS_THIRD_PARTY_SYSTEMS) {
            Toast.makeText(this, R.string.unauthorized_theme_client, Toast.LENGTH_LONG).show()
            finish()
            return false
        }

        var returnIntent = Intent()
        if (intent.action == getKeysIntent) {
            returnIntent = Intent(receiveKeysIntent)
        }

        val themeName = getString(R.string.ThemeName)
        val themeAuthor = getString(R.string.ThemeAuthor)
        val themePid = packageName
        returnIntent.putExtra("theme_name", themeName)
        returnIntent.putExtra("theme_author", themeAuthor)
        returnIntent.putExtra("theme_pid", themePid)

        val themeLaunchType = getSelfVerifiedThemeEngines(applicationContext)
        val themeHash = getSelfSignature(applicationContext)
        var themePiracyCheck = false
        if (BuildConfig.ENABLE_APP_BLACKLIST_CHECK)
            themePiracyCheck = getSelfVerifiedPirateTools(applicationContext)
        if (themePiracyCheck or (SUBSTRATUM_FILTER_CHECK && !certified)) {
            Toast.makeText(this, R.string.unauthorized, Toast.LENGTH_LONG).show()
            finish()
            return false
        }
        returnIntent.putExtra("theme_hash", themeHash)
        returnIntent.putExtra("theme_launch_type", themeLaunchType)
        returnIntent.putExtra("theme_debug", BuildConfig.DEBUG)
        returnIntent.putExtra("theme_piracy_check", themePiracyCheck)
        returnIntent.putExtra("encryption_key", BuildConfig.DECRYPTION_KEY)
        returnIntent.putExtra("iv_encrypt_key", BuildConfig.IV_KEY)

        val callingPackage = intent.getStringExtra("calling_package_name")
        if (callingPackage == null) {
            val parse = String.format(
                    getString(R.string.outdated_substratum),
                    getString(R.string.ThemeName),
                    915)
            Toast.makeText(this, parse, Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        if (!isCallingPackageAllowed(callingPackage)) {
            return false
        } else {
            returnIntent.`package` = callingPackage
        }

        if (intent.action == substratumIntentData) {
            setResult(getSelfVerifiedIntentResponse(applicationContext)!!, returnIntent)
        } else if (intent.action == getKeysIntent) {
            returnIntent.action = receiveKeysIntent
            sendBroadcast(returnIntent)
        }
        finish()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reject all other apps trying to hijack the theme first
        val caller = callingActivity?.packageName
        var callerVerified = false

        val themeSystems: MutableList<String> = mutableListOf()
        themeSystems.addAll(ORGANIZATION_THEME_SYSTEMS)
        themeSystems.addAll(OTHER_THEME_SYSTEMS)
        if (caller != null) {
            themeSystems
                    .filter { caller.startsWith(prefix = it, ignoreCase = true) }
                    .forEach { callerVerified = true }
        }
        if (!callerVerified) {
            Log.e(tag, "This theme does not support the launching theme system. [HIJACK] ($caller)")
            val hijackString =
                    String.format(getString(R.string.unauthorized_theme_client_hijack), caller)
            Toast.makeText(this, hijackString, Toast.LENGTH_LONG).show()
            finish()
            return
        } else {
            Log.d(tag, "'$caller' has been authorized to launch this theme. (Phase 1)")
        }

        // We will ensure that our support is added where it belongs
        val intent = intent
        val action = intent.action
        var verified = false
        val certified = intent.getBooleanExtra("certified", false)

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        if ((action == substratumIntentData) or (action == getKeysIntent)) {
            verified = when {
                BuildConfig.ALLOW_THIRD_PARTY_SUBSTRATUM_BUILDS -> true
                else -> checkSubstratumIntegrity(this)
            }
        } else {
            OTHER_THEME_SYSTEMS
                    .filter { action.startsWith(prefix = it, ignoreCase = true) }
                    .forEach { verified = true }
        }
        if (!verified) {
            Log.e(tag, "This theme does not support the launching theme system. ($action)")
            Toast.makeText(this, R.string.unauthorized_theme_client, Toast.LENGTH_LONG).show()
            finish()
            return
        } else {
            Log.d(tag, "'$action' has been authorized to launch this theme. (Phase 2)")
        }

        if (SHOW_LAUNCH_DIALOG) run {
            if (SHOW_DIALOG_REPEATEDLY) {
                showDialog(certified)
                sharedPref.edit().remove("dialog_showed").apply()
            } else if (!sharedPref.getBoolean("dialog_showed", false)) {
                showDialog(certified)
                sharedPref.edit().putBoolean("dialog_showed", true).apply()
            } else {
                if (BuildConfig.ENFORCE_INTERNET_CHECK) {
                    if (sharedPref.getInt("last_version", 0) == BuildConfig.VERSION_CODE) {
                        calibrateSystem(certified)
                    } else {
                        checkConnection(certified)
                    }
                } else {
                    calibrateSystem(certified)
                }
            }
        } else if (BuildConfig.ENFORCE_INTERNET_CHECK) {
            if (sharedPref.getInt("last_version", 0) == BuildConfig.VERSION_CODE) {
                calibrateSystem(certified)
            } else {
                checkConnection(certified)
            }
        } else {
            calibrateSystem(certified)
        }
    }

    private fun checkConnection(certified: Boolean) {
        val editor = getPreferences(Context.MODE_PRIVATE).edit()
        editor.putInt("last_version", BuildConfig.VERSION_CODE).apply()
        calibrateSystem(certified)
    }

    private fun showDialog(certified: Boolean) {
        val dialog = AlertDialog.Builder(this, R.style.DialogStyle)
                .setCancelable(false)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.launch_dialog_title)
                .setMessage(R.string.launch_dialog_content)
                .setPositiveButton(R.string.launch_dialog_positive) { _, _ ->
                    val sharedPref = getPreferences(Context.MODE_PRIVATE)
                    if (BuildConfig.ENFORCE_INTERNET_CHECK) {
                        if (sharedPref.getInt("last_version", 0) == BuildConfig.VERSION_CODE) {
                            calibrateSystem(certified)
                        } else {
                            checkConnection(certified)
                        }
                    } else {
                        calibrateSystem(certified)
                    }
                }
        if (getString(R.string.launch_dialog_negative).isNotEmpty()) {
            if (getString(R.string.launch_dialog_negative_url).isNotEmpty()) {
                dialog.setNegativeButton(R.string.launch_dialog_negative) { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.launch_dialog_negative_url))))
                    finish()
                }
            } else {
                dialog.setNegativeButton(R.string.launch_dialog_negative) { _, _ -> finish() }
            }
        }
        dialog.show()
    }
}
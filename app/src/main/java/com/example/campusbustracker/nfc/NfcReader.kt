package com.example.campusbustracker.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA

object NfcReader {
    val lastScannedValue = androidx.compose.runtime.mutableStateOf<String?>(null)
    fun getAdapter(activity: Activity): NfcAdapter? =
        NfcAdapter.getDefaultAdapter(activity)

    // Call this in onResume() of the activity to start listening for taps
    fun enableForegroundDispatch(activity: Activity) {
        val adapter = getAdapter(activity) ?: return
        val intent = Intent(activity, activity.javaClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, intent, PendingIntent.FLAG_MUTABLE
        )
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    // Call this in onPause() of the activity
    fun disableForegroundDispatch(activity: Activity) {
        getAdapter(activity)?.disableForegroundDispatch(activity)
    }

    // Call this from onNewIntent() when a tag is tapped, returns the text stored on the tag (or null)
    fun readTagValue(intent: Intent): String? {
        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return null

        // Try reading as NDEF (most common tag format, e.g. NTAG213/215/216 stickers)
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
                val record = message?.records?.firstOrNull()
                val payload = record?.payload
                if (payload != null && payload.isNotEmpty()) {
                    // Skip the language-code header bytes for a standard text record
                    val languageCodeLength = payload[0].toInt() and 0x3F
                    String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charsets.UTF_8)
                } else null
            } catch (e: Exception) {
                null
            } finally {
                try { ndef.close() } catch (e: Exception) {}
            }
        }

        // Fallback: use the tag's unique hardware ID if it's not NDEF-formatted
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            return tag.id.joinToString("") { "%02X".format(it) }
        }

        return null
    }
}
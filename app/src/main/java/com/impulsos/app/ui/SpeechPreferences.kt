package com.impulsos.app.ui

import android.content.Context

data class SpeechPreferences(
    val automatic: Boolean = false,
    val voiceName: String = "",
    val languageTag: String = "es-MX",
    val rate: Float = 1f,
    val pitch: Float = 1f,
    val instructionsFirst: Boolean = true,
    val readName: Boolean = true,
    val readConditions: Boolean = true,
    val readAllergies: Boolean = true,
    val readMedications: Boolean = true,
    val readContacts: Boolean = true,
    val readAvoid: Boolean = true,
    val readAdditionalInfo: Boolean = true,
)

object SpeechPreferenceStore {
    private const val FILE = "emergency-speech"

    fun load(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).let { prefs ->
        SpeechPreferences(
            automatic = prefs.getBoolean("automatic", false),
            voiceName = prefs.getString("voice", "").orEmpty(),
            languageTag = prefs.getString("language", "es-MX").orEmpty(),
            rate = prefs.getFloat("rate", 1f),
            pitch = prefs.getFloat("pitch", 1f),
            instructionsFirst = prefs.getBoolean("instructionsFirst", true),
            readName = prefs.getBoolean("readName", true),
            readConditions = prefs.getBoolean("readConditions", true),
            readAllergies = prefs.getBoolean("readAllergies", true),
            readMedications = prefs.getBoolean("readMedications", true),
            readContacts = prefs.getBoolean("readContacts", true),
            readAvoid = prefs.getBoolean("readAvoid", true),
            readAdditionalInfo = prefs.getBoolean("readAdditionalInfo", true),
        )
    }

    fun save(context: Context, value: SpeechPreferences) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("automatic", value.automatic)
            .putString("voice", value.voiceName)
            .putString("language", value.languageTag)
            .putFloat("rate", value.rate.coerceIn(0.65f, 1.5f))
            .putFloat("pitch", value.pitch.coerceIn(0.7f, 1.4f))
            .putBoolean("instructionsFirst", value.instructionsFirst)
            .putBoolean("readName", value.readName)
            .putBoolean("readConditions", value.readConditions)
            .putBoolean("readAllergies", value.readAllergies)
            .putBoolean("readMedications", value.readMedications)
            .putBoolean("readContacts", value.readContacts)
            .putBoolean("readAvoid", value.readAvoid)
            .putBoolean("readAdditionalInfo", value.readAdditionalInfo)
            .apply()
    }
}

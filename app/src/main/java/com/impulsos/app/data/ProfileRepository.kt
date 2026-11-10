package com.impulsos.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores the profile as AES/GCM ciphertext in the app's private, non-backed-up files directory. */
object ProfileRepository {
    private const val KEY_ALIAS = "impulsos-medical-profile-v1"
    private const val FILE_NAME = "medical-profile.bin"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun load(context: Context): MedicalProfile {
        val file = File(context.noBackupFilesDir, FILE_NAME)
        if (!file.exists()) return MedicalProfile()
        val payload = Base64.decode(file.readText(Charsets.UTF_8), Base64.NO_WRAP)
        require(payload.size > 12) { "El archivo del perfil no es válido." }
        val iv = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8)).toProfile()
    }

    fun save(context: Context, profile: MedicalProfile) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(profile.toJson().toString().toByteArray(Charsets.UTF_8))
        val encrypted = ByteArray(iv.size + ciphertext.size)
        iv.copyInto(encrypted)
        ciphertext.copyInto(encrypted, destinationOffset = iv.size)
        val destination = File(context.noBackupFilesDir, FILE_NAME)
        val temporary = File(context.noBackupFilesDir, "$FILE_NAME.tmp")
        temporary.writeText(Base64.encodeToString(encrypted, Base64.NO_WRAP), Charsets.UTF_8)
        if (!temporary.renameTo(destination)) {
            temporary.copyTo(destination, overwrite = true)
            temporary.delete()
        }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun MedicalProfile.toJson() = JSONObject().apply {
        put("name", name)
        put("conditions", conditions)
        put("allergies", allergies)
        put("medications", medications)
        put("rescueMedication", rescueMedication)
        put("avoid", avoid)
        put("instructions", instructions)
        put("trustedContact", trustedContact)
        put("additionalInfo", additionalInfo)
        put("emergencyPhotoSet", emergencyPhotoSet)
        put("contacts", org.json.JSONArray().apply {
            contacts.forEach { contact ->
                put(JSONObject().apply {
                    put("name", contact.name)
                    put("phone", contact.phone)
                    put("relation", contact.relation)
                    put("additionalInfo", contact.additionalInfo)
                })
            }
        })
    }

    private fun JSONObject.toProfile(): MedicalProfile {
        val parsedContacts = buildList {
            val array = optJSONArray("contacts") ?: return@buildList
            for (index in 0 until array.length()) {
                val contact = array.optJSONObject(index) ?: continue
                add(EmergencyContact(
                    name = contact.optString("name"),
                    phone = contact.optString("phone"),
                    relation = contact.optString("relation"),
                    additionalInfo = contact.optString("additionalInfo"),
                ))
            }
        }
        return MedicalProfile(
        name = optString("name"),
        conditions = optString("conditions"),
        allergies = optString("allergies"),
        medications = optString("medications"),
        rescueMedication = optString("rescueMedication"),
        avoid = optString("avoid"),
        instructions = optString("instructions"),
        trustedContact = optString("trustedContact"),
        additionalInfo = optString("additionalInfo"),
        emergencyPhotoSet = optBoolean("emergencyPhotoSet", false),
        contacts = parsedContacts,
        )
    }
}

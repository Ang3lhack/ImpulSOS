package com.impulsos.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.ExifInterface
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.graphics.Matrix
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.math.max

/** Encrypted, resized user image kept under noBackupFilesDir. */
object EmergencyPhotoRepository {
    private const val FILE_NAME = "emergency-photo.bin"
    private const val KEY_ALIAS = "impulsos-emergency-photo-v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val MAX_EDGE = 1280

    fun preview(context: Context, uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = max(1, max(bounds.outWidth, bounds.outHeight) / 800)
        val decoded = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null
        orient(context, uri, decoded)
    }.getOrNull()

    fun save(context: Context, uri: Uri) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "No se pudo abrir la fotografía." }
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "El archivo seleccionado no es una imagen válida." }
        val sample = max(1, max(bounds.outWidth, bounds.outHeight) / MAX_EDGE)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "No se pudo leer la fotografía." }
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("No se pudo decodificar la fotografía.")
        val oriented = orient(context, uri, decoded)
        val scaled = if (max(oriented.width, oriented.height) > MAX_EDGE) {
            val ratio = MAX_EDGE.toFloat() / max(oriented.width, oriented.height)
            Bitmap.createScaledBitmap(oriented, (oriented.width * ratio).toInt(), (oriented.height * ratio).toInt(), true)
        } else oriented
        val output = ByteArrayOutputStream()
        check(scaled.compress(Bitmap.CompressFormat.JPEG, 82, output)) { "No se pudo comprimir la fotografía." }
        if (scaled !== decoded) scaled.recycle()
        if (oriented !== decoded && oriented !== scaled) oriented.recycle()
        decoded.recycle()
        storeEncrypted(context, output.toByteArray())
    }

    fun load(context: Context): Bitmap? = runCatching {
        val payload = File(context.noBackupFilesDir, FILE_NAME).readBytes()
        require(payload.size > 12)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
        }
        val plain = cipher.doFinal(payload.copyOfRange(12, payload.size))
        BitmapFactory.decodeByteArray(plain, 0, plain.size)
    }.getOrNull()

    /** Stores a camera-preview bitmap using the same private encrypted format as gallery imports. */
    fun save(context: Context, bitmap: Bitmap) {
        val scaled = if (max(bitmap.width, bitmap.height) > MAX_EDGE) {
            val ratio = MAX_EDGE.toFloat() / max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap
        val output = ByteArrayOutputStream()
        check(scaled.compress(Bitmap.CompressFormat.JPEG, 82, output)) { "No se pudo comprimir la fotografía." }
        if (scaled !== bitmap) scaled.recycle()
        storeEncrypted(context, output.toByteArray())
    }

    fun delete(context: Context) {
        File(context.noBackupFilesDir, FILE_NAME).delete()
    }

    fun exists(context: Context) = File(context.noBackupFilesDir, FILE_NAME).isFile

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    private fun orient(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri).use { stream ->
            stream?.let { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        } ?: ExifInterface.ORIENTATION_NORMAL
        val transform = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> transform.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> transform.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> { transform.setRotate(180f); transform.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> { transform.setRotate(90f); transform.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> transform.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { transform.setRotate(-90f); transform.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> transform.setRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true)
    }

    private fun storeEncrypted(context: Context, plaintext: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.iv + cipher.doFinal(plaintext)
        val destination = File(context.noBackupFilesDir, FILE_NAME)
        val temporary = File(context.noBackupFilesDir, "$FILE_NAME.tmp")
        try {
            temporary.writeBytes(encrypted)
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }
}

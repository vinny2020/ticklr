package com.xaymaca.sit.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * On-device store for user-attached contact photos. The system Contacts
 * entry is never written to (Ticklr is read-only on the address book per
 * HANDOFF Privacy promises). Photos live under
 *   filesDir/photos/{contactId}.jpg
 *
 * Mirrors iOS PhotoStore (commit 4 there): JPEG, max 512px edge, q=0.85,
 * center-cropped to square. Files are inside filesDir so they're already
 * excluded from any backup the app doesn't opt into.
 */
object LocalPhotoStore {

    private const val MAX_EDGE = 512
    private const val JPEG_QUALITY = 85
    private const val FOLDER = "photos"

    fun exists(context: Context, contactId: Long): Boolean =
        fileFor(context, contactId).exists()

    suspend fun read(context: Context, contactId: Long): Bitmap? = withContext(Dispatchers.IO) {
        val file = fileFor(context, contactId)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    suspend fun write(context: Context, contactId: Long, sourceUri: Uri) {
        withContext(Dispatchers.IO) {
            val raw = context.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: throw IllegalStateException("Could not decode image at $sourceUri")
            val square = cropSquare(raw)
            val scaled = scaleDown(square, MAX_EDGE)
            val target = ensureFile(context, contactId)
            FileOutputStream(target).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
        }
    }

    fun delete(context: Context, contactId: Long) {
        fileFor(context, contactId).takeIf { it.exists() }?.delete()
    }

    // MARK: - Internals

    private fun folder(context: Context): File {
        val dir = File(context.filesDir, FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun fileFor(context: Context, contactId: Long): File =
        File(folder(context), "$contactId.jpg")

    private fun ensureFile(context: Context, contactId: Long): File {
        folder(context)
        return fileFor(context, contactId)
    }

    private fun cropSquare(bmp: Bitmap): Bitmap {
        val edge = min(bmp.width, bmp.height)
        val x = (bmp.width - edge) / 2
        val y = (bmp.height - edge) / 2
        return Bitmap.createBitmap(bmp, x, y, edge, edge)
    }

    private fun scaleDown(bmp: Bitmap, maxEdge: Int): Bitmap {
        val edge = max(bmp.width, bmp.height)
        if (edge <= maxEdge) return bmp
        val scale = maxEdge.toFloat() / edge
        val m = Matrix().apply { setScale(scale, scale) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}

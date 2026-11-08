package com.dhikr.app.audio

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Keeps user-chosen recordings inside the app (D-031): the picked file is **copied** into
 * filesDir/audio, so it keeps working offline and after the original is moved or deleted, with
 * no long-lived URI permissions. The database stores only the relative ref "audio/<uuid>.<ext>".
 */
class AudioStore(private val filesDir: File, private val resolver: ContentResolver) {

    sealed interface ImportResult {
        data class Ok(val ref: String) : ImportResult
        data object NotAudio : ImportResult
        data object TooLarge : ImportResult
        data object Failed : ImportResult
    }

    private val dir: File get() = File(filesDir, DIR).apply { mkdirs() }

    fun resolve(ref: String): File {
        require(ref.startsWith("$DIR/") && !ref.contains("..")) { "bad audio ref" }
        return File(filesDir, ref)
    }

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val mime = resolver.getType(uri)
        if (mime != null && !mime.startsWith("audio/")) return@withContext ImportResult.NotAudio
        val size = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
            }
        }.getOrNull()
        if (size != null && size > MAX_BYTES) return@withContext ImportResult.TooLarge

        val ext = extensionFor(mime, uri)
        val name = "${UUID.randomUUID()}.$ext"
        val target = File(dir, name)
        try {
            var copied = 0L
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        copied += n
                        if (copied > MAX_BYTES) {
                            out.close(); target.delete()
                            return@withContext ImportResult.TooLarge
                        }
                        out.write(buf, 0, n)
                    }
                }
            } ?: return@withContext ImportResult.Failed
            ImportResult.Ok("$DIR/$name")
        } catch (e: Exception) {
            target.delete()
            ImportResult.Failed
        }
    }

    suspend fun delete(ref: String) = withContext(Dispatchers.IO) { runCatching { resolve(ref).delete() } }

    private fun extensionFor(mime: String?, uri: Uri): String = when (mime) {
        "audio/mpeg" -> "mp3"
        "audio/mp4", "audio/aac", "audio/x-m4a", "audio/m4a" -> "m4a"
        "audio/ogg", "audio/opus" -> "ogg"
        "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
        "audio/flac" -> "flac"
        else -> uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.length in 2..4 } ?: "audio"
    }

    companion object {
        const val DIR = "audio"
        /** Recitations are short; 10 MB is generous and keeps backups small. */
        const val MAX_BYTES = 10L * 1024 * 1024
    }
}

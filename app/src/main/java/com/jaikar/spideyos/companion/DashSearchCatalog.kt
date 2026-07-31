package com.jaikar.spideyos.companion

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.jaikar.spideyos.apps.InstalledApp

enum class DashSearchKind {
    APPS,
    PICTURES,
    MUSIC,
    VIDEO,
    FILES,
    PEOPLE,
    WEB,
}

data class DashHit(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: DashSearchKind,
    val packageName: String? = null,
    val uri: Uri? = null,
    val thumb: Bitmap? = null,
)

/** Rover-style on-device search — scripted, no generative AI. */
object DashSearchCatalog {
    fun search(context: Context, kind: DashSearchKind, query: String, apps: List<InstalledApp>): List<DashHit> {
        val q = query.trim()
        return when (kind) {
            DashSearchKind.APPS -> apps
                .let { list -> if (q.isEmpty()) list.take(20) else list.filter { it.label.contains(q, true) } }
                .take(24)
                .map {
                    DashHit(
                        id = "app:${it.packageName}",
                        title = it.label,
                        subtitle = "App",
                        kind = DashSearchKind.APPS,
                        packageName = it.packageName,
                        thumb = it.icon,
                    )
                }
            DashSearchKind.PICTURES -> mediaHits(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, q, DashSearchKind.PICTURES, "image/")
            DashSearchKind.MUSIC -> mediaHits(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, q, DashSearchKind.MUSIC, "audio/")
            DashSearchKind.VIDEO -> mediaHits(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, q, DashSearchKind.VIDEO, "video/")
            DashSearchKind.FILES -> {
                (mediaHits(context, MediaStore.Files.getContentUri("external"), q, DashSearchKind.FILES, null) +
                    mediaHits(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, q, DashSearchKind.PICTURES, "image/") +
                    mediaHits(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, q, DashSearchKind.MUSIC, "audio/"))
                    .distinctBy { it.id }
                    .take(24)
            }
            DashSearchKind.PEOPLE -> peopleHits(context, q)
            DashSearchKind.WEB -> emptyList()
        }
    }

    fun openHit(context: Context, hit: DashHit) {
        when (hit.kind) {
            DashSearchKind.APPS -> {
                val launch = hit.packageName?.let { context.packageManager.getLaunchIntentForPackage(it) } ?: return
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
            }
            DashSearchKind.WEB -> Unit
            else -> {
                val uri = hit.uri ?: return
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeFor(hit.kind))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(intent) }
            }
        }
    }

    fun openWeb(context: Context, query: String) {
        val q = query.trim().ifBlank { return }
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", q)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            val browser = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(q)}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(browser) }
        }
    }

    private fun mimeFor(kind: DashSearchKind): String? = when (kind) {
        DashSearchKind.PICTURES -> "image/*"
        DashSearchKind.MUSIC -> "audio/*"
        DashSearchKind.VIDEO -> "video/*"
        else -> null
    }

    private fun mediaHits(
        context: Context,
        collection: Uri,
        query: String,
        kind: DashSearchKind,
        mimePrefix: String?,
    ): List<DashHit> {
        if (!hasMediaPermission(context, kind)) return emptyList()
        return runCatching {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
            )
            val sort = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            val selection = if (query.isBlank()) null else "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            val args = if (query.isBlank()) null else arrayOf("%$query%")
            context.contentResolver.query(collection, projection, selection, args, sort)?.use { c ->
                val out = mutableListOf<DashHit>()
                val idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeIdx = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                while (c.moveToNext() && out.size < 24) {
                    val mime = if (mimeIdx >= 0) c.getString(mimeIdx).orEmpty() else ""
                    if (mimePrefix != null && mime.isNotBlank() && !mime.startsWith(mimePrefix)) continue
                    val id = c.getLong(idIdx)
                    val name = c.getString(nameIdx) ?: continue
                    val uri = Uri.withAppendedPath(collection, id.toString())
                    val thumb = if (kind == DashSearchKind.PICTURES) {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)?.let { src ->
                                    val max = 96
                                    val scale = maxOf(src.width, src.height).toFloat() / max
                                    if (scale <= 1f) src
                                    else Bitmap.createScaledBitmap(
                                        src,
                                        (src.width / scale).toInt().coerceAtLeast(1),
                                        (src.height / scale).toInt().coerceAtLeast(1),
                                        true,
                                    )
                                }
                            }
                        }.getOrNull()
                    } else null
                    out += DashHit(
                        id = "$kind:$id",
                        title = name,
                        subtitle = kind.name.lowercase().replaceFirstChar { it.titlecase() },
                        kind = kind,
                        uri = uri,
                        thumb = thumb,
                    )
                }
                out
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun peopleHits(context: Context, query: String): List<DashHit> {
        val perm = android.Manifest.permission.READ_CONTACTS
        if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return runCatching {
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            )
            val selection = if (query.isBlank()) null else "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
            val args = if (query.isBlank()) null else arrayOf("%$query%")
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                args,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC",
            )?.use { c ->
                val out = mutableListOf<DashHit>()
                val idIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                while (c.moveToNext() && out.size < 24) {
                    val id = c.getLong(idIdx)
                    val name = c.getString(nameIdx) ?: continue
                    val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id.toString())
                    out += DashHit(
                        id = "people:$id",
                        title = name,
                        subtitle = "People",
                        kind = DashSearchKind.PEOPLE,
                        uri = uri,
                    )
                }
                out
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun hasMediaPermission(context: Context, kind: DashSearchKind): Boolean {
        val perms = when {
            android.os.Build.VERSION.SDK_INT >= 33 -> when (kind) {
                DashSearchKind.PICTURES -> listOf(android.Manifest.permission.READ_MEDIA_IMAGES)
                DashSearchKind.MUSIC -> listOf(android.Manifest.permission.READ_MEDIA_AUDIO)
                DashSearchKind.VIDEO -> listOf(android.Manifest.permission.READ_MEDIA_VIDEO)
                DashSearchKind.FILES -> listOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                )
                else -> emptyList()
            }
            else -> listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (perms.isEmpty()) return true
        return perms.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

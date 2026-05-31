package com.allensandiego.notepad.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import com.bugsnag.android.Bugsnag

object FileHelper {

    fun openFile(context: Context, pathOrUrl: String) {
        try {
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                // Open remote URL in browser/viewer
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pathOrUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                // Open local file
                val file = if (pathOrUrl.startsWith("/")) {
                    File(pathOrUrl)
                } else {
                    File(context.cacheDir, pathOrUrl)
                }

                if (!file.exists()) {
                    Toast.makeText(context, "Local file not found.", Toast.LENGTH_SHORT).show()
                    return
                }

                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "com.allensandiego.notepad.fileprovider",
                    file
                )

                val extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString())
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            Toast.makeText(context, "Cannot open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadFile(context: Context, url: String, suggestedFileName: String? = null) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(context, "File is local, no download needed.", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = Uri.parse(url)
            val fileName = suggestedFileName ?: uri.lastPathSegment ?: "downloaded_file"
            
            Toast.makeText(context, "Starting download of $fileName...", Toast.LENGTH_SHORT).show()

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(uri).apply {
                setTitle(fileName)
                setDescription("Downloading file from server")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getCleanFileName(pathOrUrl: String): String {
        val clean = pathOrUrl.substringAfterLast("/")
        // Check if it starts with a UUID format (8-4-4-4-12 followed by '_')
        if (clean.length > 37 &&
            clean[8] == '-' &&
            clean[13] == '-' &&
            clean[18] == '-' &&
            clean[23] == '-' &&
            clean[36] == '_'
        ) {
            return clean.substring(37)
        }
        return clean
    }
}

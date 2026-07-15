package io.mikoshift.natsu.data.local

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

class PackageFileStore(context: Context) {

    private val packagesDir: File = File(context.filesDir, "packages").apply { mkdirs() }

    fun getPackageFile(id: String): File = File(packagesDir, "$id.zip")

    suspend fun save(id: String, body: ResponseBody): String = withContext(Dispatchers.IO) {
        val target = getPackageFile(id)
        body.byteStream().use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        target.absolutePath
    }

    fun delete(id: String) {
        getPackageFile(id).delete()
    }

    fun deleteAll() {
        packagesDir.listFiles()?.forEach { it.delete() }
    }

    fun getPath(id: String): String? {
        val file = getPackageFile(id)
        return file.absolutePath.takeIf { file.exists() }
    }
}

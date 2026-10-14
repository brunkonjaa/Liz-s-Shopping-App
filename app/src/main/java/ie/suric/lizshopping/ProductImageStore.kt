package ie.suric.lizshopping

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Durable, offline image references. Import and filesystem work must be called on Dispatchers.IO. */
class ProductImageStore(private val filesDir: File) {
    companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        const val MAX_DIMENSION = 4096
        const val MAX_PIXELS = 12_000_000L
        private val keyPattern = Regex("([a-fA-F0-9-]{36})\\.(jpg|png|webp)")
    }
    private val directory = File(filesDir, "product-images")

    suspend fun importSelected(resolver: ContentResolver, uri: Uri): ProductImageReference = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val temporary = File.createTempFile("image-", ".tmp", directory)
        try {
            var total = 0
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_BYTES) { "Image must be 10 MiB or smaller." }
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            } ?: error("The selected image is no longer available.")
            require(total > 0) { "The selected image is empty." }
            val format = detectFormat(temporary)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.absolutePath, bounds)
            require(bounds.outWidth in 1..MAX_DIMENSION && bounds.outHeight in 1..MAX_DIMENSION && bounds.outWidth.toLong() * bounds.outHeight <= MAX_PIXELS) {
                "Image dimensions must be at most 4096 × 4096 and 12 megapixels."
            }
            val key = "${UUID.randomUUID()}.$format"
            val destination = safeFile(key)
            check(temporary.renameTo(destination)) { "Could not save the selected image." }
            ProductImageReference("local", key)
        } finally {
            temporary.delete()
        }
    }

    fun localFile(reference: ProductImageReference): File? {
        if (reference.kind != "local" || !keyPattern.matches(reference.key)) return null
        return safeFile(reference.key).takeIf { it.isFile && it.length() in 1..MAX_BYTES.toLong() }
    }

    /** Returns an app-owned image key when it is usable, otherwise the generic category illustration key. */
    suspend fun referenceOrCategoryFallback(product: Product): ProductImageReference = withContext(Dispatchers.IO) {
        val fallback = ProductImageReference("category", product.categoryId)
        val candidate = product.image?.let { runCatching { it.checked() }.getOrNull() }
        if (candidate == null) return@withContext fallback
        when (candidate.kind) {
            "bundled", "category" -> candidate
            "local" -> {
                val file = localFile(candidate)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                if (file != null) BitmapFactory.decodeFile(file.absolutePath, options)
                if (file != null && options.outWidth > 0 && options.outHeight > 0 && options.outWidth <= MAX_DIMENSION && options.outHeight <= MAX_DIMENSION && options.outWidth.toLong() * options.outHeight <= MAX_PIXELS) candidate
                else ProductImageReference("category", product.categoryId)
            }
            else -> fallback
        }
    }

    /** Call only after the updated product catalogue is persisted. Shared references are never removed. */
    fun deleteIfUnreferenced(reference: ProductImageReference?, products: Collection<Product>): Boolean {
        if (reference == null || reference.kind != "local") return false
        if (products.any { it.image == reference }) return false
        return localFile(reference)?.delete() ?: false
    }

    private fun safeFile(key: String): File {
        require(keyPattern.matches(key)) { "Invalid app-owned image key." }
        return File(directory, key)
    }

    private fun detectFormat(file: File): String {
        val header = ByteArray(12)
        val count = file.inputStream().use { it.read(header) }
        val png = count >= 8 && header.take(8).map { it.toInt() and 0xff } == listOf(137, 80, 78, 71, 13, 10, 26, 10)
        val jpeg = count >= 3 && header[0] == 0xff.toByte() && header[1] == 0xd8.toByte() && header[2] == 0xff.toByte()
        val webp = count >= 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF" && String(header, 8, 4, Charsets.US_ASCII) == "WEBP"
        return when {
            jpeg -> "jpg"
            png -> "png"
            webp -> "webp"
            else -> error("Choose a valid JPEG, PNG or WebP image.")
        }
    }
}

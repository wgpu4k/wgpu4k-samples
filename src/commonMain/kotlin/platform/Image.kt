package platform

import kotlinx.io.files.Path

class Image(
    val width: Int,
    val height: Int,
    val bytes: ByteArray,
)

/**
 * Reads an image from the specified [path] to an array containing the individual pixels in RGBA format
 */
expect fun readImage(path: Path): Image
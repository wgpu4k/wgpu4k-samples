package platform

import kotlinx.io.Buffer
import kotlinx.io.files.Path

expect fun readPNG(path: Path): ByteArray
import io.ygdrasil.webgpu.Adapter
import io.ygdrasil.webgpu.GPUPowerPreference
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.NativeSurface


class AutoClose: AutoCloseable {
    companion object {
        operator fun invoke(wgpuCode: AutoClose.() -> Unit) {
            AutoClose().use {
                it.wgpuCode()
            }
        }
    }

    private val toClose = mutableListOf<AutoCloseable>()

    val <T : AutoCloseable> T.ac: T
        get() {
            toClose.add(this)
            return this
        }

    override fun close() {
        val closeErrors = mutableListOf<Throwable>()
        for (closeable in toClose) {
            try {
                // Make sure we close everything even if we throw
                closeable.close()
            } catch (e: Throwable) {
                closeErrors.add(e)
            }
        }
        if (closeErrors.isNotEmpty()) throw closeErrors[0]
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class WebGPUWindow(fps: Int = 60, width: UInt = 800u, height: UInt = 600u, title: String = "Hello World") : AutoCloseable {
    val fps: Int
    val width: UInt
    val height: UInt
    val title: String

    fun requestAdapter(powerPreference: GPUPowerPreference? = null): Adapter
    fun getPresentationFormat(adapter: Adapter): GPUTextureFormat
    fun getWebGPUContext(): NativeSurface
    fun requestAnimationFrame(frame: () -> Unit)
    fun display()
}

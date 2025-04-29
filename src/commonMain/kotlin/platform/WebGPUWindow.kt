package platform

import io.ygdrasil.webgpu.*
import io.ygdrasil.wgpu.WGPULogLevel_Trace


class AutoClose : AutoCloseable {
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
expect class WebGPUWindow(fps: Int = 60, width: UInt = 800u, height: UInt = 600u, title: String = "Hello World", logLevel: UInt = WGPULogLevel_Trace) :
    AutoCloseable {
    val fps: Int
    val width: UInt
    val height: UInt
    val title: String
    val logLevel: UInt

    fun requestAdapter(powerPreference: GPUPowerPreference? = null): Adapter
    fun getPresentationFormat(adapter: Adapter): GPUTextureFormat
    fun getWebGPUContext(): NativeSurface
    fun requestAnimationFrame(frame: () -> Unit)
    fun display()
}

/**
 * Temporary replacement for the web copyExternalImageToTexture that is not available in other platforms
 */
fun GPUDevice.copyExternalImageToTexture(source: ByteArray, texture: GPUTexture, width: Int, height: Int) {
    source.toArrayBuffer(this) { buffer ->
        queue.writeTexture(
            TexelCopyTextureInfo(texture),
            data = buffer,
            dataLayout = TexelCopyBufferLayout(bytesPerRow = width.toUInt() * 4u, rowsPerImage = height.toUInt()),
            size = Extent3D(width = width.toUInt(), height = height.toUInt())
        )
    }
}

// Workaround for https://github.com/wgpu4k/wgpu4k/issues/132, once that is resolved you should use the function provided by wgpu4k
fun ByteArray.toArrayBuffer(device: GPUDevice, usage: (ArrayBuffer) -> Unit) {
    device.createBuffer(
        BufferDescriptor(
            size = size.toULong(),
            usage = setOf(GPUBufferUsage.CopySrc, GPUBufferUsage.CopyDst),
            mappedAtCreation = true
        )
    ).use { gpuBuffer ->
        // Copy from RAM to GPU
        gpuBuffer.mapFrom(this)
        // Copy from GPU to RAM, ideally we can just directly create an ArrayBuffer from RAM
        usage(gpuBuffer.getMappedRange())
    }
}
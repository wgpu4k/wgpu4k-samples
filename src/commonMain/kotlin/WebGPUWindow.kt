import io.ygdrasil.webgpu.Adapter
import io.ygdrasil.webgpu.GPUAdapter
import io.ygdrasil.webgpu.GPUPowerPreference
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.NativeSurface

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class WebGPUWindow(fps: Int = 60, width: UInt = 800u, height: UInt = 600u, title: String = "Hello World") {
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

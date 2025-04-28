package samples

import platform.WebGPUWindow
import io.ygdrasil.webgpu.*
import io.ygdrasil.wgpu.WGPULogLevel_Info
import kotlinx.coroutines.runBlocking
import platform.AutoClose

fun main() = AutoClose.Companion {
    val window = WebGPUWindow(logLevel = WGPULogLevel_Info).ac

    val adapter = window.requestAdapter().ac

    val device = runBlocking { adapter.requestDevice().getOrThrow() }.ac
    val context = window.getWebGPUContext()
    val presentationFormat = window.getPresentationFormat(adapter)

    context.configure(
        SurfaceConfiguration(
            device, format = presentationFormat,
        ),
        width = window.width, height = window.height
    )

    // Set MSAA sample count
    val sampleCount = 4u

    val shader = device.createShaderModule(
        ShaderModuleDescriptor(
            code = """
                @vertex
                fn vs_main(@builtin(vertex_index) in_vertex_index: u32) -> @builtin(position) vec4<f32> {
                    let x = f32(i32(in_vertex_index) - 1);
                    let y = f32(i32(in_vertex_index & 1u) * 2 - 1);
                    return vec4<f32>(x, y, 0.0, 1.0);
                }
                
                @fragment
                fn fs_main() -> @location(0) vec4<f32> {
                    return vec4<f32>(1.0, 0.0, 0.0, 1.0);
                }
            """.trimIndent()
        )
    ).ac

    val pipeline = device.createRenderPipeline(
        RenderPipelineDescriptor(
            layout = null,
            vertex = VertexState(
                module = shader,
                entryPoint = "vs_main"
            ),
            fragment = FragmentState(
                module = shader,
                targets = listOf(ColorTargetState(presentationFormat)),
                entryPoint = "fs_main"
            ),
            primitive = PrimitiveState(
                topology = GPUPrimitiveTopology.TriangleList
            ),
            multisample = MultisampleState(
                count = sampleCount
            )
        )
    ).ac

    // Create multisampled texture
    val msaaTexture = device.createTexture(
        TextureDescriptor(
            size = Extent3D(window.width, window.height),
            sampleCount = sampleCount,
            format = presentationFormat,
            usage = setOf(GPUTextureUsage.RenderAttachment)
        )
    ).ac
    val msaaView = msaaTexture.createView()

    fun frame() = AutoClose.Companion {
        val commandEncoder = device.createCommandEncoder().ac
        val textureView = context.getCurrentTexture().texture.createView().ac
        
        val renderPassDescriptor = RenderPassDescriptor(
            colorAttachments = listOf(
                RenderPassColorAttachment(
                    view = msaaView,
                    resolveTarget = textureView,
                    clearValue = Color(0.0, 0.0, 0.0, 0.0),
                    loadOp = GPULoadOp.Clear,
                    storeOp = GPUStoreOp.Discard
                )
            )
        )

        val passEncoder = commandEncoder.beginRenderPass(renderPassDescriptor)
        passEncoder.setPipeline(pipeline)
        passEncoder.draw(3u)
        passEncoder.end()

        device.queue.submit(listOf(commandEncoder.finish()))

        context.present()

        window.requestAnimationFrame(::frame)
    }

    window.requestAnimationFrame(::frame)
    window.display()
}
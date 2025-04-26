import io.ygdrasil.webgpu.Color
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUPrimitiveTopology
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.PrimitiveState
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.VertexState
import kotlinx.coroutines.runBlocking

fun main() = AutoClose {
    val window = WebGPUWindow().ac

    val adapter = window.requestAdapter().ac

    val device = runBlocking { adapter.requestDevice().getOrThrow() }.ac
    val context = window.getWebGPUContext().ac
    val presentationFormat = window.getPresentationFormat(adapter)

    context.configure(
        SurfaceConfiguration(
            device, format = presentationFormat,
        ),
        width = window.width, height = window.height
    )

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
            )
        )
    ).ac

    fun frame() = AutoClose {
        val commandEncoder = device.createCommandEncoder().ac
        val textureView = context.getCurrentTexture().texture.createView().ac
        val renderPassDescriptor = RenderPassDescriptor(
            colorAttachments = listOf(
                RenderPassColorAttachment(
                    view = textureView,
                    clearValue = Color(0.0, 0.0, 0.0, 0.0),
                    loadOp = GPULoadOp.Clear,
                    storeOp = GPUStoreOp.Store
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
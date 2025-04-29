package samples

import platform.WebGPUWindow
import io.ygdrasil.webgpu.*
import io.ygdrasil.wgpu.WGPULogLevel_Info
import kotlinx.coroutines.runBlocking
import matrix.Mat4
import matrix.Vec3
import org.intellij.lang.annotations.Language
import platform.AutoClose
import platform.copyExternalImageToTexture
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Cube vertex data
private const val cubeVertexSize = 4 * 10 // Byte size of one cube vertex.
private const val cubePositionOffset = 0
private const val cubeUVOffset = 4 * 8
private const val cubeVertexCount = 36

// Cube vertices
private val cubeVertexArray = floatArrayOf(
    // float4 position, float4 color, float2 uv,
    1f, -1f, 1f, 1f,   1f, 0f, 1f, 1f,  0f, 1f,
    -1f, -1f, 1f, 1f,  0f, 0f, 1f, 1f,  1f, 1f,
    -1f, -1f, -1f, 1f, 0f, 0f, 0f, 1f,  1f, 0f,
    1f, -1f, -1f, 1f,  1f, 0f, 0f, 1f,  0f, 0f,
    1f, -1f, 1f, 1f,   1f, 0f, 1f, 1f,  0f, 1f,
    -1f, -1f, -1f, 1f, 0f, 0f, 0f, 1f,  1f, 0f,

    1f, 1f, 1f, 1f,    1f, 1f, 1f, 1f,  0f, 1f,
    1f, -1f, 1f, 1f,   1f, 0f, 1f, 1f,  1f, 1f,
    1f, -1f, -1f, 1f,  1f, 0f, 0f, 1f,  1f, 0f,
    1f, 1f, -1f, 1f,   1f, 1f, 0f, 1f,  0f, 0f,
    1f, 1f, 1f, 1f,    1f, 1f, 1f, 1f,  0f, 1f,
    1f, -1f, -1f, 1f,  1f, 0f, 0f, 1f,  1f, 0f,

    -1f, 1f, 1f, 1f,   0f, 1f, 1f, 1f,  0f, 1f,
    1f, 1f, 1f, 1f,    1f, 1f, 1f, 1f,  1f, 1f,
    1f, 1f, -1f, 1f,   1f, 1f, 0f, 1f,  1f, 0f,
    -1f, 1f, -1f, 1f,  0f, 1f, 0f, 1f,  0f, 0f,
    -1f, 1f, 1f, 1f,   0f, 1f, 1f, 1f,  0f, 1f,
    1f, 1f, -1f, 1f,   1f, 1f, 0f, 1f,  1f, 0f,

    -1f, -1f, 1f, 1f,  0f, 0f, 1f, 1f,  0f, 1f,
    -1f, 1f, 1f, 1f,   0f, 1f, 1f, 1f,  1f, 1f,
    -1f, 1f, -1f, 1f,  0f, 1f, 0f, 1f,  1f, 0f,
    -1f, -1f, -1f, 1f, 0f, 0f, 0f, 1f,  0f, 0f,
    -1f, -1f, 1f, 1f,  0f, 0f, 1f, 1f,  0f, 1f,
    -1f, 1f, -1f, 1f,  0f, 1f, 0f, 1f,  1f, 0f,

    1f, 1f, 1f, 1f,    1f, 1f, 1f, 1f,  0f, 1f,
    -1f, 1f, 1f, 1f,   0f, 1f, 1f, 1f,  1f, 1f,
    -1f, -1f, 1f, 1f,  0f, 0f, 1f, 1f,  1f, 0f,
    -1f, -1f, 1f, 1f,  0f, 0f, 1f, 1f,  1f, 0f,
    1f, -1f, 1f, 1f,   1f, 0f, 1f, 1f,  0f, 0f,
    1f, 1f, 1f, 1f,    1f, 1f, 1f, 1f,  0f, 1f,

    1f, -1f, -1f, 1f,  1f, 0f, 0f, 1f,  0f, 1f,
    -1f, -1f, -1f, 1f, 0f, 0f, 0f, 1f,  1f, 1f,
    -1f, 1f, -1f, 1f,  0f, 1f, 0f, 1f,  1f, 0f,
    1f, 1f, -1f, 1f,   1f, 1f, 0f, 1f,  0f, 0f,
    1f, -1f, -1f, 1f,  1f, 0f, 0f, 1f,  0f, 1f,
    -1f, 1f, -1f, 1f,  0f, 1f, 0f, 1f,  1f, 0f,
)


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

    // Create a vertex buffer from the cube data
    val verticesBuffer = device.createBuffer(
        BufferDescriptor(
            size = (cubeVertexArray.size * 4).toULong(),
            usage = setOf(GPUBufferUsage.Vertex, GPUBufferUsage.CopyDst),
            mappedAtCreation = true
        )
    ).ac

    verticesBuffer.mapFrom(cubeVertexArray)
    verticesBuffer.unmap()

    // Create shader modules
    val vertexShader = device.createShaderModule(
        ShaderModuleDescriptor(
            code = """
                struct Uniforms {
                  modelViewProjectionMatrix : mat4x4f,
                }
                @binding(0) @group(0) var<uniform> uniforms : Uniforms;

                struct VertexOutput {
                  @builtin(position) Position : vec4f,
                  @location(0) fragUV : vec2f,
                  @location(1) fragPosition: vec4f,
                }

                @vertex
                fn main(
                  @location(0) position : vec4f,
                  @location(1) uv : vec2f
                ) -> VertexOutput {
                  var output : VertexOutput;
                  output.Position = uniforms.modelViewProjectionMatrix * position;
                  output.fragUV = uv;
                  output.fragPosition = 0.5 * (position + vec4(1.0, 1.0, 1.0, 1.0));
                  return output;
                }
            """.trimIndent()
        )
    ).ac


    @Language("WGSL")
    val fragmentShader = device.createShaderModule(
        ShaderModuleDescriptor(
            code = """
                @group(0) @binding(1) var mySampler: sampler;
                @group(0) @binding(2) var myTexture: texture_2d<f32>;
                @fragment
                fn main(
                  @location(0) fragUV: vec2f,
                  @location(1) fragPosition: vec4f
                ) -> @location(0) vec4f {
                  return textureSample(myTexture, mySampler, fragUV) * fragPosition;
                }
            """.trimIndent()
        )
    ).ac

    // Create the render pipeline
    val pipeline = device.createRenderPipeline(
        RenderPipelineDescriptor(
            vertex = VertexState(
                module = vertexShader,
                entryPoint = "main",
                buffers = listOf(
                    VertexBufferLayout(
                        arrayStride = cubeVertexSize.toULong(),
                        attributes = listOf(
                            VertexAttribute(
                                format = GPUVertexFormat.Float32x4,
                                offset = cubePositionOffset.toULong(),
                                shaderLocation = 0u
                            ),
                            VertexAttribute(
                                format = GPUVertexFormat.Float32x2,
                                offset = cubeUVOffset.toULong(),
                                shaderLocation = 1u
                            )
                        )
                    )
                )
            ),
            fragment = FragmentState(
                module = fragmentShader,
                entryPoint = "main",
                targets = listOf(ColorTargetState(presentationFormat))
            ),
            primitive = PrimitiveState(
                topology = GPUPrimitiveTopology.TriangleList,
                cullMode = GPUCullMode.Back
            ),
            depthStencil = DepthStencilState(
                format = GPUTextureFormat.Depth24Plus,
                depthWriteEnabled = true,
                depthCompare = GPUCompareFunction.Less
            )
        )
    ).ac

    // Create depth texture
    val depthTexture = device.createTexture(
        TextureDescriptor(
            size = Extent3D(window.width, window.height),
            format = GPUTextureFormat.Depth24Plus,
            usage = setOf(GPUTextureUsage.RenderAttachment)
        )
    ).ac

    // Create uniform buffer for the transformation matrix
    val uniformBufferSize = 4 * 16 // 4x4 matrix
    val uniformBuffer = device.createBuffer(
        BufferDescriptor(
            size = uniformBufferSize.toULong(),
            usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
        )
    ).ac
    val image = platform.readImage(kotlinx.io.files.Path("src/commonMain/resources/Kotlin_Icon.png"))

    // Create a simple 1x1 texture with a solid color
    val cubeTexture = device.createTexture(
        TextureDescriptor(
            size = Extent3D(image.width.toUInt(), image.height.toUInt(), 1u),
            // We loaded srgb data from the png so we specify srgb here. If your data is in a linear color space you can do RGBA8Unorm instead
            format = GPUTextureFormat.RGBA8UnormSrgb,
            usage = setOf(GPUTextureUsage.TextureBinding, GPUTextureUsage.RenderAttachment, GPUTextureUsage.CopyDst)
        )
    ).ac


    device.copyExternalImageToTexture(
        source = image.bytes,
        texture = cubeTexture,
        width = image.width, height = image.height
    )

    // Create a sampler
    val sampler = device.createSampler(
        SamplerDescriptor(
            magFilter = GPUFilterMode.Linear,
            minFilter = GPUFilterMode.Linear
        )
    ).ac

    // Create bind group for the uniform buffer, sampler, and texture
    val uniformBindGroup = device.createBindGroup(
        BindGroupDescriptor(
            layout = pipeline.getBindGroupLayout(0u),
            entries = listOf(
                BindGroupEntry(
                    binding = 0u,
                    resource = BufferBinding(
                        buffer = uniformBuffer
                    )
                ),
                BindGroupEntry(
                    binding = 1u,
                    resource = sampler
                ),
                BindGroupEntry(
                    binding = 2u,
                    resource = cubeTexture.createView()
                )
            )
        )
    ).ac

    // Create depth stencil view
    val depthStencilView = depthTexture.createView()

    // Calculate projection matrix
    val aspect = window.width.toFloat() / window.height.toFloat()
    val projectionMatrix = Mat4.perspective(2f * PI.toFloat() / 5f, aspect, 1f, 100.0f)
    val modelViewProjectionMatrix = Mat4.identity()

    var frame = 0

    // Function to get the transformation matrix
    fun getTransformationMatrix(): Mat4 {
        val viewMatrix = Mat4.identity()
        viewMatrix.translate(Vec3(0f, 0f, -4f), viewMatrix)
        val now = frame / 100f
        viewMatrix.rotate(Vec3(sin(now), cos(now), 0f), 1f, viewMatrix)

        projectionMatrix.multiply(viewMatrix, modelViewProjectionMatrix)
        return modelViewProjectionMatrix
    }

    fun frame() = AutoClose.Companion {
        frame++
        val transformationMatrix = getTransformationMatrix()
        device.queue.writeBuffer(
            uniformBuffer,
            0uL,
            transformationMatrix.array
        )

        val textureView = context.getCurrentTexture().texture.createView().ac

        // Create render pass descriptor for this frame
        val renderPassDescriptor = RenderPassDescriptor(
            colorAttachments = listOf(
                RenderPassColorAttachment(
                    view = textureView,
                    clearValue = Color(0.5, 0.5, 0.5, 1.0),
                    loadOp = GPULoadOp.Clear,
                    storeOp = GPUStoreOp.Store
                )
            ),
            depthStencilAttachment = RenderPassDepthStencilAttachment(
                view = depthStencilView,
                depthClearValue = 1.0f,
                depthLoadOp = GPULoadOp.Clear,
                depthStoreOp = GPUStoreOp.Store
            )
        )

        val commandEncoder = device.createCommandEncoder().ac
        val passEncoder = commandEncoder.beginRenderPass(renderPassDescriptor)
        passEncoder.setPipeline(pipeline)
        passEncoder.setBindGroup(0u, uniformBindGroup)
        passEncoder.setVertexBuffer(0u, verticesBuffer)
        passEncoder.draw(cubeVertexCount.toUInt())
        passEncoder.end()

        device.queue.submit(listOf(commandEncoder.finish()))

        context.present()

        window.requestAnimationFrame(::frame)
    }

    window.requestAnimationFrame(::frame)
    window.display()
}

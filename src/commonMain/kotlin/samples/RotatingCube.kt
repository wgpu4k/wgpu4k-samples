package samples

import WebGPUWindow
import io.ygdrasil.webgpu.*
import kotlinx.coroutines.runBlocking
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Cube vertex data
private const val cubeVertexSize = 4 * 10 // Byte size of one cube vertex.
private const val cubePositionOffset = 0
private const val cubeColorOffset = 4 * 4 // Byte offset of cube vertex color attribute.
private const val cubeUVOffset = 4 * 8
private const val cubeVertexCount = 36

// Cube vertices
// prettier-ignore
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

// Simple matrix implementation
class Mat4 {
    val data = FloatArray(16) { 0f }

    companion object {
        fun identity(): Mat4 {
            val result = Mat4()
            result.data[0] = 1f
            result.data[5] = 1f
            result.data[10] = 1f
            result.data[15] = 1f
            return result
        }

        fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): Mat4 {
            val result = Mat4()
            val f = 1.0f / kotlin.math.tan(fovY / 2)
            result.data[0] = f / aspect
            result.data[5] = f
            result.data[10] = (far + near) / (near - far)
            result.data[11] = -1f
            result.data[14] = (2 * far * near) / (near - far)
            return result
        }

        fun multiply(a: Mat4, b: Mat4): Mat4 {
            val result = Mat4()
            for (i in 0 until 4) {
                for (j in 0 until 4) {
                    var sum = 0f
                    for (k in 0 until 4) {
                        sum += a.data[i * 4 + k] * b.data[k * 4 + j]
                    }
                    result.data[i * 4 + j] = sum
                }
            }
            return result
        }
    }

    fun translate(x: Float, y: Float, z: Float): Mat4 {
        val result = Mat4()
        for (i in 0 until 16) {
            result.data[i] = data[i]
        }
        result.data[12] = data[0] * x + data[4] * y + data[8] * z + data[12]
        result.data[13] = data[1] * x + data[5] * y + data[9] * z + data[13]
        result.data[14] = data[2] * x + data[6] * y + data[10] * z + data[14]
        result.data[15] = data[3] * x + data[7] * y + data[11] * z + data[15]
        return result
    }

    fun rotate(x: Float, y: Float, z: Float, angle: Float): Mat4 {
        val len = kotlin.math.sqrt(x * x + y * y + z * z)
        val nx = x / len
        val ny = y / len
        val nz = z / len
        val c = cos(angle)
        val s = sin(angle)
        val t = 1f - c

        val result = Mat4()

        // First row
        result.data[0] = c + nx * nx * t
        result.data[1] = nx * ny * t - nz * s
        result.data[2] = nx * nz * t + ny * s
        result.data[3] = 0f

        // Second row
        result.data[4] = ny * nx * t + nz * s
        result.data[5] = c + ny * ny * t
        result.data[6] = ny * nz * t - nx * s
        result.data[7] = 0f

        // Third row
        result.data[8] = nz * nx * t - ny * s
        result.data[9] = nz * ny * t + nx * s
        result.data[10] = c + nz * nz * t
        result.data[11] = 0f

        // Fourth row
        result.data[12] = 0f
        result.data[13] = 0f
        result.data[14] = 0f
        result.data[15] = 1f

        return Mat4.multiply(this, result)
    }
}

fun main() = AutoClose.Companion {
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

    // Create a vertex buffer from the cube data
    val verticesBuffer = device.createBuffer(
        BufferDescriptor(
            size = (cubeVertexArray.size * 4).toULong(),
            usage = setOf(GPUBufferUsage.Vertex),
            mappedAtCreation = true
        )
    ).ac

    // Copy the vertex data to the buffer
    device.queue.writeBuffer(verticesBuffer, 0uL, cubeVertexArray)
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

    val fragmentShader = device.createShaderModule(
        ShaderModuleDescriptor(
            code = """
                @fragment
                fn main(
                  @location(0) fragUV: vec2f,
                  @location(1) fragPosition: vec4f
                ) -> @location(0) vec4f {
                  return fragPosition;
                }
            """.trimIndent()
        )
    ).ac

    // Create depth texture
    val depthTexture = device.createTexture(
        TextureDescriptor(
            size = Extent3D(window.width, window.height, 1u),
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

    // Create the render pipeline
    val pipeline = device.createRenderPipeline(
        RenderPipelineDescriptor(
            layout = null,
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

    // Create bind group for the uniform buffer
    val uniformBindGroup = device.createBindGroup(
        BindGroupDescriptor(
            layout = pipeline.getBindGroupLayout(0u),
            entries = listOf(
                BindGroupEntry(
                    binding = 0u,
                    resource = BufferBinding(
                        buffer = uniformBuffer,
                        offset = 0uL,
                        size = uniformBufferSize.toULong()
                    )
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

    // Function to get the transformation matrix
    fun getTransformationMatrix(): Mat4 {
        val viewMatrix = Mat4.identity()
        viewMatrix.translate(0f, 0f, -4f)
        val now = System.currentTimeMillis() / 1000.0f
        viewMatrix.rotate(sin(now), cos(now), 0f, 1f)
        return Mat4.multiply(projectionMatrix, viewMatrix)
    }

    fun frame() = AutoClose.Companion {
        val transformationMatrix = getTransformationMatrix()
        device.queue.writeBuffer(
            uniformBuffer,
            0uL,
            transformationMatrix.data
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

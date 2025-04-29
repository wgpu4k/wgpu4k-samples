package samples

import platform.WebGPUWindow
import io.ygdrasil.webgpu.*
import io.ygdrasil.wgpu.WGPULogLevel_Info
import kotlinx.coroutines.runBlocking
import matrix.Mat4
import matrix.Vec3
import platform.AutoClose
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
                  modelViewProjectionMatrix : array<mat4x4f, 16>,
                }
                @binding(0) @group(0) var<uniform> uniforms : Uniforms;

                struct VertexOutput {
                  @builtin(position) Position : vec4f,
                  @location(0) fragUV : vec2f,
                  @location(1) fragPosition: vec4f,
                }

                @vertex
                fn main(
                  @builtin(instance_index) instanceIdx : u32,
                  @location(0) position : vec4f,
                  @location(1) uv : vec2f
                ) -> VertexOutput {
                  var output : VertexOutput;
                  output.Position = uniforms.modelViewProjectionMatrix[instanceIdx] * position;
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

    // Define instance grid dimensions
    val xCount = 4
    val yCount = 4
    val numInstances = xCount * yCount
    val matrixFloatCount = 16 // 4x4 matrix
    val matrixSize = 4 * matrixFloatCount
    val uniformBufferSize = numInstances * matrixSize

    // Create uniform buffer for the transformation matrices
    val uniformBuffer = device.createBuffer(
        BufferDescriptor(
            size = uniformBufferSize.toULong(),
            usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
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
                        buffer = uniformBuffer
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
    
    // Initialize model matrices for each instance
    val modelMatrices = Array(numInstances) { Mat4.identity() }
    val mvpMatricesData = FloatArray(matrixFloatCount * numInstances)
    
    // Define spacing between instances
    val step = 4.0f
    
    // Initialize the matrix data for every instance
    var m = 0
    for (x in 0 until xCount) {
        for (y in 0 until yCount) {
            modelMatrices[m] = Mat4.translation(
                Vec3(
                    step * (x - xCount / 2 + 0.5f),
                    step * (y - yCount / 2 + 0.5f),
                    0f
                )
            )
            m++
        }
    }
    
    // Create view matrix
    val viewMatrix = Mat4.translation(Vec3(0f, 0f, -12f))
    
    // Temporary matrix for calculations
    val tmpMat4 = Mat4.identity()
    
    var frame = 0
    
    // Function to update transformation matrices
    fun updateTransformationMatrix() {
        val now = frame / 100f
        
        var m = 0
        var i = 0
        for (x in 0 until xCount) {
            for (y in 0 until yCount) {
                modelMatrices[i].rotate(
                    Vec3(
                        sin((x + 0.5f) * now),
                        cos((y + 0.5f) * now),
                        0f
                    ),
                    1f,
                    tmpMat4
                )
                
                viewMatrix.multiply(tmpMat4, tmpMat4)
                projectionMatrix.multiply(tmpMat4, tmpMat4)
                
                // Copy matrix data to the array
                System.arraycopy(tmpMat4.array, 0, mvpMatricesData, m, matrixFloatCount)
                
                i++
                m += matrixFloatCount
            }
        }
    }

    fun frame() = AutoClose.Companion {
        frame++
        updateTransformationMatrix()
        
        device.queue.writeBuffer(
            uniformBuffer,
            0uL,
            mvpMatricesData
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
        passEncoder.draw(cubeVertexCount.toUInt(), numInstances.toUInt(), 0u, 0u)
        passEncoder.end()

        device.queue.submit(listOf(commandEncoder.finish()))

        context.present()

        window.requestAnimationFrame(::frame)
    }

    window.requestAnimationFrame(::frame)
    window.display()
}
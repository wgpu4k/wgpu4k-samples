package samples

import platform.WebGPUWindow
import io.ygdrasil.webgpu.*
import io.ygdrasil.wgpu.WGPULogLevel_Info
import kotlinx.coroutines.runBlocking
import io.github.natanfudge.wgpu4k.matrix.Mat4
import io.github.natanfudge.wgpu4k.matrix.Vec3
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
                @group(0) @binding(2) var myTexture: texture_cube<f32>;

                @fragment
                fn main(
                  @location(0) fragUV: vec2f,
                  @location(1) fragPosition: vec4f
                ) -> @location(0) vec4f {
                  // Our camera and the skybox cube are both centered at (0, 0, 0)
                  // so we can use the cube geometry position to get viewing vector to sample
                  // the cube texture. The magnitude of the vector doesn't matter.
                  var cubemapVec = fragPosition.xyz - vec3(0.5);
                  // When viewed from the inside, cubemaps are left-handed (z away from viewer),
                  // but common camera matrix convention results in a right-handed world space
                  // (z toward viewer), so we have to flip it.
                  cubemapVec.z *= -1;
                  return textureSample(myTexture, mySampler, cubemapVec);
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
                // Since we are seeing from inside of the cube
                // and we are using the regular cube geometry data with outward-facing normals,
                // the cullMode should be 'none'.
                cullMode = GPUCullMode.None
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

    // Load cubemap images
    // The order of the array layers is [+X, -X, +Y, -Y, +Z, -Z]
    val imagePaths = listOf(
        "src/commonMain/resources/cubemap/posx.jpg",
        "src/commonMain/resources/cubemap/negx.jpg",
        "src/commonMain/resources/cubemap/posy.jpg",
        "src/commonMain/resources/cubemap/negy.jpg",
        "src/commonMain/resources/cubemap/posz.jpg",
        "src/commonMain/resources/cubemap/negz.jpg"
    )
    
    val images = imagePaths.map { path ->
        platform.readImage(kotlinx.io.files.Path(path))
    }
    
    // Create a cubemap texture
    val cubemapTexture = device.createTexture(
        TextureDescriptor(
            dimension = GPUTextureDimension.TwoD,
            size = Extent3D(
                width = images[0].width.toUInt(),
                height = images[0].height.toUInt(),
                depthOrArrayLayers = 6u
            ),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = setOf(
                GPUTextureUsage.TextureBinding,
                GPUTextureUsage.CopyDst,
                GPUTextureUsage.RenderAttachment
            ),
        )
    ).ac

    // Copy each image to a layer of the cubemap texture
    for (i in images.indices) {
        device.copyExternalImageToTexture(
            source = images[i].bytes,
            texture = cubemapTexture,
            width = images[i].width,
            height = images[i].height,
            origin = Origin3D(0u, 0u, i.toUInt())
        )
    }

    // Create a sampler
    val sampler = device.createSampler(
        SamplerDescriptor(
            magFilter = GPUFilterMode.Linear,
            minFilter = GPUFilterMode.Linear
        )
    ).ac

    // Create bind group for the uniform buffer, sampler, and cubemap texture
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
                    resource = cubemapTexture.createView(
                        TextureViewDescriptor(
                            dimension = GPUTextureViewDimension.Cube,
                            arrayLayerCount = 6u
                        )
                    )
                )
            )
        )
    ).ac

    // Create depth stencil view
    val depthStencilView = depthTexture.createView()

    // Calculate projection matrix
    val aspect = window.width.toFloat() / window.height.toFloat()
    val projectionMatrix = Mat4.perspective(2f * PI.toFloat() / 5f, aspect, 1f, 3000.0f)
    
    // Create model matrix with large scaling for skybox
    val modelMatrix = Mat4.scaling(Vec3(1000f, 1000f, 1000f))
    val modelViewProjectionMatrix = Mat4.identity()
    val viewMatrix = Mat4.identity()
    
    val tmpMat4 = Mat4.identity()

    var frame = 0

    // Function to update transformation matrix
    fun updateTransformationMatrix() {
        val now = frame / 100f
        
        viewMatrix.rotate(
            Vec3(1f, 0f, 0f),
            (PI.toFloat() / 10f) * sin(now),
            tmpMat4
        )
        tmpMat4.rotate(Vec3(0f, 1f, 0f), now * 0.2f, tmpMat4)
        
        tmpMat4.multiply(modelMatrix, modelViewProjectionMatrix)
        projectionMatrix.multiply(modelViewProjectionMatrix, modelViewProjectionMatrix)
    }

    fun frame() = AutoClose.Companion {
        frame++
        updateTransformationMatrix()
        device.queue.writeBuffer(
            uniformBuffer,
            0uL,
            modelViewProjectionMatrix.array
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
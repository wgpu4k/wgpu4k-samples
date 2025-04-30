package samples

import io.ygdrasil.webgpu.*
import io.ygdrasil.wgpu.WGPULogLevel_Info
import kotlinx.coroutines.runBlocking
import matrix.Mat4
import matrix.Vec3
import platform.AutoClose
import platform.WebGPUWindow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Constants from TypeScript
private const val geometryVertexSize = 4 * 8 // Byte size of one geometry vertex.
private const val geometryPositionOffset = 0
private const val geometryColorOffset = 4 * 4 // Byte offset of geometry vertex color attribute.
private const val geometryDrawCount = 6 * 2

private const val d = 0.0001f // half distance between two planes
private const val o = 0.5f // half x offset

// prettier-ignore
private val geometryVertexArray = floatArrayOf(
    // float4 position, float4 color
    -1f - o, -1f, d, 1f, 1f, 0f, 0f, 1f,
    1f - o, -1f, d, 1f, 1f, 0f, 0f, 1f,
    -1f - o, 1f, d, 1f, 1f, 0f, 0f, 1f,
    1f - o, -1f, d, 1f, 1f, 0f, 0f, 1f,
    1f - o, 1f, d, 1f, 1f, 0f, 0f, 1f,
    -1f - o, 1f, d, 1f, 1f, 0f, 0f, 1f,

    -1f + o, -1f, -d, 1f, 0f, 1f, 0f, 1f,
    1f + o, -1f, -d, 1f, 0f, 1f, 0f, 1f,
    -1f + o, 1f, -d, 1f, 0f, 1f, 0f, 1f,
    1f + o, -1f, -d, 1f, 0f, 1f, 0f, 1f,
    1f + o, 1f, -d, 1f, 0f, 1f, 0f, 1f,
    -1f + o, 1f, -d, 1f, 0f, 1f, 0f, 1f,
)

private const val xCount = 1
private const val yCount = 5
private const val numInstances = xCount * yCount
private const val matrixFloatCount = 16 // 4x4 matrix
private const val matrixStride = 4 * matrixFloatCount // 64;

private val depthRangeRemapMatrix = Mat4.identity().apply {
    this[10] = -1f
    this[14] = 1f
}

private enum class DepthBufferMode {
    Default,
    Reversed,
}

private val depthBufferModes = listOf(DepthBufferMode.Default, DepthBufferMode.Reversed)

private val depthCompareFuncs = mapOf(
    DepthBufferMode.Default to GPUCompareFunction.Less,
    DepthBufferMode.Reversed to GPUCompareFunction.Greater
)

private val depthClearValues = mapOf(
    DepthBufferMode.Default to 1.0f,
    DepthBufferMode.Reversed to 0.0f
)

// WGSL Shaders
private const val vertexWGSL = """
    struct Uniforms {
      modelMatrix : array<mat4x4f, ${numInstances}>,
    }
    struct Camera {
      viewProjectionMatrix : mat4x4f,
    }

    @binding(0) @group(0) var<uniform> uniforms : Uniforms;
    @binding(1) @group(0) var<uniform> camera : Camera;

    struct VertexOutput {
      @builtin(position) Position : vec4f,
      @location(0) fragColor : vec4f,
    }

    @vertex
    fn main(
      @builtin(instance_index) instanceIdx : u32,
      @location(0) position : vec4f,
      @location(1) color : vec4f
    ) -> VertexOutput {
      var output : VertexOutput;
      output.Position = camera.viewProjectionMatrix * uniforms.modelMatrix[instanceIdx] * position;
      output.fragColor = color;
      return output;
    }
"""

private const val fragmentWGSL = """
    @fragment
    fn main(
      @location(0) fragColor: vec4f
    ) -> @location(0) vec4f {
      return fragColor;
    }
"""

private const val vertexDepthPrePassWGSL = """
    struct Uniforms {
      modelMatrix : array<mat4x4f, ${numInstances}>,
    }
    struct Camera {
      viewProjectionMatrix : mat4x4f,
    }

    @binding(0) @group(0) var<uniform> uniforms : Uniforms;
    @binding(1) @group(0) var<uniform> camera : Camera;

    @vertex
    fn main(
      @builtin(instance_index) instanceIdx : u32,
      @location(0) position : vec4f
    ) -> @builtin(position) vec4f {
      return camera.viewProjectionMatrix * uniforms.modelMatrix[instanceIdx] * position;
    }
"""

private const val vertexTextureQuadWGSL = """
    @vertex
    fn main(
      @builtin(vertex_index) VertexIndex : u32
    ) -> @builtin(position) vec4f {
      const pos = array(
        vec2(-1.0, -1.0), vec2(1.0, -1.0), vec2(-1.0, 1.0),
        vec2(-1.0, 1.0), vec2(1.0, -1.0), vec2(1.0, 1.0),
      );

      return vec4(pos[VertexIndex], 0.0, 1.0);
    }
"""

private const val fragmentTextureQuadWGSL = """
    @group(0) @binding(0) var depthTexture: texture_2d<f32>;

    @fragment
    fn main(
      @builtin(position) coord : vec4f
    ) -> @location(0) vec4f {
      let depthValue = textureLoad(depthTexture, vec2i(floor(coord.xy)), 0).x; // Use 0u for level
      return vec4f(depthValue, depthValue, depthValue, 1.0);
    }
"""

private const val vertexPrecisionErrorPassWGSL = """
    struct Uniforms {
      modelMatrix : array<mat4x4f, ${numInstances}>,
    }
    struct Camera {
      viewProjectionMatrix : mat4x4f,
    }

    @binding(0) @group(0) var<uniform> uniforms : Uniforms;
    @binding(1) @group(0) var<uniform> camera : Camera;

    struct VertexOutput {
      @builtin(position) Position : vec4f,
      @location(0) clipPos : vec4f,
    }

    @vertex
    fn main(
      @builtin(instance_index) instanceIdx : u32,
      @location(0) position : vec4f
    ) -> VertexOutput {
      var output : VertexOutput;
      output.Position = camera.viewProjectionMatrix * uniforms.modelMatrix[instanceIdx] * position;
      output.clipPos = output.Position;
      return output;
    }
"""

private const val fragmentPrecisionErrorPassWGSL = """
    @group(1) @binding(0) var depthTexture: texture_2d<f32>;

    @fragment
    fn main(
      @builtin(position) coord: vec4f,
      @location(0) clipPos: vec4f
    ) -> @location(0) vec4f {
      let depthValue = textureLoad(depthTexture, vec2i(floor(coord.xy)), 0).x;
      let v : f32 = abs(clipPos.z / clipPos.w - depthValue) * 2000000.0;
      return vec4f(v, v, v, 1.0);
    }
"""


fun main() = AutoClose.Companion {
    val window = WebGPUWindow(logLevel = WGPULogLevel_Info).ac

    val adapter = window.requestAdapter().ac
    val device = runBlocking { adapter.requestDevice().getOrThrow() }.ac
    val context = window.getWebGPUContext()
    val presentationFormat = window.getPresentationFormat(adapter)

    context.configure(
        SurfaceConfiguration(
            device = device,
            format = presentationFormat,
            // alphaMode = GPUCanvasAlphaMode.Premultiplied // Optional, check if needed
        ),
        width = window.width, height = window.height
    )

    val verticesBuffer = device.createBuffer(
        BufferDescriptor(
            size = (geometryVertexArray.size * Float.SIZE_BYTES).toULong(),
            usage = setOf(GPUBufferUsage.Vertex, GPUBufferUsage.CopyDst),
            mappedAtCreation = true
        )
    ).ac
    verticesBuffer.mapFrom(geometryVertexArray)
    verticesBuffer.unmap()

    val depthBufferFormat = GPUTextureFormat.Depth32Float

    val depthTextureBindGroupLayout = device.createBindGroupLayout(
        BindGroupLayoutDescriptor(
            entries = listOf(
                BindGroupLayoutEntry(
                    binding = 0u,
                    visibility = setOf(GPUShaderStage.Fragment),
                    texture = TextureBindingLayout(
                        sampleType = GPUTextureSampleType.UnfilterableFloat
                    )
                )
            )
        )
    ).ac

    val uniformBindGroupLayout = device.createBindGroupLayout(
        BindGroupLayoutDescriptor(
            entries = listOf(
                BindGroupLayoutEntry(
                    binding = 0u,
                    visibility = setOf(GPUShaderStage.Vertex),
                    buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform)
                ),
                BindGroupLayoutEntry(
                    binding = 1u,
                    visibility = setOf(GPUShaderStage.Vertex),
                    buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform)
                )
            )
        )
    ).ac

    // --- Shader Modules ---
    val vertexModule = device.createShaderModule(ShaderModuleDescriptor(code = vertexWGSL)).ac
    val fragmentModule = device.createShaderModule(ShaderModuleDescriptor(code = fragmentWGSL)).ac
    val vertexDepthPrePassModule = device.createShaderModule(ShaderModuleDescriptor(code = vertexDepthPrePassWGSL)).ac
    val vertexTextureQuadModule = device.createShaderModule(ShaderModuleDescriptor(code = vertexTextureQuadWGSL)).ac
    val fragmentTextureQuadModule = device.createShaderModule(ShaderModuleDescriptor(code = fragmentTextureQuadWGSL)).ac
    val vertexPrecisionErrorPassModule = device.createShaderModule(ShaderModuleDescriptor(code = vertexPrecisionErrorPassWGSL)).ac
    val fragmentPrecisionErrorPassModule = device.createShaderModule(ShaderModuleDescriptor(code = fragmentPrecisionErrorPassWGSL)).ac


    // --- Pipeline Layouts ---
    val depthPrePassRenderPipelineLayout = device.createPipelineLayout(
        PipelineLayoutDescriptor(bindGroupLayouts = listOf(uniformBindGroupLayout))
    ).ac

    val precisionPassRenderPipelineLayout = device.createPipelineLayout(
        PipelineLayoutDescriptor(bindGroupLayouts = listOf(uniformBindGroupLayout, depthTextureBindGroupLayout))
    ).ac

    val colorPassRenderPiplineLayout = device.createPipelineLayout(
        PipelineLayoutDescriptor(bindGroupLayouts = listOf(uniformBindGroupLayout))
    ).ac

    val textureQuadPassPiplineLayout = device.createPipelineLayout(
        PipelineLayoutDescriptor(bindGroupLayouts = listOf(depthTextureBindGroupLayout))
    ).ac

    // --- Depth Pre Pass Pipeline ---
    val depthPrePassRenderPipelineDescriptorBase = RenderPipelineDescriptor(
        layout = depthPrePassRenderPipelineLayout,
        vertex = VertexState(
            module = vertexDepthPrePassModule,
            entryPoint = "main",
            buffers = listOf(
                VertexBufferLayout(
                    arrayStride = geometryVertexSize.toULong(),
                    attributes = listOf(
                        VertexAttribute(
                            format = GPUVertexFormat.Float32x4,
                            offset = geometryPositionOffset.toULong(),
                            shaderLocation = 0u
                        )
                    )
                )
            )
        ),
        primitive = PrimitiveState(
            topology = GPUPrimitiveTopology.TriangleList,
            cullMode = GPUCullMode.Back
        ),
        depthStencil = DepthStencilState(
            format = depthBufferFormat,
            depthWriteEnabled = true,
            depthCompare = GPUCompareFunction.Less // Placeholder, will be set below
        )
    )

    val depthPrePassPipelines = depthBufferModes.associateWith { mode ->
        device.createRenderPipeline(
            depthPrePassRenderPipelineDescriptorBase.copy(
                depthStencil = depthPrePassRenderPipelineDescriptorBase.depthStencil?.copy(
                    depthCompare = depthCompareFuncs[mode]!!
                )
            )
        ).ac
    }

    // --- Precision Pass Pipeline ---
    val precisionPassRenderPipelineDescriptorBase = RenderPipelineDescriptor(
        layout = precisionPassRenderPipelineLayout,
        vertex = VertexState(
            module = vertexPrecisionErrorPassModule,
            entryPoint = "main",
            buffers = listOf(
                VertexBufferLayout(
                    arrayStride = geometryVertexSize.toULong(),
                    attributes = listOf(
                        VertexAttribute(
                            format = GPUVertexFormat.Float32x4,
                            offset = geometryPositionOffset.toULong(),
                            shaderLocation = 0u
                        )
                    )
                )
            )
        ),
        fragment = FragmentState(
            module = fragmentPrecisionErrorPassModule,
            entryPoint = "main",
            targets = listOf(ColorTargetState(format = presentationFormat))
        ),
        primitive = PrimitiveState(
            topology = GPUPrimitiveTopology.TriangleList,
            cullMode = GPUCullMode.Back
        ),
        depthStencil = DepthStencilState(
            format = depthBufferFormat,
            depthWriteEnabled = true,
            depthCompare = GPUCompareFunction.Less // Placeholder
        )
    )

    val precisionPassPipelines = depthBufferModes.associateWith { mode ->
        device.createRenderPipeline(
            precisionPassRenderPipelineDescriptorBase.copy(
                depthStencil = precisionPassRenderPipelineDescriptorBase.depthStencil?.copy(
                    depthCompare = depthCompareFuncs[mode]!!
                )
            )
        ).ac
    }

    // --- Color Pass Pipeline ---
    val colorPassRenderPipelineDescriptorBase = RenderPipelineDescriptor(
        layout = colorPassRenderPiplineLayout,
        vertex = VertexState(
            module = vertexModule,
            entryPoint = "main",
            buffers = listOf(
                VertexBufferLayout(
                    arrayStride = geometryVertexSize.toULong(),
                    attributes = listOf(
                        VertexAttribute(
                            format = GPUVertexFormat.Float32x4,
                            offset = geometryPositionOffset.toULong(),
                            shaderLocation = 0u
                        ),
                        VertexAttribute(
                            format = GPUVertexFormat.Float32x4,
                            offset = geometryColorOffset.toULong(),
                            shaderLocation = 1u
                        )
                    )
                )
            )
        ),
        fragment = FragmentState(
            module = fragmentModule,
            entryPoint = "main",
            targets = listOf(ColorTargetState(format = presentationFormat))
        ),
        primitive = PrimitiveState(
            topology = GPUPrimitiveTopology.TriangleList,
            cullMode = GPUCullMode.Back
        ),
        depthStencil = DepthStencilState(
            format = depthBufferFormat,
            depthWriteEnabled = true,
            depthCompare = GPUCompareFunction.Less // Placeholder
        )
    )

    val colorPassPipelines = depthBufferModes.associateWith { mode ->
        device.createRenderPipeline(
            colorPassRenderPipelineDescriptorBase.copy(
                depthStencil = colorPassRenderPipelineDescriptorBase.depthStencil?.copy(
                    depthCompare = depthCompareFuncs[mode]!!
                )
            )
        ).ac
    }

    // --- Texture Quad Pass Pipeline ---
    val textureQuadPassPipline = device.createRenderPipeline(
        RenderPipelineDescriptor(
            layout = textureQuadPassPiplineLayout,
            vertex = VertexState(
                module = vertexTextureQuadModule,
                entryPoint = "main"
            ),
            fragment = FragmentState(
                module = fragmentTextureQuadModule,
                entryPoint = "main",
                targets = listOf(ColorTargetState(format = presentationFormat))
            ),
            primitive = PrimitiveState(
                topology = GPUPrimitiveTopology.TriangleList
            )
            // No depth stencil needed for quad pass
        )
    ).ac

    // --- Textures ---
    val depthTexture = device.createTexture(
        TextureDescriptor(
            size = Extent3D(width = window.width, height = window.height),
            format = depthBufferFormat,
            usage = setOf(GPUTextureUsage.RenderAttachment, GPUTextureUsage.TextureBinding)
        )
    ).ac
    val depthTextureView = depthTexture.createView().ac

    val defaultDepthTexture = device.createTexture( // Used for the main color/precision passes
        TextureDescriptor(
            size = Extent3D(width = window.width, height = window.height),
            format = depthBufferFormat,
            usage = setOf(GPUTextureUsage.RenderAttachment)
        )
    ).ac
    val defaultDepthTextureView = defaultDepthTexture.createView().ac

    // --- Render Pass Descriptors ---
    val depthPrePassDescriptor = RenderPassDescriptor(
        colorAttachments = emptyList(), // No color attachment for depth pre-pass
        depthStencilAttachment = RenderPassDepthStencilAttachment(
            view = depthTextureView,
            depthClearValue = 1.0f, // Placeholder, set in loop
            depthLoadOp = GPULoadOp.Clear,
            depthStoreOp = GPUStoreOp.Store
        )
    )
    //TODO: just create a dummy texture and do the copy

    // Create dummy texture, as we manipulate immutable data and we need to assign a texture early
    val dummyTexture = device.createTexture(
        TextureDescriptor(
            size = Extent3D(1u, 1u),
            format = GPUTextureFormat.Depth24Plus,
            usage = setOf(GPUTextureUsage.RenderAttachment),
        )
    ).ac


    val drawPassDescriptor = RenderPassDescriptor(
        colorAttachments = listOf(
            RenderPassColorAttachment(
                view = dummyTexture.createView().ac, // Set in frame loop
                clearValue = Color(0.0, 0.0, 0.5, 1.0),
                loadOp = GPULoadOp.Clear,
                storeOp = GPUStoreOp.Store
            )
        ),
        depthStencilAttachment = RenderPassDepthStencilAttachment(
            view = defaultDepthTextureView,
            depthClearValue = 1.0f, // Placeholder, set in loop
            depthLoadOp = GPULoadOp.Clear,
            depthStoreOp = GPUStoreOp.Store
        )
    )

    val drawPassLoadDescriptor = drawPassDescriptor.copy(
        colorAttachments = listOf(
            drawPassDescriptor.colorAttachments[0].copy(loadOp = GPULoadOp.Load)
        )
    )
    val drawPassDescriptors = listOf(drawPassDescriptor, drawPassLoadDescriptor)


    val textureQuadPassDescriptor = RenderPassDescriptor(
        colorAttachments = listOf(
            RenderPassColorAttachment(
                view = dummyTexture.createView().ac, // Set in frame loop
                clearValue = Color(0.0, 0.0, 0.5, 1.0),
                loadOp = GPULoadOp.Clear,
                storeOp = GPUStoreOp.Store
            )
        )
        // No depth stencil needed
    )
    val textureQuadPassLoadDescriptor = textureQuadPassDescriptor.copy(
        colorAttachments = listOf(
            textureQuadPassDescriptor.colorAttachments[0].copy(loadOp = GPULoadOp.Load)
        )
    )
    val textureQuadPassDescriptors = listOf(textureQuadPassDescriptor, textureQuadPassLoadDescriptor)


    // --- Buffers & Bind Groups ---
    val depthTextureBindGroup = device.createBindGroup(
        BindGroupDescriptor(
            layout = depthTextureBindGroupLayout,
            entries = listOf(
                BindGroupEntry(
                    binding = 0u,
                    resource = depthTextureView
                )
            )
        )
    ).ac

    val uniformBufferSize = (numInstances * matrixStride).toULong()

    val uniformBuffer = device.createBuffer(
        BufferDescriptor(
            size = uniformBufferSize,
            usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
        )
    ).ac

    val cameraMatrixBufferSize = (4 * 16).toULong() // 4x4 matrix size in bytes
    val cameraMatrixBuffer = device.createBuffer(
        BufferDescriptor(
            size = cameraMatrixBufferSize,
            usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
        )
    ).ac
    val cameraMatrixReversedDepthBuffer = device.createBuffer(
        BufferDescriptor(
            size = cameraMatrixBufferSize,
            usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
        )
    ).ac

    val uniformBindGroups = depthBufferModes.associateWith { mode ->
        device.createBindGroup(
            BindGroupDescriptor(
                layout = uniformBindGroupLayout,
                entries = listOf(
                    BindGroupEntry(
                        binding = 0u,
                        resource = BufferBinding(buffer = uniformBuffer)
                    ),
                    BindGroupEntry(
                        binding = 1u,
                        resource = BufferBinding(
                            buffer = if (mode == DepthBufferMode.Reversed) cameraMatrixReversedDepthBuffer else cameraMatrixBuffer
                        )
                    )
                )
            )
        ).ac
    }


    // --- Matrices & Data ---
    val modelMatrices = Array(numInstances) { Mat4.identity() }
    val mvpMatricesData = FloatArray(matrixFloatCount * numInstances)

    var mIndex = 0
    for (x in 0 until xCount) {
        for (y in 0 until yCount) {
            val z = -800f * mIndex
            val s = 1f + 50f * mIndex

            modelMatrices[mIndex] = Mat4.translation(
                Vec3(
                    x.toFloat() - xCount.toFloat() / 2f + 0.5f,
                    (4.0f - 0.2f * z) * (y.toFloat() - yCount.toFloat() / 2f + 1.0f),
                    z
                )
            )
            modelMatrices[mIndex].scale(Vec3(s, s, s), modelMatrices[mIndex])

            mIndex++
        }
    }

    val viewMatrix = Mat4.translation(Vec3(0f, 0f, -12f))

    val aspect = (0.5f * window.width.toFloat()) / window.height.toFloat()
    // Note: Kotlin Mat4.perspective might handle zFar = Infinity differently or not at all.
    // Using a large finite number as in the original TS example.
    val projectionMatrix = Mat4.perspective(2f * PI.toFloat() / 5f, aspect, 5f, 9999f)

    val viewProjectionMatrix = projectionMatrix.multiply(viewMatrix)
    val reversedRangeViewProjectionMatrix = depthRangeRemapMatrix.multiply(viewProjectionMatrix)

    device.queue.writeBuffer(cameraMatrixBuffer, 0uL, viewProjectionMatrix.array)
    device.queue.writeBuffer(cameraMatrixReversedDepthBuffer, 0uL, reversedRangeViewProjectionMatrix.array)

    val tmpMat4 = Mat4.identity()
    var frameCount = 0

    // --- Settings --- (Replaces dat.gui)
    var currentMode = "color" // "color", "precision-error", "depth-texture"
    // TODO: GUI for these samples

    fun updateTransformationMatrix() {
        val now = frameCount / 60.0f // Simulate time based on frames

        for (i in 0 until numInstances) {
            modelMatrices[i].rotate(
                Vec3(sin(now), cos(now), 0f),
                (PI / 180.0 * 30.0).toFloat(), // Convert degrees to radians
                tmpMat4
            )
            // Copy the rotated matrix data into the large mvpMatricesData array
            val offset = i * matrixFloatCount
            tmpMat4.array.copyInto(mvpMatricesData, offset)
        }
    }

    fun frame() = AutoClose.Companion {
        frameCount++
        updateTransformationMatrix()
        device.queue.writeBuffer(
            uniformBuffer,
            0uL,
            mvpMatricesData
        )

        val currentTexture = context.getCurrentTexture().texture
        val attachmentView = currentTexture.createView().ac
        val commandEncoder = device.createCommandEncoder().ac

        when (currentMode) {
            "color" -> {
                depthBufferModes.forEachIndexed { index, mode ->
                    val descriptor = drawPassDescriptors[index].copy(
                        colorAttachments = listOf(drawPassDescriptors[index].colorAttachments[0].copy(view = attachmentView)),
                        depthStencilAttachment = drawPassDescriptors[index].depthStencilAttachment?.copy(
                            depthClearValue = depthClearValues[mode]!!
                        )
                    )
                    val colorPass = commandEncoder.beginRenderPass(descriptor)
                    colorPass.setPipeline(colorPassPipelines[mode]!!)
                    colorPass.setBindGroup(0u, uniformBindGroups[mode]!!)
                    colorPass.setVertexBuffer(0u, verticesBuffer)
                    colorPass.setViewport(
                        (window.width * index.toUInt()).toFloat() / 2f,
                        0f,
                        window.width.toFloat() / 2f,
                        window.height.toFloat(),
                        0f,
                        1f
                    )
                    colorPass.draw(geometryDrawCount.toUInt(), numInstances.toUInt())
                    colorPass.end()
                }
            }

            "precision-error" -> {
                depthBufferModes.forEachIndexed { index, mode ->
                    // Depth Pre-Pass
                    run {
                        val descriptor = depthPrePassDescriptor.copy(
                            depthStencilAttachment = depthPrePassDescriptor.depthStencilAttachment?.copy(
                                depthClearValue = depthClearValues[mode]!!
                            )
                        )
                        val depthPrePass = commandEncoder.beginRenderPass(descriptor)
                        depthPrePass.setPipeline(depthPrePassPipelines[mode]!!)
                        depthPrePass.setBindGroup(0u, uniformBindGroups[mode]!!)
                        depthPrePass.setVertexBuffer(0u, verticesBuffer)
                        depthPrePass.setViewport(
                            (window.width * index.toUInt()).toFloat() / 2f, 0f, window.width.toFloat() / 2f, window.height.toFloat(), 0f, 1f
                        )
                        depthPrePass.draw(geometryDrawCount.toUInt(), numInstances.toUInt())
                        depthPrePass.end()
                    }
                    // Precision Error Pass
                    run {
                        val descriptor = drawPassDescriptors[index].copy(
                            colorAttachments = listOf(drawPassDescriptors[index].colorAttachments[0].copy(view = attachmentView)),
                            depthStencilAttachment = drawPassDescriptors[index].depthStencilAttachment?.copy(
                                depthClearValue = depthClearValues[mode]!!
                            )
                        )
                        val precisionErrorPass = commandEncoder.beginRenderPass(descriptor)
                        precisionErrorPass.setPipeline(precisionPassPipelines[mode]!!)
                        precisionErrorPass.setBindGroup(0u, uniformBindGroups[mode]!!)
                        precisionErrorPass.setBindGroup(1u, depthTextureBindGroup)
                        precisionErrorPass.setVertexBuffer(0u, verticesBuffer)
                        precisionErrorPass.setViewport(
                            (window.width * index.toUInt()).toFloat() / 2f, 0f, window.width.toFloat() / 2f, window.height.toFloat(), 0f, 1f
                        )
                        precisionErrorPass.draw(geometryDrawCount.toUInt(), numInstances.toUInt())
                        precisionErrorPass.end()
                    }
                }
            }

            "depth-texture" -> {
                depthBufferModes.forEachIndexed { index, mode ->
                    // Depth Pre-Pass
                    run {
                        val descriptor = depthPrePassDescriptor.copy(
                            depthStencilAttachment = depthPrePassDescriptor.depthStencilAttachment?.copy(
                                depthClearValue = depthClearValues[mode]!!
                            )
                        )
                        val depthPrePass = commandEncoder.beginRenderPass(descriptor)
                        depthPrePass.setPipeline(depthPrePassPipelines[mode]!!)
                        depthPrePass.setBindGroup(0u, uniformBindGroups[mode]!!)
                        depthPrePass.setVertexBuffer(0u, verticesBuffer)
                        depthPrePass.setViewport(
                            (window.width * index.toUInt()).toFloat() / 2f, 0f, window.width.toFloat() / 2f, window.height.toFloat(), 0f, 1f
                        )
                        depthPrePass.draw(geometryDrawCount.toUInt(), numInstances.toUInt())
                        depthPrePass.end()
                    }
                    // Texture Quad Pass
                    run {
                        val descriptor = textureQuadPassDescriptors[index].copy(
                            colorAttachments = listOf(textureQuadPassDescriptors[index].colorAttachments[0].copy(view = attachmentView))
                        )
                        val depthTextureQuadPass = commandEncoder.beginRenderPass(descriptor)
                        depthTextureQuadPass.setPipeline(textureQuadPassPipline)
                        depthTextureQuadPass.setBindGroup(0u, depthTextureBindGroup)
                        depthTextureQuadPass.setViewport(
                            (window.width * index.toUInt()).toFloat() / 2f, 0f, window.width.toFloat() / 2f, window.height.toFloat(), 0f, 1f
                        )
                        depthTextureQuadPass.draw(6u) // Draw 6 vertices for the quad
                        depthTextureQuadPass.end()
                    }
                }
            }
        }

        device.queue.submit(listOf(commandEncoder.finish()))
        context.present()

        window.requestAnimationFrame(::frame)
    }

    window.requestAnimationFrame(::frame)
    window.display()
}

private fun GPUDepthStencilState.copy(depthCompare: GPUCompareFunction? = null) = DepthStencilState(
    format = format,
    depthWriteEnabled = depthWriteEnabled,
    depthCompare = depthCompare ?: this.depthCompare,
    stencilFront = stencilFront,
    stencilBack = stencilBack,
    stencilReadMask = stencilReadMask,
    stencilWriteMask = stencilWriteMask,
    depthBias = depthBias,
    depthBiasSlopeScale = depthBiasSlopeScale,
    depthBiasClamp = depthBiasClamp
)

private fun GPURenderPassColorAttachment.copy(loadOp: GPULoadOp? = null, view: GPUTextureView? = null) = RenderPassColorAttachment(
    view ?: this.view,
    loadOp ?: this.loadOp,
    storeOp,
    depthSlice,
    resolveTarget,
    clearValue
)

private fun GPURenderPassDepthStencilAttachment.copy(depthClearValue: Float? = null) = RenderPassDepthStencilAttachment(
    view,
    depthClearValue ?: this.depthClearValue,
    depthLoadOp,
    depthStoreOp,
    depthReadOnly,
    stencilClearValue,
    stencilLoadOp,
    stencilStoreOp,
    stencilReadOnly,
)
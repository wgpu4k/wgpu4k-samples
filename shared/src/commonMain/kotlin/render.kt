import io.ygdrasil.webgpu.AutoClosableContext
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.GPUTextureUsage
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.WGPUContext


fun AutoClosableContext.createScene(context: WGPUContext): RotatingCubeScene {

    val alphaMode = CompositeAlphaMode.Inherit?.takeIf { context.surface.supportedAlphaMode.contains(it) }
        ?: CompositeAlphaMode.Opaque

    context.surface
        .configure(
            SurfaceConfiguration(
                device = context.device,
                format = context.renderingContext.textureFormat,
                usage = setOf(GPUTextureUsage.RenderAttachment),
                alphaMode = alphaMode
            )
        )

    return RotatingCubeScene(context).also { scene ->
        context.bind()
        scene.bind()
        scene.initialize()
    }

}


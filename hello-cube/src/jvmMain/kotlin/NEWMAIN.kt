import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import darwin.CAMetalLayer
import darwin.NSWindow
import ffi.LibraryLoader
import io.ygdrasil.webgpu.*
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow
import org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandDisplay
import org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandWindow
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window
import org.lwjgl.system.MemoryUtil.NULL
import org.rococoa.ID
import org.rococoa.Rococoa
import java.util.LinkedList
import java.util.Queue

class GlfwWebgpuWindow {
    init {
        LibraryLoader.load()
        initLog()
        glfwInit()
        // WGPU will show the window at the right timing
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        // Disable context creation, WGPU will manage that
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
    }

    //TODO: better align API so we can access the adapter from the main code
    // TODO: make sure to close things

    val windowHandler = glfwCreateWindow(800, 600, "Hello cube", NULL, NULL)
    val wgpu = WGPU.createInstance() ?: error("fail to wgpu instance")
    val nativeSurface = wgpu.getNativeSurface(windowHandler)
    val surface = Surface(nativeSurface, windowHandler)
    val adapter = wgpu.requestAdapter(nativeSurface) ?: error("fail to get adapter")

    init {
        glfwShowWindow(windowHandler)
    }

    fun getPresentationFormat(): GPUTextureFormat {
        nativeSurface.computeSurfaceCapabilities(adapter)
        return surface.supportedFormats.first()
    }

    fun getWebgpuContext() = surface

    private val requestedFrames: Queue<() -> Unit> = LinkedList()

    fun requestAnimationFrame(frame: () -> Unit) {
        requestedFrames.add(frame)
    }

    fun display() {
        while (!glfwWindowShouldClose(windowHandler)) {
            glfwPollEvents()
            if(requestedFrames.isNotEmpty()) {
                val nextFrame = requestedFrames.remove()
                nextFrame()
            }
        }
    }
}


fun main() {
    val window = GlfwWebgpuWindow()

    val device = runBlocking { window.adapter.requestDevice().getOrThrow() }

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
    )

    val pipelineLayout = device.createPipelineLayout(
        PipelineLayoutDescriptor(bindGroupLayouts = listOf())
    )

    val presentationFormat = window.getPresentationFormat()


    val pipeline = device.createRenderPipeline(
        RenderPipelineDescriptor(
            layout = pipelineLayout,
            vertex = VertexState(
                module = shader,
                entryPoint = "vs_main"
            ),
            fragment = FragmentState(
                module = shader, targets = listOf(
                    ColorTargetState(
                        presentationFormat
                    )
                ),
                entryPoint = "fs_main"
            ),
            primitive = PrimitiveState(
                topology = GPUPrimitiveTopology.TriangleList
            )
        )
    )

    val context = window.getWebgpuContext()

    context.configure(
        SurfaceConfiguration(
            device, format = presentationFormat
        )
    )



    fun frame() {
        val commandEncoder = device.createCommandEncoder()
        val frame = context.getCurrentTexture()
        val view = frame.texture.createView()
        val renderPassDescriptor = RenderPassDescriptor(
            colorAttachments = listOf(
                RenderPassColorAttachment(
                    view = view,
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

        Thread.sleep(30)
    }

    window.requestAnimationFrame(::frame)
    window.display()
}

internal enum class Os {
    Linux,
    Window,
    MacOs
}

internal object Platform {
    val os: Os
        get() = System.getProperty("os.name").let { name ->
            when {
                arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } -> Os.Linux
                arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> Os.MacOs
                arrayOf("Windows").any { name.startsWith(it) } -> Os.Window
                else -> error("Unrecognized or unsupported operating system.")
            }
        }

}


private fun WGPU.getNativeSurface(window: Long): NativeSurface = when (Platform.os) {
    Os.Linux -> when {
        glfwGetWaylandWindow(window) == 0L -> {
            println("running on X11")
            val display = glfwGetX11Display().toNativeAddress()
            val x11_window = glfwGetX11Window(window).toULong()
            getSurfaceFromX11Window(display, x11_window) ?: error("fail to get surface on Linux")
        }

        else -> {
            println("running on Wayland")
            val display = glfwGetWaylandDisplay().toNativeAddress()
            val wayland_window = glfwGetWaylandWindow(window).toNativeAddress()
            getSurfaceFromWaylandWindow(display, wayland_window)
        }
    }

    Os.Window -> {
        val hwnd = glfwGetWin32Window(window).toNativeAddress()
        val hinstance = Kernel32.INSTANCE.GetModuleHandle(null).pointer.toNativeAddress()
        getSurfaceFromWindows(hinstance, hwnd) ?: error("fail to get surface on Windows")
    }

    Os.MacOs -> {
        val nsWindowPtr = glfwGetCocoaWindow(window)
        val nswindow = Rococoa.wrap(ID.fromLong(nsWindowPtr), NSWindow::class.java)
        nswindow.contentView()?.setWantsLayer(true)
        val layer = CAMetalLayer.layer()
        nswindow.contentView()?.setLayer(layer.id().toLong().toPointer())
        getSurfaceFromMetalLayer(layer.id().toLong().toNativeAddress())
    }
} ?: error("fail to get surface")


private fun Long.toPointer(): Pointer = Pointer(this)


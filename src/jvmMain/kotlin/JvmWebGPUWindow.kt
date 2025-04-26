import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import darwin.CAMetalLayer
import darwin.NSWindow
import ffi.LibraryLoader
import ffi.globalMemory
import io.ygdrasil.webgpu.*
import io.ygdrasil.wgpu.WGPULogCallback
import io.ygdrasil.wgpu.WGPULogLevel_Trace
import io.ygdrasil.wgpu.wgpuSetLogCallback
import io.ygdrasil.wgpu.wgpuSetLogLevel
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
import java.util.*
import java.util.Queue

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WebGPUWindow actual constructor(actual val fps: Int, actual val width: UInt, actual val height: UInt, actual val title: String) {
    init {
        LibraryLoader.load()
        wgpuSetLogLevel(WGPULogLevel_Trace)
        val callback = WGPULogCallback.allocate(globalMemory) { level, cMessage, userdata ->
            val message = cMessage?.data?.toKString(cMessage.length) ?: "empty message"
            println("$level: $message")
        }
        wgpuSetLogCallback(callback, globalMemory.bufferOfAddress(callback.handler).handler)
        glfwInit()
        // WGPU will show the window at the right timing
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        // Disable context creation, WGPU will manage that
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
    }

    // TODO: make sure to close things

    val windowHandler = glfwCreateWindow(width.toInt(), height.toInt(), title, NULL, NULL)
    val gpu = WGPU.createInstance() ?: error("failed to create wgpu instance")
    val nativeSurface = gpu.getNativeSurface(windowHandler)

    actual fun requestAdapter(powerPreference: GPUPowerPreference?): Adapter = gpu.requestAdapter(nativeSurface, powerPreference)
        ?: error("failed to get WebGPU adapter")

    init {
        glfwShowWindow(windowHandler)
    }

    actual fun getPresentationFormat(adapter: Adapter): GPUTextureFormat {
        nativeSurface.computeSurfaceCapabilities(adapter)
        return nativeSurface.supportedFormats.first()
    }

    actual fun getWebGPUContext() = nativeSurface

    private val requestedFrames: Queue<() -> Unit> = LinkedList()

    actual fun requestAnimationFrame(frame: () -> Unit) {
        requestedFrames.add(frame)
    }

    private var lastFrameTimeNano = 0L

    actual fun display() {
        while (!glfwWindowShouldClose(windowHandler)) {
            glfwPollEvents()
            val time = System.nanoTime()
            val delta = time - lastFrameTimeNano
            if (delta >= 1e9 / fps) {
                lastFrameTimeNano = time
                if (requestedFrames.isNotEmpty()) {
                    val nextFrame = requestedFrames.remove()
                    nextFrame()
                }
            }
        }
    }
}

private enum class Os {
    Linux,
    Window,
    MacOs
}


private val os = System.getProperty("os.name").let { name ->
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } -> Os.Linux
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> Os.MacOs
        arrayOf("Windows").any { name.startsWith(it) } -> Os.Window
        else -> error("Unrecognized or unsupported operating system.")
    }
}

private fun WGPU.getNativeSurface(window: Long): NativeSurface = when (os) {
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

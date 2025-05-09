# wgpu4k Samples

This project aims to provide a starting point for learning to use WGPU4K, 
bringing in samples such as the [Javascript WebGPU samples](https://webgpu.github.io/webgpu-samples/), but rewritten in Kotlin.
It uses [wgpu4k-matrix](https://github.com/natanfudge/wgpu4k-matrix), similarly to how the Javascript samples use `wgpu-matrix`,
to avoid reimplementing matrices for every example.
# Running Samples
Simply run `gradlew jvmRun -DmainClass=samples.HelloTriangleKt`, and replace `HelloTriangle` with your desired sample.
You can also run the `main` functions directly in IntelliJ. 
# Status
This project is in **alpha**.
## Limitations
- Currently you can only run the samples on the JVM, but the samples themselves will work in a multiplatform project. 
Consequentially, other platforms could be trivially added. Contributions are welcome in this area.
- The samples are almost directly converted from Javascript, which means code quality is poor. 
This isn't a bad thing per se, as the goal is to stay faithful to the original samples. 
- Some samples are missing (contributions welcome). 
- There is currently no way to add GUIs to the samples like in JS. 
# Available Samples
### Basic Graphics
- [x] HelloTriangle
- [x] HelloTriangleMSAA
- [x] RotatingCube
- [x] TwoCubes
- [x] TexturedCube
- [x] InstancedCube
- [x] FractalCube
- [x] Cubemap
### WebGPU Features
- [x] reversedZ (partially, requires GUI)
- [ ] RenderBundles
- [ ] OcclusionQuery
- [ ] SamplerParameters
- [ ] TimestampQuery
- [ ] Blending
### GPGPU Demos
- [ ] ComputeBoids
- [ ] GameOfLife
- [ ] BitonicSort
### Graphics Techniques
- [ ] Cameras
- [ ] NormalMap
- [ ] ShadowMapping
- [ ] DeferredRendering
- [ ] Particles (HDR)
- [ ] Points
- [ ] ImageBlur
- [ ] GenerateMipmap
- [ ] Cornell
- [ ] A-Buffer
- [ ] SkinnedMesh
- [ ] StencilMask
- [ ] TextRenderingMsdf
- [ ] VolumeRenderingTexture3D
- [ ] Wireframe
### External Samples
- [ ] BundleCulling
- [ ] Metaballs
- [ ] PristineGrid
- [ ] ClusteredShading
- [ ] Spookyball
- [ ] MarchingCubes
- [ ] AlphaToCoverageEmulator

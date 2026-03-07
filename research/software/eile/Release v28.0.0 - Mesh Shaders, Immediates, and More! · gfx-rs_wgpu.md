---
title: "Release v28.0.0 - Mesh Shaders, Immediates, and More! · gfx-rs/wgpu"
source: "https://github.com/gfx-rs/wgpu/releases/tag/v28.0.0"
author:
  - "[[GitHub]]"
published:
created: 2025-12-20
description: "A cross-platform, safe, pure-Rust graphics API. Contribute to gfx-rs/wgpu development by creating an account on GitHub."
tags:
  - "clippings"
---
## v28.0.0 - Mesh Shaders, Immediates, and More!

[Latest](https://github.com/gfx-rs/wgpu/releases/latest)

[Latest](https://github.com/gfx-rs/wgpu/releases/latest)

[cwfitzgerald](https://github.com/cwfitzgerald) released this

· [7 commits](https://github.com/gfx-rs/wgpu/compare/v28.0.0...trunk) to trunk since this release

[v28.0.0](https://github.com/gfx-rs/wgpu/tree/v28.0.0)

[`3f02781`](https://github.com/gfx-rs/wgpu/commit/3f02781bb5a0a1fe1922ea36c9bdacf9792abcbc)

### Major Changes

#### Mesh Shaders

This has been a long time coming. See [the tracking issue](https://github.com/gfx-rs/wgpu/issues/7197) for more information.  
They are now fully supported on Vulkan, and supported on Metal and DX12 with passthrough shaders. WGSL parsing and rewriting  
is supported, meaning they can be used through WESL or naga\_oil.

Mesh shader pipelines replace the standard vertex shader pipelines and allow new ways to render meshes.  
They are ideal for meshlet rendering, a form of rendering where small groups of triangles are handled together,  
for both culling and rendering.

They are compute-like shaders, and generate primitives which are passed directly to the rasterizer, rather  
than having a list of vertices generated individually and then using a static index buffer. This means that certain computations  
on nearby groups of triangles can be done together, the relationship between vertices and primitives is more programmable, and  
you can even pass non-interpolated per-primitive data to the fragment shader, independent of vertices.

Mesh shaders are very versatile, and are powerful enough to replace vertex shaders, tesselation shaders, and geometry shaders  
on their own or with task shaders.

A full example of mesh shaders in use can be seen in the `mesh_shader` example. For the full specification of mesh shaders in wgpu, go to [docs/api-specs/mesh\_shading.md](https://github.com/gfx-rs/wgpu/blob/v28.0.0/docs/api-specs/mesh_shading.md). Below is a small snippet of shader code demonstrating their usage:

```
@task
@payload(taskPayload)
@workgroup_size(1)
fn ts_main() -> @builtin(mesh_task_size) vec3<u32> {
    // Task shaders can use workgroup variables like compute shaders
    workgroupData = 1.0;
    // Pass some data to all mesh shaders dispatched by this workgroup
    taskPayload.colorMask = vec4(1.0, 1.0, 0.0, 1.0);
    taskPayload.visible = 1;
    // Dispatch a mesh shader grid with one workgroup
    return vec3(1, 1, 1);
}

@mesh(mesh_output)
@payload(taskPayload)
@workgroup_size(1)
fn ms_main(@builtin(local_invocation_index) index: u32, @builtin(global_invocation_id) id: vec3<u32>) {
    // Set how many outputs this workgroup will generate
    mesh_output.vertex_count = 3;
    mesh_output.primitive_count = 1;
    // Can also use workgroup variables
    workgroupData = 2.0;

    // Set vertex outputs
    mesh_output.vertices[0].position = positions[0];
    mesh_output.vertices[0].color = colors[0] * taskPayload.colorMask;

    mesh_output.vertices[1].position = positions[1];
    mesh_output.vertices[1].color = colors[1] * taskPayload.colorMask;

    mesh_output.vertices[2].position = positions[2];
    mesh_output.vertices[2].color = colors[2] * taskPayload.colorMask;
    
    // Set the vertex indices for the only primitive
    mesh_output.primitives[0].indices = vec3<u32>(0, 1, 2);
    // Cull it if the data passed by the task shader says to
    mesh_output.primitives[0].cull = taskPayload.visible == 1;
    // Give a noninterpolated per-primitive vec4 to the fragment shader
    mesh_output.primitives[0].colorMask = vec4<f32>(1.0, 0.0, 1.0, 1.0);
}
```

##### Thanks

This was a monumental effort from many different people, but it was championed by [@inner-daemons](https://github.com/inner-daemons), without whom it would not have happened.  
Thank you [@cwfitzgerald](https://github.com/cwfitzgerald) for doing the bulk of the code review. Finally thank you [@ColinTimBarndt](https://github.com/ColinTimBarndt) for coordinating the testing effort.

Reviewers:

- [@cwfitzgerald](https://github.com/cwfitzgerald)
- [@jimblandy](https://github.com/jimblandy)
- [@ErichDonGubler](https://github.com/ErichDonGubler)

`wgpu` Contributions:

- Metal implementation in wgpu-hal. By [@inner-daemons](https://github.com/inner-daemons) in [#8139](https://github.com/gfx-rs/wgpu/pull/8139).
- DX12 implementation in wgpu-hal. By [@inner-daemons](https://github.com/inner-daemons) in [#8110](https://github.com/gfx-rs/wgpu/pull/8110).
- Vulkan implementation in wgpu-hal. By [@inner-daemons](https://github.com/inner-daemons) in [#7089](https://github.com/gfx-rs/wgpu/pull/7089).
- wgpu/wgpu-core implementation. By [@inner-daemons](https://github.com/inner-daemons) in [#7345](https://github.com/gfx-rs/wgpu/pull/7345).
- New mesh shader limits and validation. By [@inner-daemons](https://github.com/inner-daemons) in [#8507](https://github.com/gfx-rs/wgpu/pull/8507).

`naga` Contributions:

- Naga IR implementation. By [@inner-daemons](https://github.com/inner-daemons) in [#8104](https://github.com/gfx-rs/wgpu/pull/8104).
- `wgsl-in` implementation in naga. By [@inner-daemons](https://github.com/inner-daemons) in [#8370](https://github.com/gfx-rs/wgpu/pull/8370).
- `spv-out` implementation in naga. By [@inner-daemons](https://github.com/inner-daemons) in [#8456](https://github.com/gfx-rs/wgpu/pull/8456).
- `wgsl-out` implementation in naga. By [@Slightlyclueless](https://github.com/Slightlyclueless) in [#8481](https://github.com/gfx-rs/wgpu/pull/8481).
- Allow barriers in mesh/task shaders. By [@inner-daemons](https://github.com/inner-daemons) in [#8749](https://github.com/gfx-rs/wgpu/pull/8749)

Testing Assistance:

- [@ColinTimBarndt](https://github.com/ColinTimBarndt)
- [@AdamK2003](https://github.com/AdamK2003)
- [@Mhowser](https://github.com/Mhowser)
- [@9291Sam](https://github.com/9291Sam)
- 3 more testers who wished to remain anonymous.

Thank you to everyone to made this happen!

#### Switch from gpu-alloc to gpu-allocator in the vulkan backend

`gpu-allocator` is the allocator used in the `dx12` backend, allowing to configure  
the allocator the same way in those two backends converging their behavior.

This also brings the `Device::generate_allocator_report` feature to  
the vulkan backend.

By [@DeltaEvo](https://github.com/DeltaEvo) in [#8158](https://github.com/gfx-rs/wgpu/pull/8158).

#### wgpu::Instance::enumerate\_adapters is now async & available on WebGPU

BREAKING CHANGE: `enumerate_adapters` is now `async`:

```
- pub fn enumerate_adapters(&self, backends: Backends) -> Vec<Adapter> {
+ pub fn enumerate_adapters(&self, backends: Backends) -> impl Future<Output = Vec<Adapter>> {
```

This yields two benefits:

- This method is now implemented on non-native using the standard `Adapter::request_adapter(…)`, making `enumerate_adapters` a portable surface. This was previously a nontrivial pain point when an application wanted to do some of its own filtering of adapters.
- This method can now be implemented in custom backends.

By [@R-Cramer4](https://github.com/R-Cramer4) in [#8230](https://github.com/gfx-rs/wgpu/pull/8230)

#### New LoadOp::DontCare

In the case where a renderpass unconditionally writes to all pixels in the rendertarget,  
`Load` can cause unnecessary memory traffic, and `Clear` can spend time unnecessarily  
clearing the rendertargets. `DontCare` is a new `LoadOp` which will leave the contents  
of the rendertarget undefined. Because this could lead to undefined behavior, this API  
requires that the user gives an unsafe token to use the api.

While you can use this unconditionally, on platforms where `DontCare` is not available,  
it will internally use a different load op.

```
load: LoadOp::DontCare(unsafe { wgpu::LoadOpDontCare::enabled() })
```

By [@cwfitzgerald](https://github.com/cwfitzgerald) in [#8549](https://github.com/gfx-rs/wgpu/pull/8549)

#### MipmapFilterMode is split from FilterMode

This is a breaking change that aligns wgpu with spec.

```
SamplerDescriptor {
...
-     mipmap_filter: FilterMode::Nearest
+     mipmap_filter: MipmapFilterMode::Nearest
...
}
```

By [@sagudev](https://github.com/sagudev) in [#8314](https://github.com/gfx-rs/wgpu/pull/8314).

#### Multiview on all major platforms and support for multiview bitmasks

Multiview is a feature that allows rendering the same content to multiple layers of a texture.  
This is useful primarily in VR where you wish to display almost identical content to 2 views,  
just with a different perspective. Instead of using 2 draw calls or 2 instances for each object, you  
can use this feature.

Multiview is also called view instancing in DX12 or vertex amplification in Metal.

Multiview has been reworked, adding support for Metal and DX12, and adding testing and validation to wgpu itself.  
This change also introduces a view bitmask, a new field in `RenderPassDescriptor` that allows a render pass to render  
to multiple non-adjacent layers when using the `SELECTIVE_MULTIVIEW` feature. If you don't use multi-view,  
you can set this field to none.

```
- wgpu::RenderPassDescriptor {
-     label: None,
-     color_attachments: &color_attachments,
-     depth_stencil_attachment: None,
-     timestamp_writes: None,
-     occlusion_query_set: None,
- }
+ wgpu::RenderPassDescriptor {
+     label: None,
+     color_attachments: &color_attachments,
+     depth_stencil_attachment: None,
+     timestamp_writes: None,
+     occlusion_query_set: None,
+     multiview_mask: NonZero::new(3),
+ }
```

One other breaking change worth noting is that in WGSL `@builtin(view_index)` now requires a type of `u32`, where previously it required `i32`.

By [@inner-daemons](https://github.com/inner-daemons) in [#8206](https://github.com/gfx-rs/wgpu/pull/8206).

#### Error scopes now use guards and are thread-local.

```
- device.push_error_scope(wgpu::ErrorFilter::Validation);
+ let scope = device.push_error_scope(wgpu::ErrorFilter::Validation);
  // ... perform operations on the device ...
- let error: Option<Error> = device.pop_error_scope().await;
+ let error: Option<Error> = scope.pop().await;
```

Device error scopes now operate on a per-thread basis. This allows them to be used easily within multithreaded contexts,  
without having the error scope capture errors from other threads.

When the `std` feature is **not** enabled, we have no way to differentiate between threads, so error scopes return to be  
global operations.

By [@cwfitzgerald](https://github.com/cwfitzgerald) in [#8685](https://github.com/gfx-rs/wgpu/pull/8685)

#### Log Levels

We have received complaints about wgpu being way too log spammy at log levels `info` / `warn` / `error`. We have  
adjusted our log policy and changed logging such that `info` and above should be silent unless some exceptional  
event happens. Our new log policy is as follows:

- Error: if we can’t (for some reason, usually a bug) communicate an error any other way.
- Warning: similar, but there may be one-shot warnings about almost certainly sub-optimal.
- Info: do not use
- Debug: Used for interesting events happening inside wgpu.
- Trace: Used for all events that might be useful to either `wgpu` or application developers.

By [@cwfitzgerald](https://github.com/cwfitzgerald) in [#8579](https://github.com/gfx-rs/wgpu/pull/8579).

#### Push constants renamed immediates, API brought in line with spec.

As the "immediate data" api is getting close to stabilization in the WebGPU specification,  
we're bringing our implementation in line with what the spec dictates.

First, in the `PipelineLayoutDescriptor`, you now pass a unified size for all stages:

```
- push_constant_ranges: &[wgpu::PushConstantRange {
-     stages: wgpu::ShaderStages::VERTEX_FRAGMENT,
-     range: 0..12,
- }]
+ immediate_size: 12,
```

Second, on the command encoder you no longer specify a shader stage, uploads apply  
to all shader stages that use immediate data.

```
- rpass.set_push_constants(wgpu::ShaderStages::FRAGMENT, 0, bytes);
+ rpass.set_immediates(0, bytes);
```

Third, immediates are now declared with the `immediate` address space instead of  
the `push_constant` address space. Due to a [known issue on DX12](https://github.com/gfx-rs/wgpu/issues/5683)  
it is advised to always use a structure for your immediates until that issue  
is fixed.

```
- var<push_constant> my_pc: MyPushConstant;
+ var<immediate> my_imm: MyImmediate;
```

Finally, our implementation currently still zero-initializes the immediate data  
range you declared in the pipeline layout. This is not spec compliant and failing  
to populate immediate "slots" that are used in the shader will be a validation error  
in a future version. See [the proposal](https://github.com/gpuweb/gpuweb/blob/main/proposals/immediate-data.md#immediate-slots) for details for determining  
which slots are populated in a given shader.

By [@cwfitzgerald](https://github.com/cwfitzgerald) in [#8724](https://github.com/gfx-rs/wgpu/pull/8724).

#### subgroup\_{min,max}\_size renamed and moved from Limits -> AdapterInfo

To bring our code in line with the WebGPU spec, we have moved information about subgroup size  
from limits to adapter info. Limits was not the correct place for this anyway, and we had some  
code special casing those limits.

Additionally we have renamed the fields to match the spec.

```
- let min = limits.min_subgroup_size;
+ let min = info.subgroup_min_size;
- let max = limits.max_subgroup_size;
+ let max = info.subgroup_max_size;
```

By [@cwfitzgerald](https://github.com/cwfitzgerald) in [#8609](https://github.com/gfx-rs/wgpu/pull/8609).

### New Features

- Added support for transient textures on Vulkan and Metal. By [@opstic](https://github.com/opstic) in [#8247](https://github.com/gfx-rs/wgpu/pull/8247)
- Implement shader triangle barycentric coordinate builtins. By [@atlv24](https://github.com/atlv24) in [#8320](https://github.com/gfx-rs/wgpu/pull/8320).
- Added support for binding arrays of storage textures on Metal. By [@msvbg](https://github.com/msvbg) in [#8464](https://github.com/gfx-rs/wgpu/pull/8464)
- Added support for multisampled texture arrays on Vulkan through adapter feature `MULTISAMPLE_ARRAY`. By [@LaylBongers](https://github.com/LaylBongers) in [#8571](https://github.com/gfx-rs/wgpu/pull/8571).
- Added `get_configuration` to `wgpu::Surface`, that returns the current configuration of `wgpu::Surface`. By [@sagudev](https://github.com/sagudev) in [#8664](https://github.com/gfx-rs/wgpu/pull/8664).
- Add `wgpu_core::Global::create_bind_group_layout_error`. By [@ErichDonGubler](https://github.com/ErichDonGubler) in [#8650](https://github.com/gfx-rs/wgpu/pull/8650).

### Changes

#### General

- Require new enable extensions when using ray queries and position fetch (`wgpu_ray_query`, `wgpu_ray_query_vertex_return`). By [@Vecvec](https://github.com/Vecvec) in [#8545](https://github.com/gfx-rs/wgpu/pull/8545).
- Texture now has `from_custom`. By [@R-Cramer4](https://github.com/R-Cramer4) in [#8315](https://github.com/gfx-rs/wgpu/pull/8315).
- Using both the wgpu command encoding APIs and `CommandEncoder::as_hal_mut` on the same encoder will now result in a panic.
- Allow `include_spirv!` and `include_spirv_raw!` macros to be used in constants and statics. By [@clarfonthey](https://github.com/clarfonthey) in [#8250](https://github.com/gfx-rs/wgpu/pull/8250).
- Added support for rendering onto multi-planar textures. By [@noituri](https://github.com/noituri) in [#8307](https://github.com/gfx-rs/wgpu/pull/8307).
- Validation errors from `CommandEncoder::finish()` will report the label of the invalid encoder. By [@kpreid](https://github.com/kpreid) in [#8449](https://github.com/gfx-rs/wgpu/pull/8449).
- Corrected documentation of the minimum alignment of the *end* of a mapped range of a buffer (it is 4, not 8). By [@kpreid](https://github.com/kpreid) in [#8450](https://github.com/gfx-rs/wgpu/pull/8450).
- `util::StagingBelt` now takes a `Device` when it is created instead of when it is used. By [@kpreid](https://github.com/kpreid) in [#8462](https://github.com/gfx-rs/wgpu/pull/8462).
- `wgpu_hal::vulkan::Texture` API changes to handle externally-created textures and memory more flexibly. By [@s-ol](https://github.com/s-ol) in [#8512](https://github.com/gfx-rs/wgpu/pull/8512), [#8521](https://github.com/gfx-rs/wgpu/pull/8521).
- Render passes are now validated against the `maxColorAttachmentBytesPerSample` limit. By [@andyleiserson](https://github.com/andyleiserson) in [#8697](https://github.com/gfx-rs/wgpu/pull/8697).

#### Metal

- Expose render layer. By [@xiaopengli89](https://github.com/xiaopengli89) in [#8707](https://github.com/gfx-rs/wgpu/pull/8707)
- `MTLDevice` is thread-safe. By [@uael](https://github.com/uael) in [#8168](https://github.com/gfx-rs/wgpu/pull/8168)

#### naga

- Prevent UB with invalid ray query calls on spirv. By [@Vecvec](https://github.com/Vecvec) in [#8390](https://github.com/gfx-rs/wgpu/pull/8390).
- Update the set of binding\_array capabilities. In most cases, they are set automatically from `wgpu` features, and this change should not be user-visible. By [@andyleiserson](https://github.com/andyleiserson) in [#8671](https://github.com/gfx-rs/wgpu/pull/8671).
- Naga now accepts the `var<function>` syntax for declaring local variables. By [@andyleiserson](https://github.com/andyleiserson) in [#8710](https://github.com/gfx-rs/wgpu/pull/8710).

### Bug Fixes

#### General

- Fixed a bug where mapping sub-ranges of a buffer on web would fail with `OperationError: GPUBuffer.getMappedRange: GetMappedRange range extends beyond buffer's mapped range`. By [@ryankaplan](https://github.com/ryankaplan) in [#8349](https://github.com/gfx-rs/wgpu/pull/8349)
- Reject fragment shader output `location` s > `max_color_attachments` limit. By [@ErichDonGubler](https://github.com/ErichDonGubler) in [#8316](https://github.com/gfx-rs/wgpu/pull/8316).
- WebGPU device requests now support the required limits `maxColorAttachments` and `maxColorAttachmentBytesPerSample`. By [@evilpie](https://github.com/evilpie) in [#8328](https://github.com/gfx-rs/wgpu/pull/8328)
- Reject binding indices that exceed `wgpu_types::Limits::max_bindings_per_bind_group` when deriving a bind group layout for a pipeline. By [@jimblandy](https://github.com/jimblandy) in [#8325](https://github.com/gfx-rs/wgpu/pull/8325).
- Removed three features from `wgpu-hal` which did nothing useful: `"cargo-clippy"`, `"gpu-allocator"`, and `"rustc-hash"`. By [@kpreid](https://github.com/kpreid) in [#8357](https://github.com/gfx-rs/wgpu/pull/8357).
- `wgpu_types::PollError` now always implements the `Error` trait. By [@kpreid](https://github.com/kpreid) in [#8384](https://github.com/gfx-rs/wgpu/pull/8384).
- The texture subresources used by the color attachments of a render pass are no longer allowed to overlap when accessed via different texture views. By [@andyleiserson](https://github.com/andyleiserson) in [#8402](https://github.com/gfx-rs/wgpu/pull/8402).
- The `STORAGE_READ_ONLY` texture usage is now permitted to coexist with other read-only usages. By [@andyleiserson](https://github.com/andyleiserson) in [#8490](https://github.com/gfx-rs/wgpu/pull/8490).
- Validate that buffers are unmapped in `write_buffer` calls. By [@ErichDonGubler](https://github.com/ErichDonGubler) in [#8454](https://github.com/gfx-rs/wgpu/pull/8454).
- Shorten critical section inside present such that the snatch write lock is no longer held during present, preventing other work happening on other threads. By [@cwfitzgerald](https://github.com/cwfitzgerald) in [#8608](https://github.com/gfx-rs/wgpu/pull/8608).

#### naga

- The `||` and `&&` operators now "short circuit", i.e., do not evaluate the RHS if the result can be determined from just the LHS. By [@andyleiserson](https://github.com/andyleiserson) in [#7339](https://github.com/gfx-rs/wgpu/pull/7339).
- Fix a bug that resulted in the Metal error `program scope variable must reside in constant address space` in some cases. By [@teoxoy](https://github.com/teoxoy) in [#8311](https://github.com/gfx-rs/wgpu/pull/8311).
- Handle `rayQueryTerminate` in spv-out instead of ignoring it. By [@Vecvec](https://github.com/Vecvec) in [#8581](https://github.com/gfx-rs/wgpu/pull/8581).

#### DX12

- Align copies b/w textures and buffers via a single intermediate buffer per copy when `D3D12_FEATURE_DATA_D3D12_OPTIONS13.UnrestrictedBufferTextureCopyPitchSupported` is `false`. By [@ErichDonGubler](https://github.com/ErichDonGubler) in [#7721](https://github.com/gfx-rs/wgpu/pull/7721).
- Fix detection of Int64 Buffer/Texture atomic features. By [@cwfitzgerald](https://github.com/cwfitzgerald) in [#8667](https://github.com/gfx-rs/wgpu/pull/8667).

#### Vulkan

- Fixed a validation error regarding atomic memory semantics. By [@atlv24](https://github.com/atlv24) in [#8391](https://github.com/gfx-rs/wgpu/pull/8391).

#### Metal

- Fixed a variety of feature detection related bugs. By [@inner-daemons](https://github.com/inner-daemons) in [#8439](https://github.com/gfx-rs/wgpu/pull/8439).

#### WebGPU

- Fixed a bug where the texture aspect was not passed through when calling `copy_texture_to_buffer` in WebGPU, causing the copy to fail for depth/stencil textures. By [@Tim-Evans-Seequent](https://github.com/Tim-Evans-Seequent) in [#8445](https://github.com/gfx-rs/wgpu/pull/8445).

#### GLES

- Fix race when downloading texture from compute shader pass. By [@SpeedCrash100](https://github.com/SpeedCrash100) in [#8527](https://github.com/gfx-rs/wgpu/pull/8527)
- Fix double window class registration when dynamic libraries are used. By [@Azorlogh](https://github.com/Azorlogh) in [#8548](https://github.com/gfx-rs/wgpu/pull/8548)
- Fix context loss on device initialization on GL3.3-4.1 contexts. By [@cwfitzgerald](https://github.com/cwfitzgerald) in [#8674](https://github.com/gfx-rs/wgpu/pull/8674).
- `VertexFormat::Unorm10_10_10_2` can now be used on `gl` backends. By [@mooori](https://github.com/mooori) in [#8717](https://github.com/gfx-rs/wgpu/pull/8717).

#### hal

- `DropCallback` s are now called after dropping all other fields of their parent structs. By [@jerzywilczek](https://github.com/jerzywilczek) in [#8353](https://github.com/gfx-rs/wgpu/pull/8353)
# Mod Texture Super Resolution - Improvement & Roadmap Plan

This document details recommended architectural enhancements, performance optimizations, feature additions, and UI improvements for the **Mod Texture Super Resolution** (MTSR) mod.

---

## 1. User Experience & Gameplay Integration

### 🔔 1.1 In-Game Toast & Notification Prompt
* **Current State**: During atlas stitching, [`AtlasSpriteUpscaler.java`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/upscale/AtlasSpriteUpscaler.java#L101-L105) enqueues missing textures to the background daemon thread while falling back to low-resolution originals. When upscaling finishes in the background, there is no in-game feedback informing the player that a resource reload will load the upscaled textures.
* **Proposed Enhancement**:
  - Add a completion listener in [`UpscaleManager.java`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/upscale/UpscaleManager.java).
  - When all enqueued batch items are processed, display a Minecraft System Toast or Actionbar message:
    > *"Textures upscaled in background! Press F3 + T to load high-resolution textures."*

### 🎬 1.2 Animated Texture Super Resolution
* **Current State**: [`AtlasSpriteUpscaler.java`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/upscale/AtlasSpriteUpscaler.java#L86-L89) explicitly skips textures containing `AnimationMetadataSection` to prevent frame metadata mismatch.
* **Proposed Enhancement**:
  - Extract individual vertical animation frames from sprite strip textures.
  - Upscale each frame independently or tile-stitch the strip.
  - Update `FrameSize` and scaling parameters in `AnimationMetadataSection` / `SpriteContents`.

### ⚙️ 1.3 Custom Mod Whitelist & Blacklist Configuration
* **Current State**: [`TextureDetector.EXCLUDED_NAMESPACES`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/upscale/detect/TextureDetector.java#L13) hardcodes `Set.of("minecraft", "realms", "mtsr")`.
* **Proposed Enhancement**:
  - Implement a persistent JSON / `owo-lib` config option allowing players to exclude specific mod namespaces (e.g. `optifine`, `sodium`) or force-include specific resource paths.

---

## 2. Performance & Hardware Acceleration

### ⚡ 2.1 GPU Acceleration (DirectML / CUDA)
* **Current State**: [`EsrganModel.java`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/upscale/model/EsrganModel.java#L47-L50) initializes ONNX Runtime with standard CPU execution provider (`OrtSession.SessionOptions`).
* **Proposed Enhancement**:
  - Add optional DirectML (Windows DirectX 12) or CUDA execution provider support.
  - Utilizing GPU acceleration via DirectML increases inference speed by **5x–20x** on modern graphics hardware (NVIDIA, AMD, Intel).

### 🧵 2.2 Multi-Threaded Worker Pool
* **Current State**: [`UpscaleManager.java`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/upscale/UpscaleManager.java#L47-L52) uses a single-threaded executor (`Executors.newSingleThreadExecutor()`).
* **Proposed Enhancement**:
  - Support configurable multi-threaded batch upscaling (e.g., `Math.max(1, Runtime.getRuntime().availableProcessors() / 2)` threads) to process initial texture loads significantly faster on multi-core CPUs.

### 🧹 2.3 Native Resource Lifecycle Management
* **Current State**: `OrtEnvironment.getEnvironment()` is retrieved in `EsrganModel.load()` without explicit tracking or closure when the game shuts down or when models are swapped.
* **Proposed Enhancement**:
  - Implement proper lifecycle tracking and cleanup for `OrtEnvironment` and `OrtSession` within `ModelManager.close()`.

---

## 3. Model Management & GUI Capabilities

### 🎛️ 3.1 Active Model Selector & Hot-Swapping
* **Current State**: [`ModelManager.java`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/upscale/model/ModelManager.java#L93-L103) automatically picks the first `.onnx` model found alphabetically. [`ModelSettingsWindow.java`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/config/components/ModelSettingsWindow.java) lists models read-only.
* **Proposed Enhancement**:
  - Add an active model dropdown in `ModelSettingsWindow` so users can switch models (e.g. 2x vs 4x or general vs anime models) dynamically without renaming files.
  - Add a "Clear & Re-upscale" button to trigger a cache invalidation upon changing models.

### 📊 3.2 Live Activity Log Stream
* **Current State**: [`ActivityLogWindow.java`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/config/components/ActivityLogWindow.java#L60-L65) displays static text placeholders.
* **Proposed Enhancement**:
  - Implement a thread-safe ring-buffer log appender in `UpscaleManager` to stream real-time upscaling logs to the UI (e.g. `[12:34:56] Upscaled techreborn:block/generator (64x64 -> 256x256) in 38ms`).

### 🎚️ 3.3 Interactive owo-lib GUI Controls
* **Current State**: Configuration screen windows ([`ConfigScreen.java`](file:///c:/Users/Dan/App%20Dev/mod-texture-super-resolution/app/src/main/java/me/danvb10/mtsr/config/ConfigScreen.java)) render static labels.
* **Proposed Enhancement**:
  - Convert label displays into interactive `owo-lib` widgets (toggles for mod exclusion, sliders for tile size & thread count, dropdowns for active models).

---

## 4. Summary Roadmap Matrix

| Priority | Feature / Optimization | Impact | Target Component |
| :--- | :--- | :--- | :--- |
| **High** | In-Game Toast Prompt | High UX improvement | `UpscaleManager`, `AtlasSpriteUpscaler` |
| **High** | DirectML GPU Acceleration | 5x–20x faster inference | `EsrganModel`, `OnnxRuntimeBootstrap` |
| **Medium** | Active Model Dropdown in GUI | Dynamic model switching | `ModelManager`, `ModelSettingsWindow` |
| **Medium** | Animated Texture Support | Expands upscale coverage | `AtlasSpriteUpscaler` |
| **Medium** | Multi-Threaded Batch Worker | Faster initial load | `UpscaleManager` |
| **Low** | Interactive GUI Controls & Log Stream | Visual polish | `ConfigScreen` components |

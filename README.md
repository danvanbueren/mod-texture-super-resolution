# Mod Texture Super Resolution

A client-side Fabric mod for Minecraft that automatically detects textures loaded by mods and runs ESRGAN to upscale, cache and load upscaled textures. This mod enhances visual quality without affecting gameplay or logic.

**This mod is made possible thanks to players like you.** If you would like to show support for continued development, consider [buying me a coffee][ko-fi].

## Features

- **Automatic texture detection**: Identifies textures loaded by other mods
- **ESRGAN upscaling**: Uses advanced AI upscaling to enhance texture quality
- **Smart caching**: Efficiently caches upscaled textures to improve performance
- **Client-side only**: Does not affect server-side gameplay or logic
- **Configurable**: Easy-to-use configuration via ModMenu integration

## 📥 Downloads

### Stable builds

The latest stable release can be downloaded from our official [Modrinth][modrinth] page.

### Development builds

For developers who want to test the latest changes, you can build from source using the instructions below.

## 🖥️ Installation

### Requirements

- **Minecraft 26.1.2**
- **Fabric Loader 0.19.3+**
- **Fabric API**
- **ModMenu 18.0.0+**
- **owo-lib 0.13.0+**
- **Java 25**

### Installing the mod

1. Install the [Fabric Loader](https://fabricmc.net/wiki/start:introduction/)
2. Download the latest stable release from [Modrinth][modrinth]
3. Place the downloaded JAR file in your Minecraft mods folder
4. Launch Minecraft with the Fabric profile

## 🛠️ Building from Source

This project uses the [Gradle build tool][gradle] with the Fabric Loom plugin. Build artifacts can be found in the `app/build/libs` directory.

### Build Requirements

- **Java 25** - Required for compilation and runtime
  - We recommend using the [Eclipse Temurin][temurin] distribution
- **Gradle** - The [Gradle wrapper][gradle-wrapper] is provided for ease of use

### Development Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/danvanbueren/mod-texture-super-resolution.git
   cd mod-texture-super-resolution
   ```

2. **Install Java 25:**
   - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or use a package manager
   - Set `JAVA_HOME` to your JDK 25 installation path
   - Add `%JAVA_HOME%\bin` to your PATH
   - Verify installation: `java -version`

### Building

Navigate to the app directory and build using Gradle:
```bash
cd app
.\gradlew.bat build
```

The built mod JAR will be in `app\build\libs\`.

### Testing

Run the test suite:
```bash
.\gradlew.bat test
```

### Local Development

For rapid iteration during development, use the built-in Fabric Loom dev client:

```bash
.\gradlew.bat runClient
```

This command:
- Automatically builds your mod if needed
- Launches Minecraft with your mod loaded
- No manual file copying required

### Other Useful Commands

- `.\gradlew.bat clean` - Clean build artifacts
- `.\gradlew.bat classes` - Compile main classes only
- `.\gradlew.bat jar` - Build the mod JAR without running tests
- `.\gradlew.bat tasks` - List all available Gradle tasks

## 📬 Getting Help & Reporting Issues

### Technical Support

For help with installation, configuration, or crashes, please open an issue on the [project issue tracker][issues].

### Bug Reports

If you encounter a bug or crash, please report it with:
- Minecraft version
- Mod version
- Crash report (if applicable)
- Steps to reproduce the issue

### Feature Requests

Feature requests are welcome via the issue tracker. Development is primarily focused on improving upscaling quality, performance, and mod compatibility.

## 💬 Community

Join the community to:
- Get installation help and technical support
- Share feedback and suggestions
- Stay updated on development progress
- Connect with other users

## 📜 License

This project is licensed under the [GPL-3.0 License][license].

## 🙇 Acknowledgments

This mod uses and integrates with several excellent projects:
- [Fabric API](https://fabricmc.net/) - The core Fabric modding API
- [ModMenu](https://modrinth.com/mod/modmenu) - Configuration menu integration
- [owo-lib](https://wisp-forest.io/owo-lib/) - UI library for configuration

---

[ko-fi]: https://ko-fi.com/danvanbueren
[modrinth]: https://modrinth.com/mod/mod-texture-super-resolution
[gradle]: https://gradle.org/
[temurin]: https://adoptium.net/
[gradle-wrapper]: https://docs.gradle.org/current/userguide/gradle_wrapper.html
[issues]: https://github.com/danvanbueren/mod-texture-super-resolution/issues
[license]: LICENSE

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

This is a Minecraft mod for beta 1.7.3 using Fabric with StationAPI and BIN mappings. All development uses Gradle:

- **Build**: `./gradlew build`
- **Run client**: `./gradlew runClient`
- **Run server**: `./gradlew runServer`
- **Clean**: `./gradlew clean`

## Architecture Overview

SmoothBeta is a Minecraft mod that provides rendering optimizations and smoother gameplay for Minecraft beta 1.7.3. The mod is structured as follows:

### Core Structure
- **Main mod class**: `SmoothBeta` - Basic entrypoint with namespace and logging
- **Client entrypoint**: `SmoothBetaClient` - Client-side initialization
- **Rendering system**: Located in `client/render/` package - Contains the core rendering optimizations including:
  - `SmoothChunkRenderer` - Optimized chunk rendering
  - `SmoothWorldRenderer` - World rendering improvements  
  - `SmoothTessellator` - Tessellation optimizations
  - Shader system (`Shaders`, `Shader`, `GlShader`)
  - OpenGL state management (`GlStateManager`, `GlBlendState`)
  - VBO and vertex buffer management

### Mixin Integration
The mod uses Mixins extensively for compatibility:
- **Multi-draw compatibility**: `multidraw/` package contains mixins for batch rendering
- **Mod compatibility**: Specific compatibility mixins for Arsenic and StationRendererAPI
- **Entity system**: Mixins for entity registry and rendering

### Key Technologies
- **StationAPI 2.0.0+** - Core modding framework
- **Fabric Loom with babric-loom-extension** - Build toolchain
- **OpenGL shaders** - Custom terrain rendering shaders in `assets/minecraft/smoothbeta/shaders/`
- **Java 17** - Required runtime

### Important Notes
- The mod targets specifically Minecraft beta 1.7.3
- Uses BIN mappings (Biny) for obfuscation mapping
- Includes conditional compilation through mixin groups for different rendering paths
- Heavy focus on rendering performance optimization through VBOs and batching
# Neat (Cobblemon Fork)

A fork of [Neat](https://github.com/VazkiiMods/Neat) specifically built for **Cobblemon 1.7.1** on **Minecraft 1.21.1** with **Fabric 0.18.4**.

Functional minimalistic Unit Frames for the modern Minecrafter.

## About This Fork

This is a specialized fork of the original Neat mod, maintained specifically for Cobblemon servers. This fork **requires Cobblemon** to function.

### Target Versions
- **Cobblemon**: 1.7.1+ (required)
- **Minecraft**: 1.21.1
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.116.4+1.21.1

### Key Features
- Health bars for entities (mobs, players, bosses)
- Customizable display options
- Optimized for Cobblemon gameplay
- Fabric-only support (NeoForge excluded from this fork)

## Requirements

- **Minecraft**: 1.21.1
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.116.4+1.21.1

**Note:** This fork is designed for use with Cobblemon 1.7.1 servers. Please check the CurseForge page for compatibility information.

## Original Project

This project is a fork of [Neat by VazkiiMods](https://github.com/VazkiiMods/Neat).

**Original Authors:**
- Vazkii
- williewillus
- Alwinfy
- Uraneptus

## Building

### Prerequisites
- Java 21 (JDK)
- Gradle (included via wrapper)

### Build Commands

```powershell
# Clean and build
.\gradlew.bat clean build

# Build only Fabric module
.\gradlew.bat :Fabric:build
```

Built JAR files will be located in `Fabric/build/libs/`.

## License

This project maintains the same license as the original Neat mod:
- Code: MIT License
- Assets: CC BY-NC-SA 3.0

See [LICENSE.txt](LICENSE.txt) for full details.

## Credits

This fork is based on the excellent work by the original Neat team. All credit for the original mod goes to VazkiiMods and contributors.

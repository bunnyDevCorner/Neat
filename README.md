# Cobblemon Neat Bunny

A fork of [Neat](https://github.com/VazkiiMods/Neat) specifically built for **Cobblemon 1.7.1** on **Minecraft 1.21.1** with **Fabric 0.18.4**.

Functional minimalistic Unit Frames for the modern Minecrafter, enhanced with smooth animations and optimized for Cobblemon gameplay.

## 🎮 What is This?

Cobblemon Neat Bunny is a specialized fork of the popular Neat mod, designed specifically for Cobblemon servers. It provides clean, minimalistic health bars for entities with enhanced visual effects including smooth animations and fade-out effects.

## About This Fork

This is a specialized fork of the original Neat mod, maintained specifically for Cobblemon servers. This fork **requires Cobblemon** to function.

### Target Versions
- **Cobblemon**: 1.7.1+ (required)
- **Minecraft**: 1.21.1
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.116.4+1.21.1

### Key Features
- Health bars for entities (mobs, players, bosses)
- **Smooth health bar animations** - Health changes animate smoothly with lerp interpolation
- **Fade-out on death** - Health bars gracefully fade out when entities die
- Customizable display options
- Optimized for Cobblemon gameplay
- Fabric-only support (NeoForge excluded from this fork)

## 📋 Requirements

- **Minecraft**: 1.21.1
- **Fabric Loader**: 0.18.4+
- **Fabric API**: 0.116.4+1.21.1
- **Cobblemon**: 1.7.1+ (required)

**Note:** This fork is designed for use with Cobblemon 1.7.1 servers. While it may work without Cobblemon, it's optimized specifically for Cobblemon gameplay.

## 🚀 Installation

1. Download the latest JAR file from the [Releases](https://github.com/bunnyDevCorner/Neat/releases) page
2. Place it in your `mods` folder
3. Ensure you have Fabric Loader, Fabric API, and Cobblemon installed
4. Launch Minecraft and enjoy smooth health bar animations!

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

## 📝 Changelog

See [CHANGELOG.md](CHANGELOG.md) for a detailed list of changes.

## 🤝 Contributing

This is a specialized fork for Cobblemon. If you find bugs or have suggestions, please open an issue on the [GitHub repository](https://github.com/bunnyDevCorner/Neat/issues).

## 📄 License

This project maintains the same license as the original Neat mod:
- **Code**: MIT License
- **Assets**: CC BY-NC-SA 3.0

See [LICENSE.txt](LICENSE.txt) for full details.

## 🙏 Credits & Attribution

This fork is based on the excellent work by the original Neat team. All credit for the original mod goes to VazkiiMods and contributors.

**Original Neat Mod:**
- [GitHub](https://github.com/VazkiiMods/Neat)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neat)

**Fork Maintainer:**
- BunnyDevCorner

---

**Disclaimer:** This is an unofficial fork. The original Neat mod is maintained by VazkiiMods. This fork is not affiliated with or endorsed by VazkiiMods.

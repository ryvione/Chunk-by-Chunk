# Gathering Chunks (Ryvione's Fork)

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg)](https://fabricmc.net/)
[![Mod Loader](https://img.shields.io/badge/NeoForge-1.21+-orange?style=for-the-badge)](https://neoforged.net)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Discord Server Link : https://discord.gg/3S9aKukmmJ

Gathering Chunks is a unique Minecraft mod where players start their journey in a single isolated chunk surrounded by the void. Your goal is to gather resources, craft specialized items, and expand your world chunk by chunk.

## Core Gameplay Loop

Begin your journey by gathering resources from your initial chunk to craft World Cores. By combining these cores with materials like copper, you can create Chunk Spawners. Place these spawners in the void and activate them to generate new adjacent chunks, gradually rebuilding the world around you.

This is a maintained fork of the original [Chunk By Chunk](https://github.com/immortius/chunkbychunk) by immortius.

## About This Fork

The original Chunk By Chunk is an excellent mod that deserves to be kept alive and updated. Since the original repository appears to be discontinued, I've decided to continue its development, fixing bugs, adding features, and ensuring compatibility with newer Minecraft versions.

## New Features & Improvements

- Configuration Management: Command-based config reload and UI modification hooks.
- Platform Support: Full parity between Fabric and NeoForge versions.
- Progression Helper: Intelligent system to assist players when stuck, featuring expanded search radius and multi-container detection.
- Server Optimization: Improved chunk generation search logic for smoother performance.
- Chunk Eraser: Capability to remove existing chunks for better world management.
- Modern Compatibility: Fully updated for Minecraft 1.21.1.

## Features

- World Expansion: Start with a single chunk and expand your world manually.
- Multiple Chunk Types: Support for normal, unstable, and biome-themed spawners.
- Custom Blocks: World Forge, Scanner, Mender, World Eraser, and Chunk Engine.
- Admin Commands: Comprehensive management tools for server administrators.

## Building from Source

```bash
# Clone the repository
git clone https://github.com/ryvione/Gathering-Chunks.git
cd Gathering-Chunks

# Build the mod
./gradlew build

# The compiled JARS will be in Fabric/build/libs/ and NeoForge/build/libs
```

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/chests` | Player | List all available chests in current dimension |
| `/chests tracker enable` | Player | Enable chest location notifications |
| `/chests tracker disable` | Player | Disable chest location notifications |
| `/spawnchunk` | Admin | Manually spawn a chunk |
| `/gatheringchunks help` | Player | Show help information |
| `/gatheringchunks config reload` | Admin | Reload configuration from disk |
| `/gatheringchunks config modify` | Admin | Open the configuration menu UI |


## Roadmap

- [ ] Support for Minecraft 1.21.2+
- [x] NeoForge compatibility
- [x] Additional configuration options
- [x] Performance optimizations
- [ ] More biome-specific features
- [ ] Nether specific blocks
- [ ] Integration with popular mods
- [x] Enhanced multiplayer features

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Development Setup

1. Fork the repository
2. Create your feature branch (git checkout -b feature/AmazingFeature)
3. Commit your changes (git commit -m 'Add some AmazingFeature')
4. Push to the branch (git push origin feature/AmazingFeature)
5. Open a Pull Request

## Credits

Original Author: [immortius](https://github.com/immortius)
Fork Maintainer & Active Developer: [Ryvione](https://github.com/Ryvione)

### Original Repository
The original mod can be found at: [github.com/immortius/chunkbychunk](https://github.com/immortius/chunkbychunk)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

Original work Copyright (c) immortius
Modified work Copyright (c) 2026 Ryvione

## Bug Reports

Found a bug? Please report it on our Issues page with:
- Minecraft version
- Mod version
- Steps to reproduce
- Expected vs actual behavior
- Crash logs (if applicable)

## Community

- Issues: [GitHub Issues](../../issues)
- Discussions: [GitHub Discussions](../../discussions)
- Download (Modrinth): [Modrinth](https://modrinth.com/mod/gathering-chunks)
- Download (CurseForge): [CurseForge](https://www.curseforge.com/minecraft/mc-mods/gathering-chunks)

---

*If you enjoy this mod, please consider starring the repository!*

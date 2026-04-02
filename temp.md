**Available on Fabric & NeoForge**

### New Features

**Chunk Orb System**
A new ore now generates between Y -16 and Y 80 in stone and deepslate. Mining it drops 2-3 Chunk Orbs from regular ore and 3-4 from the deepslate variant. Fortune works on it, Silk Touch gives you the block back.

The progression chain has been reworked around Orbs: 4 Chunk Orbs craft 1 World Fragment, and 8 Chunk Orbs plus a World Core craft a Chunk Spawner. Fragments, Shards, and Crystals still work the same way from there. The copper recipe for the Chunk Spawner is gone.

Chests now spawn with 3-8 Chunk Orbs instead of a pile of fragments and crystals. Finding one is still a nice boost but you no longer depend on them to progress.

This entire system can be disabled in `GatheringChunks/gatheringchunks.toml` under the `GatheringChunks` section by setting `Enable Chunk Orb System = false`. This restores the old full-loot chest behaviour and the original copper spawner recipe, and stops ore from generating. Enabled by default.

**Biome Mod Compatibility**
Biome O Plenty, Terralith, and Oh The Biomes We Go biomes now map correctly to biome-themed chunk spawners. Unknown modded biomes also get a best-effort match based on their name, so most generation mods should work without any extra config.

### Fixes
- Fixed running water leaking into the void from waterlogged blocks at chunk edges
- Fixed game lagging badly after playing for 1-2 hours
- Fixed config changes not actually saving when closing the config screen
- Fixed players sometimes spawning mid-chunk or underground on first join or respawn
- Fixed mobs spawning in void (empty/unspawned) chunks
- Fixed chunk spawners breaking, forcing use of the World Mender for no reason
- Fixed the Chunk Engine spawner mode setting (Edge / Void / Both) doing absolutely nothing
- Fixed chunk boundaries enabling by itself
- Fixed overwrite system and edge spawning systems
- Fixed a bug where Both or Edge generation modes would block the chunk overwriting confirmation if no empty adjacent chunks were found
- Fixed Nether portals spawning on top of the world
- Fixed a crash on servers that would happen when creating or loading a world
- Fixed an issue where expanding your world would sometimes use the wrong biome or theme (e.g. getting a snow forest when you were in a plains village)
- Fixed several crashes and error messages that appeared during server startup
- Fixed a bug where the World Scanner would stop working correctly after a configuration reload

### Changes & Improvements
- **Better Expansion**: Improved how the mod remembers the theme of your chunks, making it much more reliable when you expand your world
- **More Stable World Gen**: Added safety checks to prevent the game from crashing if world generation settings are missing or slightly incorrect
- **Cleaner Logs**: Removed redundant background tasks that were cluttering the server console with warnings
- Improved biome consistency between adjacent chunks
- Sped up finding specific biome themes
- The game now stays at a stable framerate and TPS even after hours of play
- Biome scanning now runs much less often in the background, reducing CPU load
- Long sessions no longer slowly consume more and more memory
- Added chunk biome filter with a proper selection menu in the config screen: toggle each biome on or off individually
- Default allowed biomes expanded to all forested and grassy biomes (Plains, Sunflower Plains, Forest, Flower Forest, Birch Forest, Old Growth Birch Forest, Dark Forest, Taiga, Old Growth Pine Taiga, Old Growth Spruce Taiga, Savanna, Savanna Plateau, Windswept Savanna, Jungle, Sparse Jungle, Bamboo Jungle, Cherry Grove, Meadow, Windswept Forest, Windswept Hills, Grove)
- Improved configuration handling: better syncing and validation across client and server
- **UI Enhancements**:
  - Added a "?" Help Icon to both the World Scanner and Chunk Engine screens
  - Implemented a detailed tooltip for the World Scanner that explains resource density color codes and controls
  - Added a tooltip to the Chunk Engine to clarify its role in preserving chunks during Hard Mode
- **Starter's Guide (Book)**:
  - Added a new Starter Book item that introduces players to the mod's mechanics
  - Designed a custom multi-page UI specifically for the book
  - Players automatically receive the book the first time they join a world
  - Integrated the book into the creative tab
- **World Migration (V5)**:
  - Implemented a more robust migration system designed to prevent world corruption
  - The system now performs a forced level save immediately upon successful migration
  - Recalculates spawned chunk counts to fix sync issues that could block progression
  - Automatically repairs missing or corrupted dimension origin data to ensure cross-dimensional synchronization works correctly
  - Added failure handling that prevents the mod version from updating if critical migration steps fail

### Known Issues
- **World Scanner**: Manual scan mode can occasionally fail or get stuck
- **Dimension Sync** (Nether / Overworld): Rare desync or failed chunk transfers still possible
- **Ocean Chunk Spawner**: May spawn land instead of ocean
- **Fabric Version**: Your server or game may crash

> If a feature isn't working well or doesn't feel right, please let me know — feedback is always welcome.

Found a bug or crash? Report it [here](https://github.com/ryvione/Gathering-Chunks/issues).
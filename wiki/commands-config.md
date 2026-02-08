# Commands and Configuration

Gathering Chunks provides several commands for both players and administrators to manage the mod's features.

## Player Commands

### /chests

Lists all available/tracked chests in the current dimension. This is helpful for finding resources you may have forgotten about or that were generated with new chunks.

- `/chests tracker enable`: Enables notifications when a new chest is located.
- `/chests tracker disable`: Disables these notifications.

### /gatheringchunks help

Provides a list of help topics related to the mod's features, such as tree spawning and progression.

## Administrator Commands

These commands require permission level 2 (OP) by default.

### /gatheringchunks config reload

Reloads the mod's configuration files from the disk. This allows for live updating of settings without restarting the server.

### /gatheringchunks config modify

Trigger a request to open the configuration GUI. Note that in a client-server environment, the actual GUI is handled by the client-side mod.

### /spawnchunk

Manually spawns a chunk at a specified location.

- `/gatheringchunks spawnChunk <pos>`: Spawns a default chunk.
- `/gatheringchunks spawnRandomChunk <pos>`: Spawns a chunk with a random biome/theme.
- `/gatheringchunks spawnThemedChunk <theme> <pos>`: Spawns a chunk with a specific theme.

## Configuration

The mod uses a standard TOML configuration system. Key settings include:

- Fragment drop chances
- Progression Helper status (enabled/disabled)
- Fluid flow prevention settings
- Leaf decay toggles
- Starting world restriction types

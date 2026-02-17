# Gathering Chunks Configuration API

This document explains how to access Gathering Chunks configuration from other mods, especially useful for modpack creators, KubeJS, Create mod integration, and other mod interactions.

## Quick Start

The `ConfigAPI` class provides a safe, public interface to access all Gathering Chunks configuration values:

```java
import com.ryvione.gatheringchunks.api.ConfigAPI;

// Check if the mod is enabled
boolean isEnabled = ConfigAPI.isEnabled();

// Get specific configs
boolean blockPlacementAllowed = ConfigAPI.getGameplayConfig()
    .isBlockPlacementAllowedOutsideSpawnedChunks();

// Check world scanner mode
String scannerMode = ConfigAPI.getWorldScannerMode(); // "Auto" or "Manual"
boolean isManualMode = ConfigAPI.isWorldScannerManualMode();
```

## Available Methods

### Main Configuration Access

- `ConfigAPI.getGameplayConfig()` - Access gameplay settings
- `ConfigAPI.getGenerationConfig()` - Access chunk generation settings
- `ConfigAPI.getGatheringChunksConfig()` - Access general gathering chunks settings
- `ConfigAPI.getWorldScannerConfig()` - Access world scanner settings
- `ConfigAPI.getDifficultyConfig()` - Access difficulty settings
- `ConfigAPI.getHardModeConfig()` - Access hard mode settings

### Quick Checks

- `ConfigAPI.isEnabled()` - Check if CBC generation is enabled
- `ConfigAPI.isSyncNetherEnabled()` - Check if nether/overworld sync is enabled
- `ConfigAPI.getWorldScannerMode()` - Get scanner mode as string ("Auto"/"Manual")
- `ConfigAPI.isWorldScannerManualMode()` - Check if scanner is in manual mode

## GameplayConfig Methods

```java
GameplayConfig config = ConfigAPI.getGameplayConfig();

config.isBlockPlacementAllowedOutsideSpawnedChunks()
config.isChunkSpawnLeafDecayDisabled()
config.isEnableChunkBarriers()
config.getUnstableChunkChance()
```

## GenerationConfig Methods

```java
GenerationConfig config = ConfigAPI.getGenerationConfig();

config.isEnabled()
config.sealWorld()
config.isSynchNether()
config.useBedrockChest()
config.getInitialChunks()
config.getChunkLayerSpawnRate()
// ... and more
```

## WorldScannerConfig Methods

```java
WorldScannerConfig config = ConfigAPI.getWorldScannerConfig();

config.getWorldScannerScanMode() // Returns WorldScannerMode.Auto or WorldScannerMode.Manual
config.getFuelRequiredPerChunk()
config.getFuelConsumedPerTick()
config.isExperimentalMode()
// ... and more
```

## GatheringChunksConfig Methods

```java
GatheringChunksConfig config = ConfigAPI.getGatheringChunksConfig();

config.isMobsDropFragments()
config.getFragmentDropChance()
config.isAutoSpawnTrees()
config.isPreventFluidFlowIntoVoid()
// ... and more
```

## KubeJS Integration

For KubeJS users, you can access config values in scripts:

```javascript
// Check if generation is enabled
if (Java.type('com.ryvione.gatheringchunks.api.ConfigAPI').isEnabled()) {
    // Do something
}

// Get world scanner mode
let scannerMode = Java.type('com.ryvione.gatheringchunks.api.ConfigAPI').getWorldScannerMode();
if (scannerMode === "Auto") {
    // Handle auto scan mode
}
```

## Create Mod Integration

For Create mod interactions, you can check if Gathering Chunks is configured in a specific way:

```java
// Example: Check if certain features are enabled before adding custom machinery
if (ConfigAPI.isEnabled() && ConfigAPI.isSyncNetherEnabled()) {
    // Add nether-specific machinery or mechanics
}
```

## Configuration Persistence

Starting from version 3 of the config system:

- Config files are **no longer reset** when the mod updates (if version is compatible)
- Old config values are **preserved** through mod updates
- A backup is automatically created when migration is needed (`.backup` file)
- Only new fields are added, existing values remain unchanged

### Config Version History

- **Version 1**: Initial config
- **Version 2**: Added new fields
- **Version 3**: Added WorldScannerMode configuration - Auto/Manual scanning selection

## Safety and Thread Safety

All ConfigAPI methods are safe to call from any context:
- Configuration values are loaded during server startup
- Multiple reads are safe and concurrent
- Configuration changes are only reflected after a config reload or server restart

## Error Handling

If config is not properly loaded or initialized, ConfigAPI will return default values. No exceptions are thrown, ensuring stability.

## Support

If you encounter any issues with the ConfigAPI or need to access additional configuration values, please report them to the mod maintainer.

package com.ryvione.gatheringchunks.common.util;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import net.minecraft.resources.ResourceLocation;

public class GuiTextures {

    public static final ResourceLocation WORLD_FORGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            GatheringChunksConstants.MOD_ID, "textures/gui/container/worldforge.png"
    );
    public static final int WORLD_FORGE_TEX_WIDTH = 256;
    public static final int WORLD_FORGE_TEX_HEIGHT = 256;
    public static final float WORLD_FORGE_TICKS_PER_FRAME = 2f;
    public static final int WORLD_FORGE_FRAME_COUNT = 8;
    public static final int WORLD_FORGE_PROGRESS_U = 176;
    public static final int WORLD_FORGE_PROGRESS_V_BASE = 0;
    public static final int WORLD_FORGE_PROGRESS_HEIGHT = 11;
    public static final ResourceLocation WORLD_MENDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            GatheringChunksConstants.MOD_ID, "textures/gui/container/worldmender.png"
    );
    public static final int WORLD_MENDER_TEX_WIDTH = 512;
    public static final int WORLD_MENDER_TEX_HEIGHT = 512;
    public static final float WORLD_MENDER_TICKS_PER_FRAME = 4f;
    public static final int WORLD_MENDER_FRAME_COUNT = 8;
    public static final int WORLD_MENDER_HIGHLIGHT_SIZE = 128;
    public static final ResourceLocation BEDROCK_CHEST_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            GatheringChunksConstants.MOD_ID, "textures/gui/container/bedrockchest.png"
    );
}
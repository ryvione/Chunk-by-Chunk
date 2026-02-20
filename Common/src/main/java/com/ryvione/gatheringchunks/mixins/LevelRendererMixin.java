/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ryvione.gatheringchunks.client.render.ExperimentalScannerRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
            method = "renderDebug",
            at = @At("HEAD")
    )
    private void gatheringchunks$onRenderDebug(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, CallbackInfo ci) {
        ExperimentalScannerRenderer.render(poseStack, bufferSource, camera);
    }
}
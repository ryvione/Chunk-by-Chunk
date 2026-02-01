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
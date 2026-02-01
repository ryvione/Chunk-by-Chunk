package com.ryvione.gatheringchunks.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ryvione.gatheringchunks.client.render.ExperimentalScannerRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V", shift = At.Shift.AFTER))
    private void gatheringchunks$onRenderLevel(PoseStack poseStack, float partialTick, long gameTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        ExperimentalScannerRenderer.render(poseStack, partialTick, gameTime, renderBlockOutline, camera, gameRenderer, lightTexture, projectionMatrix);
    }
}

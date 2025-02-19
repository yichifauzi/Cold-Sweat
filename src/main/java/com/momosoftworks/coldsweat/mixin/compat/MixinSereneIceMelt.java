package com.momosoftworks.coldsweat.mixin.compat;

import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sereneseasons.handler.season.RandomUpdateHandler;

@Mixin(RandomUpdateHandler.class)
public class MixinSereneIceMelt
{
    @Inject(method = "meltInChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    private static void getBiomeTemperatureOverride(ChunkMap chunkManager, LevelChunk chunkIn, float meltChance, CallbackInfo ci,
                                                    //locals
                                                    ServerLevel level, ChunkPos chunkPos, int minX, int minZ, BlockPos topAirPos)
    {
        BlockPos groundPos = topAirPos.below();
        if (WorldHelper.getBiomeTemperatureAt(level, level.getBiome(groundPos).value(), groundPos) < 0.15F)
        {
            ci.cancel();
        }
    }
}

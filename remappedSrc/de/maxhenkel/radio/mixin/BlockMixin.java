package de.maxhenkel.radio.mixin;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.radio.radio.RadioData;
import de.maxhenkel.radio.radio.RadioManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlayerSkullBlock;
import net.minecraft.block.WallPlayerSkullBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "playerDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"), cancellable = true)
    public void playerDestroy(World level, PlayerEntity player, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity, ItemStack itemStack, CallbackInfo ci) {
        if (level.isClient()) {
            return;
        }
        if (!(blockState.getBlock() instanceof PlayerSkullBlock || blockState.getBlock() instanceof WallPlayerSkullBlock)) {
            return;
        }
        if (!(blockEntity instanceof SkullBlockEntity skullBlockEntity)) {
            return;
        }
        GameProfile ownerProfile = skullBlockEntity.getOwner();
        if (ownerProfile == null) {
            return;
        }
        RadioData radioData = RadioData.fromGameProfile(ownerProfile);
        if (radioData != null) {
            RadioManager.getInstance().onRemoveHead(radioData.getId());
            ItemStack speakerItem = radioData.toItemWithNoId();
            Block.dropStack(level, blockPos, speakerItem);
            ci.cancel();
        }
    }

    @Inject(method = "playerWillDestroy", at = @At(value = "HEAD"))
    public void destroy(World level, BlockPos blockPos, BlockState blockState, PlayerEntity player, CallbackInfo ci) {
        if (level.isClient()) {
            return;
        }
        if (!(blockState.getBlock() instanceof PlayerSkullBlock || blockState.getBlock() instanceof WallPlayerSkullBlock)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof SkullBlockEntity skullBlockEntity)) {
            return;
        }
        GameProfile ownerProfile = skullBlockEntity.getOwner();
        if (ownerProfile == null) {
            return;
        }
        RadioData radioData = RadioData.fromGameProfile(ownerProfile);
        if (radioData != null) {
            RadioManager.getInstance().onRemoveHead(radioData.getId());
        }
    }

    // TODO Stop radio when block is broken by explosion or non-player

}

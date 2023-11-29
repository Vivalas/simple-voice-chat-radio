package de.maxhenkel.radio.mixin;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.radio.radio.RadioManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin extends BlockEntity {

    public SkullBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "setOwner", at = @At("RETURN"))
    public void setOwner(GameProfile gameProfile, CallbackInfo ci) {
        if (world != null && !world.isClient) {
            RadioManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    public void load(NbtCompound compoundTag, CallbackInfo ci) {
        if (world != null && !world.isClient) {
            RadioManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Override
    public void setWorld(World newLevel) {
        World oldLevel = world;
        super.setWorld(newLevel);
        if (oldLevel == null && newLevel != null && !newLevel.isClient) {
            RadioManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }
}

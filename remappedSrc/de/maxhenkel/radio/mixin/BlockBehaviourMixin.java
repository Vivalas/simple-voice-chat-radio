package de.maxhenkel.radio.mixin;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.radio.radio.RadioData;
import de.maxhenkel.radio.radio.RadioManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public class BlockBehaviourMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void use(BlockState blockState, World level, BlockPos blockPos, PlayerEntity player, Hand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (level.isClient()) {
            return;
        }
        if (!interactionHand.equals(Hand.MAIN_HAND)) {
            return;
        }
        if (!blockState.getBlock().equals(Blocks.PLAYER_HEAD) && !blockState.getBlock().equals(Blocks.PLAYER_WALL_HEAD)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(blockPos);

        if (!(blockEntity instanceof SkullBlockEntity skullBlockEntity)) {
            return;
        }

        GameProfile profile = skullBlockEntity.getOwner();
        RadioData radioData = RadioData.fromGameProfile(profile);
        if (radioData == null) {
            return;
        }

        radioData.setOn(!radioData.isOn());
        radioData.updateProfile(profile);
        skullBlockEntity.markDirty();
        RadioManager.getInstance().updateHeadOnState(radioData.getId(), radioData.isOn());

        level.playSound(null, blockPos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1F, 1F);

        cir.setReturnValue(ActionResult.SUCCESS);
    }

}

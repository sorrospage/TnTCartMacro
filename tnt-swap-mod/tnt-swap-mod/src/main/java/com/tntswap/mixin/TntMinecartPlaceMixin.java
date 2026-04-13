package com.tntswap.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MinecartItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartItem.class)
public class TntMinecartPlaceMixin {

    @Inject(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z",
            shift = At.Shift.AFTER
        )
    )
    private void onMinecartPlaced(World world, PlayerEntity player, Hand hand,
                                  CallbackInfoReturnable<ActionResult> cir) {
        // Server-side only, TNT minecart only
        if (world.isClient) return;
        if ((Object) this != Items.TNT_MINECART) return;

        // 1) Prefer an existing flint and steel already in the hotbar
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.FLINT_AND_STEEL)) {
                player.getInventory().selectedSlot = i;
                return;
            }
        }

        // 2) Otherwise give the player a new one
        int slot = player.getInventory().selectedSlot;
        ItemStack flintAndSteel = new ItemStack(Items.FLINT_AND_STEEL);

        if (player.getInventory().getStack(slot).isEmpty()) {
            // Slot freed up because the last minecart was consumed
            player.getInventory().setStack(slot, flintAndSteel);
        } else {
            // Player still has minecarts — add F&S elsewhere or drop if full
            if (!player.getInventory().insertStack(flintAndSteel)) {
                player.dropStack(flintAndSteel);
            }
        }
    }
}

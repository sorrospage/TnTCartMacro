package com.railmacros.mixin;

import com.railmacros.HoverRefill;
import com.railmacros.RailMacrosMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into HandledScreen to detect when the mouse hovers over a tnt_minecart
 * in the inventory and trigger the HoverRefill swap to hotbar slot 3.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HoverRefill hoverRefill = RailMacrosMod.HOVER_REFILL;
        if (!hoverRefill.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null || client.player == null) return;

        // If not hovering over a slot or the slot is empty, reset tracking
        if (focusedSlot == null || !focusedSlot.hasStack()) {
            hoverRefill.resetTracking();
            return;
        }

        // Only trigger for tnt_minecart items
        if (focusedSlot.getStack().getItem() != Items.TNT_MINECART) {
            hoverRefill.resetTracking();
            return;
        }

        // Don't re-trigger on the same slot
        if (focusedSlot.id == hoverRefill.getLastSwappedSlotId()) {
            return;
        }

        // Perform swap: SlotActionType.SWAP with button=2 means swap with hotbar slot 3 (0-indexed)
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        int syncId = screen.getScreenHandler().syncId;
        client.interactionManager.clickSlot(syncId, focusedSlot.id, 2, SlotActionType.SWAP, client.player);

        hoverRefill.setLastSwappedSlotId(focusedSlot.id);
    }
}

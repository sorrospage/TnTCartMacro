package com.railmacros;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CartGuard: Prevents placing multiple TNT minecarts on a single rail.
 * When enabled, right-clicking a rail with a TNT minecart is blocked if
 * a minecart entity already exists on that rail block.
 * Toggleable from the HUD menu only.
 */
public class CartGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger("RailMacros");

    private boolean enabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }

    public void toggle() {
        enabled = !enabled;
    }

    /**
     * Register the UseBlockCallback to intercept TNT minecart placement.
     * Called once during mod initialization.
     */
    public void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!enabled) return ActionResult.PASS;

            // Check if either hand holds a TNT minecart
            boolean mainHandHasTnt = player.getMainHandStack().getItem() == Items.TNT_MINECART;
            boolean offHandHasTnt = player.getOffHandStack().getItem() == Items.TNT_MINECART;
            if (!mainHandHasTnt && !offHandHasTnt) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();

            // Only check rail blocks
            if (block != Blocks.RAIL && block != Blocks.POWERED_RAIL
                    && block != Blocks.DETECTOR_RAIL && block != Blocks.ACTIVATOR_RAIL) {
                return ActionResult.PASS;
            }

            // Check if a minecart entity already exists on this rail block
            Box checkBox = new Box(pos).expand(0.1);
            boolean hasMinecart = !world.getEntitiesByClass(AbstractMinecartEntity.class, checkBox, Entity::isAlive).isEmpty();

            if (hasMinecart) {
                LOGGER.info("[CartGuard] Blocked TNT minecart placement — rail at {} already has a minecart", pos);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
}

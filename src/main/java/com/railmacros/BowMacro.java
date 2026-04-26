package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InstaCart Macro: Toggle with "z"
 * - Upon firing a bow (arrow consumed while using bow), wait then swap to rails.
 * - Upon placing a rail (rail consumed), wait then swap to tnt_minecart.
 * - If a bow was shot within the suppression window of tnt_minecart placement, suppress flint_and_steel swap.
 */
public class BowMacro {

    private static final Logger LOGGER = LoggerFactory.getLogger("RailMacros");

    private boolean enabled = false;

    // Track previous item counts for consumption detection
    private final Map<Item, Integer> previousCounts = new HashMap<>();

    // Timestamp of the last bow shot (for suppression window)
    private long lastBowShotTimeMs = 0;

    // Track if player was using a bow (to detect bow release = shot)
    private boolean wasUsingBow = false;

    private static final Item[] RAIL_ITEMS = {
            Items.RAIL,
            Items.POWERED_RAIL,
            Items.DETECTOR_RAIL,
            Items.ACTIVATOR_RAIL
    };

    // All sword items for offhand TNT minecart swap
    private static final Item[] SWORD_ITEMS = {
            Items.WOODEN_SWORD, Items.COPPER_SWORD, Items.STONE_SWORD,
            Items.GOLDEN_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD,
            Items.NETHERITE_SWORD
    };

    // ---- Configurable settings ----

    // Bow release -> rail swap delay (ms)
    private int bowToRailDelay = 10;

    // Rail consumed -> TNT Minecart swap delay (ms)
    private int railToTntDelay = 15;

    // Suppression window: if bow was shot within this many ms, suppress flint_and_steel swap
    private int suppressionWindowMs = 350;

    // ---- Getters and setters ----

    public int getBowToRailDelay() { return bowToRailDelay; }
    public void setBowToRailDelay(int v) { bowToRailDelay = Math.max(0, Math.min(500, v)); }

    public int getRailToTntDelay() { return railToTntDelay; }
    public void setRailToTntDelay(int v) { railToTntDelay = Math.max(0, Math.min(500, v)); }

    public int getSuppressionWindowMs() { return suppressionWindowMs; }
    public void setSuppressionWindowMs(int v) { suppressionWindowMs = Math.max(0, Math.min(2000, v)); }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        previousCounts.clear();
        lastBowShotTimeMs = 0;
        wasUsingBow = false;
    }

    /**
     * Reset tracked item counts to current values without triggering any swaps.
     * Called when inventory screen closes to avoid false consumption detection.
     */
    public void resetCounts(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        for (Item railItem : RAIL_ITEMS) {
            previousCounts.put(railItem, MacroUtils.countInInventory(player, railItem));
        }
        previousCounts.put(Items.TNT_MINECART, MacroUtils.countInInventory(player, Items.TNT_MINECART));
        wasUsingBow = false;
    }

    /**
     * Returns true if a bow was shot within the last 350ms.
     * Used by RailMacro to suppress flint_and_steel swap.
     */
    /**
     * Returns true if a bow was shot within the BowMacro's own suppression window.
     * Used by RailMacro to suppress flint_and_steel swap.
     */
    public boolean isBowShotRecent() {
        if (!enabled) return false;
        return (System.currentTimeMillis() - lastBowShotTimeMs) <= suppressionWindowMs;
    }

    /**
     * Returns true if a bow was shot within the given number of milliseconds.
     * Used by RailMacro for its own configurable bow suppression on rail->TNT swaps.
     */
    public boolean isBowShotWithin(int ms) {
        return (System.currentTimeMillis() - lastBowShotTimeMs) <= ms;
    }

    public void tick(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Detect bow firing by detecting bow release (player was drawing bow and stopped)
        // This is more reliable than tracking arrow count, which has inventory sync delay
        boolean isCurrentlyUsingBow = player.isUsingItem()
                && player.getActiveItem().getItem() == Items.BOW;

        // Bow released = player was drawing last tick but isn't now → they fired
        if (wasUsingBow && !isCurrentlyUsingBow) {
            lastBowShotTimeMs = System.currentTimeMillis();

            // Only swap to rail if looking at a block where a block can be placed
            boolean canPlace = false;
            if (client.crosshairTarget != null
                    && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
                // Check if the block adjacent to the hit face is air/replaceable (i.e., a block can be placed there)
                BlockPos placePos = blockHit.getBlockPos().offset(blockHit.getSide());
                BlockState stateAtPlace = client.world.getBlockState(placePos);
                if (stateAtPlace.isReplaceable()) {
                    canPlace = true;
                }
            }

            if (canPlace) {
                LOGGER.info("[BowMacro] Bow release detected, placeable block target, swapping to rail in {}ms", bowToRailDelay);
                // Bow fired -> swap to any rail type
                MacroUtils.scheduleSwapFirstAvailable(RAIL_ITEMS, bowToRailDelay);
            } else {
                LOGGER.info("[BowMacro] Bow release detected, but no placeable block target — skipping swap");
            }
        }

        // Debug: log bow use state changes
        if (isCurrentlyUsingBow && !wasUsingBow) {
            LOGGER.info("[BowMacro] Bow draw started");
        }

        wasUsingBow = isCurrentlyUsingBow;

        // Check for rail consumption (when bow macro is active, swap to tnt_minecart)
        for (Item railItem : RAIL_ITEMS) {
            int currentCount = MacroUtils.countInInventory(player, railItem);
            Integer prevCount = previousCounts.get(railItem);
            if (prevCount != null && currentCount < prevCount) {
                // Rail consumed -> check if tnt_minecart is in hotbar or offhand
                boolean tntInHotbar = MacroUtils.findHotbarSlot(player, Items.TNT_MINECART) != -1;
                boolean tntInOffhand = player.getOffHandStack().getItem() == Items.TNT_MINECART;

                if (tntInHotbar) {
                    // TNT minecart in hotbar -> swap to it directly
                    MacroUtils.scheduleSwap(Items.TNT_MINECART, railToTntDelay);
                } else if (tntInOffhand) {
                    // TNT minecart in offhand only -> swap to sword so offhand places
                    MacroUtils.scheduleSwapFirstAvailable(SWORD_ITEMS, railToTntDelay);
                }
            }
            previousCounts.put(railItem, currentCount);
        }

        // Track tnt_minecart count (suppression is checked by RailMacro via isBowShotRecent)
        int tntMinecartCount = MacroUtils.countInInventory(player, Items.TNT_MINECART);
        previousCounts.put(Items.TNT_MINECART, tntMinecartCount);
    }
}

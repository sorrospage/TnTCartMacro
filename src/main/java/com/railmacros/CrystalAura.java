package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;

/**
 * CrystalAura Module: While the player holds right click on obsidian while holding end crystals,
 * repeatedly places and breaks end crystals.
 *
 * Has configurable place delay and break delay in milliseconds (0-350ms).
 * Toggled from menu only.
 */
public class CrystalAura {

    private boolean enabled = false;

    // Configurable delays in milliseconds
    private int placeMinDelay = 0;
    private int placeMaxDelay = 50;
    private int breakMinDelay = 0;
    private int breakMaxDelay = 50;

    // State machine
    private enum State {
        IDLE,
        WAIT_TO_PLACE,   // Waiting for place delay before placing crystal
        WAIT_TO_BREAK    // Waiting for break delay before breaking crystal
    }

    private State state = State.IDLE;
    private long actionTimeMs = -1; // System.currentTimeMillis() when the delay expires

    // ---- Getters and setters ----

    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        reset();
    }

    public int getPlaceMinDelay() { return placeMinDelay; }
    public void setPlaceMinDelay(int v) { placeMinDelay = Math.max(0, Math.min(v, placeMaxDelay)); }

    public int getPlaceMaxDelay() { return placeMaxDelay; }
    public void setPlaceMaxDelay(int v) { placeMaxDelay = Math.max(placeMinDelay, Math.min(350, v)); }

    public int getBreakMinDelay() { return breakMinDelay; }
    public void setBreakMinDelay(int v) { breakMinDelay = Math.max(0, Math.min(v, breakMaxDelay)); }

    public int getBreakMaxDelay() { return breakMaxDelay; }
    public void setBreakMaxDelay(int v) { breakMaxDelay = Math.max(breakMinDelay, Math.min(350, v)); }

    private void reset() {
        state = State.IDLE;
        actionTimeMs = -1;
    }

    /**
     * Called every client tick. Handles the place/break crystal loop.
     * Uses millisecond-based delays for precise timing.
     */
    public void tick(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        // Only active while right click is held
        if (!client.options.useKey.isPressed()) {
            reset();
            return;
        }

        // Must be holding end crystals in main hand
        if (player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
            reset();
            return;
        }

        switch (state) {
            case IDLE -> {
                // Check if looking at obsidian or bedrock (valid crystal placement surfaces)
                if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
                    BlockPos pos = blockHit.getBlockPos();
                    var blockState = client.world.getBlockState(pos);
                    if (blockState.isOf(Blocks.OBSIDIAN) || blockState.isOf(Blocks.BEDROCK)) {
                        // Start the place cycle
                        state = State.WAIT_TO_PLACE;
                        actionTimeMs = System.currentTimeMillis() + randomDelay(placeMinDelay, placeMaxDelay);
                    }
                }
            }
            case WAIT_TO_PLACE -> {
                if (System.currentTimeMillis() >= actionTimeMs) {
                    // Place a crystal (right-click interaction)
                    if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
                        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);
                        player.swingHand(Hand.MAIN_HAND);
                    }
                    // Transition to break phase
                    state = State.WAIT_TO_BREAK;
                    actionTimeMs = System.currentTimeMillis() + randomDelay(breakMinDelay, breakMaxDelay);
                }
            }
            case WAIT_TO_BREAK -> {
                if (System.currentTimeMillis() >= actionTimeMs) {
                    // Find and break the nearest end crystal we're looking at
                    boolean broke = false;
                    if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                        EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
                        Entity entity = entityHit.getEntity();
                        if (entity instanceof EndCrystalEntity) {
                            client.interactionManager.attackEntity(player, entity);
                            player.swingHand(Hand.MAIN_HAND);
                            broke = true;
                        }
                    }

                    // If we didn't hit a crystal directly, search nearby
                    if (!broke && client.world != null) {
                        for (Entity entity : client.world.getEntities()) {
                            if (entity instanceof EndCrystalEntity && player.squaredDistanceTo(entity) <= 16.0) {
                                client.interactionManager.attackEntity(player, entity);
                                player.swingHand(Hand.MAIN_HAND);
                                broke = true;
                                break;
                            }
                        }
                    }

                    // Loop back to place
                    state = State.WAIT_TO_PLACE;
                    actionTimeMs = System.currentTimeMillis() + randomDelay(placeMinDelay, placeMaxDelay);
                }
            }
        }
    }

    private int randomDelay(int min, int max) {
        if (min >= max) return min;
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}

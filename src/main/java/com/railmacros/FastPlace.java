package com.railmacros;

import com.railmacros.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * FastPlace Module: When holding right-click, repeats right-click input
 * at a configurable delay interval (randomized between min and max ms).
 * Supports half-tick mode for ~25ms placement intervals.
 */
public class FastPlace {

    private boolean enabled = false;

    // Configurable delay range (ms)
    private int minDelayMs = 0;
    private int maxDelayMs = 50;

    // Half-tick mode: places between ticks for ~25ms intervals
    private boolean halfTick = false;

    // Internal state
    private boolean wasHoldingUse = false;
    private long lastPlaceTimeMs = 0;
    private int currentDelayMs = 0;

    // Scheduler for half-tick placement
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "FastPlace-Scheduler");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> halfTickTask = null;

    // ---- Getters and setters ----

    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        wasHoldingUse = false;
        cancelHalfTickTask();
    }

    public int getMinDelayMs() { return minDelayMs; }
    public void setMinDelayMs(int v) { minDelayMs = Math.max(0, Math.min(v, maxDelayMs)); }

    public int getMaxDelayMs() { return maxDelayMs; }
    public void setMaxDelayMs(int v) { maxDelayMs = Math.max(minDelayMs, Math.min(500, v)); }

    public boolean isHalfTick() { return halfTick; }
    public void setHalfTick(boolean v) { halfTick = v; }

    /** Returns true if the held item should not be fast-placed (chargeable, edible, or throwable). */
    private boolean shouldSkipItem(MinecraftClient client) {
        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        // Crossbow: skip only if unloaded (reloading). Allow fast-fire when loaded.
        if (item == Items.CROSSBOW) {
            net.minecraft.component.type.ChargedProjectilesComponent charged =
                    stack.getComponents().get(net.minecraft.component.DataComponentTypes.CHARGED_PROJECTILES);
            // Skip if unloaded (no projectiles or empty component)
            return charged == null || charged.isEmpty();
        }
        // Other chargeable items — always skip
        if (item == Items.BOW || item == Items.TRIDENT || item == Items.SHIELD) return true;
        // Experience bottle
        if (item == Items.EXPERIENCE_BOTTLE) return true;
        // Edible items (any item with a food component)
        if (stack.getComponents().get(net.minecraft.component.DataComponentTypes.FOOD) != null) return true;
        return false;
    }

    public void tick(MinecraftClient client) {
        if (!enabled) return;
        if (client.player == null) return;

        // Don't interfere with chargeable, edible, or throwable items
        if (shouldSkipItem(client)) {
            wasHoldingUse = false;
            cancelHalfTickTask();
            return;
        }

        boolean holdingUse = client.options.useKey.isPressed();

        if (holdingUse) {
            long now = System.currentTimeMillis();
            if (!wasHoldingUse) {
                // Just started holding - initialize timing
                wasHoldingUse = true;
                lastPlaceTimeMs = now;
                currentDelayMs = randomDelay();
            } else if (now - lastPlaceTimeMs >= currentDelayMs) {
                // Enough time has passed - allow the next right-click by resetting cooldown
                ((MinecraftClientAccessor) client).setItemUseCooldown(0);
                lastPlaceTimeMs = now;
                currentDelayMs = randomDelay();

                // Schedule a mid-tick placement if half-tick mode is on
                if (halfTick) {
                    scheduleHalfTickPlace(client);
                }
            }
        } else {
            wasHoldingUse = false;
            cancelHalfTickTask();
        }
    }

    private void scheduleHalfTickPlace(MinecraftClient client) {
        cancelHalfTickTask();
        halfTickTask = SCHEDULER.schedule(() -> {
            client.execute(() -> {
                if (!enabled || client.player == null) return;
                if (!client.options.useKey.isPressed()) return;
                // Reset cooldown and trigger a use action mid-tick
                ((MinecraftClientAccessor) client).setItemUseCooldown(0);
                ((MinecraftClientAccessor) client).invokeDoItemUse();
            });
        }, 25, TimeUnit.MILLISECONDS);
    }

    private void cancelHalfTickTask() {
        if (halfTickTask != null && !halfTickTask.isDone()) {
            halfTickTask.cancel(false);
            halfTickTask = null;
        }
    }

    private int randomDelay() {
        if (minDelayMs >= maxDelayMs) return minDelayMs;
        return ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
    }
}

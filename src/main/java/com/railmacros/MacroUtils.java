package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacroUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger("RailMacros");

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "RailMacros-Scheduler");
                t.setDaemon(true);
                return t;
            });

    /**
     * Find the hotbar slot index (0-8) containing the given item.
     * Returns -1 if not found.
     */
    public static int findHotbarSlot(ClientPlayerEntity player, Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Switch the player's selected hotbar slot to the slot containing the target item.
     * This runs on the main client thread.
     */
    public static void swapToItem(Item targetItem) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;
            int slot = findHotbarSlot(player, targetItem);
            if (slot != -1) {
                player.getInventory().setSelectedSlot(slot);
            }
        });
    }

    /**
     * Find the first available item from the candidates array in the hotbar and swap to it.
     * This runs on the main client thread.
     */
    public static void swapToFirstAvailable(Item[] candidates) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;
            for (Item item : candidates) {
                int slot = findHotbarSlot(player, item);
                if (slot != -1) {
                    player.getInventory().setSelectedSlot(slot);
                    return;
                }
            }
        });
    }

    /**
     * Schedule a hotbar swap to the first available item from candidates after a fixed delay.
     */
    public static void scheduleSwapFirstAvailable(Item[] candidates, int delayMs) {
        SCHEDULER.schedule(() -> swapToFirstAvailable(candidates), delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule a hotbar swap to the target item after a random delay within the given range.
     */
    public static void scheduleSwap(Item targetItem, int minDelayMs, int maxDelayMs) {
        int delay = ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
        SCHEDULER.schedule(() -> swapToItem(targetItem), delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule a hotbar swap to the target item after a fixed delay.
     */
    public static void scheduleSwap(Item targetItem, int delayMs) {
        SCHEDULER.schedule(() -> swapToItem(targetItem), delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Count how many of a given item are in the player's hotbar.
     */
    public static int countInHotbar(ClientPlayerEntity player, Item item) {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Count how many of a given item are in the player's entire inventory.
     */
    public static int countInInventory(ClientPlayerEntity player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
}

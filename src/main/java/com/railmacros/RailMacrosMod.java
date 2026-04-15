package com.railmacros;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class RailMacrosMod implements ClientModInitializer {

    public static final RailMacro RAIL_MACRO = new RailMacro();
    public static final BowMacro BOW_MACRO = new BowMacro();
    public static final TriggerBot TRIGGER_BOT = new TriggerBot();
    public static final AutoSprint AUTO_SPRINT = new AutoSprint();
    public static final ShieldBreaker SHIELD_BREAKER = new ShieldBreaker();
    public static final SafeAnchor SAFE_ANCHOR = new SafeAnchor();
    public static final ElytraSwap ELYTRA_SWAP = new ElytraSwap();
    public static final RocketUse ROCKET_USE = new RocketUse();

    private static KeyBinding railMacroToggle;
    private static KeyBinding bowMacroToggle;
    private static KeyBinding triggerBotToggle;
    private static KeyBinding menuToggle;

    // Track whether a screen was open last tick so we can reset counts on close
    private boolean wasScreenOpen = false;

    @Override
    public void onInitializeClient() {
        // Register key binding for "]" to toggle rail macro
        railMacroToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.railmacros.toggle_rail",
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                KeyBinding.Category.MISC
        ));

        // Register key binding for "z" to toggle bow macro
        bowMacroToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.railmacros.toggle_bow",
                GLFW.GLFW_KEY_Z,
                KeyBinding.Category.MISC
        ));

        // Register key binding for "PageDown" to toggle triggerbot
        triggerBotToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.railmacros.toggle_triggerbot",
                GLFW.GLFW_KEY_PAGE_DOWN,
                KeyBinding.Category.MISC
        ));

        // Register key binding for "Pause" to open module menu
        menuToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.railmacros.menu",
                GLFW.GLFW_KEY_PAUSE,
                KeyBinding.Category.MISC
        ));

        // Register tick event for inventory monitoring and key handling
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Register per-frame callback for frame-based swap processing
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                RAIL_MACRO.onFrame(mc);
                SAFE_ANCHOR.onFrame(mc);
                ELYTRA_SWAP.onFrame(mc);
                ROCKET_USE.onFrame(mc);
            }
        });
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) return;

        // Handle key toggles (silent - no on-screen message)
        while (railMacroToggle.wasPressed()) {
            RAIL_MACRO.toggle();
        }

        while (bowMacroToggle.wasPressed()) {
            BOW_MACRO.toggle();
        }

        while (triggerBotToggle.wasPressed()) {
            TRIGGER_BOT.toggle();
        }

        while (menuToggle.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new ModMenuScreen());
            }
        }

        // Pause macros while any screen (inventory, chest, etc.) is open
        // but not our own menu screen
        boolean isScreenOpen = client.currentScreen != null && !(client.currentScreen instanceof ModMenuScreen);
        if (isScreenOpen) {
            wasScreenOpen = true;
            return;
        }

        // If screen just closed, reset item counts to avoid false triggers
        // (inventory counts may have changed while the screen was open)
        if (wasScreenOpen) {
            wasScreenOpen = false;
            RAIL_MACRO.resetCounts(client);
            BOW_MACRO.resetCounts(client);
            SAFE_ANCHOR.resetCounts(client);
        }

        // Handle ElytraSwap trigger: key "4" (GLFW_KEY_4)
        if (InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_4)) {
            ELYTRA_SWAP.trigger(client);
        }
        // Handle RocketUse trigger: Mouse Button 5 (forward side button = GLFW_MOUSE_BUTTON_5)
        if (InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_MOUSE_BUTTON_5)) {
            ROCKET_USE.trigger(client);
        }

        // Tick all macros
        RAIL_MACRO.tick(client);
        BOW_MACRO.tick(client);
        TRIGGER_BOT.tick(client);
        AUTO_SPRINT.tick(client);
        SHIELD_BREAKER.tick(client);
        SAFE_ANCHOR.tick(client);
    }
}

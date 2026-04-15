package com.railmacros;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Module menu opened with the Pause key.
 * Each module has a toggle button and a collapsible dropdown with settings sliders.
 */
public class ModMenuScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int SLIDER_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;
    private static final int SPACING = 24;

    // Track which dropdown sections are expanded
    private boolean xbowExpanded = false;
    private boolean instaCartExpanded = false;
    private boolean triggerBotExpanded = false;
    private boolean autoSprintExpanded = false;
    private boolean shieldBreakerExpanded = false;
    private boolean safeAnchorExpanded = false;
    private boolean elytraSwapExpanded = false;
    private boolean rocketUseExpanded = false;
    private boolean crystalAuraExpanded = false;
    private boolean stunSlamExpanded = false;
    private boolean autoMaceExpanded = false;

    // Track slider widgets per section so we can show/hide them
    private final List<ClickableWidget> xbowSliders = new ArrayList<>();
    private final List<ClickableWidget> instaCartSliders = new ArrayList<>();
    private final List<ClickableWidget> triggerBotSliders = new ArrayList<>();
    private final List<ClickableWidget> autoSprintSliders = new ArrayList<>();
    private final List<ClickableWidget> shieldBreakerSliders = new ArrayList<>();
    private final List<ClickableWidget> safeAnchorSliders = new ArrayList<>();
    private final List<ClickableWidget> elytraSwapSliders = new ArrayList<>();
    private final List<ClickableWidget> rocketUseSliders = new ArrayList<>();
    private final List<ClickableWidget> crystalAuraSliders = new ArrayList<>();
    private final List<ClickableWidget> stunSlamSliders = new ArrayList<>();
    private final List<ClickableWidget> autoMaceSliders = new ArrayList<>();

    public ModMenuScreen() {
        super(Text.literal("Rappture Client"));
    }

    @Override
    protected void init() {
        super.init();
        xbowSliders.clear();
        instaCartSliders.clear();
        triggerBotSliders.clear();
        autoSprintSliders.clear();
        shieldBreakerSliders.clear();
        safeAnchorSliders.clear();
        elytraSwapSliders.clear();
        rocketUseSliders.clear();
        crystalAuraSliders.clear();
        stunSlamSliders.clear();
        autoMaceSliders.clear();

        int centerX = this.width / 2 - BUTTON_WIDTH / 2;
        int halfWidth = (BUTTON_WIDTH - 4) / 2;
        int y = 30;

        // ===== Xbow Macro =====
        // Toggle + dropdown button on the same row
        addDrawableChild(ButtonWidget.builder(getXbowText(), button -> {
            RailMacrosMod.RAIL_MACRO.toggle();
            button.setMessage(getXbowText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(xbowExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { xbowExpanded = !xbowExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (xbowExpanded) {
            RailMacro rm = RailMacrosMod.RAIL_MACRO;
            y = addSlider(xbowSliders, centerX, y, "Rail\u2192TNT Min", 0, 10, rm.getRailToTntMinDelay(),
                    v -> rm.setRailToTntMinDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
            y = addSlider(xbowSliders, centerX, y, "Rail\u2192TNT Max", 0, 10, rm.getRailToTntMaxDelay(),
                    v -> rm.setRailToTntMaxDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
            y = addSlider(xbowSliders, centerX, y, "TNT\u2192Flint Min", 0, 10, rm.getTntToFlintMinDelay(),
                    v -> rm.setTntToFlintMinDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
            y = addSlider(xbowSliders, centerX, y, "TNT\u2192Flint Max", 0, 10, rm.getTntToFlintMaxDelay(),
                    v -> rm.setTntToFlintMaxDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
            y = addSlider(xbowSliders, centerX, y, "Bow Suppress", 0, 2000, rm.getBowSuppressionMs(),
                    v -> rm.setBowSuppressionMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
        }

        // ===== InstaCart Macro =====
        addDrawableChild(ButtonWidget.builder(getInstaCartText(), button -> {
            RailMacrosMod.BOW_MACRO.toggle();
            button.setMessage(getInstaCartText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(instaCartExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { instaCartExpanded = !instaCartExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (instaCartExpanded) {
            BowMacro bm = RailMacrosMod.BOW_MACRO;
            y = addSlider(instaCartSliders, centerX, y, "Bow\u2192Rail Delay", 0, 500, bm.getBowToRailDelay(),
                    v -> bm.setBowToRailDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(instaCartSliders, centerX, y, "Rail\u2192TNT Delay", 0, 500, bm.getRailToTntDelay(),
                    v -> bm.setRailToTntDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(instaCartSliders, centerX, y, "Suppress Window", 0, 2000, bm.getSuppressionWindowMs(),
                    v -> bm.setSuppressionWindowMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
        }

        // ===== TriggerBot =====
        addDrawableChild(ButtonWidget.builder(getTriggerBotText(), button -> {
            RailMacrosMod.TRIGGER_BOT.toggle();
            button.setMessage(getTriggerBotText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(triggerBotExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { triggerBotExpanded = !triggerBotExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (triggerBotExpanded) {
            TriggerBot tb = RailMacrosMod.TRIGGER_BOT;
            y = addSlider(triggerBotSliders, centerX, y, "Miss Chance", 0.0, 0.50, tb.getMissChance(),
                    tb::setMissChance, v -> String.format("%.0f%%", v * 100));
            y = addSlider(triggerBotSliders, centerX, y, "Min Delay", 0, 500, tb.getMinReactionDelayMs(),
                    v -> tb.setMinReactionDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(triggerBotSliders, centerX, y, "Max Delay", 0, 500, tb.getMaxReactionDelayMs(),
                    v -> tb.setMaxReactionDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(triggerBotSliders, centerX, y, "Sprint CD Min", 0.50, 1.50, tb.getSprintCooldownMin(),
                    v -> tb.setSprintCooldownMin(v.floatValue()), v -> String.format("%.0f%%", v * 100));
            y = addSlider(triggerBotSliders, centerX, y, "Sprint CD Max", 0.50, 1.50, tb.getSprintCooldownMax(),
                    v -> tb.setSprintCooldownMax(v.floatValue()), v -> String.format("%.0f%%", v * 100));
            y = addSlider(triggerBotSliders, centerX, y, "Crit CD Min", 0.50, 1.50, tb.getCritCooldownMin(),
                    v -> tb.setCritCooldownMin(v.floatValue()), v -> String.format("%.0f%%", v * 100));
            y = addSlider(triggerBotSliders, centerX, y, "Crit CD Max", 0.50, 1.50, tb.getCritCooldownMax(),
                    v -> tb.setCritCooldownMax(v.floatValue()), v -> String.format("%.0f%%", v * 100));
            y = addSlider(triggerBotSliders, centerX, y, "Sweep CD Min", 0.50, 1.50, tb.getSweepCooldownMin(),
                    v -> tb.setSweepCooldownMin(v.floatValue()), v -> String.format("%.0f%%", v * 100));
            y = addSlider(triggerBotSliders, centerX, y, "Sweep CD Max", 0.50, 1.50, tb.getSweepCooldownMax(),
                    v -> tb.setSweepCooldownMax(v.floatValue()), v -> String.format("%.0f%%", v * 100));
        }

        // ===== ShieldBreaker =====
        addDrawableChild(ButtonWidget.builder(getShieldBreakerText(), button -> {
            RailMacrosMod.SHIELD_BREAKER.toggle();
            button.setMessage(getShieldBreakerText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(shieldBreakerExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { shieldBreakerExpanded = !shieldBreakerExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (shieldBreakerExpanded) {
            ShieldBreaker sb = RailMacrosMod.SHIELD_BREAKER;
            y = addSlider(shieldBreakerSliders, centerX, y, "Miss Chance", 0.0, 0.50, sb.getMissChance(),
                    sb::setMissChance, v -> String.format("%.0f%%", v * 100));
            y = addSlider(shieldBreakerSliders, centerX, y, "Min Delay", 0, 500, sb.getMinDelayMs(),
                    v -> sb.setMinDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(shieldBreakerSliders, centerX, y, "Max Delay", 0, 500, sb.getMaxDelayMs(),
                    v -> sb.setMaxDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
        }

        // ===== SafeAnchor =====
        addDrawableChild(ButtonWidget.builder(getSafeAnchorText(), button -> {
            RailMacrosMod.SAFE_ANCHOR.toggle();
            button.setMessage(getSafeAnchorText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(safeAnchorExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { safeAnchorExpanded = !safeAnchorExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (safeAnchorExpanded) {
            SafeAnchor sa = RailMacrosMod.SAFE_ANCHOR;
            y = addSlider(safeAnchorSliders, centerX, y, "Min Delay", 0, 10, sa.getMinDelay(),
                    v -> sa.setMinDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
            y = addSlider(safeAnchorSliders, centerX, y, "Max Delay", 0, 10, sa.getMaxDelay(),
                    v -> sa.setMaxDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
        }

        // ===== ElytraSwap =====
        addDrawableChild(ButtonWidget.builder(getElytraSwapText(), button -> {
            RailMacrosMod.ELYTRA_SWAP.toggle();
            button.setMessage(getElytraSwapText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(elytraSwapExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { elytraSwapExpanded = !elytraSwapExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (elytraSwapExpanded) {
            ElytraSwap es = RailMacrosMod.ELYTRA_SWAP;
            y = addSlider(elytraSwapSliders, centerX, y, "Min Delay", 0, 10, es.getMinDelay(),
                    v -> es.setMinDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
            y = addSlider(elytraSwapSliders, centerX, y, "Max Delay", 0, 10, es.getMaxDelay(),
                    v -> es.setMaxDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
        }

        // ===== RocketUse =====
        addDrawableChild(ButtonWidget.builder(getRocketUseText(), button -> {
            RailMacrosMod.ROCKET_USE.toggle();
            button.setMessage(getRocketUseText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(rocketUseExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { rocketUseExpanded = !rocketUseExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (rocketUseExpanded) {
            RocketUse ru = RailMacrosMod.ROCKET_USE;
            y = addSlider(rocketUseSliders, centerX, y, "Min Delay", 0, 10, ru.getMinDelay(),
                    v -> ru.setMinDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
            y = addSlider(rocketUseSliders, centerX, y, "Max Delay", 0, 10, ru.getMaxDelay(),
                    v -> ru.setMaxDelay((int) Math.round(v)), v -> String.format("%df", (int) Math.round(v)));
        }

        // ===== CrystalAura =====
        addDrawableChild(ButtonWidget.builder(getCrystalAuraText(), button -> {
            RailMacrosMod.CRYSTAL_AURA.toggle();
            button.setMessage(getCrystalAuraText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(crystalAuraExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { crystalAuraExpanded = !crystalAuraExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (crystalAuraExpanded) {
            CrystalAura ca = RailMacrosMod.CRYSTAL_AURA;
            y = addSlider(crystalAuraSliders, centerX, y, "Place Min", 0, 350, ca.getPlaceMinDelay(),
                    v -> ca.setPlaceMinDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(crystalAuraSliders, centerX, y, "Place Max", 0, 350, ca.getPlaceMaxDelay(),
                    v -> ca.setPlaceMaxDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(crystalAuraSliders, centerX, y, "Break Min", 0, 350, ca.getBreakMinDelay(),
                    v -> ca.setBreakMinDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(crystalAuraSliders, centerX, y, "Break Max", 0, 350, ca.getBreakMaxDelay(),
                    v -> ca.setBreakMaxDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
        }

        // ===== StunSlam =====
        addDrawableChild(ButtonWidget.builder(getStunSlamText(), button -> {
            RailMacrosMod.STUN_SLAM.toggle();
            button.setMessage(getStunSlamText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(stunSlamExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { stunSlamExpanded = !stunSlamExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (stunSlamExpanded) {
            StunSlam ss = RailMacrosMod.STUN_SLAM;
            y = addSlider(stunSlamSliders, centerX, y, "Min Delay", 0, 500, ss.getMinDelayMs(),
                    v -> ss.setMinDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(stunSlamSliders, centerX, y, "Max Delay", 0, 500, ss.getMaxDelayMs(),
                    v -> ss.setMaxDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
        }

        // ===== AutoMace =====
        addDrawableChild(ButtonWidget.builder(getAutoMaceText(), button -> {
            RailMacrosMod.AUTO_MACE.toggle();
            button.setMessage(getAutoMaceText());
            ModConfig.save();
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(autoMaceExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { autoMaceExpanded = !autoMaceExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (autoMaceExpanded) {
            AutoMace am = RailMacrosMod.AUTO_MACE;
            y = addSlider(autoMaceSliders, centerX, y, "Min Delay", 0, 500, am.getMinDelayMs(),
                    v -> am.setMinDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(autoMaceSliders, centerX, y, "Max Delay", 0, 500, am.getMaxDelayMs(),
                    v -> am.setMaxDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
        }

        // ===== AutoSprint =====
        addDrawableChild(ButtonWidget.builder(getAutoSprintText(), button -> {
            RailMacrosMod.AUTO_SPRINT.toggle();
            button.setMessage(getAutoSprintText());
            ModConfig.save();
        }).dimensions(centerX, y, BUTTON_WIDTH, WIDGET_HEIGHT).build());

        y += SPACING;
    }

    private int addSlider(List<ClickableWidget> list, int x, int y, String label,
                          double min, double max, double current,
                          java.util.function.Consumer<Double> onChange,
                          java.util.function.Function<Double, String> formatter) {
        ConfigSliderWidget slider = new ConfigSliderWidget(x, y, SLIDER_WIDTH, WIDGET_HEIGHT,
                label, min, max, current, onChange, formatter);
        list.add(slider);
        addDrawableChild(slider);
        return y + SPACING;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        // Draw "Rappture Client" title prominently at top
        int titleWidth = this.textRenderer.getWidth(this.title);
        int titleX = this.width / 2 - titleWidth / 2;
        int titleY = 10;
        // Draw dark background behind title for visibility
        context.fill(titleX - 4, titleY - 2, titleX + titleWidth + 4, titleY + 12, 0xAA000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, titleY, 0xFFFFFF);
    }

    @Override
    public void close() {
        // Save config whenever the menu is closed
        ModConfig.save();
        super.close();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private Text getXbowText() {
        boolean on = RailMacrosMod.RAIL_MACRO.isEnabled();
        return Text.literal("Xbow Macro: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getInstaCartText() {
        boolean on = RailMacrosMod.BOW_MACRO.isEnabled();
        return Text.literal("InstaCart: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getTriggerBotText() {
        boolean on = RailMacrosMod.TRIGGER_BOT.isEnabled();
        return Text.literal("TriggerBot: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getShieldBreakerText() {
        boolean on = RailMacrosMod.SHIELD_BREAKER.isEnabled();
        return Text.literal("ShieldBreaker: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getSafeAnchorText() {
        boolean on = RailMacrosMod.SAFE_ANCHOR.isEnabled();
        return Text.literal("SafeAnchor: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getElytraSwapText() {
        boolean on = RailMacrosMod.ELYTRA_SWAP.isEnabled();
        return Text.literal("ElytraSwap: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getRocketUseText() {
        boolean on = RailMacrosMod.ROCKET_USE.isEnabled();
        return Text.literal("RocketUse: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getCrystalAuraText() {
        boolean on = RailMacrosMod.CRYSTAL_AURA.isEnabled();
        return Text.literal("CrystalAura: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getStunSlamText() {
        boolean on = RailMacrosMod.STUN_SLAM.isEnabled();
        return Text.literal("StunSlam: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getAutoMaceText() {
        boolean on = RailMacrosMod.AUTO_MACE.isEnabled();
        return Text.literal("AutoMace: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getAutoSprintText() {
        boolean on = RailMacrosMod.AUTO_SPRINT.isEnabled();
        return Text.literal("AutoSprint: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }
}

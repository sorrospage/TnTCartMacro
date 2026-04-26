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

    // Track slider widgets per section so we can show/hide them
    private final List<ClickableWidget> xbowSliders = new ArrayList<>();
    private final List<ClickableWidget> instaCartSliders = new ArrayList<>();
    private final List<ClickableWidget> triggerBotSliders = new ArrayList<>();
    private final List<ClickableWidget> autoSprintSliders = new ArrayList<>();
    private final List<ClickableWidget> shieldBreakerSliders = new ArrayList<>();

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

            // Crossbow swap sub-toggle
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("  Xbow Swap: " + (rm.isCrossbowSwapEnabled() ? "\u00a7aON" : "\u00a7cOFF")),
                    button -> {
                        rm.setCrossbowSwapEnabled(!rm.isCrossbowSwapEnabled());
                        button.setMessage(Text.literal("  Xbow Swap: " + (rm.isCrossbowSwapEnabled() ? "\u00a7aON" : "\u00a7cOFF")));
                        ModConfig.save();
                    }
            ).dimensions(centerX, y, BUTTON_WIDTH, WIDGET_HEIGHT).build());
            y += SPACING;

            if (rm.isCrossbowSwapEnabled()) {
                y = addSlider(xbowSliders, centerX, y, "Flint\u2192Xbow Min", 0, 2000, rm.getFlintToXbowMinDelayMs(),
                        v -> rm.setFlintToXbowMinDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
                y = addSlider(xbowSliders, centerX, y, "Flint\u2192Xbow Max", 0, 2000, rm.getFlintToXbowMaxDelayMs(),
                        v -> rm.setFlintToXbowMaxDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            }
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

    private Text getAutoSprintText() {
        boolean on = RailMacrosMod.AUTO_SPRINT.isEnabled();
        return Text.literal("AutoSprint: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }
}

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
    private boolean fastPlaceExpanded = false;
    private boolean spearMacroExpanded = false;

    // Track slider widgets per section so we can show/hide them
    private final List<ClickableWidget> xbowSliders = new ArrayList<>();
    private final List<ClickableWidget> instaCartSliders = new ArrayList<>();
    private final List<ClickableWidget> triggerBotSliders = new ArrayList<>();
    private final List<ClickableWidget> fastPlaceSliders = new ArrayList<>();
    private final List<ClickableWidget> spearMacroSliders = new ArrayList<>();

    public ModMenuScreen() {
        super(Text.literal("Modules"));
    }

    @Override
    protected void init() {
        super.init();
        xbowSliders.clear();
        instaCartSliders.clear();
        triggerBotSliders.clear();
        fastPlaceSliders.clear();
        spearMacroSliders.clear();

        int centerX = this.width / 2 - BUTTON_WIDTH / 2;
        int halfWidth = (BUTTON_WIDTH - 4) / 2;
        int y = 30;

        // ===== Xbow Macro =====
        // Toggle + dropdown button on the same row
        addDrawableChild(ButtonWidget.builder(getXbowText(), button -> {
            RailMacrosMod.RAIL_MACRO.toggle();
            button.setMessage(getXbowText());
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(xbowExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { xbowExpanded = !xbowExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (xbowExpanded) {
            RailMacro rm = RailMacrosMod.RAIL_MACRO;
            y = addSlider(xbowSliders, centerX, y, "Rail\u2192TNT Min", 0, 100, rm.getRailToTntMinDelay(),
                    v -> rm.setRailToTntMinDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(xbowSliders, centerX, y, "Rail\u2192TNT Max", 0, 100, rm.getRailToTntMaxDelay(),
                    v -> rm.setRailToTntMaxDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(xbowSliders, centerX, y, "TNT\u2192Flint Min", 0, 100, rm.getTntToFlintMinDelay(),
                    v -> rm.setTntToFlintMinDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(xbowSliders, centerX, y, "TNT\u2192Flint Max", 0, 100, rm.getTntToFlintMaxDelay(),
                    v -> rm.setTntToFlintMaxDelay((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(xbowSliders, centerX, y, "Bow Suppress", 0, 2000, rm.getBowSuppressionMs(),
                    v -> rm.setBowSuppressionMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
        }

        // ===== InstaCart Macro =====
        addDrawableChild(ButtonWidget.builder(getInstaCartText(), button -> {
            RailMacrosMod.BOW_MACRO.toggle();
            button.setMessage(getInstaCartText());
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

        // ===== FastPlace =====
        addDrawableChild(ButtonWidget.builder(getFastPlaceText(), button -> {
            RailMacrosMod.FAST_PLACE.toggle();
            button.setMessage(getFastPlaceText());
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(fastPlaceExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { fastPlaceExpanded = !fastPlaceExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (fastPlaceExpanded) {
            FastPlace fp = RailMacrosMod.FAST_PLACE;
            y = addSlider(fastPlaceSliders, centerX, y, "Min Delay", 0, 500, fp.getMinDelayMs(),
                    v -> fp.setMinDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(fastPlaceSliders, centerX, y, "Max Delay", 0, 500, fp.getMaxDelayMs(),
                    v -> fp.setMaxDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            // Half-tick toggle button
            ButtonWidget halfTickBtn = ButtonWidget.builder(
                    Text.literal("Half Tick: " + (fp.isHalfTick() ? "\u00a7aON" : "\u00a7cOFF")),
                    button -> {
                        fp.setHalfTick(!fp.isHalfTick());
                        button.setMessage(Text.literal("Half Tick: " + (fp.isHalfTick() ? "\u00a7aON" : "\u00a7cOFF")));
                    }
            ).dimensions(centerX, y, SLIDER_WIDTH, WIDGET_HEIGHT).build();
            fastPlaceSliders.add(halfTickBtn);
            addDrawableChild(halfTickBtn);
            y += SPACING;
        }
        // ===== Spear Macro =====
        addDrawableChild(ButtonWidget.builder(getSpearMacroText(), button -> {
            RailMacrosMod.SPEAR_MACRO.toggle();
            button.setMessage(getSpearMacroText());
        }).dimensions(centerX, y, halfWidth, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(spearMacroExpanded ? "\u00a77\u25BC Settings" : "\u00a77\u25B6 Settings"),
                button -> { spearMacroExpanded = !spearMacroExpanded; clearAndInit(); }
        ).dimensions(centerX + halfWidth + 4, y, halfWidth, WIDGET_HEIGHT).build());

        y += SPACING;
        if (spearMacroExpanded) {
            SpearMacro sm = RailMacrosMod.SPEAR_MACRO;
            y = addSlider(spearMacroSliders, centerX, y, "Pre-Swap Delay", 0, 500, sm.getPreSwapDelayMs(),
                    v -> sm.setPreSwapDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(spearMacroSliders, centerX, y, "Attack Delay", 0, 500, sm.getAttackDelayMs(),
                    v -> sm.setAttackDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
            y = addSlider(spearMacroSliders, centerX, y, "Swap-Back Delay", 0, 500, sm.getSwapBackDelayMs(),
                    v -> sm.setSwapBackDelayMs((int) Math.round(v)), v -> String.format("%dms", (int) Math.round(v)));
        }
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
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);
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

    private Text getFastPlaceText() {
        boolean on = RailMacrosMod.FAST_PLACE.isEnabled();
        return Text.literal("FastPlace: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Text getSpearMacroText() {
        boolean on = RailMacrosMod.SPEAR_MACRO.isEnabled();
        return Text.literal("Spear Macro: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }
}

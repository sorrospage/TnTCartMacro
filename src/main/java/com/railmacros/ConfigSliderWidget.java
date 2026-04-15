package com.railmacros;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A reusable slider widget that maps a 0-1 slider value to a configurable range.
 */
public class ConfigSliderWidget extends SliderWidget {

    private final String label;
    private final double minVal;
    private final double maxVal;
    private final Consumer<Double> onChange;
    private final Function<Double, String> formatter;

    /**
     * @param x         x position
     * @param y         y position
     * @param width     widget width
     * @param height    widget height
     * @param label     display label prefix
     * @param minVal    minimum real value
     * @param maxVal    maximum real value
     * @param current   current real value
     * @param onChange  callback when value changes (receives the real value)
     * @param formatter formats the real value for display
     */
    public ConfigSliderWidget(int x, int y, int width, int height,
                              String label, double minVal, double maxVal,
                              double current, Consumer<Double> onChange,
                              Function<Double, String> formatter) {
        super(x, y, width, height, Text.literal(label + ": " + formatter.apply(current)),
                (current - minVal) / (maxVal - minVal));
        this.label = label;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.onChange = onChange;
        this.formatter = formatter;
    }

    private double getRealValue() {
        return minVal + this.value * (maxVal - minVal);
    }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal(label + ": " + formatter.apply(getRealValue())));
    }

    @Override
    protected void applyValue() {
        onChange.accept(getRealValue());
    }
}

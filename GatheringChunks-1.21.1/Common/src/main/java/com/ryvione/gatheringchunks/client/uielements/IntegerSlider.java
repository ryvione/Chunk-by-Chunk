/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.client.uielements;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;
public class IntegerSlider extends AbstractSliderButton {
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;
    private final int minValue;
    private final int maxValue;
    private final Component valueName;
    public IntegerSlider(int x, int y, int width, int height, Component message, int minValue, int maxValue, Supplier<Integer> getter, Consumer<Integer> setter) {
        super(x, y, width, height, message, (double) (getter.get() - minValue) / (maxValue - minValue));
        this.valueName = message;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.getter = getter;
        this.setter = setter;
        updateMessage();
    }
    @Override
    protected void updateMessage() {
        setMessage(Component.literal(valueName.getString() + ": " + getter.get()));
    }
    @Override
    protected void applyValue() {
        setter.accept(Mth.floor(Mth.clamp(this.value, 0.0D, 1.0D) * (maxValue - minValue) + minValue));
    }
    public void setValue(int newValue) {
        if (newValue >= minValue && newValue <= maxValue) {
            setter.accept(newValue);
            value = (double) (getter.get() - minValue) / (maxValue - minValue);
            updateMessage();
        }
    }
}

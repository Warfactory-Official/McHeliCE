package com.norwood.mcheli.helper.client;

import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;

public class ProgressHolder {

    private static ProgressBar currentBar;

    public static void push(String title, int steps) {
        currentBar = ProgressManager.push(title, steps);
    }

    public static void step(String message) {
        currentBar.step(message);
    }

    public static void pop() {
        ProgressManager.pop(currentBar);
        currentBar = null;
    }
}

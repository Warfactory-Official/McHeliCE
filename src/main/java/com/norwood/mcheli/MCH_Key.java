package com.norwood.mcheli;

import com.norwood.mcheli.wrapper.W_KeyBinding;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class MCH_Key {

    public final int key;
    private boolean isPress;
    private boolean isBeforePress;

    public MCH_Key(int k) {
        this.key = k;
        this.isPress = false;
        this.isBeforePress = false;
    }

    public static boolean isKeyDown(int key) {
        if (key > 0) {
            return Keyboard.isKeyDown(key);
        } else {
            return key < 0 && Mouse.isButtonDown(key + 100);
        }
    }

    public static boolean isKeyDown(KeyBinding keyBind) {
        return isKeyDown(W_KeyBinding.getKeyCode(keyBind));
    }

    public boolean isKeyDown() {
        return !this.isBeforePress && this.isPress;
    }

    public boolean isKeyPress() {
        return this.isPress;
    }

    public boolean isKeyUp() {
        return this.isBeforePress && !this.isPress;
    }

    public void update() {
        if (key == 0) return;

        isBeforePress = isPress;

        if (key >= 0) {
            isPress = Keyboard.isKeyDown(key);
        } else {
            int mouseButton = key + 100;
            isPress = Mouse.isButtonDown(mouseButton);
        }
    }
}

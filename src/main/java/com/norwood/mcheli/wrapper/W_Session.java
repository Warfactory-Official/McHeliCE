package com.norwood.mcheli.wrapper;

import net.minecraft.util.Session;

public class W_Session {

    public static Session newSession() {
        return new Session("McHeliDummyEntity", "", "", "");
    }
}

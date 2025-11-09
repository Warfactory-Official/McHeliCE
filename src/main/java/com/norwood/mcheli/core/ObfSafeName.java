package com.norwood.mcheli.core;

public final class ObfSafeName {

    public final String mcp;
    public final String srg;

    public ObfSafeName(String mcp, String srg) {
        this.mcp = mcp;
        this.srg = srg;
    }

    public String getName() {
        return MCHCore.runtimeDeobfEnabled() ? this.srg : this.mcp;
    }

    public boolean matches(String name) {
        return this.mcp.equals(name) || this.srg.equals(name);
    }
}

package com.norwood.mcheli;

public abstract class MCH_InfoManagerBase<T extends MCH_BaseInfo> {

    protected void put(String name, T info) {}

    protected abstract boolean contains(String var1);

    protected abstract int size();
}

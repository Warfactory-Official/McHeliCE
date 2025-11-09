package com.norwood.mcheli.eval.eval.func;

public interface Function {

    long evalLong(Object var1, String var2, Long[] var3) throws Throwable;

    double evalDouble(Object var1, String var2, Double[] var3) throws Throwable;

    Object evalObject(Object var1, String var2, Object[] var3) throws Throwable;
}

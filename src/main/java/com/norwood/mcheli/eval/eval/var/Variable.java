package com.norwood.mcheli.eval.eval.var;

public interface Variable {

    void setValue(Object var1, Object var2);

    Object getObject(Object var1);

    long evalLong(Object var1);

    double evalDouble(Object var1);

    Object getObject(Object var1, int var2);

    void setValue(Object var1, int var2, Object var3);

    Object getObject(Object var1, String var2);

    void setValue(Object var1, String var2, Object var3);
}

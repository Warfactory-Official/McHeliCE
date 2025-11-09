package com.norwood.mcheli.eval.eval.func;

import java.lang.reflect.Method;

public class InvokeFunction implements Function {

    public static Object callMethod(Object obj, String name, Object[] args) throws Exception {
        Class<?> c = obj.getClass();
        Class<?>[] types = new Class[args.length];

        for (int i = 0; i < types.length; i++) {
            types[i] = args[i].getClass();
        }

        Method m = c.getMethod(name, types);
        return m.invoke(obj, args);
    }

    @Override
    public long evalLong(Object object, String name, Long[] args) throws Throwable {
        if (object == null) {
            return 0L;
        } else {
            Object r = callMethod(object, name, args);
            return ((Number) r).longValue();
        }
    }

    @Override
    public double evalDouble(Object object, String name, Double[] args) throws Throwable {
        if (object == null) {
            return 0.0;
        } else {
            Object r = callMethod(object, name, args);
            return ((Number) r).doubleValue();
        }
    }

    @Override
    public Object evalObject(Object object, String name, Object[] args) throws Throwable {
        return object == null ? null : callMethod(object, name, args);
    }
}

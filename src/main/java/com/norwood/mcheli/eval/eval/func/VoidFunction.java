package com.norwood.mcheli.eval.eval.func;

public class VoidFunction implements Function {

    @Override
    public long evalLong(Object object, String name, Long[] args) {
        System.out.println(object + "." + name + "関数が呼ばれた(long)");

        for (int i = 0; i < args.length; i++) {
            System.out.println("arg[" + i + "] " + args[i]);
        }

        return 0L;
    }

    @Override
    public double evalDouble(Object object, String name, Double[] args) {
        System.out.println(object + "." + name + "関数が呼ばれた(double)");

        for (int i = 0; i < args.length; i++) {
            System.out.println("arg[" + i + "] " + args[i]);
        }

        return 0.0;
    }

    @Override
    public Object evalObject(Object object, String name, Object[] args) {
        System.out.println(object + "." + name + "関数が呼ばれた(Object)");

        for (int i = 0; i < args.length; i++) {
            System.out.println("arg[" + i + "] " + args[i]);
        }

        return null;
    }
}

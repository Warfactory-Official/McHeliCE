package com.norwood.mcheli.eval.eval.oper;

public interface Operator {

    Object power(Object var1, Object var2);

    Object signPlus(Object var1);

    Object signMinus(Object var1);

    Object plus(Object var1, Object var2);

    Object minus(Object var1, Object var2);

    Object mult(Object var1, Object var2);

    Object div(Object var1, Object var2);

    Object mod(Object var1, Object var2);

    Object bitNot(Object var1);

    Object shiftLeft(Object var1, Object var2);

    Object shiftRight(Object var1, Object var2);

    Object shiftRightLogical(Object var1, Object var2);

    Object bitAnd(Object var1, Object var2);

    Object bitOr(Object var1, Object var2);

    Object bitXor(Object var1, Object var2);

    Object not(Object var1);

    Object equal(Object var1, Object var2);

    Object notEqual(Object var1, Object var2);

    Object lessThan(Object var1, Object var2);

    Object lessEqual(Object var1, Object var2);

    Object greaterThan(Object var1, Object var2);

    Object greaterEqual(Object var1, Object var2);

    boolean bool(Object var1);

    Object inc(Object var1, int var2);
}

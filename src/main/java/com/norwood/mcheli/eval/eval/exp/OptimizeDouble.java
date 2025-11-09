package com.norwood.mcheli.eval.eval.exp;

public class OptimizeDouble extends OptimizeObject {

    @Override
    protected boolean isTrue(AbstractExpression x) {
        return x.evalDouble() != 0.0;
    }

    @Override
    protected AbstractExpression toConst(AbstractExpression exp) {
        try {
            double val = exp.evalDouble();
            return NumberExpression.create(exp, Double.toString(val));
        } catch (Exception var4) {
            return exp;
        }
    }
}

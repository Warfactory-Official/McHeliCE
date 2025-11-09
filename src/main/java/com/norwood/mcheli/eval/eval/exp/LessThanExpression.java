package com.norwood.mcheli.eval.eval.exp;

public class LessThanExpression extends Col2Expression {

    public LessThanExpression() {
        this.setOperator("<");
    }

    protected LessThanExpression(LessThanExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new LessThanExpression(this, s);
    }

    @Override
    protected long operateLong(long vl, long vr) {
        return vl < vr ? 1L : 0L;
    }

    @Override
    protected double operateDouble(double vl, double vr) {
        return vl < vr ? 1.0 : 0.0;
    }

    @Override
    protected Object operateObject(Object vl, Object vr) {
        return this.share.oper.lessThan(vl, vr);
    }
}

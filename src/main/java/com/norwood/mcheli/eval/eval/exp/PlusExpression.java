package com.norwood.mcheli.eval.eval.exp;

public class PlusExpression extends Col2Expression {

    public PlusExpression() {
        this.setOperator("+");
    }

    protected PlusExpression(PlusExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new PlusExpression(this, s);
    }

    @Override
    protected long operateLong(long vl, long vr) {
        return vl + vr;
    }

    @Override
    protected double operateDouble(double vl, double vr) {
        return vl + vr;
    }

    @Override
    protected Object operateObject(Object vl, Object vr) {
        return this.share.oper.plus(vl, vr);
    }
}

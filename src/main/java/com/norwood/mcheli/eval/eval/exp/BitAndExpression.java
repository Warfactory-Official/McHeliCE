package com.norwood.mcheli.eval.eval.exp;

public class BitAndExpression extends Col2Expression {

    public BitAndExpression() {
        this.setOperator("&");
    }

    protected BitAndExpression(BitAndExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new BitAndExpression(this, s);
    }

    @Override
    protected long operateLong(long vl, long vr) {
        return vl & vr;
    }

    @Override
    protected double operateDouble(double vl, double vr) {
        return (long) vl & (long) vr;
    }

    @Override
    protected Object operateObject(Object vl, Object vr) {
        return this.share.oper.bitAnd(vl, vr);
    }
}

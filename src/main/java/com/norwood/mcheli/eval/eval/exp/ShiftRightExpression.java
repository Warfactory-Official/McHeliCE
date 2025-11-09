package com.norwood.mcheli.eval.eval.exp;

public class ShiftRightExpression extends Col2Expression {

    public ShiftRightExpression() {
        this.setOperator(">>");
    }

    protected ShiftRightExpression(ShiftRightExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new ShiftRightExpression(this, s);
    }

    @Override
    protected long operateLong(long vl, long vr) {
        return vl >> (int) vr;
    }

    @Override
    protected double operateDouble(double vl, double vr) {
        return vl / Math.pow(2.0, vr);
    }

    @Override
    protected Object operateObject(Object vl, Object vr) {
        return this.share.oper.shiftRight(vl, vr);
    }
}

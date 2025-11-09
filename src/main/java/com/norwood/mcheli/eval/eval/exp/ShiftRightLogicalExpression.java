package com.norwood.mcheli.eval.eval.exp;

public class ShiftRightLogicalExpression extends Col2Expression {

    public ShiftRightLogicalExpression() {
        this.setOperator(">>>");
    }

    protected ShiftRightLogicalExpression(ShiftRightLogicalExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new ShiftRightLogicalExpression(this, s);
    }

    @Override
    protected long operateLong(long vl, long vr) {
        return vl >>> (int) vr;
    }

    @Override
    protected double operateDouble(double vl, double vr) {
        if (vl < 0.0) {
            vl = -vl;
        }

        return vl / Math.pow(2.0, vr);
    }

    @Override
    protected Object operateObject(Object vl, Object vr) {
        return this.share.oper.shiftRightLogical(vl, vr);
    }
}

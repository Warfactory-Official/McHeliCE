package com.norwood.mcheli.eval.eval.exp;

public class ModExpression extends Col2Expression {

    public ModExpression() {
        this.setOperator("%");
    }

    protected ModExpression(ModExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new ModExpression(this, s);
    }

    @Override
    protected long operateLong(long vl, long vr) {
        return vl % vr;
    }

    @Override
    protected double operateDouble(double vl, double vr) {
        return vl % vr;
    }

    @Override
    protected Object operateObject(Object vl, Object vr) {
        return this.share.oper.mod(vl, vr);
    }
}

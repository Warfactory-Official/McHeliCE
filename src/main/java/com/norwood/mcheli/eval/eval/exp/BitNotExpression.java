package com.norwood.mcheli.eval.eval.exp;

public class BitNotExpression extends Col1Expression {

    public BitNotExpression() {
        this.setOperator("~");
    }

    protected BitNotExpression(BitNotExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new BitNotExpression(this, s);
    }

    @Override
    protected long operateLong(long val) {
        return ~val;
    }

    @Override
    protected double operateDouble(double val) {
        return ~((long) val);
    }

    @Override
    public Object evalObject() {
        return this.share.oper.bitNot(this.exp.evalObject());
    }
}

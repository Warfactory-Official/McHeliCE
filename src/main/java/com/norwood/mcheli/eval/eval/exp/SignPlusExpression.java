package com.norwood.mcheli.eval.eval.exp;

public class SignPlusExpression extends Col1Expression {

    public SignPlusExpression() {
        this.setOperator("+");
    }

    protected SignPlusExpression(SignPlusExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new SignPlusExpression(this, s);
    }

    @Override
    protected long operateLong(long val) {
        return val;
    }

    @Override
    protected double operateDouble(double val) {
        return val;
    }

    @Override
    public Object evalObject() {
        return this.share.oper.signPlus(this.exp.evalObject());
    }
}

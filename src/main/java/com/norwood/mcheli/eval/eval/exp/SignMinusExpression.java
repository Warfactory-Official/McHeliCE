package com.norwood.mcheli.eval.eval.exp;

public class SignMinusExpression extends Col1Expression {

    public SignMinusExpression() {
        this.setOperator("-");
    }

    protected SignMinusExpression(SignMinusExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new SignMinusExpression(this, s);
    }

    @Override
    protected long operateLong(long val) {
        return -val;
    }

    @Override
    protected double operateDouble(double val) {
        return -val;
    }

    @Override
    public Object evalObject() {
        return this.share.oper.signMinus(this.exp.evalObject());
    }
}

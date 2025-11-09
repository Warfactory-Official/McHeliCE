package com.norwood.mcheli.eval.eval.exp;

public class NotExpression extends Col1Expression {

    public NotExpression() {
        this.setOperator("!");
    }

    protected NotExpression(NotExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new NotExpression(this, s);
    }

    @Override
    protected long operateLong(long val) {
        return val == 0L ? 1L : 0L;
    }

    @Override
    protected double operateDouble(double val) {
        return val == 0.0 ? 1.0 : 0.0;
    }

    @Override
    public Object evalObject() {
        return this.share.oper.not(this.exp.evalObject());
    }
}

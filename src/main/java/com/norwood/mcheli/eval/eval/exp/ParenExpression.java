package com.norwood.mcheli.eval.eval.exp;

public class ParenExpression extends Col1Expression {

    public ParenExpression() {
        this.setOperator("(");
        this.setEndOperator(")");
    }

    protected ParenExpression(ParenExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new ParenExpression(this, s);
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
        return this.exp.evalObject();
    }

    @Override
    public String toString() {
        return this.exp == null ? "" : this.getOperator() + this.exp + this.getEndOperator();
    }
}

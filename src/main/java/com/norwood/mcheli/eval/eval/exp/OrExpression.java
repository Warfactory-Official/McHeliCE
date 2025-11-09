package com.norwood.mcheli.eval.eval.exp;

public class OrExpression extends Col2OpeExpression {

    public OrExpression() {
        this.setOperator("||");
    }

    protected OrExpression(OrExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new OrExpression(this, s);
    }

    @Override
    public long evalLong() {
        long val = this.expl.evalLong();
        return val != 0L ? val : this.expr.evalLong();
    }

    @Override
    public double evalDouble() {
        double val = this.expl.evalDouble();
        return val != 0.0 ? val : this.expr.evalDouble();
    }

    @Override
    public Object evalObject() {
        Object val = this.expl.evalObject();
        return this.share.oper.bool(val) ? val : this.expr.evalObject();
    }
}

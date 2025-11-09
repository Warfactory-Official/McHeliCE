package com.norwood.mcheli.eval.eval.exp;

import java.util.List;

public class FuncArgExpression extends Col2OpeExpression {

    public FuncArgExpression() {
        this.setOperator(",");
    }

    protected FuncArgExpression(FuncArgExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new FuncArgExpression(this, s);
    }

    @Override
    protected void evalArgsLong(List<Long> args) {
        this.expl.evalArgsLong(args);
        this.expr.evalArgsLong(args);
    }

    @Override
    protected void evalArgsDouble(List<Double> args) {
        this.expl.evalArgsDouble(args);
        this.expr.evalArgsDouble(args);
    }

    @Override
    protected void evalArgsObject(List<Object> args) {
        this.expl.evalArgsObject(args);
        this.expr.evalArgsObject(args);
    }

    @Override
    protected String toStringLeftSpace() {
        return "";
    }
}

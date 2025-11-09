package com.norwood.mcheli.eval.eval.exp;

import com.norwood.mcheli.eval.eval.repl.ReplaceAdapter;

public class OptimizeObject extends ReplaceAdapter {

    protected boolean isConst(AbstractExpression x) {
        return x instanceof NumberExpression || x instanceof StringExpression || x instanceof CharExpression;
    }

    protected boolean isTrue(AbstractExpression x) {
        return x.evalLong() != 0L;
    }

    protected AbstractExpression toConst(AbstractExpression exp) {
        try {
            Object val = exp.evalObject();
            if (val instanceof String) {
                return StringExpression.create(exp, (String) val);
            } else {
                return val instanceof Character ? CharExpression.create(exp, val.toString()) :
                        NumberExpression.create(exp, val.toString());
            }
        } catch (Exception var3) {
            return exp;
        }
    }

    @Override
    public AbstractExpression replace0(WordExpression exp) {
        return exp instanceof VariableExpression ? this.toConst(exp) : exp;
    }

    @Override
    public AbstractExpression replace1(Col1Expression exp) {
        if (exp instanceof ParenExpression) {
            return exp.exp;
        } else if (exp instanceof SignPlusExpression) {
            return exp.exp;
        } else {
            return this.isConst(exp.exp) ? this.toConst(exp) : exp;
        }
    }

    @Override
    public AbstractExpression replace2(Col2Expression exp) {
        boolean const_l = this.isConst(exp.expl);
        boolean const_r = this.isConst(exp.expr);
        return const_l && const_r ? this.toConst(exp) : exp;
    }

    @Override
    public AbstractExpression replace2(Col2OpeExpression exp) {
        if (exp instanceof ArrayExpression) {
            return this.isConst(exp.expr) ? this.toConst(exp) : exp;
        } else if (exp instanceof FieldExpression) {
            return this.toConst(exp);
        } else {
            boolean const_l = this.isConst(exp.expl);
            if (exp instanceof AndExpression) {
                if (const_l) {
                    return this.isTrue(exp.expl) ? exp.expr : exp.expl;
                } else {
                    return exp;
                }
            } else if (exp instanceof OrExpression) {
                if (const_l) {
                    return this.isTrue(exp.expl) ? exp.expl : exp.expr;
                } else {
                    return exp;
                }
            } else if (exp instanceof CommaExpression) {
                return const_l ? exp.expr : exp;
            } else {
                return exp;
            }
        }
    }

    @Override
    public AbstractExpression replace3(Col3Expression exp) {
        if (this.isConst(exp.exp1)) {
            return this.isTrue(exp.exp1) ? exp.exp2 : exp.exp3;
        } else {
            return exp;
        }
    }
}

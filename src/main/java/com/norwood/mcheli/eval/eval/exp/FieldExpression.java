package com.norwood.mcheli.eval.eval.exp;

import com.norwood.mcheli.eval.eval.EvalException;

public class FieldExpression extends Col2OpeExpression {

    public FieldExpression() {
        this.setOperator(".");
    }

    protected FieldExpression(FieldExpression from, ShareExpValue s) {
        super(from, s);
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new FieldExpression(this, s);
    }

    @Override
    public long evalLong() {
        try {
            return this.share.var.evalLong(this.getVariable());
        } catch (EvalException var2) {
            throw var2;
        } catch (Exception var3) {
            throw new EvalException(2003, this.toString(), this.string, this.pos, var3);
        }
    }

    @Override
    public double evalDouble() {
        try {
            return this.share.var.evalDouble(this.getVariable());
        } catch (EvalException var2) {
            throw var2;
        } catch (Exception var3) {
            throw new EvalException(2003, this.toString(), this.string, this.pos, var3);
        }
    }

    @Override
    public Object evalObject() {
        return this.getVariable();
    }

    @Override
    protected Object getVariable() {
        Object obj = this.expl.getVariable();
        if (obj == null) {
            throw new EvalException(2104, this.expl.toString(), this.string, this.pos, null);
        } else {
            String word = this.expr.getWord();

            try {
                return this.share.var.getObject(obj, word);
            } catch (EvalException var4) {
                throw var4;
            } catch (Exception var5) {
                throw new EvalException(2301, this.toString(), this.string, this.pos, var5);
            }
        }
    }

    @Override
    protected void let(Object val, int pos) {
        Object obj = this.expl.getVariable();
        if (obj == null) {
            throw new EvalException(2104, this.expl.toString(), this.string, pos, null);
        } else {
            String word = this.expr.getWord();

            try {
                this.share.var.setValue(obj, word, val);
            } catch (EvalException var6) {
                throw var6;
            } catch (Exception var7) {
                throw new EvalException(2302, this.toString(), this.string, pos, var7);
            }
        }
    }

    @Override
    protected AbstractExpression replace() {
        this.expl = this.expl.replaceVar();
        return this.share.repl.replace2(this);
    }

    @Override
    protected AbstractExpression replaceVar() {
        this.expl = this.expl.replaceVar();
        return this.share.repl.replaceVar2(this);
    }

    @Override
    public String toString() {
        return this.expl.toString() +
                '.' +
                this.expr.toString();
    }
}

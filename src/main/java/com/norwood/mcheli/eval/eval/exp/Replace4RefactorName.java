package com.norwood.mcheli.eval.eval.exp;

import com.norwood.mcheli.eval.eval.EvalException;
import com.norwood.mcheli.eval.eval.ref.Refactor;
import com.norwood.mcheli.eval.eval.repl.ReplaceAdapter;

@Deprecated
public class Replace4RefactorName extends ReplaceAdapter {

    protected final Refactor ref;

    Replace4RefactorName(Refactor ref) {
        this.ref = ref;
    }

    protected void var(VariableExpression exp) {
        String name = this.ref.getNewName(null, exp.getWord());
        if (name != null) {
            exp.setWord(name);
        }
    }

    protected void field(FieldExpression exp) {
        AbstractExpression exp1 = exp.expl;
        Object obj = exp1.getVariable();
        if (obj == null) {
            throw new EvalException(2104, this.toString(), exp1.string, exp1.pos, null);
        } else {
            AbstractExpression exp2 = exp.expr;
            String name = this.ref.getNewName(obj, exp2.getWord());
            if (name != null) {
                exp2.setWord(name);
            }
        }
    }

    protected void func(FunctionExpression exp) {
        Object obj = null;
        if (exp.target != null) {
            obj = exp.target.getVariable();
        }

        String name = this.ref.getNewFuncName(obj, exp.name);
        if (name != null) {
            exp.name = name;
        }
    }

    @Override
    public AbstractExpression replace0(WordExpression exp) {
        if (exp instanceof VariableExpression) {
            this.var((VariableExpression) exp);
        }

        return exp;
    }

    @Override
    public AbstractExpression replace2(Col2Expression exp) {
        if (exp instanceof FieldExpression) {
            this.field((FieldExpression) exp);
        }

        return exp;
    }

    @Override
    public AbstractExpression replaceFunc(FunctionExpression exp) {
        this.func(exp);
        return exp;
    }

    public AbstractExpression replaceVar(AbstractExpression exp) {
        if (exp instanceof VariableExpression) {
            this.var((VariableExpression) exp);
        } else if (exp instanceof FieldExpression) {
            this.field((FieldExpression) exp);
        } else if (exp instanceof FunctionExpression) {
            this.func((FunctionExpression) exp);
        }

        return exp;
    }
}

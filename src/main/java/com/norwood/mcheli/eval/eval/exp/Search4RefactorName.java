package com.norwood.mcheli.eval.eval.exp;

import com.norwood.mcheli.eval.eval.EvalException;
import com.norwood.mcheli.eval.eval.ref.Refactor;
import com.norwood.mcheli.eval.eval.srch.SearchAdapter;

public class Search4RefactorName extends SearchAdapter {

    protected final Refactor ref;

    Search4RefactorName(Refactor ref) {
        this.ref = ref;
    }

    @Override
    public void search0(WordExpression exp) {
        if (exp instanceof VariableExpression) {
            String name = this.ref.getNewName(null, exp.getWord());
            if (name != null) {
                exp.setWord(name);
            }
        }
    }

    @Override
    public boolean search2_2(Col2Expression exp) {
        if (exp instanceof FieldExpression) {
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

                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean searchFunc_2(FunctionExpression exp) {
        Object obj = null;
        if (exp.target != null) {
            obj = exp.target.getVariable();
        }

        String name = this.ref.getNewFuncName(obj, exp.name);
        if (name != null) {
            exp.name = name;
        }

        return false;
    }
}

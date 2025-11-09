package com.norwood.mcheli.eval.eval.exp;

import com.norwood.mcheli.eval.eval.Rule;
import com.norwood.mcheli.eval.eval.ref.Refactor;
import com.norwood.mcheli.eval.eval.repl.ReplaceAdapter;
import com.norwood.mcheli.eval.eval.rule.ShareRuleValue;

public class Replace4RefactorGetter extends ReplaceAdapter {

    protected final Refactor ref;
    protected final ShareRuleValue rule;

    Replace4RefactorGetter(Refactor ref, Rule rule) {
        this.ref = ref;
        this.rule = (ShareRuleValue) rule;
    }

    protected AbstractExpression var(VariableExpression exp) {
        String name = this.ref.getNewName(null, exp.getWord());
        return name == null ? exp : this.rule.parse(name, exp.share);
    }

    protected AbstractExpression field(FieldExpression exp) {
        AbstractExpression exp1 = exp.expl;
        Object obj = exp1.getVariable();
        if (obj != null) {
            AbstractExpression exp2 = exp.expr;
            String name = this.ref.getNewName(obj, exp2.getWord());
            if (name != null) {
                exp.expr = this.rule.parse(name, exp2.share);
            }
        }
        return exp;
    }

    @Override
    public AbstractExpression replace0(WordExpression exp) {
        return exp instanceof VariableExpression ? this.var((VariableExpression) exp) : exp;
    }

    @Override
    public AbstractExpression replace2(Col2OpeExpression exp) {
        return exp instanceof FieldExpression ? this.field((FieldExpression) exp) : exp;
    }
}

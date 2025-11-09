package com.norwood.mcheli.eval.eval.rule;

import com.norwood.mcheli.eval.eval.EvalException;
import com.norwood.mcheli.eval.eval.exp.AbstractExpression;
import com.norwood.mcheli.eval.eval.exp.Col3Expression;
import com.norwood.mcheli.eval.eval.lex.Lex;

public class IfRule extends AbstractRule {

    public AbstractExpression cond;

    public IfRule(ShareRuleValue share) {
        super(share);
    }

    @Override
    protected AbstractExpression parse(Lex lex) {
        AbstractExpression x = this.nextRule.parse(lex);
        if (lex.getType() == 2147483634) {
            String ope = lex.getOperator();
            int pos = lex.getPos();
            if (this.isMyOperator(ope) && lex.isOperator(this.cond.getOperator())) {
                x = this.parseCond(lex, x, ope, pos);
            }

            return x;
        }
        return x;
    }

    protected AbstractExpression parseCond(Lex lex, AbstractExpression x, String ope, int pos) {
        AbstractExpression y = this.parse(lex.next());
        if (!lex.isOperator(this.cond.getEndOperator())) {
            throw new EvalException(1001, new String[] { this.cond.getEndOperator() }, lex);
        } else {
            AbstractExpression z = this.parse(lex.next());
            return Col3Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos, x, y, z);
        }
    }
}

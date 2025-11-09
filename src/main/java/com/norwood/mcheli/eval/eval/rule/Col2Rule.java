package com.norwood.mcheli.eval.eval.rule;

import com.norwood.mcheli.eval.eval.exp.AbstractExpression;
import com.norwood.mcheli.eval.eval.exp.Col2Expression;
import com.norwood.mcheli.eval.eval.lex.Lex;

public class Col2Rule extends AbstractRule {

    public Col2Rule(ShareRuleValue share) {
        super(share);
    }

    @Override
    protected AbstractExpression parse(Lex lex) {
        AbstractExpression x = this.nextRule.parse(lex);

        while (true) {
            if (lex.getType() == 2147483634) {
                String ope = lex.getOperator();
                if (!this.isMyOperator(ope)) {
                    return x;
                }

                int pos = lex.getPos();
                AbstractExpression y = this.nextRule.parse(lex.next());
                x = Col2Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos, x, y);
            } else {
                return x;
            }
        }
    }
}

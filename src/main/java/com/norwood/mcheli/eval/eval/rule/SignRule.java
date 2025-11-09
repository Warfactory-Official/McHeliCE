package com.norwood.mcheli.eval.eval.rule;

import com.norwood.mcheli.eval.eval.exp.AbstractExpression;
import com.norwood.mcheli.eval.eval.exp.Col1Expression;
import com.norwood.mcheli.eval.eval.lex.Lex;

public class SignRule extends AbstractRule {

    public SignRule(ShareRuleValue share) {
        super(share);
    }

    @Override
    public AbstractExpression parse(Lex lex) {
        if (lex.getType() == 2147483634) {
            String ope = lex.getOperator();
            if (this.isMyOperator(ope)) {
                int pos = lex.getPos();
                return Col1Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos,
                        this.parse(lex.next()));
            }

            return this.nextRule.parse(lex);
        }
        return this.nextRule.parse(lex);
    }
}

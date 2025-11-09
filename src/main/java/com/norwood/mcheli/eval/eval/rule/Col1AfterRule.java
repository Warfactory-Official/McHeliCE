package com.norwood.mcheli.eval.eval.rule;

import com.norwood.mcheli.eval.eval.EvalException;
import com.norwood.mcheli.eval.eval.exp.AbstractExpression;
import com.norwood.mcheli.eval.eval.exp.Col1Expression;
import com.norwood.mcheli.eval.eval.exp.Col2Expression;
import com.norwood.mcheli.eval.eval.exp.FunctionExpression;
import com.norwood.mcheli.eval.eval.lex.Lex;

public class Col1AfterRule extends AbstractRule {

    public AbstractExpression func;
    public AbstractExpression array;
    public AbstractExpression field;

    public Col1AfterRule(ShareRuleValue share) {
        super(share);
    }

    @Override
    public AbstractExpression parse(Lex lex) {
        AbstractExpression x = this.nextRule.parse(lex);

        while (true) {
            if (lex.getType() == 2147483634) {
                String ope = lex.getOperator();
                int pos = lex.getPos();
                if (!this.isMyOperator(ope)) {
                    return x;
                }

                if (lex.isOperator(this.func.getOperator())) {
                    x = this.parseFunc(lex, x);
                } else if (lex.isOperator(this.array.getOperator())) {
                    x = this.parseArray(lex, x, ope, pos);
                } else if (lex.isOperator(this.field.getOperator())) {
                    x = this.parseField(lex, x, ope, pos);
                } else {
                    x = Col1Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos, x);
                    lex.next();
                }
            } else {
                return x;
            }
        }
    }

    protected AbstractExpression parseFunc(Lex lex, AbstractExpression x) {
        AbstractExpression a = null;
        lex.next();
        if (!lex.isOperator(this.func.getEndOperator())) {
            a = this.share.funcArgRule.parse(lex);
            if (!lex.isOperator(this.func.getEndOperator())) {
                throw new EvalException(1001, new String[] { this.func.getEndOperator() }, lex);
            }
        }

        lex.next();
        return FunctionExpression.create(x, a, this.prio, lex.getShare());
    }

    protected AbstractExpression parseArray(Lex lex, AbstractExpression x, String ope, int pos) {
        AbstractExpression y = this.share.topRule.parse(lex.next());
        if (!lex.isOperator(this.array.getEndOperator())) {
            throw new EvalException(1001, new String[] { this.array.getEndOperator() }, lex);
        } else {
            lex.next();
            return Col2Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos, x, y);
        }
    }

    protected AbstractExpression parseField(Lex lex, AbstractExpression x, String ope, int pos) {
        AbstractExpression y = this.nextRule.parse(lex.next());
        return Col2Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos, x, y);
    }
}

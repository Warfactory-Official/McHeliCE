package com.norwood.mcheli.eval.eval.rule;

import java.util.List;

import com.norwood.mcheli.eval.eval.EvalException;
import com.norwood.mcheli.eval.eval.Expression;
import com.norwood.mcheli.eval.eval.Rule;
import com.norwood.mcheli.eval.eval.exp.AbstractExpression;
import com.norwood.mcheli.eval.eval.exp.ShareExpValue;
import com.norwood.mcheli.eval.eval.lex.Lex;
import com.norwood.mcheli.eval.eval.lex.LexFactory;
import com.norwood.mcheli.eval.eval.oper.Operator;
import com.norwood.mcheli.eval.eval.ref.Refactor;
import com.norwood.mcheli.eval.eval.srch.Search;
import com.norwood.mcheli.eval.eval.var.Variable;

public class ShareRuleValue extends Rule {

    public AbstractRule topRule;
    public AbstractRule funcArgRule;
    public LexFactory lexFactory;
    public AbstractExpression paren;
    protected final List<String>[] opeList = new List[4];

    @Override
    public Expression parse(String str) {
        if (str == null) {
            return null;
        } else if (str.trim().isEmpty()) {
            return new ShareRuleValue.EmptyExpression();
        } else {
            ShareExpValue exp = new ShareExpValue();
            AbstractExpression x = this.parse(str, exp);
            exp.setAbstractExpression(x);
            return exp;
        }
    }

    public AbstractExpression parse(String str, ShareExpValue exp) {
        if (str == null) {
            return null;
        } else {
            Lex lex = this.lexFactory.create(str, this.opeList, this, exp);
            lex.check();
            AbstractExpression x = this.topRule.parse(lex);
            if (lex.getType() != Integer.MAX_VALUE) {
                throw new EvalException(1005, lex);
            } else {
                return x;
            }
        }
    }

    class EmptyExpression extends Expression {

        @Override
        public long evalLong() {
            return 0L;
        }

        @Override
        public double evalDouble() {
            return 0.0;
        }

        @Override
        public Object eval() {
            return null;
        }

        @Override
        public void optimizeLong(Variable var) {}

        @Override
        public void optimizeDouble(Variable var) {}

        @Override
        public void optimize(Variable var, Operator oper) {}

        @Override
        public void search(Search srch) {}

        @Override
        public void refactorName(Refactor ref) {}

        @Override
        public void refactorFunc(Refactor ref, Rule rule) {}

        @Override
        public Expression dup() {
            return ShareRuleValue.this.new EmptyExpression();
        }

        @Override
        public boolean same(Expression obj) {
            return obj instanceof ShareRuleValue.EmptyExpression;
        }

        @Override
        public String toString() {
            return "";
        }
    }
}

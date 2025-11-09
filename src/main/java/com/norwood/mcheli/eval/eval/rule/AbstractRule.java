package com.norwood.mcheli.eval.eval.rule;

import java.util.*;

import com.norwood.mcheli.eval.eval.exp.AbstractExpression;
import com.norwood.mcheli.eval.eval.exp.ParenExpression;
import com.norwood.mcheli.eval.eval.exp.ShareExpValue;
import com.norwood.mcheli.eval.eval.lex.Lex;

public abstract class AbstractRule {

    private final Map<String, AbstractExpression> opes = new HashMap<>();
    public AbstractRule nextRule;
    public int prio;
    protected final ShareRuleValue share;

    public AbstractRule(ShareRuleValue share) {
        this.share = share;
    }

    public final void addExpression(AbstractExpression exp) {
        if (exp != null) {
            String ope = exp.getOperator();
            this.addOperator(ope, exp);
            this.addLexOperator(exp.getEndOperator());
            if (exp instanceof ParenExpression) {
                this.share.paren = exp;
            }
        }
    }

    public final void addOperator(String ope, AbstractExpression exp) {
        this.opes.put(ope, exp);
        this.addLexOperator(ope);
    }

    public final String[] getOperators() {
        List<String> list = new ArrayList<>(this.opes.keySet());

        return list.toArray(new String[0]);
    }

    public final void addLexOperator(String ope) {
        if (ope != null) {
            int n = ope.length() - 1;
            if (this.share.opeList[n] == null) {
                this.share.opeList[n] = new ArrayList<>();
            }

            this.share.opeList[n].add(ope);
        }
    }

    protected final boolean isMyOperator(String ope) {
        return this.opes.containsKey(ope);
    }

    protected final AbstractExpression newExpression(String ope, ShareExpValue share) {
        try {
            AbstractExpression org = this.opes.get(ope);
            AbstractExpression n = org.dup(share);
            n.setPriority(this.prio);
            n.share = share;
            return n;
        } catch (RuntimeException var5) {
            throw var5;
        } catch (Exception var6) {
            throw new RuntimeException(var6);
        }
    }

    public final void initPriority(int prio) {
        this.prio = prio;
        if (this.nextRule != null) {
            this.nextRule.initPriority(prio + 1);
        }
    }

    protected abstract AbstractExpression parse(Lex var1);
}

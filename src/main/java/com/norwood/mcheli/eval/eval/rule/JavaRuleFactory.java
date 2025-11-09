package com.norwood.mcheli.eval.eval.rule;

import com.norwood.mcheli.eval.eval.ExpRuleFactory;
import com.norwood.mcheli.eval.eval.exp.AbstractExpression;

public class JavaRuleFactory extends ExpRuleFactory {

    private static JavaRuleFactory me;

    public static ExpRuleFactory getInstance() {
        if (me == null) {
            me = new JavaRuleFactory();
        }

        return me;
    }

    @Override
    protected AbstractRule createCommaRule(ShareRuleValue share) {
        return null;
    }

    @Override
    protected AbstractRule createPowerRule(ShareRuleValue share) {
        return null;
    }

    @Override
    protected AbstractExpression createLetPowerExpression() {
        return null;
    }
}

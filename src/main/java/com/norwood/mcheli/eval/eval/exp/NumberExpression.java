package com.norwood.mcheli.eval.eval.exp;

import com.norwood.mcheli.eval.eval.EvalException;
import com.norwood.mcheli.eval.eval.lex.Lex;
import com.norwood.mcheli.eval.util.NumberUtil;

public class NumberExpression extends WordExpression {

    public NumberExpression(String str) {
        super(str);
    }

    protected NumberExpression(NumberExpression from, ShareExpValue s) {
        super(from, s);
    }

    public static AbstractExpression create(Lex lex, int prio) {
        AbstractExpression exp = new NumberExpression(lex.getWord());
        exp.setPos(lex.getString(), lex.getPos());
        exp.setPriority(prio);
        exp.share = lex.getShare();
        return exp;
    }

    public static NumberExpression create(AbstractExpression from, String word) {
        NumberExpression n = new NumberExpression(word);
        n.string = from.string;
        n.pos = from.pos;
        n.prio = from.prio;
        n.share = from.share;
        return n;
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new NumberExpression(this, s);
    }

    @Override
    public long evalLong() {
        try {
            return NumberUtil.parseLong(this.word);
        } catch (Exception var6) {
            try {
                return Long.parseLong(this.word);
            } catch (Exception var5) {
                try {
                    return (long) Double.parseDouble(this.word);
                } catch (Exception var4) {
                    throw new EvalException(2003, this.word, this.string, this.pos, var4);
                }
            }
        }
    }

    @Override
    public double evalDouble() {
        try {
            return Double.parseDouble(this.word);
        } catch (Exception var4) {
            try {
                return NumberUtil.parseLong(this.word);
            } catch (Exception var3) {
                throw new EvalException(2003, this.word, this.string, this.pos, var4);
            }
        }
    }

    @Override
    public Object evalObject() {
        try {
            return NumberUtil.parseLong(this.word);
        } catch (Exception var6) {
            try {
                return Long.valueOf(this.word);
            } catch (Exception var5) {
                try {
                    return Double.valueOf(this.word);
                } catch (Exception var4) {
                    throw new EvalException(2003, this.word, this.string, this.pos, var4);
                }
            }
        }
    }
}

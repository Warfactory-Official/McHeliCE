package com.norwood.mcheli.eval.eval.exp;

import com.norwood.mcheli.eval.eval.EvalException;
import com.norwood.mcheli.eval.eval.lex.Lex;
import com.norwood.mcheli.eval.util.CharUtil;
import com.norwood.mcheli.eval.util.NumberUtil;

public class StringExpression extends WordExpression {

    public StringExpression(String str) {
        super(str);
        this.setOperator("\"");
        this.setEndOperator("\"");
    }

    protected StringExpression(StringExpression from, ShareExpValue s) {
        super(from, s);
    }

    public static AbstractExpression create(Lex lex, int prio) {
        String str = lex.getWord();
        str = CharUtil.escapeString(str, 1, str.length() - 2);
        AbstractExpression exp = new StringExpression(str);
        exp.setPos(lex.getString(), lex.getPos());
        exp.setPriority(prio);
        exp.share = lex.getShare();
        return exp;
    }

    public static StringExpression create(AbstractExpression from, String word) {
        StringExpression n = new StringExpression(word);
        n.string = from.string;
        n.pos = from.pos;
        n.prio = from.prio;
        n.share = from.share;
        return n;
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new StringExpression(this, s);
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
        return this.word;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringExpression e) {
            return this.word.equals(e.word);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.getOperator() +
                this.word +
                this.getEndOperator();
    }
}

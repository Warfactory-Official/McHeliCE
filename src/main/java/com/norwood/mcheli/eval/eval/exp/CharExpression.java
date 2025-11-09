package com.norwood.mcheli.eval.eval.exp;

import com.norwood.mcheli.eval.eval.EvalException;
import com.norwood.mcheli.eval.eval.lex.Lex;
import com.norwood.mcheli.eval.util.CharUtil;

public class CharExpression extends WordExpression {

    public CharExpression(String str) {
        super(str);
        this.setOperator("'");
        this.setEndOperator("'");
    }

    protected CharExpression(CharExpression from, ShareExpValue s) {
        super(from, s);
    }

    public static AbstractExpression create(Lex lex, int prio) {
        String str = lex.getWord();
        str = CharUtil.escapeString(str, 1, str.length() - 2);
        AbstractExpression exp = new CharExpression(str);
        exp.setPos(lex.getString(), lex.getPos());
        exp.setPriority(prio);
        exp.share = lex.getShare();
        return exp;
    }

    public static CharExpression create(AbstractExpression from, String word) {
        CharExpression n = new CharExpression(word);
        n.string = from.string;
        n.pos = from.pos;
        n.prio = from.prio;
        n.share = from.share;
        return n;
    }

    @Override
    public AbstractExpression dup(ShareExpValue s) {
        return new CharExpression(this, s);
    }

    @Override
    public long evalLong() {
        try {
            return this.word.charAt(0);
        } catch (Exception var2) {
            throw new EvalException(2003, this.word, this.string, this.pos, var2);
        }
    }

    @Override
    public double evalDouble() {
        try {
            return this.word.charAt(0);
        } catch (Exception var2) {
            throw new EvalException(2003, this.word, this.string, this.pos, var2);
        }
    }

    @Override
    public Object evalObject() {
        return this.word.charAt(0);
    }

    @Override
    public String toString() {
        return this.getOperator() +
                this.word +
                this.getEndOperator();
    }
}

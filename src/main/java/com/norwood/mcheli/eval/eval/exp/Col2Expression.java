package com.norwood.mcheli.eval.eval.exp;

public abstract class Col2Expression extends AbstractExpression {

    public AbstractExpression expl;
    public AbstractExpression expr;

    protected Col2Expression() {}

    protected Col2Expression(Col2Expression from, ShareExpValue s) {
        super(from, s);
        if (from.expl != null) {
            this.expl = from.expl.dup(s);
        }

        if (from.expr != null) {
            this.expr = from.expr.dup(s);
        }
    }

    public static AbstractExpression create(AbstractExpression exp, String string, int pos, AbstractExpression x,
                                            AbstractExpression y) {
        Col2Expression n = (Col2Expression) exp;
        n.setExpression(x, y);
        n.setPos(string, pos);
        return n;
    }

    public final void setExpression(AbstractExpression x, AbstractExpression y) {
        this.expl = x;
        this.expr = y;
    }

    @Override
    protected final int getCols() {
        return 2;
    }

    @Override
    protected final int getFirstPos() {
        return this.expl.getFirstPos();
    }

    @Override
    public long evalLong() {
        return this.operateLong(this.expl.evalLong(), this.expr.evalLong());
    }

    @Override
    public double evalDouble() {
        return this.operateDouble(this.expl.evalDouble(), this.expr.evalDouble());
    }

    @Override
    public Object evalObject() {
        return this.operateObject(this.expl.evalObject(), this.expr.evalObject());
    }

    protected abstract long operateLong(long var1, long var3);

    protected abstract double operateDouble(double var1, double var3);

    protected abstract Object operateObject(Object var1, Object var2);

    @Override
    protected void search() {
        this.share.srch.search(this);
        if (!this.share.srch.end()) {
            if (!this.share.srch.search2_begin(this)) {
                if (!this.share.srch.end()) {
                    this.expl.search();
                    if (!this.share.srch.end()) {
                        if (!this.share.srch.search2_2(this)) {
                            if (!this.share.srch.end()) {
                                this.expr.search();
                                if (!this.share.srch.end()) {
                                    this.share.srch.search2_end(this);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected AbstractExpression replace() {
        this.expl = this.expl.replace();
        this.expr = this.expr.replace();
        return this.share.repl.replace2(this);
    }

    @Override
    protected AbstractExpression replaceVar() {
        this.expl = this.expl.replaceVar();
        this.expr = this.expr.replaceVar();
        return this.share.repl.replaceVar2(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Col2Expression e) {
            if (this.getClass() == e.getClass()) {
                return this.expl.equals(e.expl) && this.expr.equals(e.expr);
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode() ^ this.expl.hashCode() ^ this.expr.hashCode() * 2;
    }

    @Override
    public void dump(int n) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }

        sb.append(this.getOperator());
        System.out.println(sb);
        this.expl.dump(n + 1);
        this.expr.dump(n + 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.expl.getPriority() < this.prio) {
            sb.append(this.share.paren.getOperator());
            sb.append(this.expl);
            sb.append(this.share.paren.getEndOperator());
        } else {
            sb.append(this.expl);
        }

        sb.append(this.toStringLeftSpace());
        sb.append(this.getOperator());
        sb.append(' ');
        if (this.expr.getPriority() < this.prio) {
            sb.append(this.share.paren.getOperator());
            sb.append(this.expr);
            sb.append(this.share.paren.getEndOperator());
        } else {
            sb.append(this.expr);
        }

        return sb.toString();
    }

    protected String toStringLeftSpace() {
        return " ";
    }
}

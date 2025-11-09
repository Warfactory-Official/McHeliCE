package com.norwood.mcheli.eval.eval.exp;

public abstract class Col3Expression extends AbstractExpression {

    protected AbstractExpression exp1;
    protected AbstractExpression exp2;
    protected AbstractExpression exp3;

    protected Col3Expression() {}

    protected Col3Expression(Col3Expression from, ShareExpValue s) {
        super(from, s);
        if (from.exp1 != null) {
            this.exp1 = from.exp1.dup(s);
        }

        if (from.exp2 != null) {
            this.exp2 = from.exp2.dup(s);
        }

        if (from.exp3 != null) {
            this.exp3 = from.exp3.dup(s);
        }
    }

    public static AbstractExpression create(AbstractExpression exp, String string, int pos, AbstractExpression x,
                                            AbstractExpression y, AbstractExpression z) {
        Col3Expression n = (Col3Expression) exp;
        n.setExpression(x, y, z);
        n.setPos(string, pos);
        return n;
    }

    public final void setExpression(AbstractExpression x, AbstractExpression y, AbstractExpression z) {
        this.exp1 = x;
        this.exp2 = y;
        this.exp3 = z;
    }

    @Override
    protected final int getCols() {
        return 3;
    }

    @Override
    protected int getFirstPos() {
        return this.exp1.getFirstPos();
    }

    @Override
    protected void search() {
        this.share.srch.search(this);
        if (!this.share.srch.end()) {
            if (!this.share.srch.search3_begin(this)) {
                if (!this.share.srch.end()) {
                    this.exp1.search();
                    if (!this.share.srch.end()) {
                        if (!this.share.srch.search3_2(this)) {
                            if (!this.share.srch.end()) {
                                this.exp2.search();
                                if (!this.share.srch.end()) {
                                    if (!this.share.srch.search3_3(this)) {
                                        if (!this.share.srch.end()) {
                                            this.exp3.search();
                                            if (!this.share.srch.end()) {
                                                this.share.srch.search3_end(this);
                                            }
                                        }
                                    }
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
        this.exp1 = this.exp1.replace();
        this.exp2 = this.exp2.replace();
        this.exp3 = this.exp3.replace();
        return this.share.repl.replace3(this);
    }

    @Override
    protected AbstractExpression replaceVar() {
        this.exp1 = this.exp1.replace();
        this.exp2 = this.exp2.replaceVar();
        this.exp3 = this.exp3.replaceVar();
        return this.share.repl.replaceVar3(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Col3Expression e) {
            if (this.getClass() == e.getClass()) {
                return this.exp1.equals(e.exp1) && this.exp2.equals(e.exp2) && this.exp3.equals(e.exp3);
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode() ^ this.exp1.hashCode() ^ this.exp2.hashCode() * 2 ^ this.exp3.hashCode() * 3;
    }

    @Override
    public void dump(int n) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }

        sb.append(this.getOperator());
        System.out.println(sb);
        this.exp1.dump(n + 1);
        this.exp2.dump(n + 1);
        this.exp3.dump(n + 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.exp1.getPriority() > this.prio && this.exp1.getCols() < 2) {
            sb.append(this.exp1.toString());
        } else {
            sb.append(this.share.paren.getOperator());
            sb.append(this.exp1.toString());
            sb.append(this.share.paren.getEndOperator());
        }

        sb.append(' ');
        sb.append(this.getOperator());
        sb.append(' ');
        if (this.exp2.getPriority() > this.prio && this.exp2.getCols() < 2) {
            sb.append(this.exp2.toString());
        } else {
            sb.append(this.share.paren.getOperator());
            sb.append(this.exp2.toString());
            sb.append(this.share.paren.getEndOperator());
        }

        sb.append(' ');
        sb.append(this.getEndOperator());
        sb.append(' ');
        if (this.exp3.getPriority() > this.prio && this.exp3.getCols() < 2) {
            sb.append(this.exp3.toString());
        } else {
            sb.append(this.share.paren.getOperator());
            sb.append(this.exp3.toString());
            sb.append(this.share.paren.getEndOperator());
        }

        return sb.toString();
    }
}

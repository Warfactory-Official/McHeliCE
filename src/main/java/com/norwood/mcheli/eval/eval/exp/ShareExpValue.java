package com.norwood.mcheli.eval.eval.exp;

import com.norwood.mcheli.eval.eval.Expression;
import com.norwood.mcheli.eval.eval.Rule;
import com.norwood.mcheli.eval.eval.func.InvokeFunction;
import com.norwood.mcheli.eval.eval.oper.JavaExOperator;
import com.norwood.mcheli.eval.eval.oper.Operator;
import com.norwood.mcheli.eval.eval.ref.Refactor;
import com.norwood.mcheli.eval.eval.repl.Replace;
import com.norwood.mcheli.eval.eval.srch.Search;
import com.norwood.mcheli.eval.eval.var.MapVariable;
import com.norwood.mcheli.eval.eval.var.Variable;

public class ShareExpValue extends Expression {

    public AbstractExpression paren;

    public void setAbstractExpression(AbstractExpression ae) {
        this.ae = ae;
    }

    public void initVar() {
        if (this.var == null) {
            this.var = new MapVariable();
        }
    }

    public void initOper() {
        if (this.oper == null) {
            this.oper = new JavaExOperator();
        }
    }

    public void initFunc() {
        if (this.func == null) {
            this.func = new InvokeFunction();
        }
    }

    @Override
    public long evalLong() {
        this.initVar();
        this.initFunc();
        return this.ae.evalLong();
    }

    @Override
    public double evalDouble() {
        this.initVar();
        this.initFunc();
        return this.ae.evalDouble();
    }

    @Override
    public Object eval() {
        this.initVar();
        this.initOper();
        this.initFunc();
        return this.ae.evalObject();
    }

    @Override
    public void optimizeLong(Variable var) {
        this.optimize(var, new OptimizeLong());
    }

    @Override
    public void optimizeDouble(Variable var) {
        this.optimize(var, new OptimizeDouble());
    }

    @Override
    public void optimize(Variable var, Operator oper) {
        Operator bak = this.oper;
        this.oper = oper;

        try {
            this.optimize(var, new OptimizeObject());
        } finally {
            this.oper = bak;
        }
    }

    protected void optimize(Variable var, Replace repl) {
        Variable bak = this.var;
        if (var == null) {
            var = new MapVariable();
        }

        this.var = var;
        this.repl = repl;

        try {
            this.ae = this.ae.replace();
        } finally {
            this.var = bak;
        }
    }

    @Override
    public void search(Search srch) {
        if (srch == null) {
            throw new NullPointerException();
        } else {
            this.srch = srch;
            this.ae.search();
        }
    }

    @Override
    public void refactorName(Refactor ref) {
        if (ref == null) {
            throw new NullPointerException();
        } else {
            this.srch = new Search4RefactorName(ref);
            this.ae.search();
        }
    }

    @Override
    public void refactorFunc(Refactor ref, Rule rule) {
        if (ref == null) {
            throw new NullPointerException();
        } else {
            this.repl = new Replace4RefactorGetter(ref, rule);
            this.ae.replace();
        }
    }

    @Override
    public boolean same(Expression obj) {
        if (!(obj instanceof ShareExpValue)) {
            return false;
        } else {
            AbstractExpression p = ((ShareExpValue) obj).paren;
            return this.paren.same(p) && super.same(obj);
        }
    }

    @Override
    public Expression dup() {
        ShareExpValue n = new ShareExpValue();
        n.ae = this.ae.dup(n);
        n.paren = this.paren.dup(n);
        return n;
    }
}

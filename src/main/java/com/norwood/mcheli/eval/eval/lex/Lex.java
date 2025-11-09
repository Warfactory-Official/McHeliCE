package com.norwood.mcheli.eval.eval.lex;

import java.util.List;

import com.norwood.mcheli.eval.eval.exp.AbstractExpression;
import com.norwood.mcheli.eval.eval.exp.ShareExpValue;
import com.norwood.mcheli.eval.util.CharUtil;

public class Lex {

    public static final int TYPE_WORD = 2147483632;
    public static final int TYPE_NUM = 2147483633;
    public static final int TYPE_OPE = 2147483634;
    public static final int TYPE_STRING = 2147483635;
    public static final int TYPE_CHAR = 2147483636;
    public static final int TYPE_EOF = Integer.MAX_VALUE;
    public static final int TYPE_ERR = -1;
    protected final List<String>[] opeList;
    protected final String string;
    protected int pos = 0;
    protected int len = 0;
    protected int type = -1;
    protected String ope;
    protected final ShareExpValue expShare;
    protected final String SPC_CHAR = " \t\r\n";
    protected final String NUMBER_CHAR = "._";

    protected Lex(String str, List<String>[] lists, AbstractExpression paren, ShareExpValue exp) {
        this.string = str;
        this.opeList = lists;
        this.expShare = exp;
        if (this.expShare.paren == null) {
            this.expShare.paren = paren;
        }
    }

    protected boolean isSpace(int pos) {
        if (pos >= this.string.length()) {
            return true;
        } else {
            char c = this.string.charAt(pos);
            return this.SPC_CHAR.indexOf(c) >= 0;
        }
    }

    protected boolean isNumberTop(int pos) {
        if (pos >= this.string.length()) {
            return false;
        } else {
            char c = this.string.charAt(pos);
            return '0' <= c && c <= '9';
        }
    }

    protected boolean isSpecialNumber(int pos) {
        if (pos >= this.string.length()) {
            return false;
        } else {
            char c = this.string.charAt(pos);
            return this.NUMBER_CHAR.indexOf(c) >= 0;
        }
    }

    protected String isOperator(int pos) {
        for (int i = this.opeList.length - 1; i >= 0; i--) {
            if (pos + i < this.string.length()) {
                List<String> list = this.opeList[i];
                if (list != null) {
                    label36:
                    for (String ope : list) {
                        for (int k = 0; k <= i; k++) {
                            char c = this.string.charAt(pos + k);
                            char o = ope.charAt(k);
                            if (c != o) {
                                continue label36;
                            }
                        }

                        return ope;
                    }
                }
            }
        }

        return null;
    }

    protected boolean isStringTop(int pos) {
        if (pos >= this.string.length()) {
            return false;
        } else {
            char c = this.string.charAt(pos);
            return c == '"';
        }
    }

    protected boolean isStringEnd(int pos) {
        return this.isStringTop(pos);
    }

    protected boolean isCharTop(int pos) {
        if (pos >= this.string.length()) {
            return false;
        } else {
            char c = this.string.charAt(pos);
            return c == '\'';
        }
    }

    protected boolean isCharEnd(int pos) {
        return this.isCharTop(pos);
    }

    public void check() {
        while (this.isSpace(this.pos)) {
            if (this.pos >= this.string.length()) {
                this.type = Integer.MAX_VALUE;
                this.len = 0;
                return;
            }

            this.pos++;
        }

        if (this.isStringTop(this.pos)) {
            this.processString();
        } else if (this.isCharTop(this.pos)) {
            this.processChar();
        } else {
            String ope = this.isOperator(this.pos);
            if (ope != null) {
                this.type = 2147483634;
                this.ope = ope;
                this.len = ope.length();
            } else {
                boolean number = this.isNumberTop(this.pos);
                this.type = number ? 2147483633 : 2147483632;
                this.len = 1;

                while (!this.isSpace(this.pos + this.len) && (number && this.isSpecialNumber(this.pos + this.len) ||
                        this.isOperator(this.pos + this.len) == null)) {
                    this.len++;
                }
            }
        }
    }

    protected void processString() {
        int[] ret = new int[1];
        this.type = 2147483635;
        this.len = 1;

        do {
            this.len = this.len + this.getCharLen(this.pos + this.len, ret);
            if (this.pos + this.len >= this.string.length()) {
                this.type = Integer.MAX_VALUE;
                break;
            }
        } while (!this.isStringEnd(this.pos + this.len));

        this.len++;
    }

    protected void processChar() {
        int[] ret = new int[1];
        this.type = 2147483636;
        this.len = 1;

        do {
            this.len = this.len + this.getCharLen(this.pos + this.len, ret);
            if (this.pos + this.len >= this.string.length()) {
                this.type = Integer.MAX_VALUE;
                break;
            }
        } while (!this.isCharEnd(this.pos + this.len));

        this.len++;
    }

    protected int getCharLen(int pos, int[] ret) {
        CharUtil.escapeChar(this.string, pos, this.string.length(), ret);
        return ret[0];
    }

    public Lex next() {
        this.pos = this.pos + this.len;
        this.check();
        return this;
    }

    public int getType() {
        return this.type;
    }

    public String getOperator() {
        return this.ope;
    }

    public boolean isOperator(String ope) {
        return this.type == 2147483634 && this.ope.equals(ope);
    }

    public String getWord() {
        return this.string.substring(this.pos, this.pos + this.len);
    }

    public String getString() {
        return this.string;
    }

    public int getPos() {
        return this.pos;
    }

    public ShareExpValue getShare() {
        return this.expShare;
    }
}

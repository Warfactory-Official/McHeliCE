package com.norwood.mcheli.eval.eval;

import com.norwood.mcheli.eval.eval.lex.Lex;

public class EvalException extends RuntimeException {

    public static final int PARSE_NOT_FOUND_END_OP = 1001;
    public static final int PARSE_INVALID_OP = 1002;
    public static final int PARSE_INVALID_CHAR = 1003;
    public static final int PARSE_END_OF_STR = 1004;
    public static final int PARSE_STILL_EXIST = 1005;
    public static final int PARSE_NOT_FUNC = 1101;
    public static final int EXP_FORBIDDEN_CALL = 2001;
    public static final int EXP_NOT_VARIABLE = 2002;
    public static final int EXP_NOT_NUMBER = 2003;
    public static final int EXP_NOT_LET = 2004;
    public static final int EXP_NOT_VAR_VALUE = 2101;
    public static final int EXP_NOT_LET_VAR = 2102;
    public static final int EXP_NOT_DEF_VAR = 2103;
    public static final int EXP_NOT_DEF_OBJ = 2104;
    public static final int EXP_NOT_ARR_VALUE = 2201;
    public static final int EXP_NOT_LET_ARR = 2202;
    public static final int EXP_NOT_FLD_VALUE = 2301;
    public static final int EXP_NOT_LET_FIELD = 2302;
    public static final int EXP_FUNC_CALL_ERROR = 2401;
    private static final long serialVersionUID = 4174576689426433715L;
    protected final int msg_code;
    protected String[] msg_opt;
    protected String string;
    protected int pos = -1;
    protected String word;

    public EvalException(int msg, Lex lex) {
        this(msg, null, lex);
    }

    public EvalException(int msg, String[] opt, Lex lex) {
        this.msg_code = msg;
        this.msg_opt = opt;
        if (lex != null) {
            this.string = lex.getString();
            this.pos = lex.getPos();
            this.word = lex.getWord();
        }
    }

    public EvalException(int msg, String word, String string, int pos, Throwable e) {
        while (e != null && e.getClass() == RuntimeException.class && e.getCause() != null) {
            e = e.getCause();
        }

        if (e != null) {
            super.initCause(e);
        }

        this.msg_code = msg;
        this.string = string;
        this.pos = pos;
        this.word = word;
    }

    public static String getErrCodeMessage(int code) {
        return switch (code) {
            case 1001 -> "演算子「%0」が在りません。";
            case 1002 -> "演算子の文法エラーです。";
            case 1003 -> "未対応の識別子です。";
            case 1004 -> "式の解釈の途中で文字列が終了しています。";
            case 1005 -> "式の解釈が終わりましたが文字列が残っています。";
            case 1101 -> "関数として使用できません。";
            case 2001 -> "禁止されているメソッドを呼び出しました。";
            case 2002 -> "変数として使用できません。";
            case 2003 -> "数値として使用できません。";
            case 2004 -> "代入できません。";
            case 2101 -> "変数の値が取得できません。";
            case 2102 -> "変数に代入できません。";
            case 2103 -> "変数が未定義です。";
            case 2104 -> "オブジェクトが未定義です。";
            case 2201 -> "配列の値が取得できません。";
            case 2202 -> "配列に代入できません。";
            case 2301 -> "フィールドの値が取得できません。";
            case 2302 -> "フィールドに代入できません。";
            case 2401 -> "関数の呼び出しに失敗しました。";
            default -> "エラーが発生しました。";
        };
    }

    public int getErrorCode() {
        return this.msg_code;
    }

    public String[] getOption() {
        return this.msg_opt;
    }

    public String getWord() {
        return this.word;
    }

    public String getString() {
        return this.string;
    }

    public int getPos() {
        return this.pos;
    }

    public String getDefaultFormat(String msgFmt) {
        StringBuilder fmt = new StringBuilder(128);
        fmt.append(msgFmt);
        boolean bWord = false;
        if (this.word != null && !this.word.isEmpty()) {
            bWord = !this.word.equals(this.string);
        }

        if (bWord) {
            fmt.append(" word=「%w」");
        }

        if (this.pos >= 0) {
            fmt.append(" pos=%p");
        }

        if (this.string != null) {
            fmt.append(" string=「%s」");
        }

        if (this.getCause() != null) {
            fmt.append(" cause by %e");
        }

        return fmt.toString();
    }

    @Override
    public String toString() {
        String msg = getErrCodeMessage(this.msg_code);
        String fmt = this.getDefaultFormat(msg);
        return this.toString(fmt);
    }

    public String toString(String fmt) {
        StringBuilder sb = new StringBuilder(256);
        int len = fmt.length();

        for (int i = 0; i < len; i++) {
            char c = fmt.charAt(i);
            if (c != '%') {
                sb.append(c);
            } else {
                if (i + 1 >= len) {
                    sb.append(c);
                    break;
                }

                c = fmt.charAt(++i);
                switch (c) {
                    case '%':
                        sb.append('%');
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        int n = c - '0';
                        if (this.msg_opt != null && n < this.msg_opt.length) {
                            sb.append(this.msg_opt[n]);
                        }
                        break;
                    case 'c':
                        sb.append(this.msg_code);
                        break;
                    case 'e':
                        sb.append(this.getCause());
                        break;
                    case 'p':
                        sb.append(this.pos);
                        break;
                    case 's':
                        sb.append(this.string);
                        break;
                    case 'w':
                        sb.append(this.word);
                        break;
                    default:
                        sb.append('%');
                        sb.append(c);
                }
            }
        }

        return sb.toString();
    }
}

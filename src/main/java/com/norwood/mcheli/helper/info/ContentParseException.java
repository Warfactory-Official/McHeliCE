package com.norwood.mcheli.helper.info;

public class ContentParseException extends RuntimeException {

    private static final long serialVersionUID = 4338814389788695295L;
    private final int lineNo;

    public ContentParseException(int lineNo) {
        this.lineNo = lineNo;
    }

    public ContentParseException(String msg, int lineNo) {
        super(msg);
        this.lineNo = lineNo;
    }

    public ContentParseException(Throwable cause, int lineNo) {
        super(cause);
        this.lineNo = lineNo;
    }

    public int getLineNo() {
        return this.lineNo;
    }
}

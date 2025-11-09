package com.norwood.mcheli.helper.debug;

public class DebugException extends RuntimeException {

    private static final long serialVersionUID = 12023042301000021L;

    public DebugException(String msg) {
        super(msg);
    }

    public DebugException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

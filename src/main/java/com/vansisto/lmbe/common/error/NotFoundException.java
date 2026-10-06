package com.vansisto.lmbe.common.error;

public abstract class NotFoundException extends BaseException {

    protected NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

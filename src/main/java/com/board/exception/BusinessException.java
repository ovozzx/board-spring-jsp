package com.board.exception;

public class BusinessException extends RuntimeException{ // 언체크
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}

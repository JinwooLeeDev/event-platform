package com.busan.eventplatform.global.error.exception;

public class AlreadyPurchaseException extends RuntimeException{
    public AlreadyPurchaseException(String message) {
        super(message);
    }
}

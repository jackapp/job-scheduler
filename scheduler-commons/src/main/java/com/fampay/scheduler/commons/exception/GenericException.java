package com.fampay.scheduler.commons.exception;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = false)
@Data
@Builder
@NoArgsConstructor
public class GenericException extends RuntimeException {
    private static final long serialVersionUID = -676390742147669618L;
    private Integer errorCode;
    private String message;
    private String displayMessage;

    public GenericException(Integer errorCode, String message, String displayMessage) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.displayMessage = displayMessage;
    }


    public GenericException(Integer errorCode, String message, String displayMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.message = message;
        this.displayMessage = displayMessage;
    }

    public static GenericException buildException(Integer errorCode) {
        return GenericException.builder().errorCode(errorCode).build();
    }

    public static GenericException buildException(Integer errorCode, String message) {
        return GenericException.builder().errorCode(errorCode).message(message).build();
    }

    public static GenericException buildException(Integer errorCode, String message, String displayMessage) {
        return GenericException.builder().errorCode(errorCode).message(message).displayMessage(displayMessage).build();
    }
}
package com.fampay.scheduler.commons.exception;

import lombok.Builder;

public class InternalLibraryException extends GenericException {

    private static final long serialVersionUID = -4663635199535617011L;

    @Builder(builderMethodName = "childBuilder")
    public InternalLibraryException(Integer errorCode, String message, String displayMessage) {
        super(errorCode, message, displayMessage);
    }
}

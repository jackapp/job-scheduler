package com.fampay.scheduler.commons.lock.exception;

import com.fampay.scheduler.commons.exception.GenericException;
import lombok.Builder;

public class ParallelLockException extends GenericException {
    @Builder(builderMethodName = "childBuilder")
    public ParallelLockException(Integer errorCode, String message, String displayMessage) {
        super(errorCode, message, displayMessage);
    }
}


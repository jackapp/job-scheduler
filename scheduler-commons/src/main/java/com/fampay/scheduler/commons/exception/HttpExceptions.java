package com.fampay.scheduler.commons.exception;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpExceptions {

    @Getter
    public abstract static class CustomHttpException extends GenericException {
        private final Integer statusCode;
        private final String rawResponse;
        protected CustomHttpException(Integer errorCode, String message, String displayMessage, Integer statusCode, String rawResponse) {
            super(errorCode, message, displayMessage);
            this.statusCode = statusCode;
            this.rawResponse = rawResponse;
        }
    }

    @Getter
    public static class ClientErrorException extends CustomHttpException {
        @Builder(builderMethodName = "childBuilder")
        public ClientErrorException(Integer errorCode, String message, String displayMessage, Integer statusCode,
                                    String rawResponse) {
            super(errorCode, message, displayMessage, statusCode, rawResponse);
        }
    }

    @Getter
    public static class ServerErrorException extends CustomHttpException {
        @Builder(builderMethodName = "childBuilder")
        public ServerErrorException(Integer errorCode, String message, String displayMessage, Integer statusCode,
                                    String rawResponse) {
            super(errorCode, message, displayMessage, statusCode, rawResponse);
        }
    }
}

package com.fampay.scheduler.commons.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LibraryErrorMessages {


    public static final String CONFIG_MISSING = "CONFIG_MISSING";
    public static final String IMPROPER_CONFIG = "IMPROPER_CONFIG";

    public static final String DB_HEALTH_FAILED = "DB_HEALTH_FAILED";
    public static final String DB_KEY_MISSING = "DB_KEY_FAILED";
    public static final String DB_INSERT_FAILED = "DB_INSERT_FAILED";
    public static final String DB_EXECUTION_FAILED = "DB_EXECUTION_FAILED";
    public static final String DB_UPDATE_FAILED = "DB_UPDATE_FAILED";
    public static final String DB_DELETE_FAILED = "DB_DELETE_FAILED";
    public static final String DB_GET_FAILED = "DB_GET_FAILED";
    public static final String DB_NOT_CONNECTED = "DB_NOT_CONNECTED";
    public static final String DB_DATASOURCE_MISSING = "DB_DATASOURCE_MISSING";
    public static final String DB_INVALID_TENANT_ID = "DB_INVALID_TENANT_ID";
    public static final String DB_NULL_TENANT_ID = "DB_NULL_TENANT_ID";
    public static final String DB_UNSUPPORTED_ATTRIBUTE = "DB_UNSUPPORTED_ATTRIBUTE";
    public static final String DB_ILLEGAL_ACCESS = "DB_ILLEGAL_ACCESS";
    public static final String DB_PREPARED_STATEMENT_FAILED = "DB_PREPARED_STATEMENT_FAILED";

    public static final String KEY_PARSING_ERROR = "KEY_PARSING_ERROR";
    public static final String RATE_LIMIT_BREACHED = "RATE_LIMIT_BREACHED";
    public static final String ACQUIRE_LOCK_FAILED = "ACQUIRE_LOCK_FAILED";

    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    public static final String PREFIX_MISSING = "PREFIX_MISSING";
    public static final String INVALID_LOCK_ID_KEYS = "INVALID_LOCK_ID_KEYS";

    public static final String REDIS_NOT_STARTED = "REDIS_NOT_STARTED";

    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";



}

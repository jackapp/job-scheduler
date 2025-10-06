package com.fampay.scheduler.commons.mongo;

public interface BulkOperation<T> {
    enum Type {
        INSERT, UPDATE, UPSERT, DELETE
    }

    Type getType();
    T getFilter();
    T getUpdateOrDocument();
    boolean isUpsert();
}
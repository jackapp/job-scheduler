package com.fampay.scheduler.commons.mongo.helper.impl;

import com.fampay.scheduler.commons.mongo.BulkOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;


public class BulkOperationImpl implements BulkOperation<Map<String,Object>> {
    private final Type type;
    private final Map<String, Object> filter;
    private final Map<String, Object> updateOrDoc;
    private final boolean upsert;

    public BulkOperationImpl(Type type, Map<String, Object> filter, Map<String, Object> updateOrDoc, boolean upsert) {
        this.type = type;
        this.filter = filter;
        this.updateOrDoc = updateOrDoc;
        this.upsert = upsert;
    }

    @Override
    public Type getType() { return type; }

    @Override
    public Map<String, Object> getFilter() { return filter; }

    @Override
    public Map<String, Object> getUpdateOrDocument() { return updateOrDoc; }

    @Override
    public boolean isUpsert() { return upsert; }
}

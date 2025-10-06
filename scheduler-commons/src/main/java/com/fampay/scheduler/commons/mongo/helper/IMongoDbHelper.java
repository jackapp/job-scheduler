package com.fampay.scheduler.commons.mongo.helper;

import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fampay.scheduler.commons.mongo.BulkOperation;
import com.fampay.scheduler.commons.mongo.client.impl.ConsistencyLevel;
import com.fampay.scheduler.commons.mongo.dto.PagedResult;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IMongoDbHelper {
    IMongoDbHelper withConsistencyLevel(ConsistencyLevel consistencyLevel);
    void save(String collectionName, String key, Object value) throws InternalLibraryException;
    void saveWithTtlCompatible(String collectionName, String key, Object value) throws InternalLibraryException;
    void bulkSave(String collectionName, Map<String, Object> values) throws InternalLibraryException;
    void bulkUpdate(String collectionName, Bson filter, Bson params) throws InternalLibraryException;
    void deleteOne(String collectionName, String id);
    boolean updateById(String collectionName, String key, Object valuesToUpdate);
    boolean upsertById(String collectionName, String id, Object valuesToUpdate);
    MongoCollection<Document> getCollection(String collectionName);
    <T> T findById(String collectionName, String key, Class<T> cls) throws InternalLibraryException;
    <T> List<T> findAll(String collectionName, Map<String, Object> searchParams, Class<T> cls) throws InternalLibraryException;
    <T> List<T> findAllByExpression(String collectionName, String expression, Class<T> cls) throws InternalLibraryException;
    <T> Optional<T> findOptionalById(String collectionName, String key, Class<T> cls) throws InternalLibraryException;
    <T> List<T> findAll(String collectionName, Bson bson, Class<T> cls) throws InternalLibraryException;
    <T> List<T> findDistinctFromAll(String collectionName, String key, Bson bson, Class<T> cls) throws InternalLibraryException;
    <T> T findOne(String collectionName, Map<String, Object> searchParams, Class<T> cls) throws InternalLibraryException;
    <T> void executeBulkWrite(String collectionName, List<BulkOperation<Map<String, T>>> operations);
    <T> PagedResult<T> getDocumentsPaginated(String collectionName,Bson filter,Bson sort,int pageSize,List<String>cursorFields, Class<T> cls);
    boolean isHealthy();
}

package com.fampay.scheduler.commons.mongo.helper.impl;


import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fampay.scheduler.commons.helper.utils.CommonSerializationUtil;
import com.fampay.scheduler.commons.mongo.BulkOperation;
import com.fampay.scheduler.commons.mongo.client.IMongoClient;
import com.fampay.scheduler.commons.mongo.client.impl.ConsistencyLevel;
import com.fampay.scheduler.commons.mongo.dto.PagedResult;
import com.fampay.scheduler.commons.mongo.helper.IMongoDbHelper;
import com.fampay.scheduler.commons.exception.LibraryErrorMessages;
import com.mongodb.ReadPreference;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(prefix = "mongo-config",name = {"database"})
public class MongoDbHelper implements IMongoDbHelper {

    private final IMongoClient mongoClient;
    private final ConsistencyLevel consistencyLevel;

    @Autowired
    public MongoDbHelper(IMongoClient mongoClient) {
        this(mongoClient, ConsistencyLevel.STRONG);
    }

    public MongoDbHelper(IMongoClient mongoClient, ConsistencyLevel consistencyLevel) {
        this.mongoClient = mongoClient;
        this.consistencyLevel = consistencyLevel;
    }

    public IMongoDbHelper withConsistencyLevel(ConsistencyLevel consistencyLevel) {
        return new MongoDbHelper(mongoClient, consistencyLevel);
    }

    @Override
    public void save(String collectionName, String key, Object value) throws InternalLibraryException {
        if (StringUtils.isBlank(key)) {
            log.error("[MongoDb]Key cannot be null");
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_KEY_MISSING)
                    .displayMessage("Key cannot be null").build();
        }
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            Document document = new Document(CommonSerializationUtil.convertObjectToMap(value));
            document.put("_id", key);
            collection.insertOne(document);
        } catch (Exception ex) {
            log.error("[MongoDb]Failed to write in database with error = ", ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_INSERT_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to save in mongoDb").build();
        }
    }

    @Override
    public void saveWithTtlCompatible(String collectionName, String key, Object value) throws InternalLibraryException {
        if (StringUtils.isBlank(key)) {
            log.error("[MongoDb]Key cannot be null");
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_KEY_MISSING)
                    .displayMessage("Key cannot be null").build();
        }
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            Document document = new Document(CommonSerializationUtil.convertObjectToMap(value));
            document.put("_id", key);
            document.put("ttlCompatibleDate", new Date(DateTimeUtils.currentTimeMillis()));
            collection.insertOne(document);
        } catch (Exception ex) {
            log.error("[MongoDb]Failed to write in database with error = ", ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_INSERT_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to save in mongoDb").build();
        }
    }

    @Override
    public void bulkSave(String collectionName, Map<String, Object> values) throws InternalLibraryException {
        if (Objects.isNull(values)) {
            log.error("[MongoDb]Values cannot be null for bulk save");
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_KEY_MISSING)
                    .displayMessage("Key cannot be null").build();
        }
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            final List<Document> documents = new ArrayList<>();
            values.forEach((key, value) -> {
                Document document = new Document(CommonSerializationUtil.convertObjectToMap(value));
                document.put("_id", key);
                documents.add(document);
            });
            collection.insertMany(documents);
        } catch (Exception ex) {
            log.error("[MongoDb]Failed to write in database with error = ", ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_INSERT_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to save in mongoDb").build();
        }
    }

    @Override
    public void bulkUpdate(String collectionName, Bson filter, Bson param) throws InternalLibraryException {
        if (Objects.isNull(filter) || Objects.isNull(param)) {
            log.error("[MongoDb]filter/param cannot be null for bulk update");
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_KEY_MISSING)
                    .displayMessage("Filter and param cannot be null").build();
        }
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            collection.updateMany(filter, param);
        } catch (Exception ex) {
            log.error("[MongoDb]Failed to write in database with error = ", ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_INSERT_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to save in mongoDb").build();
        }
    }

    @Override
    public void deleteOne(String collectionName, String id) {
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            collection.deleteOne(eq("_id", id));
        } catch (Exception ex) {
            log.error("[DB] failed to delete error = ", ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_DELETE_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Failed to delete from database").build();
        }
    }

    @Override
    public boolean updateById(String collectionName, String id, Object valuesToUpdate) {
        return updateById(collectionName, id, valuesToUpdate, false);
    }

    @Override
    public boolean upsertById(String collectionName, String id, Object valuesToUpdate) {
        return updateById(collectionName, id, valuesToUpdate, true);
    }

    private boolean updateById(String collectionName, String id, Object valuesToUpdate, boolean upsert) {
        boolean updateDone;
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            Map<String, Object> toUpdate = CommonSerializationUtil.convertObjectToMap(valuesToUpdate);
            List<Bson> updates = new ArrayList<>();
            toUpdate.forEach((key, value) -> {
                if (Objects.nonNull(value)) {
                    if ((value instanceof Map) && (ObjectUtils.isEmpty(value))) {
                        updates.add(Updates.unset(key));
                    } else {
                        updates.add(Updates.set(key, value));
                    }
                }
            });
            Bson update = Updates.combine(updates);
            UpdateResult updateResult = collection.updateOne(eq("_id", id), update, new UpdateOptions().upsert(upsert));
            updateDone = (updateResult.getModifiedCount() > 0);
        } catch (Exception ex) {
            log.error("[MongoDb]Failed to write in database with error = ", ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_INSERT_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to save in mongoDb").build();
        }
        return updateDone;
    }

    @Override
    public MongoCollection<Document> getCollection(String collectionName) {
        MongoCollection<Document> collection;
        try {
            MongoDatabase mongoDatabase = mongoClient.getMongoDatabase();
            collection = mongoDatabase.getCollection(collectionName);

            if (consistencyLevel == ConsistencyLevel.EVENTUAL) {
                collection = collection.withReadPreference(ReadPreference.secondaryPreferred());
            }
        } catch (Exception ex) {
            log.error("[MongoDb]Failed to fetch collection with error = ", ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_EXECUTION_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to fetch collection from mongoDb").build();
        }
        return collection;
    }

    @Override
    public <T> T findById(String collectionName, String key, Class<T> cls) throws InternalLibraryException {
        T result;
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            Document searchResult = collection.find(eq("_id", key)).first();
            result = CommonSerializationUtil.readObject(CommonSerializationUtil.writeString(searchResult), cls);
        } catch (final Exception ex) {
            log.error("[MongoDb]Failed to fetch from database for key: {} with error = ", key, ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_GET_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to fetch data from couchbase").build();
        }
        return result;
    }

    @Override
    public <T> Optional<T> findOptionalById(String collectionName, String key, Class<T> cls) throws InternalLibraryException {
        T result = null;
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            Document searchResult = collection.find(eq("_id", key)).first();
            if (Objects.nonNull(searchResult)) {
                result = CommonSerializationUtil.readObject(CommonSerializationUtil.writeString(searchResult), cls);
            }
        } catch (final Exception ex) {
            log.error("[MongoDb]Failed to fetch from database for key: {} with error = ", key, ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_GET_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to fetch data from couchbase").build();
        }
        return Optional.ofNullable(result);
    }

    @Override
    public <T> List<T> findAllByExpression(String collectionName, String expression, Class<T> cls) throws InternalLibraryException {
        List<T> result = new ArrayList<>();
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            Bson filterXpr = expr(Document.parse(expression));
            collection.find(filterXpr).forEach(document -> result.add(CommonSerializationUtil.readObject(CommonSerializationUtil.writeString(document), cls)));
        } catch (final Exception ex) {
            log.error("[MongoDb]Failed to fetch from database for expression: {} with error = ", expression, ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_GET_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to fetch data from couchbase").build();
        }
        return result;
    }

    @Override
    public <T> List<T> findAll(String collectionName, Map<String, Object> searchParams, Class<T> cls) throws InternalLibraryException {
        List<T> result = new ArrayList<>();
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            List<Bson> search = searchParams.keySet().stream().map(key -> eq(key, searchParams.get(key))).collect(Collectors.toList());
            collection.find(and(search)).forEach(document -> result.add(CommonSerializationUtil.readObject(CommonSerializationUtil.writeString(document), cls)));
        } catch (final Exception ex) {
            log.error("[MongoDb]Failed to fetch all from database for searchParams: {} with error = ", searchParams, ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_GET_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to fetch data from couchbase").build();
        }
        return result;
    }

    @Override
    public <T> List<T> findAll(String collectionName, Bson bson, Class<T> cls) throws InternalLibraryException {
        List<T> result = new ArrayList<>();
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            collection.find(bson).forEach(document -> result.add(CommonSerializationUtil.readObject(CommonSerializationUtil.writeString(document), cls)));
        } catch (final Exception ex) {
            log.error("[MongoDb]Failed to fetch all from database with error = ", ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_GET_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to fetch data from couchbase").build();
        }
        return result;
    }

    @Override
    public <T> List<T> findDistinctFromAll(String collectionName, String key, Bson bson, Class<T> cls) throws InternalLibraryException {
        List<T> result = new ArrayList<>();
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            collection.distinct(key, bson, cls).forEach(document -> result.add(CommonSerializationUtil.readObject(CommonSerializationUtil.writeString(document), cls)));
        } catch (final Exception ex) {
            log.error("[MongoDb]Failed to fetch all from database with error = ", ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_GET_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to fetch data from couchbase").build();
        }
        return result;
    }

    @Override
    public <T> T findOne(String collectionName, Map<String, Object> searchParams, Class<T> cls) throws InternalLibraryException {
        T result;
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            List<Bson> search = searchParams.keySet().stream().map(key -> eq(key, searchParams.get(key))).collect(Collectors.toList());
            Document searchResult = collection.find(and(search)).first();
            result = CommonSerializationUtil.readObject(CommonSerializationUtil.writeString(searchResult), cls);
        } catch (final Exception ex) {
            log.error("[MongoDb]Failed to fetch one from database for searchParams: {} with error = ", searchParams, ex);
            throw InternalLibraryException.childBuilder().message(LibraryErrorMessages.DB_GET_FAILED)
                    .displayMessage(StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unable to fetch data from couchbase").build();
        }
        return result;
    }


    @Override
    public <T> void executeBulkWrite(String collectionName, List<BulkOperation<Map<String, T>>> operations) {
        List<WriteModel<Document>> models = new ArrayList<>();
        MongoCollection<Document> collection = getCollection(collectionName);

        for (BulkOperation<Map<String, T>> op : operations) {
            Document filterDoc = op.getFilter() != null ? new Document(op.getFilter()) : new Document();
            Document updateDoc = op.getUpdateOrDocument() != null ? new Document(op.getUpdateOrDocument()) : null;

            switch (op.getType()) {
                case INSERT:
                    models.add(new InsertOneModel<>(updateDoc));
                    break;
                case UPDATE:
                    models.add(new UpdateOneModel<>(filterDoc, updateDoc, new UpdateOptions().upsert(op.isUpsert())));
                    break;
                case UPSERT:
                    models.add(new UpdateOneModel<>(filterDoc, updateDoc, new UpdateOptions().upsert(true)));
                    break;
                case DELETE:
                    models.add(new DeleteOneModel<>(filterDoc));
                    break;
            }
        }
        collection.bulkWrite(models, new BulkWriteOptions().ordered(false));
    }

    @Override
    public <T> PagedResult<T> getDocumentsPaginated(String collectionName, Bson finalFilter, Bson sorts, int pageSize, List<String> cursorList,Class<T> cls) {
        List<T> results = new ArrayList<>();
        List<Document> documents = new ArrayList<>();
        MongoCollection<Document> collection = getCollection(collectionName);
        try (MongoCursor<Document> mongoCursor = collection
                .find(finalFilter)
                .sort(sorts)
                .limit(pageSize + 1)
                .iterator()) {

            while (mongoCursor.hasNext() && documents.size() < pageSize) {
                documents.add(mongoCursor.next());
            }
            documents.forEach(d -> results.add(CommonSerializationUtil.readObject(CommonSerializationUtil.writeString(d), cls)));

            boolean hasMore = mongoCursor.hasNext();
            Map<String, Object> cursorMap = new HashMap<>();
            if (hasMore && !documents.isEmpty()) {
                Document lastDoc = documents.getLast();
                for (String key : cursorList) {
                    cursorMap.put(key, lastDoc.get(key));
                }

                return PagedResult.<T>builder().results(results).cursorMap(cursorMap).lastPage(false).build();
            } else {
                return PagedResult.<T>builder().results(results).cursorMap(cursorMap).lastPage(true).build();
            }
        }

    }


    @Override
    public boolean isHealthy() {
        boolean healthy = true;
        try {
            MongoDatabase db = mongoClient.getMongoDatabase();
            Document result = db.runCommand(new Document("ping", 1));
            log.debug("Ping result: {}", result.toJson());
        } catch (Exception ex) {
            log.error("[MongoDb Unhealthy] Health check failed with error = ", ex);
            healthy = false;
        }
        return healthy;
    }
}

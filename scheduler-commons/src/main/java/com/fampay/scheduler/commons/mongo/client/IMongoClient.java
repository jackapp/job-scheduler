package com.fampay.scheduler.commons.mongo.client;

import com.mongodb.client.MongoDatabase;

public interface IMongoClient {
    void start();
    void shutdown();

    MongoDatabase getMongoDatabase();
}
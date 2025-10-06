package com.fampay.scheduler.commons.mongo.client.impl;


import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fampay.scheduler.commons.mongo.client.IMongoClient;
import com.fampay.scheduler.commons.mongo.config.MongoConfiguration;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadConcern;
import com.mongodb.ReadConcernLevel;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.micrometer.common.util.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.CONFIG_MISSING;
import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.IMPROPER_CONFIG;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(prefix = "mongo-config",name = {"database"})
public class MongoDbClient implements IMongoClient, DisposableBean {

    private final MongoConfiguration config;

    @Getter
    private MongoClient client;

    @Autowired
    public MongoDbClient(MongoConfiguration config) {
        this.config = config;
        validate();
        start();
    }

    private void validate() {
        if (StringUtils.isBlank(config.getDatabase())) {
            throw InternalLibraryException.childBuilder()
                    .message(CONFIG_MISSING)
                    .displayMessage("Missing database")
                    .build();
        }
    }

    @Override
    public void start() {
        MongoClientSettings.Builder settings = MongoClientSettings.builder();
        if (config.isSrvMode()) {
            // https://www.mongodb.com/docs/drivers/java/sync/v4.8/fundamentals/connection/mongoclientsettings/
            // If you want to enable the processing of TXT records, you must specify the SRV host in the connection string using the applyConnectionString() method.
            // Also credential building adds extra parameters (e.g. ssl) in the ConnectionString obj
            StringBuilder connString = new StringBuilder("mongodb+srv://");

            //Credentials
            connString.append(config.getUsername()).append(":").append(config.getPassword()).append("@");

            //Host & Database
            connString.append(config.getAddress()).append("/").append(config.getDatabase()).append("?");

            settings.applyConnectionString(new ConnectionString(connString.toString()));
        } else {
            settings.applyToClusterSettings(builder -> {
                List<ServerAddress> serverAddress = new ArrayList<>();
                Arrays.stream(config.getAddress().split(",")).forEach(server -> {
                    String[] node = server.split(":");
                    if (node.length != 2) {
                        throw InternalLibraryException.childBuilder()
                                .message(IMPROPER_CONFIG)
                                .displayMessage("Incorrect format for connection string: " + config.getAddress())
                                .build();
                    }
                    serverAddress.add(new ServerAddress(node[0], Integer.parseInt(node[1])));
                });
                builder.hosts(serverAddress);
            });

            if (StringUtils.isNotBlank(config.getUsername()) && StringUtils.isNotBlank(config.getPassword())) {
                settings.credential(MongoCredential.createCredential(
                        config.getUsername(),
                        config.getDatabase(),
                        config.getPassword().toCharArray()
                ));
            }
        }

        settings.writeConcern(WriteConcern.valueOf(config.getWriteConcernLevel())
                        .withWTimeout(config.getWriteTimeoutMS(), TimeUnit.MILLISECONDS)
                        .withJournal(config.isJournalEnabled()))
                .readConcern(new ReadConcern(ReadConcernLevel.fromString(config.getReadConcernLevel())))
                .readPreference(ReadPreference.valueOf(config.getReadPreference()))
                .applyToSocketSettings(builder -> {
                    builder.connectTimeout(config.getConnectTimeoutMS(), TimeUnit.MILLISECONDS);
                    builder.readTimeout(config.getReadTimeoutMS(), TimeUnit.MILLISECONDS);
                })
                .applyToConnectionPoolSettings(builder -> {
                    builder.maxSize(config.getMaxPoolSize());
                    builder.minSize(config.getMinPoolSize());
                    builder.maxConnectionIdleTime(config.getMaxConnectionIdleTimeMS(), TimeUnit.MILLISECONDS);
                });

        client = MongoClients.create(settings.build());
    }

    @Override
    public void shutdown() {
        if (Objects.nonNull(client)) {
            client.close();
            client = null;
            log.info("Mongo client has been shutdown");
        }
    }

    @Override
    public MongoDatabase getMongoDatabase() {
        return client.getDatabase(config.getDatabase());
    }

    @Override
    public void destroy() throws Exception {
        shutdown();
    }
}

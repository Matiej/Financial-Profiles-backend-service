package com.emat.reapi.config;

import com.mongodb.client.MongoClient;
import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@EnableMongock
@EnableReactiveMongoAuditing
public class MongoConfiguration {

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient,
                                       MongoProperties properties) {
        return new MongoTemplate(mongoClient, properties.getDatabase());
    }

}

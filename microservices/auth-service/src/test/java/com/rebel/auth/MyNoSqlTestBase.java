package com.rebel.auth;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class MyNoSqlTestBase {

	private static MongoDBContainer database = new MongoDBContainer(DockerImageName.parse("mongo:5.0"));

	static {
		database.start();
	}

	@DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.mongodb.uri",
                database::getReplicaSetUrl
        );
    }

}

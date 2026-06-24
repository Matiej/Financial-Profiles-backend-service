package com.emat.reapi

import com.emat.reapi.configuration.TestSecurityConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfiguration)
@Testcontainers
abstract class BaseIntegrationSpec extends Specification {

    static final MongoDBContainer MONGO_DB = new MongoDBContainer("mongo:7.0")
            .withReuse(true)

    static {
        MONGO_DB.start()
    }

    @Autowired
    ReactiveMongoTemplate mongoTemplate

    @Autowired
    WebTestClient webTestClient

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri") { MONGO_DB.replicaSetUrl }
    }

    def setup() {
        mongoTemplate.collectionNames
                .flatMap { name -> mongoTemplate.dropCollection(name) }
                .blockLast()
    }

    protected WebTestClient.RequestHeadersSpec<?> authenticatedGet(String uri, String... roles) {
        webTestClient.get().uri(uri).headers(bearer(roles))
    }

    protected WebTestClient.RequestBodySpec authenticatedPost(String uri, String... roles) {
        webTestClient.post().uri(uri).headers(bearer(roles))
    }

    protected WebTestClient.RequestBodySpec authenticatedPut(String uri, String... roles) {
        webTestClient.put().uri(uri).headers(bearer(roles))
    }

    protected WebTestClient.RequestHeadersSpec<?> authenticatedDelete(String uri, String... roles) {
        webTestClient.delete().uri(uri).headers(bearer(roles))
    }

    /**
     * Builds an Authorization header consumer carrying the roles as the bearer token value.
     * The {@link TestSecurityConfiguration} decoder reads them back into the
     * {@code realm_access.roles} claim.
     */
    private static Closure bearer(String... roles) {
        return { headers -> headers.setBearerAuth(roles.join(",")) }
    }
}

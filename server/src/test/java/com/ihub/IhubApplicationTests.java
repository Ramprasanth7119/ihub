package com.ihub;

import com.ihub.search.IdeaSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the whole application context wires up — every controller, service, DAO,
 * scheduler, security filter and the STOMP interceptor.
 *
 * <p>Runs on the {@code test} profile (in-memory H2, no Flyway, disabled schedulers)
 * so no MySQL instance, Elasticsearch cluster or exported secret is required.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class IhubApplicationTests {

    @MockBean
    private IdeaSearchRepository ideaSearchRepository;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    void contextLoads() {
    }

}

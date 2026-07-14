package com.ihub;

import com.ihub.search.IdeaSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@SpringBootTest
class IhubApplicationTests {

    @MockBean
    private IdeaSearchRepository ideaSearchRepository;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    void contextLoads() {
    }

}

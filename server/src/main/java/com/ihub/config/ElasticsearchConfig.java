//package com.ihub.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.elasticsearch.client.ClientConfiguration;
//import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
//import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
//
//@Configuration
//public class ElasticsearchConfig extends ElasticsearchConfiguration {
//
//    @Override
//    public ClientConfiguration clientConfiguration() {
//        return ClientConfiguration.builder()
//                .connectedTo("localhost:9200")
//                .build();
//    }
//
//    @Bean(name = "elasticsearchTemplate")
//    public ElasticsearchOperations elasticsearchTemplate(ElasticsearchOperations operations) {
//        return operations;
//    }
//}
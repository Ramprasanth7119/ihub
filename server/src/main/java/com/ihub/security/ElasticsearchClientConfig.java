//package com.ihub.security;
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.json.jackson.JacksonJsonpMapper;
//import co.elastic.clients.transport.ElasticsearchTransport;
//import co.elastic.clients.transport.rest_client.RestClientTransport;
//import org.apache.http.HttpHost;
//import org.apache.http.auth.AuthScope;
//import org.apache.http.auth.UsernamePasswordCredentials;
//import org.apache.http.client.CredentialsProvider;
//import org.apache.http.impl.client.BasicCredentialsProvider;
//import org.apache.http.ssl.SSLContexts;
//import org.apache.http.conn.ssl.NoopHostnameVerifier;
//import org.apache.http.conn.ssl.TrustAllStrategy;
//import org.elasticsearch.client.RestClient;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import javax.net.ssl.SSLContext;
//
//@Configuration
//public class ElasticsearchClientConfig {
//
//    @Bean
//    public ElasticsearchClient elasticsearchClient(
//            @Value("${elasticsearch.url:https://localhost:9200}") String url, 
//            @Value("${elasticsearch.username:elastic}") String username, 
//            @Value("${elasticsearch.password:33TiQ2U=HrSW1v22r5e7}") String password) throws Exception {
//
//        // 1. Setup SSL context to trust the default self-signed cert of ES 8
//        SSLContext sslContext = SSLContexts.custom()
//                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
//                .build();
//
//        // 2. Setup Credentials
//        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//        credentialsProvider.setCredentials(AuthScope.ANY, 
//                new UsernamePasswordCredentials(username, password));
//
//        // 3. Build the RestClient
//        RestClient restClient = RestClient.builder(HttpHost.create(url))
//                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
//                        .setDefaultCredentialsProvider(credentialsProvider)
//                        .setSSLContext(sslContext)
//                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE))
//                .build();
//
//        // 4. Create the Transport and Client
//        ElasticsearchTransport transport = new RestClientTransport(
//                restClient, new JacksonJsonpMapper());
//
//        return new ElasticsearchClient(transport);
//    }
//}
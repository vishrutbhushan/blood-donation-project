package com.hemo.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ElasticsearchConfig {
    @Bean
    RestClient elasticsearchRestClient(RestClient.Builder builder,
            @Value("${app.elasticsearch-url:http://elasticsearch:9200}") String elasticsearchUrl) {
        return builder.baseUrl(elasticsearchUrl).build();
    }
}

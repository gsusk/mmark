package org.adso.minimarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestMinimarketApplication {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("minimarket");
    }

    @Bean
    @ServiceConnection
    ElasticsearchContainer elasticsearchContainer() {
        return new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.2.4")
                .withEnv("xpack.security.enabled", "false")
                .withEnv("xpack.security.http.ssl.enabled", "false");
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(MinimarketApplication.class)
                .sources(TestMinimarketApplication.class)
                .properties("spring.elasticsearch.username=", "spring.elasticsearch.password=")
                .run(args);
    }
}

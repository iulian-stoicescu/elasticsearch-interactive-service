package com.example.elasticsearchinteractiveservice.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Configuration
public class ElasticsearchConfiguration {

    @Value("${my.elasticsearch.hostname}")
    private String hostname;
    @Value("${my.elasticsearch.port}")
    private int port;
    @Value("${my.elasticsearch.username}")
    private String username;
    @Value("${my.elasticsearch.password}")
    private String password;
    @Value("${my.elasticsearch.fingerprint}")
    private String fingerprint;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // create the ssl context
        SSLContext sslContext = TransportUtils.sslContextFromCaFingerprint(this.fingerprint);

        // create the credentials provider
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(this.username, this.password));

        // create the low-level client
        RestClient restClient = RestClient.builder(new HttpHost(this.hostname, this.port, "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setSSLContext(sslContext)
                        .setDefaultCredentialsProvider(credentialsProvider)).build();

        // create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // create the API client
        return new ElasticsearchClient(transport);
    }
}

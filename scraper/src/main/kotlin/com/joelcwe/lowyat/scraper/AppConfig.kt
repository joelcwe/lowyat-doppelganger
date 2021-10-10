package com.joelcwe.lowyat.scraper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.http.ssl.SSLContextBuilder
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.RestClients
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import javax.net.ssl.SSLContext

@Configuration
@EnableElasticsearchRepositories
class AppConfig(private val properties: ElasticsearchProperties, private val trustStoreProperties: TrustStoreProperties) {

    var sslContext: SSLContext = SSLContextBuilder()
        .loadTrustMaterial(trustStoreProperties.trustStore.url, trustStoreProperties.trustStorePassword.toCharArray())
        .build()

    @Bean
    fun elasticsearchClient(): RestHighLevelClient {
        val clientConfiguration = ClientConfiguration.builder().connectedTo(properties.uris).usingSsl(sslContext)
            .withBasicAuth(properties.username, properties.password).withPathPrefix("elasticsearch").build()

        return RestClients.create(clientConfiguration).rest()
    }

    @Bean
    fun crScope(): CoroutineScope = CoroutineScope(Dispatchers.Default)
}

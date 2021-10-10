package com.joelcwe.lowyat.scraper

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.Resource

@ConstructorBinding
@ConfigurationProperties("spring.elasticsearch.rest")
data class ElasticsearchProperties(var uris:String, var username : String, var password : String)

@ConstructorBinding
@ConfigurationProperties("self-signed")
data class TrustStoreProperties(var trustStore : Resource, var trustStorePassword: String)

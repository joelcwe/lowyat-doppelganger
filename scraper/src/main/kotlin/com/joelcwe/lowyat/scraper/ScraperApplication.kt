package com.joelcwe.lowyat.scraper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ElasticsearchProperties::class, TrustStoreProperties:: class)
class ScraperApplication

fun main(args: Array<String>) {
	runApplication<ScraperApplication>(*args)
}

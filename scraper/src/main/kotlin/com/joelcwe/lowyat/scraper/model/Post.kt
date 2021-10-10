package com.joelcwe.lowyat.scraper.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.elasticsearch.annotations.*
import org.springframework.data.elasticsearch.core.join.JoinField
import java.time.Instant

@Document(indexName = "lowyat")
@TypeAlias("forum_post")
data class Post(
    @Id val id: String,
    val title: String? = null,
    val content: String,
    @CreatedDate
    @Field(type = FieldType.Date, format = [DateFormat.basic_date_time])
    val dateTime: Instant,
    @JoinTypeRelations(
        relations = [JoinTypeRelation(parent = "topic", children = ["post"])]
    )
    val type: JoinField<String>,
    val username: String,
    val topicId: Int
)
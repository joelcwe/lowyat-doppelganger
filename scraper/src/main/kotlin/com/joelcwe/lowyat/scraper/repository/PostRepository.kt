package com.joelcwe.lowyat.scraper.repository

import com.joelcwe.lowyat.scraper.model.Post
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service

@Service
interface PostRepository : CrudRepository<Post, String> {

}
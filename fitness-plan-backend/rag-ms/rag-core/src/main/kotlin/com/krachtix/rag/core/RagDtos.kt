package com.krachtix.rag.core

import java.time.Instant
import java.util.UUID

data class RagDocumentRequest(
    val merchantId: UUID,
    val source: String,
    val title: String,
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)

data class RagDocumentResponse(
    val documentId: UUID,
    val merchantId: UUID,
    val source: String,
    val title: String,
    val contentPreview: String,
    val createdAt: Instant
)

data class RagQueryRequest(
    val merchantId: UUID,
    val query: String,
    val topK: Int = 5,
    val filters: Map<String, Any> = emptyMap()
)

data class RagQueryResultItem(
    val documentId: UUID,
    val chunkId: String,
    val score: Double,
    val content: String,
    val source: String,
    val title: String
)

data class RagQueryResponse(
    val query: String,
    val results: List<RagQueryResultItem>
)

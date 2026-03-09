package com.krachtix.rag.app

import com.krachtix.rag.core.RagDocumentRequest
import com.krachtix.rag.core.RagDocumentResponse
import com.krachtix.rag.core.RagQueryRequest
import com.krachtix.rag.core.RagQueryResponse
import com.krachtix.rag.core.RagQueryResultItem
import com.krachtix.rag.core.RagService
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class RagServiceImpl(
    private val embeddingModel: EmbeddingModel,
    private val embeddingStore: EmbeddingStore<TextSegment>,
    private val embeddingStoreIngestor: EmbeddingStoreIngestor
) : RagService {

    override fun ingest(request: RagDocumentRequest): RagDocumentResponse {
        val now = Instant.now()
        val documentId = UUID.randomUUID()
        val metadata = Metadata.from(
            mapOf(
                "merchant_id" to request.merchantId.toString(),
                "source" to request.source,
                "title" to request.title,
                "document_id" to documentId
            ) + request.metadata.mapValues { it.value.toString() }
        )
        val document = Document.from(request.content, metadata)
        embeddingStoreIngestor.ingest(document)

        return RagDocumentResponse(
            documentId = documentId,
            merchantId = request.merchantId,
            source = request.source,
            title = request.title,
            contentPreview = request.content.take(200),
            createdAt = now
        )
    }

    override fun query(request: RagQueryRequest): RagQueryResponse {
        val embedding = embeddingModel.embed(request.query).content()
        val searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(embedding)
            .maxResults(request.topK)
            .filter(metadataKey("merchant_id").isEqualTo(request.merchantId.toString()))
            .build()
        val searchResult = embeddingStore.search(searchRequest)
        val results = searchResult.matches().map { match ->
            val segment = match.embedded()
            val metadata = segment.metadata()
            val documentId = metadata.getUUID("document_id") ?: UUID.randomUUID()
            RagQueryResultItem(
                documentId = documentId,
                chunkId = match.embeddingId(),
                score = match.score(),
                content = segment.text(),
                source = metadata.getString("source") ?: "unknown",
                title = metadata.getString("title") ?: "unknown"
            )
        }
        return RagQueryResponse(query = request.query, results = results)
    }
}

package com.krachtix.rag.app

import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.data.segment.TextSegment
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import javax.sql.DataSource

@Configuration
class RagConfiguration {

    @Bean
    fun embeddingModel(
        @Value("\${langchain4j.ollama.embedding.base-url:http://localhost:11434}") baseUrl: String,
        @Value("\${langchain4j.ollama.embedding.model-name:nomic-embed-text}") modelName: String
    ): EmbeddingModel {
        return OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName(modelName)
            .build()
    }

    @Bean
    @DependsOn("flywayInitializer")
    fun embeddingStore(
        dataSource: DataSource,
        embeddingModel: EmbeddingModel,
        @Value("\${rag.pgvector.table-name:rag_embedding}") tableName: String
    ): EmbeddingStore<TextSegment> {
        return PgVectorEmbeddingStore.datasourceBuilder()
            .datasource(dataSource)
            .table(tableName)
            .dimension(embeddingModel.dimension())
            .createTable(true)
            .build()
    }

    @Bean
    fun documentSplitter(
        @Value("\${rag.splitter.max-segment-size:1000}") maxSegmentSize: Int,
        @Value("\${rag.splitter.max-overlap-size:200}") maxOverlapSize: Int
    ): DocumentSplitter {
        return DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize)
    }

    @Bean
    fun embeddingStoreIngestor(
        embeddingModel: EmbeddingModel,
        embeddingStore: EmbeddingStore<TextSegment>,
        documentSplitter: DocumentSplitter
    ): EmbeddingStoreIngestor {
        return EmbeddingStoreIngestor.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .documentSplitter(documentSplitter)
            .build()
    }
}

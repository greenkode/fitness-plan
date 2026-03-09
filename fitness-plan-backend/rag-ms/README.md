# RAG Service (PgVector)

## Overview
This service provides RAG ingestion and query APIs backed by LangChain4j and PgVector. It stores embeddings in Postgres using the PgVector extension and uses metadata filters for tenant isolation.

## Configuration

### Postgres
- Ensure the PgVector extension is enabled.
- Migration: `rag-ms/rag-app/src/main/resources/db/migration/V1__enable_pgvector.sql`

### Environment variables
- `SPRING_DATASOURCE_URL` (e.g. `jdbc:postgresql://localhost:5432/ragdb`)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### LangChain4j (Ollama embeddings)
- `LANGCHAIN4J_OLLAMA_EMBEDDING_BASE_URL` (default `http://localhost:11434`)
- `LANGCHAIN4J_OLLAMA_EMBEDDING_MODEL_NAME` (default `nomic-embed-text`)

### RAG settings
- `RAG_PGVECTOR_TABLE_NAME` (default `rag_embedding`)
- `RAG_SPLITTER_MAX_SEGMENT_SIZE` (default `1000`)
- `RAG_SPLITTER_MAX_OVERLAP_SIZE` (default `200`)

## Notes
- Metadata is stored alongside embeddings and used for tenant filtering.
- Query API uses vector similarity and metadata filters.
- For hybrid (BM25 + vector) retrieval, add a separate search backend later.

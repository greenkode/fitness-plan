package com.krachtix.rag.core

import java.util.UUID

interface RagService {
    fun ingest(request: RagDocumentRequest): RagDocumentResponse
    fun query(request: RagQueryRequest): RagQueryResponse
}

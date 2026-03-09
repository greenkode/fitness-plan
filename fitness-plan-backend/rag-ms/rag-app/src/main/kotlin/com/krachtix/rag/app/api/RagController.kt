package com.krachtix.rag.app.api

import com.krachtix.rag.core.RagDocumentRequest
import com.krachtix.rag.core.RagDocumentResponse
import com.krachtix.rag.core.RagQueryRequest
import com.krachtix.rag.core.RagQueryResponse
import com.krachtix.rag.core.RagService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rag")
class RagController(
    private val ragService: RagService
) {

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    fun ingest(@RequestBody request: RagDocumentRequest): RagDocumentResponse {
        return ragService.ingest(request)
    }

    @PostMapping("/query")
    fun query(@RequestBody request: RagQueryRequest): RagQueryResponse {
        return ragService.query(request)
    }
}

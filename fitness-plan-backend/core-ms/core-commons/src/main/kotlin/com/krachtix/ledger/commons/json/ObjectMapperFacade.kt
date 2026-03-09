package com.krachtix.commons.json

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.core.JacksonException
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.datatype.moneta.MonetaMoneyModule
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.convertValue
import java.io.BufferedReader
import java.io.IOException

object ObjectMapperFacade {
    private val objectMapper: ObjectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .addModule(MonetaMoneyModule().withAmountFieldName("number").withCurrencyFieldName("currency"))
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultPropertyInclusion { JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL) }
        .build()

    fun writeValueAsString(`object`: Any): String {
        try {
            return objectMapper.writeValueAsString(`object`)
        } catch (e: JacksonException) {
            throw RuntimeException(
                String.format(
                    "Unable to convert object of type %s to json: reason - %s",
                    `object`.javaClass.name,
                    e.message
                )
            )
        }
    }

    fun writeValueAsStringMap(`object`: Any): Map<String, Any?> {
        try {
            return objectMapper.convertValue<Map<String, Any?>>(`object`)
        } catch (e: JacksonException) {
            throw RuntimeException(
                String.format(
                    "Unable to convert object of type %s to string map - %s",
                    `object`.javaClass.name,
                    e.message
                )
            )
        }
    }

    fun <T> fromJson(json: String, clazz: Class<T>?): T {
        try {
            return objectMapper.readValue(json, clazz)
        } catch (e: JacksonException) {
            throw RuntimeException("Error deserializing JSON to object", e)
        }
    }

    fun <T> fromJson(json: String?, valueTypeRef: TypeReference<T>): T {
        try {
            return objectMapper.readValue(json, valueTypeRef)
        } catch (e: JacksonException) {
            throw RuntimeException("Unable to convert to type ${valueTypeRef.type.typeName},  reason - ${e.message}")
        }
    }

    fun readTree(reader: BufferedReader?): JsonNode {
        return try {
            objectMapper.readTree(reader)
        } catch (e: IOException) {
            JsonNodeFactory.instance.missingNode()
        }
    }
}

package me.retrodaredevil.solarthing.android

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import me.retrodaredevil.solarthing.util.JacksonUtil

fun createDefaultObjectMapper(): ObjectMapper {
    return JacksonUtil.defaultMapper().apply {
        registerModule(KotlinModule())
    }
}

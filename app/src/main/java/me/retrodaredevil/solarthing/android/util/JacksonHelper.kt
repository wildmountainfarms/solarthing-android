package me.retrodaredevil.solarthing.android.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import me.retrodaredevil.solarthing.util.JacksonUtil

fun createDefaultObjectMapper(): ObjectMapper {
    return JacksonUtil.defaultMapper().apply {
        registerKotlinModule()
    }
}

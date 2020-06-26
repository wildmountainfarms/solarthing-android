package me.retrodaredevil.solarthing.android.prefs.saving

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import me.retrodaredevil.solarthing.android.prefs.ProfileHolder

class JacksonProfileHolder<T>(
        private val stringValueSaver: StringValueSaver,
        private val javaType: JavaType,
        private val objectMapper: ObjectMapper,
        private val defaultProfileCreator: () -> T
) : ProfileHolder<T> {
    override var profile: T
        get() {
            val string = stringValueSaver.stringValue
            if (string == null) {
                println("Creating a new default profile. javaType: $javaType")
                val profile = defaultProfileCreator()
                stringValueSaver.stringValue = objectMapper.writeValueAsString(profile)
                return profile
            }
            return objectMapper.readValue(string, javaType)
        }
        set(value) {
            stringValueSaver.stringValue = objectMapper.writeValueAsString(value)
        }

}

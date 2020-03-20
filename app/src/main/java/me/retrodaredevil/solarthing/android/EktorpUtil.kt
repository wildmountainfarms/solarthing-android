package me.retrodaredevil.solarthing.android

import me.retrodaredevil.couchdb.CouchProperties
import org.ektorp.android.http.AndroidHttpClient
import org.ektorp.http.HttpClient
import java.net.MalformedURLException

fun createHttpClient(properties: CouchProperties): HttpClient {
    val maxConnections = properties.maxConnections
    val proxyPort = properties.proxyPort
    val connectionTimeout = properties.connectionTimeoutMillis
    val socketTimeout = properties.socketTimeoutMillis
    val path = properties.path ?: ""
    try {
        val builder = AndroidHttpClient.Builder()
            .url(properties.protocol + "://" + properties.host + ":" + properties.port + path)
            .username(properties.username)
            .password(properties.password)
            .proxy(properties.proxyHost)

        if(connectionTimeout != null){
            builder.connectionTimeout(connectionTimeout)
        }
        if(socketTimeout != null){
            builder.socketTimeout(socketTimeout)
        }
        if(maxConnections != null){
            builder.maxConnections(maxConnections)
        }
        if(proxyPort != null){
            builder.proxyPort(proxyPort)
        }
        return builder.build()
    } catch (e: MalformedURLException) {
        throw RuntimeException(e)
    }
}

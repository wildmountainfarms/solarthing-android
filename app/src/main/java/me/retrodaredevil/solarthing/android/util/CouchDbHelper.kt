package me.retrodaredevil.solarthing.android.util

import me.retrodaredevil.couchdb.CouchDbUtil
import me.retrodaredevil.couchdb.CouchProperties
import me.retrodaredevil.couchdbjava.CouchDbInstance
import me.retrodaredevil.okhttp3.OkHttpPropertiesBuilder


fun createCouchDbInstance(properties: CouchProperties): CouchDbInstance {
    return CouchDbUtil.createInstance(
            properties,
            OkHttpPropertiesBuilder()
                    .setConnectTimeoutMillis(10_000)
                    .setCallTimeoutMillis(Int.MAX_VALUE)
                    .build()
    )
}

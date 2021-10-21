package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.type.closed.meta.MetaDatabase

class MetaHandler {
    @get:Synchronized
    @set:Synchronized
    var metaDatabase: MetaDatabase? = null
}

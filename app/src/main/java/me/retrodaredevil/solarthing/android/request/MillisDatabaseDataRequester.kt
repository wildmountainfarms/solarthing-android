package me.retrodaredevil.solarthing.android.request

import me.retrodaredevil.couchdbjava.exception.CouchDbException
import me.retrodaredevil.solarthing.database.MillisDatabase
import me.retrodaredevil.solarthing.database.MillisQuery
import me.retrodaredevil.solarthing.database.MillisQueryBuilder
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*


class MillisDatabaseDataRequester(
        private val database: MillisDatabase,
        private val host: String,
        private val queryGetter: () -> MillisQuery
) : DataRequester {

    @Volatile
    override var currentlyUpdating = false
        private set

    /**
     * Takes over the current thread and runs until data is retrieved or an error occurred.
     *
     * @throws IllegalStateException Thrown when [currentlyUpdating] is true
     * @return The [DataRequest] which holds data about if the request was successful or not
     */
    override fun requestData(): DataRequest {
        synchronized(this) {
            if (currentlyUpdating) {
                throw IllegalStateException("The data is currently being updated!")
            }
            currentlyUpdating = true
        }
        try {
            val query = queryGetter()
            val packetGroups = database.query(query)
            if(packetGroups.isEmpty()){
                return DataRequest(
                    emptyList(),
                    null,
                    false, "Got all unknown packets",
                    host,
                    null,
                    null,
                )
            }
            return DataRequest(packetGroups, query, true, "Request Successful", host)
        } catch(ex: CouchDbException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), null, false,
                "Request Failed", host, getStackTrace(ex), ex.message)
        } catch(ex: NullPointerException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), null, false,
                "(Please report) NPE (Likely Parsing Error)", host, getStackTrace(ex), ex.message)
        } catch(ex: Exception) {
            ex.printStackTrace()
            return DataRequest(
                    Collections.emptyList(),
                    null,
                    false,
                    "(Please report) ${ex.javaClass.simpleName} (Unknown)",
                    host,
                    getStackTrace(ex),
                    ex.message
            )
        } finally {
            currentlyUpdating = false
        }
    }
    private fun getStackTrace(throwable: Throwable): String{
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }
}

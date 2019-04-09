package me.retrodaredevil.solarthing.android.request


private class RequesterState(
    val requester: DataRequester
){
    var failedLastTime = false
    var priorityUntil: Int? = null
}


class DataRequesterMultiplexer : DataRequester {

    private val requesterStateList: List<RequesterState>
    private var requestCount = 0

    @Volatile
    override var currentlyUpdating: Boolean = false
        private set

    constructor(requesterList: List<DataRequester>){
        if (requesterList.isEmpty()){
            throw IllegalArgumentException("Cannot use a list of DataRequester's that is empty")
        }
        requesterStateList = requesterList.map { RequesterState(it) }
    }

    override fun requestData(): DataRequest {
        if(currentlyUpdating){
            throw IllegalStateException("Cannot request data while already requesting data!")
        }
        currentlyUpdating = true
        try {
            var lastRequesterState: RequesterState? = null
            var requesterState: RequesterState? = null
            for(element in requesterStateList.reversed()){ // go through the least priority first
                if(element.failedLastTime){ // even if this is the DataRequester with the least priority, it will go back to the first one
                    requesterState = lastRequesterState
                    break
                }
                val priorityUntil = element.priorityUntil
                if(priorityUntil != null && priorityUntil > requestCount){
                    requesterState = element
                    break
                }
                lastRequesterState = element
            }

            if(requesterState == null){
                requesterState = requesterStateList.first()
            }
            for(element in requesterStateList){
                if(element !== requesterState) {
                    element.failedLastTime = false
                    element.priorityUntil = null
                }
            }
            val dataRequest = requesterState.requester.requestData()
            if(dataRequest.successful){
                requesterState.failedLastTime = false
                if(requesterState.priorityUntil == null) {
                    requesterState.priorityUntil = requestCount + 3
                }
            } else {
                requesterState.failedLastTime = true
            }

            requestCount++
            return dataRequest
        } finally {
            currentlyUpdating = false
        }
    }

}
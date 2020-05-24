package me.retrodaredevil.solarthing.android.request


private class RequesterState(val index: Int){
    var failedLastTime = false
    var priorityUntil: Int? = null
}


class DataRequesterMultiplexer (
        private val requesterListSupplier: () -> List<DataRequester>
) : DataRequester {

    private var requesterStateList: List<RequesterState>? = null
    private var requestCount = 0

    constructor(requesterList: List<DataRequester>) : this({requesterList})

    @Volatile
    override var currentlyUpdating: Boolean = false
        private set

    override fun requestData(): DataRequest {
        synchronized(this) {
            if (currentlyUpdating) {
                throw IllegalStateException("The data is currently being updated!")
            }
            currentlyUpdating = true
        }
        try {
            val requesters = requesterListSupplier()
            var requesterStateList = this.requesterStateList
            if(requesterStateList == null || requesterStateList.size != requesters.size){
                requesterStateList = requesters.mapIndexed { index, _ ->  RequesterState(index)}
                this.requesterStateList = requesterStateList
            }

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
            val dataRequest = requesters[requesterState.index].requestData()
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
package me.retrodaredevil.solarthing.android.service

import java.util.*


fun <T> createComparator (dateMillisGetter: (T) -> Long): Comparator<T>{
    return Comparator {o1, o2 -> (dateMillisGetter(o1) - dateMillisGetter(o2)).toInt() }
}

fun <T> MutableCollection<T>.removeIfBefore(time: Long, dateMillisGetter: (T) -> Long){
//    removeIf { dateMillisGetter(it) < time }
    val iterator = iterator()
    while(iterator.hasNext()){
        val element = iterator.next()
        if(dateMillisGetter(element) < time){
            iterator.remove()
        }
    }
}

/**
 * Limits the size of a [NavigableSet] by removing elements from the beginning of the [NavigableSet] if necessary
 */
fun NavigableSet<*>.limitSize(maxSize: Int, sizeIfResizeNeeded: Int = maxSize){
    if(sizeIfResizeNeeded > maxSize){
        throw IllegalArgumentException()
    }
    if(size > maxSize){
        val removeAmount = size - sizeIfResizeNeeded
        for(i in 1..removeAmount){
            pollFirst()
        }
    }
}
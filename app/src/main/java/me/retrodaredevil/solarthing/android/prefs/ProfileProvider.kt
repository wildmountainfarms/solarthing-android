package me.retrodaredevil.solarthing.android.prefs

interface ProfileProvider<T> {
    val activeProfile: T
}
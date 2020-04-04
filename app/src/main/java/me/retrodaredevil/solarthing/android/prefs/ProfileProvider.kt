package me.retrodaredevil.solarthing.android.prefs

interface ProfileProvider<T> {
    val activeProfile: ProfileHolder<T>
}
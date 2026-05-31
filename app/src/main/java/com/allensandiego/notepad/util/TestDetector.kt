package com.allensandiego.notepad.util

import android.content.Context

object TestDetector {
    private var isTesting: Boolean? = null

    fun isUnderTest(context: Context): Boolean {
        return isTesting ?: synchronized(this) {
            isTesting ?: run {
                val detected = try {
                    Class.forName("androidx.test.espresso.Espresso")
                    true
                } catch (e: ClassNotFoundException) {
                    false
                }
                isTesting = detected
                detected
            }
        }
    }
}

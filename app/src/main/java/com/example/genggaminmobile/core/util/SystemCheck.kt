package com.example.genggaminmobile.core.util

import android.content.Context
import com.scottyab.rootbeer.RootBeer

object SystemCheck {
    fun isDeviceRooted(context: Context): Boolean {
        val rootBeer = RootBeer(context)
        // isRooted() checks for standard su binaries, dangerous apps, and RW system partition
        return rootBeer.isRooted
    }
}

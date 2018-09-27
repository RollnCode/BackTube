package com.rollncode.utility.receiver

import android.support.annotation.IdRes

interface ObjectsReceiver {
    fun onObjectsReceive(@IdRes event: Int, vararg objects: Any)
}
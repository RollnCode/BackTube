package com.rollncode.receiver

import android.support.annotation.IdRes

interface ObjectsReceiver {
    fun onObjectsReceive(@IdRes event: Int, vararg objects: Any)
}
package com.rollncode.receiver

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.annotation.AnyThread
import android.support.annotation.IdRes
import android.support.annotation.MainThread
import android.support.v4.util.ArraySet
import android.support.v4.util.SparseArrayCompat
import java.lang.ref.Reference
import java.lang.ref.SoftReference

object ReceiverBus {

    private val mainHandler by lazy { MainHandler() }
    private val receivers = SparseArrayCompat<Reference<ObjectsReceiver>>()
    private val eventReceivers = SparseArrayCompat<MutableSet<Int>>()

    fun subscribe(receiver: ObjectsReceiver, vararg events: Int) {
        if (events.isEmpty()) throw IllegalStateException("empty events is not possible")

        val id = receiver.hashCode()
        receivers.put(id, SoftReference(receiver))

        var set: MutableSet<Int>?
        for (event in events) {
            set = eventReceivers[event]
            if (set == null) {
                set = ArraySet<Int>()
                eventReceivers.put(event, set)
            }
            set.add(id)
        }
    }

    fun unSubscribe(receiver: ObjectsReceiver, vararg events: Int) {
        val id = receiver.hashCode()
        if (events.isEmpty()) {
            receivers.remove(id)

            var i = 0
            while (i < eventReceivers.size())
                eventReceivers.valueAt(i++)?.remove(id)

        } else {
            for (event in events)
                eventReceivers[event]?.remove(id)
        }
    }

    @SuppressLint("WrongThread")
    @AnyThread
    fun notify(@IdRes event: Int, vararg objects: Any) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val iterator = eventReceivers[event]?.iterator() ?: return
            while (iterator.hasNext())
                if (!notify(receivers[iterator.next()], event, *objects))
                    iterator.remove()

        } else {
            val msg = mainHandler.obtainMessage()
            msg.arg1 = event
            msg.obj = objects

            mainHandler.sendMessage(msg)
        }
    }

    @MainThread
    private fun notify(wReceiver: Reference<ObjectsReceiver>?, @IdRes event: Int, vararg objects: Any): Boolean {
        val receiver = wReceiver?.get() ?: return false
        receiver.onObjectsReceive(event, *objects)

        return true
    }
}

private class MainHandler : Handler(Looper.getMainLooper()) {

    override fun handleMessage(msg: Message?) {
        if (msg == null) {
            return
        }
        val event = msg.arg1
        @Suppress("UNCHECKED_CAST")
        val objects = msg.obj as Array<Any>

        ReceiverBus.notify(event, *objects)
    }
}
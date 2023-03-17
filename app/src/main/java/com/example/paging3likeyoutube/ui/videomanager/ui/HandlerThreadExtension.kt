package com.example.paging3likeyoutube.ui.videomanager.ui

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import java.lang.Thread.UncaughtExceptionHandler


class HandlerThreadExtension(name: String?, setupExceptionHandler: Boolean) :
    HandlerThread(name) {
    private var mHandler: Handler? = null
    private val mStart = Object()

    /**
     * @param name
     * @param setupExceptionHandler
     */
    init {
        if (setupExceptionHandler) {
            uncaughtExceptionHandler = UncaughtExceptionHandler { thread, ex ->
                if (SHOW_LOGS) Log.v(TAG, "uncaughtException, " + ex.message)
                ex.printStackTrace()
                System.exit(0)
            }
        }
    }

    override fun onLooperPrepared() {
        if (SHOW_LOGS) Log.v(
            TAG,
            "onLooperPrepared $this"
        )
        mHandler = Handler()
        mHandler!!.post { synchronized(mStart) { mStart.notifyAll() } }
    }

    fun post(r: Runnable?) {
        val successfullyAddedToQueue = mHandler!!.post(r!!)
        if (SHOW_LOGS) Log.v(
            TAG,
            "post, successfullyAddedToQueue $successfullyAddedToQueue"
        )
    }

    fun postAtFrontOfQueue(r: Runnable?) {
        mHandler!!.postAtFrontOfQueue(r!!)
    }

    fun startThread() {
        if (SHOW_LOGS) Log.v(TAG, ">> startThread")
        synchronized(mStart) {
            start()
            try {
                mStart.wait()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        if (SHOW_LOGS) Log.v(TAG, "<< startThread")
    }

    fun postQuit() {
        mHandler!!.post {
            if (SHOW_LOGS) Log.v(
                TAG,
                "postQuit, run"
            )
            Looper.myLooper()!!.quit()
        }
    }

    fun remove(runnable: Runnable?) {
        mHandler!!.removeCallbacks(runnable!!)
    }

    companion object {
        private const val SHOW_LOGS = Config.SHOW_LOGS
        private val TAG = HandlerThreadExtension::class.java.simpleName
    }
}


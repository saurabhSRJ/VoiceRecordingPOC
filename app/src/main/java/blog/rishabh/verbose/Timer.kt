package blog.rishabh.verbose

import android.os.Handler
import android.os.Looper

class Timer(listener: OnTimerTickListener, delay: Long) {
    interface OnTimerTickListener {
        fun onTimerTick(duration: Long, noOfTick: Int)
    }

    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private var duration = 0L
    private var delay: Long = 1000L
    private var noOfTick: Int = 0

    init {
        this.delay = delay
        noOfTick = 0
        runnable = Runnable {
            duration += delay
            noOfTick += 1
            handler.postDelayed(runnable, delay)
            listener.onTimerTick(duration, noOfTick)
        }
    }

    fun start() {
        handler.postDelayed(runnable, delay)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
        duration = 0
        noOfTick = 0
    }
}
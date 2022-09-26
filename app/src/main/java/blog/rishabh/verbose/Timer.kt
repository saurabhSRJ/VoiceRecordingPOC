package blog.rishabh.verbose

import android.os.Handler
import android.os.Looper

class Timer(listener: OnTimerTickListener, delay: Long) {
    interface OnTimerTickListener {
        fun onTimerTick(duration: Long)
    }

    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private var duration = 0L
    private var delay: Long = 1000L

    init {
        this.delay = delay
        runnable = Runnable {
            duration += delay
            handler.postDelayed(runnable, delay)
            listener.onTimerTick(duration)
        }
    }

    fun start() {
        handler.postDelayed(runnable, delay)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
        duration = 0
    }
}
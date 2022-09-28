package blog.rishabh.verbose

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import java.io.IOException

private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val MAX_BACKGROUND_NOISE_THRESHOLD = 10000
private const val MIN_BACKGROUND_NOISE_THRESHOLD = 1000
private const val SAMPLING_DELAY = 100L

class AudioRecordTest : AppCompatActivity(), Timer.OnTimerTickListener {

    private var fileName: String = ""

    private var recordButton: RecordButton? = null
    private var recorder: MediaRecorder? = null

    private var playButton: PlayButton? = null
    private var player: MediaPlayer? = null
    private lateinit var countDownTimer: CountDownTimer
    private var medianBackgroundNoiseAmplitude: Int = 0
    private var noiseSampleAmplitudeArray = arrayListOf<Int>()
    private var recordingSampleArray = arrayListOf<Int>()

    private lateinit var timer: Timer

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    } else {
        stopPlaying()
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(fileName)
            setMaxDuration(8000)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRecording()
                    Log.d(LOG_TAG, "Max time reached")
                    showToast("Max time reached")
                }
            }
            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recordButton?.mStartRecording = true
        recordButton?.text = "Start recording"
        timer.stop()
        countDownTimer.cancel()
        recorder = null
    }

    internal inner class RecordButton(ctx: Context) : AppCompatButton(ctx) {
        var mStartRecording = true
        var clicker: OnClickListener = OnClickListener {
            startRecording()
            text = when (mStartRecording) {
                true -> "Stop recording"
                false -> "Start recording"
            }
            if (mStartRecording) {
                showToast("Don't speak, calibrating the mic")
                countDownTimer.start()
            } else {
                countDownTimer.cancel()
            }
            mStartRecording = !mStartRecording
        }

        init {
            text = "Start recording"
            setOnClickListener(clicker)
        }
    }

    internal inner class PlayButton(ctx: Context) : AppCompatButton(ctx) {
        var mStartPlaying = true
        private var clicker: OnClickListener = OnClickListener {
            onPlay(mStartPlaying)
            text = when (mStartPlaying) {
                true -> "Stop playing"
                false -> "Start playing"
            }
            mStartPlaying = !mStartPlaying
        }

        init {
            text = "Start playing"
            setOnClickListener(clicker)
        }
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        // Record to the external cache directory for visibility
        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest.mp3"
        Log.d(LOG_TAG, fileName)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        countDownTimer = object : CountDownTimer(2000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val amplitude: Int = recorder?.maxAmplitude ?: 0
//                medianBackgroundNoiseAmplitude.plus(amplitude)
                noiseSampleAmplitudeArray.add(amplitude)
//                Log.d(LOG_TAG, "$millisUntilFinished Amplitude: $amplitude")
            }

            override fun onFinish() {
                countDownTimer.cancel()
                //discarding first two samples since mic is still getting initialized and the amplitude values are erroneous
//                medianBackgroundNoiseAmplitude = medianBackgroundNoiseAmplitude.div(18)
//                Log.d(LOG_TAG, "Avg Amplitude: Max of 1500 & $medianBackgroundNoiseAmplitude")
//                medianBackgroundNoiseAmplitude = Math.max(medianBackgroundNoiseAmplitude, MIN_BACKGROUND_NOISE_THRESHOLD)
                noiseSampleAmplitudeArray.sort()
                Log.d(LOG_TAG, "Amplitude Array: $noiseSampleAmplitudeArray")
                medianBackgroundNoiseAmplitude = MIN_BACKGROUND_NOISE_THRESHOLD.coerceAtLeast(
                    Utils.median(noiseSampleAmplitudeArray)
                )
                Log.d(LOG_TAG, "Median Amplitude: $medianBackgroundNoiseAmplitude")
                if (medianBackgroundNoiseAmplitude < MAX_BACKGROUND_NOISE_THRESHOLD) {
                    showToast("Start speaking")
                    timer.start()
                } else {
                    showToast("Background noise is too much. Move to a quieter place")
                    stopRecording()
                }
            }

        }
        recordButton = RecordButton(this)
        playButton = PlayButton(this)
        timer = Timer(this, SAMPLING_DELAY)
        val ll = LinearLayout(this).apply {
            addView(recordButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0f))
            addView(playButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0f))
        }
        setContentView(ll)
    }

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
        timer.stop()
        countDownTimer.cancel()
    }

    override fun onTimerTick(duration: Long, noOfTick: Int) {
        val amplitude = recorder?.maxAmplitude
        Log.d(LOG_TAG, "Timer: $duration, Amplitude: $amplitude")
        if (amplitude != null) {
//            Log.d(LOG_TAG, "Amplitude: $maxAmplitudeSinceLastCall, no: $noOfTick")
            recordingSampleArray.add(amplitude)
            if (noOfTick % 15 == 0) {
                recordingSampleArray.sort()
                Log.d(LOG_TAG, "Amplitude Array: $recordingSampleArray")
//                val currentMedianAmplitude = Utils.median(recordingSampleArray)
                val maxAmplitudeSinceLastSamplingThreshold: Int = recordingSampleArray.max() ?: 0
                Log.d(LOG_TAG, "Max Amplitude for last 1.5 sec: $maxAmplitudeSinceLastSamplingThreshold")
                if (maxAmplitudeSinceLastSamplingThreshold <= medianBackgroundNoiseAmplitude) {
                    showToast("Silence Detected")
                    Log.d(LOG_TAG, "Silence Detected")
                    stopRecording()
                }
                recordingSampleArray.clear()
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
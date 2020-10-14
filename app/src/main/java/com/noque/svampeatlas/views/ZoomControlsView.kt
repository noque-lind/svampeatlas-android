package com.noque.svampeatlas.views

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.motion.widget.MotionLayout
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_zoom_controls.view.*
import kotlin.concurrent.timer

class ZoomControlsView(context: Context, attrs: AttributeSet?) : MotionLayout(context, attrs) {

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                seekBar.progress = seekBar.progress - (seekBar.max / 10)
                countDownTimer.start()
                if (root.progress == 0F) expand()
                expand()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                seekBar.progress = seekBar.progress - (seekBar.max / 10)
                countDownTimer.start()
                if (root.progress == 0F) expand()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }


    interface Listener {
        fun zoomLevelSet(zoomRatio: Float)
        fun collapsed()
        fun expanded()
    }

    private val root by lazy { zoomControlsView_root }
    private val seekBar by lazy { zoomControlsView_seekbar }
    private val zoomInButton by lazy  {zoomControlsView_zoomInButton}
    private val zoomOutButton by lazy {zoomControlsView_zoomOutButton}

    private var listener: Listener? = null
    private var countDownTimer = object: CountDownTimer(1500,2000) {
        override fun onTick(p0: Long) { }

        override fun onFinish() {
            collapse()
        }
    }
    private val minZoomRatio: Float = 0f

    private val seekbarChangeLister = object: SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            listener?.zoomLevelSet(getZoomRatio(p1))
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {
            countDownTimer.cancel()
        }
        override fun onStopTrackingTouch(p0: SeekBar?) {
            countDownTimer.start()
        }
    }

    private val zoomOutButtonClickListener = View.OnClickListener {
        seekBar.progress = seekBar.progress - (seekBar.max / 10)
        listener?.zoomLevelSet(getZoomRatio(seekBar.progress))
        countDownTimer.start()
    }

    private val zoomInButtonClickListener = View.OnClickListener {
        if (root.progress == 1F) {
            seekBar.progress =  (seekBar.max / 10) + seekBar.progress
            listener?.zoomLevelSet(getZoomRatio(seekBar.progress))
        } else if (root.progress == 0F) {
            expand()
        }

        countDownTimer.start()
    }

    init {
        LayoutInflater.from(getContext()).inflate(R.layout.view_zoom_controls, this)
        setupView()
    }

    private fun setupView() {
        seekBar.max = 100
        seekBar.setOnSeekBarChangeListener(seekbarChangeLister)
        zoomOutButton.setOnClickListener(zoomOutButtonClickListener)
        zoomInButton.setOnClickListener(zoomInButtonClickListener)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setValue(zoomRatio: Float) {
        seekBar.progress = ((zoomRatio * 100) - (minZoomRatio * 100)).toInt()
    }

    private fun expand() {
        root.transitionToEnd()
    }

    fun collapse() {
        root.transitionToStart()
    }

    private fun getZoomRatio(newValue: Int): Float {
        return newValue.toFloat() / 100
    }

    fun rotate(transform: Float, animationDuration: Long) {
        zoomInButton.animate().rotation(transform).setDuration(animationDuration).start()
        zoomOutButton.animate().rotation(transform).setDuration(animationDuration).start()
    }
}
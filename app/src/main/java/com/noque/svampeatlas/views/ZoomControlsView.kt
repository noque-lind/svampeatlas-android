package com.noque.svampeatlas.views

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.motion.widget.MotionLayout
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_zoom_controls.view.*
import kotlin.concurrent.timer

class ZoomControlsView(context: Context, attrs: AttributeSet?) : MotionLayout(context, attrs) {

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
    private var minZoomRatio: Float = 0f
    private var isExpanded = false

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
        seekBar.setOnSeekBarChangeListener(seekbarChangeLister)
        zoomOutButton.setOnClickListener(zoomOutButtonClickListener)
        zoomInButton.setOnClickListener(zoomInButtonClickListener)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun configure(zoomRatio: Float, maxZoomRatio: Float, minZoomRatio: Float) {
        Log.d("Some", "$maxZoomRatio $minZoomRatio")
        seekBar.max = ((maxZoomRatio * 100) - (minZoomRatio * 100)).toInt()
        this.minZoomRatio = minZoomRatio
        setValue(zoomRatio)
    }

    fun setValue(zoomRatio: Float) {
        seekBar.progress = ((zoomRatio * 100) - (minZoomRatio * 100)).toInt()
    }

    fun expand() {
        root.transitionToEnd()
    }
    fun collapse() {
        root.transitionToStart()

    }

    private fun getZoomRatio(newValue: Int): Float {
        return (newValue / seekBar.max.toFloat()) * (seekBar.max.toFloat() / 100) + minZoomRatio
    }
}
package com.luck.picture.lib.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.luck.picture.lib.R
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.utils.DateUtils

/**
 * @author：luck
 * @date：2023/1/4 4:55 下午
 * @describe：Video Player Controller
 */
open class VideoController : ConstraintLayout, AbsController {
    private lateinit var seekBar: SeekBar
    private lateinit var ivPlay: ImageView
    private lateinit var tvDuration: TextView
    private lateinit var tvCurrentDuration: TextView
    private lateinit var mediaPlayer: IMediaPlayer
    private val mHandler = Handler(Looper.getMainLooper())
    private val mTickerRunnable = object : Runnable {
        override fun run() {
            val duration = mediaPlayer.getDuration()
            val currentPosition = mediaPlayer.getCurrentPosition()
            val time = DateUtils.formatDurationTime(currentPosition)
            if (TextUtils.equals(time, tvCurrentDuration.text)) {
                // Same progress ignored
            } else {
                tvCurrentDuration.text = time
                if (duration - currentPosition > getMinCurrentPosition()) {
                    seekBar.progress = currentPosition.toInt()
                } else {
                    seekBar.progress = duration.toInt()
                }
            }
            mHandler.postDelayed(
                this,
                getMaxUpdateIntervalDuration() - currentPosition % getMaxUpdateIntervalDuration()
            )
        }
    }

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
    }

    private fun initView() {
        View.inflate(context, R.layout.ps_video_controller, this)
        seekBar = findViewById(R.id.seek_bar)
        ivPlay = findViewById(R.id.iv_play_video)
        tvDuration = findViewById(R.id.tv_total_duration)
        tvCurrentDuration = findViewById(R.id.tv_current_time)
        tvDuration.text = String.format("00:00")
        tvCurrentDuration.text = String.format("00:00")
    }

    override fun getViewPlay(): ImageView? {
        return ivPlay
    }

    override fun getSeekBar(): SeekBar? {
        return seekBar
    }

    override fun getFast(): ImageView? {
        return null
    }

    override fun getBack(): ImageView? {
        return null
    }

    override fun getTvDuration(): TextView? {
        return tvDuration
    }

    override fun getTvCurrentDuration(): TextView? {
        return tvCurrentDuration
    }

    override fun setMediaInfo(media: LocalMedia) {
        tvDuration.text = DateUtils.formatDurationTime(media.duration)
        seekBar.max = media.duration.toInt()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    seekBar?.progress = progress
                    tvCurrentDuration.text = DateUtils.formatDurationTime(progress.toLong())
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.seekTo(progress)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        ivPlay.setOnClickListener {
            dispatchPlay(media.getAvailablePath()!!)
        }
    }

    open fun dispatchPlay(path: String) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause()
            stop()
        } else {
            mediaPlayer.resume()
            start()
        }
    }

    override fun start() {
        mHandler.post(mTickerRunnable)
        ivPlay.setImageResource(R.drawable.ps_ic_action_pause)
    }

    override fun stop() {
        mHandler.removeCallbacks(mTickerRunnable)
        tvCurrentDuration.text = String.format("00:00")
        seekBar.progress = 0
        ivPlay.setImageResource(R.drawable.ps_ic_action_play)
    }


    override fun setIMediaPlayer(mediaPlayer: IMediaPlayer) {
        this.mediaPlayer = mediaPlayer
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.removeCallbacks(mTickerRunnable)
    }

    open fun getMaxUpdateIntervalDuration(): Long {
        return 1000L
    }

    open fun getMinCurrentPosition(): Long {
        return 1000L
    }
}
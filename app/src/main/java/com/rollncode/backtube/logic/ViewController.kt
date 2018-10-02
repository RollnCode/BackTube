package com.rollncode.backtube.logic

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.support.v4.content.ContextCompat
import android.text.TextUtils.TruncateAt.END
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayerView
import com.rollncode.backtube.R
import com.rollncode.backtube.api.TubeVideo
import com.rollncode.utility.receiver.ReceiverBus

@SuppressLint("InflateParams")
class ViewController(context: Application, playerController: PlayerController) {

    private val wm by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val view by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_player, null, false)
    }

    private val viewPlayer by lazy {
        view.findViewById<YouTubePlayerView>(R.id.view_player).apply {
            initialize(playerController, true)
            playerUIController.run {
                showFullscreenButton(false)
                showYouTubeButton(false)
                showCustomAction1(false)
                showCustomAction2(false)
                showMenuButton(false)
            }
            val params = layoutParams
            if (params is MarginLayoutParams)
                params.topMargin = context.actionBarHeight + context.statusBarHeight
        }
    }

    private val hideParams by lazy {
        createWindowLayoutParams(1, 1)
    }

    private val showParams by lazy {
        val metrics = context.resources.displayMetrics
        val min = Math.min(metrics.heightPixels, metrics.widthPixels)

        createWindowLayoutParams(min)
    }

    private val listView by lazy {
        view.findViewById<ListView>(R.id.list_view).apply {
            setOnItemClickListener { _, _, position, _ -> ReceiverBus.notify(TubeState.PLAY, position) }
        }
    }

    private val adapter by lazy {
        VideosAdapter().apply { listView.adapter = this }
    }

    fun show() {
        toLog("ViewController.show")
        TubeState.windowShowed = true

        if (view.layoutParams == null)
            wm.addView(view, showParams)
        else
            wm.updateViewLayout(view, showParams)

        viewPlayer
    }

    fun hide() {
        toLog("ViewController.hide")

        TubeState.windowShowed = false

        if (view.layoutParams == null)
            wm.addView(view, hideParams)
        else
            wm.updateViewLayout(view, hideParams)
    }

    fun showList(videos: List<TubeVideo>) {
        adapter.data = videos
    }

    fun highlightCurrent(videoPosition: Int) {
        adapter.currentPosition = videoPosition
        listView.smoothScrollToPosition(videoPosition)
    }

    fun release() = attempt {
        toLog("ViewController.release")
        TubeState.windowShowed = false

        viewPlayer.release()
        wm.removeViewImmediate(view)
    }

    @Suppress("DEPRECATION")
    private fun createWindowLayoutParams(width: Int = WindowManager.LayoutParams.MATCH_PARENT,
                                         height: Int = WindowManager.LayoutParams.MATCH_PARENT
                                        ) = WindowManager.LayoutParams(width, height,
            if (VERSION.SDK_INT >= VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT)
}

private class VideosAdapter : BaseAdapter() {

    var data: List<TubeVideo> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var currentPosition = -1
        set(value) {
            val notifyDataSetChanged = field != value
            field = value

            if (notifyDataSetChanged)
                notifyDataSetChanged()
        }

    private var grayColor: Int = Color.LTGRAY

    override fun getView(position: Int, cV: View?, parent: ViewGroup): View {
        val view: TextView =
                if (cV == null)
                    TextView(parent.context).apply {
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.text_title).toFloat())
                        grayColor = ContextCompat.getColor(context, R.color.gray)

                        val padding = resources.getDimensionPixelSize(R.dimen.spacing)
                        setPadding(padding, padding, padding, padding)
                        compoundDrawablePadding = padding

                        ellipsize = END
                        maxLines = 3
                    }
                else
                    cV as TextView

        view.text = getItem(position).title
        view.setTextColor(if (currentPosition == position) Color.WHITE else grayColor)

        return view
    }

    override fun getCount() = data.size
    override fun getItem(position: Int) = data[position]
    override fun getItemId(position: Int) = position.toLong()
}
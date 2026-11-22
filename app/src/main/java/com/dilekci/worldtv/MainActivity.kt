
package com.dilekci.worldtv

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.dilekci.worldtv.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private var videoScreen: FrameLayout? = null
    private var channelPanel: LinearLayout? = null
    private var channelList: LinearLayout? = null
    private var currentChannel = 0

    private val sections = listOf(
        "Ana Sayfa", "Canlı TV", "Filmler", "Diziler",
        "Belgeseller", "Spor", "Haber", "Ayarlar"
    )

    private data class Channel(
        val name: String,
        val url: String
    )

    private val channels = listOf(
        Channel(
            "NASA TV",
            "https://ntv1.akamaized.net/hls/live/2017906/NASA-NTV1/master.m3u8"
        ),
        Channel(
            "DW English",
            "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sectionContainer.removeAllViews()

        sections.forEachIndexed { index, title ->
            val item = TextView(this).apply {
                text = title
                textSize = 16f
                setTextColor(getColor(android.R.color.white))
                setPadding(22, 14, 22, 14)
                isFocusable = true
                setBackgroundResource(R.drawable.nav_item_background)
                setOnClickListener {
                    selectSection(title)
                }
            }

            binding.sectionContainer.addView(item)

            if (index == 0) {
                item.requestFocus()
            }
        }

        binding.primaryAction.setOnClickListener {
            selectSection("Canlı TV")
        }

        binding.secondaryAction.setOnClickListener {
            selectSection("Filmler")
        }
    }

    private fun selectSection(section: String) {
        closeVideo()
        removeChannelList()

        binding.sectionTitle.text = section
        binding.contentTitle.visibility = View.VISIBLE
        binding.contentDescription.visibility = View.VISIBLE
        binding.primaryAction.visibility = View.VISIBLE
        binding.secondaryAction.visibility = View.VISIBLE

        binding.contentTitle.text = when (section) {
            "Ana Sayfa" -> "Dünyanın ekranı"
            else -> section
        }

        if (section == "Canlı TV") {
            binding.contentDescription.text =
                "İzlemek istediğiniz kanalı seçin."
            showChannels()
        } else {
            binding.contentDescription.text = when (section) {
                "Ayarlar" ->
                    "Xtream Codes ve M3U desteği sonraki aşamada eklenecek."
                else ->
                    "Bu bölüm için içerik kaynağı henüz eklenmedi."
            }
        }
    }

    private fun showChannels() {
        val parent =
            binding.contentDescription.parent as LinearLayout

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 0)
        }

        channels.forEachIndexed { index, channel ->
            val item = TextView(this).apply {
                text = "${index + 1}. ${channel.name}"
                textSize = 20f
                setTextColor(getColor(android.R.color.white))
                setPadding(20, 18, 20, 18)
                isFocusable = true
                setBackgroundResource(R.drawable.nav_item_background)
                setOnClickListener {
                    openVideo(index)
                }
            }
            list.addView(item)
        }

        parent.addView(list)
        channelList = list
        list.getChildAt(0)?.requestFocus()
    }

    private fun removeChannelList() {
        channelList?.let {
            (it.parent as? LinearLayout)?.removeView(it)
        }
        channelList = null
    }

    private fun openVideo(index: Int) {
        closeVideo()
        currentChannel = index

        val root = binding.root.parent as? FrameLayout
        val screen = FrameLayout(this).apply {
            setBackgroundColor(
                getColor(android.R.color.black)
            )
            isFocusable = true
            isFocusableInTouchMode = true
        }

        if (root != null) {
            root.addView(
                screen,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        } else {
            addContentView(
                screen,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        videoScreen = screen

        val videoView = PlayerView(this).apply {
            useController = false
            setBackgroundColor(
                getColor(android.R.color.black)
            )
        }

        screen.addView(
            videoView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        videoView.player = exoPlayer

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(
                error: PlaybackException
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "Yayın açılamadı: ${channels[currentChannel].name}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        playChannel(index)
        screen.requestFocus()
    }

    private fun playChannel(index: Int) {
        currentChannel = index
        val channel = channels[index]

        try {
            player?.apply {
                stop()
                clearMediaItems()
                setMediaItem(MediaItem.fromUri(channel.url))
                prepare()
                playWhenReady = true
            }

            Toast.makeText(
                this,
                "${index + 1}. ${channel.name} açılıyor",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Kanal başlatılamadı",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showSideChannels() {
        val screen = videoScreen ?: return

        if (channelPanel != null) return

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 24, 16, 24)
            setBackgroundColor(0xEE192638.toInt())
            isFocusable = false
        }

        val width = (280 * resources.displayMetrics.density).toInt()

        screen.addView(
            panel,
            FrameLayout.LayoutParams(
                width,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
        )

        channelPanel = panel

        val heading = TextView(this).apply {
            text = "KANALLAR"
            textSize = 22f
            setTextColor(getColor(android.R.color.white))
            setPadding(12, 12, 12, 24)
        }

        panel.addView(heading)

        channels.forEachIndexed { index, channel ->
            val item = TextView(this).apply {
                text = "${index + 1}. ${channel.name}"
                textSize = 19f
                setTextColor(getColor(android.R.color.white))
                setPadding(14, 20, 14, 20)
                isFocusable = true
                setBackgroundResource(R.drawable.nav_item_background)

                setOnClickListener {
                    playChannel(index)
                    hideSideChannels()
                }
            }

            panel.addView(item)
        }

        panel.getChildAt(currentChannel + 1)?.requestFocus()
    }

    private fun hideSideChannels() {
        channelPanel?.let {
            (it.parent as? FrameLayout)?.removeView(it)
        }

        channelPanel = null
        videoScreen?.requestFocus()
    }

    private fun closeVideo() {
        hideSideChannels()

        player?.release()
        player = null

        videoScreen?.let {
            (it.parent as? FrameLayout)?.removeView(it)
        }

        videoScreen = null
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (
            event.action == KeyEvent.ACTION_DOWN &&
            videoScreen != null
        ) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    showSideChannels()
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (channelPanel != null) {
                        hideSideChannels()
                    } else {
                        selectSection("Canlı TV")
                    }
                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        closeVideo()
        super.onDestroy()
    }
}

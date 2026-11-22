
package com.dilekci.worldtv

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private var channelList: View? = null

    private var currentCategory = "Dünya TV"
    private var currentChannel = 0

    private data class Channel(
        val name: String,
        val url: String,
        val category: String
    )

    private val categories = listOf(
        "🇹🇷 Türkiye",
        "🌍 Dünya TV",
        "🎬 Sinema",
        "📺 Diziler",
        "🌿 Belgeseller",
        "⚽ Spor",
        "📰 Haber",
        "⭐ Favoriler",
        "📡 IPTV"
    )

    private val channels = listOf(
        Channel(
            "DW English",
            "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
            "🌍 Dünya TV"
        )
    )

    private fun channelsFor(category: String): List<Channel> {
        return channels.filter {
            it.category == category
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sectionContainer.removeAllViews()

        binding.primaryAction.visibility = View.GONE
        binding.secondaryAction.visibility = View.GONE

        binding.contentTitle.text = "KANALLAR"
        binding.contentDescription.text =
            "Soldan kategori seçin."

        categories.forEach { category ->

            val item = TextView(this).apply {
                text = category
                textSize = 17f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(16, 20, 12, 20)
                isFocusable = true
                setBackgroundResource(
                    R.drawable.nav_item_background
                )

                setOnClickListener {
                    selectCategory(category)
                }
            }

            binding.sectionContainer.addView(item)
        }

        selectCategory("🌍 Dünya TV")

        binding.sectionContainer.getChildAt(1)
            ?.requestFocus()
    }

    private fun selectCategory(category: String) {
        closeVideo()
        removeChannelList()

        currentCategory = category
        binding.sectionTitle.text = category
        binding.contentTitle.text = "KANAL LİSTESİ"

        val available = channelsFor(category)

        if (available.isEmpty()) {
            binding.contentDescription.text =
                when (category) {
                    "📡 IPTV" ->
                        "Xtream Codes, M3U URL ve dosya ekleme sonraki aşamada hazırlanacak."
                    "⭐ Favoriler" ->
                        "Favori kanal kaydetme sonraki aşamada eklenecek."
                    else ->
                        "Bu kategoriye henüz yayın eklenmedi."
                }
            return
        }

        binding.contentDescription.text =
            "${available.size} kanal bulundu."

        showChannels(available)
    }

    private fun showChannels(
        available: List<Channel>
    ) {
        val parent =
            binding.contentDescription.parent as LinearLayout

        val scroll = ScrollView(this).apply {
            isFillViewport = false
        }

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 0)
        }

        available.forEachIndexed { index, channel ->

            val item = TextView(this).apply {
                text = "${index + 1}. ${channel.name}"
                textSize = 21f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(20, 20, 20, 20)
                isFocusable = true
                setBackgroundResource(
                    R.drawable.nav_item_background
                )

                setOnClickListener {
                    openVideo(index)
                }
            }

            list.addView(item)
        }

        scroll.addView(list)
        parent.addView(scroll)
        channelList = scroll
    }

    private fun removeChannelList() {
        channelList?.let {
            (it.parent as? LinearLayout)
                ?.removeView(it)
        }
        channelList = null
    }

    private fun openVideo(index: Int) {
        val available = channelsFor(currentCategory)

        if (index !in available.indices) return

        closeVideo()
        currentChannel = index

        val screen = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            isFocusable = true
            isFocusableInTouchMode = true
        }

        addContentView(
            screen,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        videoScreen = screen

        val videoView = PlayerView(this).apply {
            useController = false
            setBackgroundColor(0xFF000000.toInt())
        }

        screen.addView(
            videoView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val exoPlayer =
            ExoPlayer.Builder(this).build()

        player = exoPlayer
        videoView.player = exoPlayer

        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        "Yayın açılamadı",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        playChannel(index)
        screen.requestFocus()
    }

    private fun playChannel(index: Int) {
        val available = channelsFor(currentCategory)

        if (index !in available.indices) return

        currentChannel = index
        val channel = available[index]

        try {
            player?.apply {
                stop()
                clearMediaItems()
                setMediaItem(
                    MediaItem.fromUri(channel.url)
                )
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

        val available = channelsFor(currentCategory)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 24, 16, 24)
            setBackgroundColor(0xEE192638.toInt())
        }

        val width =
            (280 * resources.displayMetrics.density)
                .toInt()

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
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(12, 12, 12, 24)
        }

        panel.addView(heading)

        available.forEachIndexed { index, channel ->

            val item = TextView(this).apply {
                text = "${index + 1}. ${channel.name}"
                textSize = 19f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(14, 20, 14, 20)
                isFocusable = true
                setBackgroundResource(
                    R.drawable.nav_item_background
                )

                setOnClickListener {
                    playChannel(index)
                    hideSideChannels()
                }
            }

            panel.addView(item)
        }

        panel.getChildAt(currentChannel + 1)
            ?.requestFocus()
    }

    private fun hideSideChannels() {
        channelPanel?.let {
            (it.parent as? FrameLayout)
                ?.removeView(it)
        }

        channelPanel = null
        videoScreen?.requestFocus()
    }

    private fun closeVideo() {
        hideSideChannels()

        player?.release()
        player = null

        videoScreen?.let {
            (it.parent as? FrameLayout)
                ?.removeView(it)
        }

        videoScreen = null
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

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
                        selectCategory(currentCategory)
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

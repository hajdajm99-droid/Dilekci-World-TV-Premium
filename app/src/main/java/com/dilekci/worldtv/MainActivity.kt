
package com.dilekci.worldtv

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.dilekci.worldtv.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var channelList: LinearLayout? = null
    private var isPlaying = false

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
                isFocusableInTouchMode = true
                setBackgroundResource(R.drawable.nav_item_background)
                setOnClickListener { selectSection(title) }
                contentDescription = title
            }

            binding.sectionContainer.addView(item)
            if (index == 0) item.requestFocus()
        }

        binding.primaryAction.setOnClickListener {
            selectSection("Canlı TV")
        }

        binding.secondaryAction.setOnClickListener {
            selectSection("Filmler")
        }
    }

    private fun selectSection(section: String) {
        stopPlayer()
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
        val parent = binding.contentDescription.parent as LinearLayout

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
                    playChannel(channel)
                }
            }

            list.addView(item)
        }

        parent.addView(list)
        channelList = list
    }

    private fun playChannel(channel: Channel) {
        stopPlayer()
        removeChannelList()

        binding.contentTitle.visibility = View.GONE
        binding.contentDescription.visibility = View.GONE
        binding.primaryAction.visibility = View.GONE
        binding.secondaryAction.visibility = View.GONE

        val parent = binding.sectionTitle.parent as LinearLayout

        val videoView = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            useController = true
            isFocusable = true
        }

        parent.addView(videoView)
        playerView = videoView

        try {
            val exoPlayer = ExoPlayer.Builder(this).build()
            player = exoPlayer
            videoView.player = exoPlayer

            exoPlayer.setMediaItem(
                MediaItem.fromUri(channel.url)
            )
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            isPlaying = true

            Toast.makeText(
                this,
                "${channel.name} açılıyor",
                Toast.LENGTH_SHORT
            ).show()

            videoView.requestFocus()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Yayın başlatılamadı",
                Toast.LENGTH_LONG
            ).show()
            stopPlayer()
            selectSection("Canlı TV")
        }
    }

    private fun removeChannelList() {
        channelList?.let { list ->
            (list.parent as? LinearLayout)?.removeView(list)
        }
        channelList = null
    }

    private fun stopPlayer() {
        playerView?.let { view ->
            view.player = null
            (view.parent as? LinearLayout)?.removeView(view)
        }

        player?.release()
        player = null
        playerView = null
        isPlaying = false
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isPlaying) {
                selectSection("Canlı TV")
                return true
            }

            if (binding.sectionTitle.text != "Ana Sayfa") {
                selectSection("Ana Sayfa")
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        stopPlayer()
        super.onDestroy()
    }
}

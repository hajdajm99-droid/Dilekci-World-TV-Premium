
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

    private var currentCategory = "🌍 Dünya TV"
    private var currentCountry = "Almanya"
    private var currentChannel = 0

    private data class Channel(
        val name: String,
        val url: String,
        val country: String,
        val category: String
    )

    private data class Country(
        val name: String,
        val flag: String
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
            "Almanya",
            "🌍 Dünya TV"
        )
    )

    private val countries = listOf(
        Country("Almanya", "🇩🇪")
    )

    private var countryContainer: LinearLayout? = null
    private var countryColumn: LinearLayout? = null
    private var channelColumn: LinearLayout? = null

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun channelsForCountry(
        country: String
    ): List<Channel> {
        return channels.filter {
            it.country == country &&
                it.category == "🌍 Dünya TV"
        }
    }

    private fun visibleCountries(): List<Country> {
        return countries.filter {
            channelsForCountry(it.name).isNotEmpty()
        }.sortedBy { it.name }
    }

    private fun activeChannels(): List<Channel> {
        return if (currentCategory == "🌍 Dünya TV") {
            channelsForCountry(currentCountry)
        } else {
            channels.filter {
                it.category == currentCategory
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sectionContainer.removeAllViews()

        binding.primaryAction.visibility = View.GONE
        binding.secondaryAction.visibility = View.GONE

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

        if (category == "🌍 Dünya TV") {
            showWorldTV()
        } else {
            showOtherCategory(category)
        }
    }

    private fun showWorldTV() {
        binding.contentTitle.text = "DÜNYA TV"
        binding.contentDescription.text =
            "Ülke seçin, kanalları sağda görün."

        val parent =
            binding.contentDescription.parent as LinearLayout

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, 0)
        }

        val countriesList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val channelsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val countriesScroll = ScrollView(this).apply {
            addView(countriesList)
        }

        val channelsScroll = ScrollView(this).apply {
            addView(channelsList)
        }

        container.addView(
            countriesScroll,
            LinearLayout.LayoutParams(
                0,
                dp(360),
                1f
            )
        )

        container.addView(
            channelsScroll,
            LinearLayout.LayoutParams(
                0,
                dp(360),
                1.5f
            )
        )

        parent.addView(container)

        countryContainer = container
        countryColumn = countriesList
        channelColumn = channelsList
        channelList = container

        val availableCountries = visibleCountries()

        availableCountries.forEach { country ->

            val item = TextView(this).apply {
                text = "${country.flag} ${country.name}"
                textSize = 19f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(12, 18, 12, 18)
                isFocusable = true
                setBackgroundResource(
                    R.drawable.nav_item_background
                )

                setOnClickListener {
                    currentCountry = country.name
                    showCountryChannels()
                }
            }

            countriesList.addView(item)
        }

        if (availableCountries.isNotEmpty()) {
            if (availableCountries.none {
                    it.name == currentCountry
                }) {
                currentCountry = availableCountries.first().name
            }

            showCountryChannels()
        } else {
            channelsList.addView(
                makeText("Henüz ülke kanalı eklenmedi.")
            )
        }
    }

    private fun showCountryChannels() {
        val column = channelColumn ?: return
        column.removeAllViews()

        val heading = makeText(
            "$currentCountry KANALLARI"
        )
        column.addView(heading)

        val available =
            channelsForCountry(currentCountry)

        available.forEachIndexed { index, channel ->

            val item = TextView(this).apply {
                text = "${index + 1}. ${channel.name}"
                textSize = 19f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(14, 18, 14, 18)
                isFocusable = true
                setBackgroundResource(
                    R.drawable.nav_item_background
                )

                setOnClickListener {
                    openVideo(index)
                }
            }

            column.addView(item)
        }
    }

    private fun showOtherCategory(category: String) {
        binding.contentTitle.text = category

        val available = activeChannels()

        if (available.isEmpty()) {
            binding.contentDescription.text =
                when (category) {
                    "📡 IPTV" ->
                        "Xtream Codes, M3U URL ve dosya desteği sonraki aşamada eklenecek."
                    "⭐ Favoriler" ->
                        "Favoriler özelliği sonraki aşamada eklenecek."
                    else ->
                        "Bu kategoriye henüz yayın eklenmedi."
                }
            return
        }

        binding.contentDescription.text =
            "${available.size} kanal bulundu."

        val parent =
            binding.contentDescription.parent as LinearLayout

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        available.forEachIndexed { index, channel ->

            val item = TextView(this).apply {
                text = "${index + 1}. ${channel.name}"
                textSize = 19f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(16, 18, 16, 18)
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

        parent.addView(list)
        channelList = list
    }

    private fun makeText(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 19f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(12, 16, 12, 16)
        }
    }

    private fun removeChannelList() {
        channelList?.let {
            (it.parent as? LinearLayout)
                ?.removeView(it)
        }

        channelList = null
        countryContainer = null
        countryColumn = null
        channelColumn = null
    }

    private fun openVideo(index: Int) {
        val available = activeChannels()

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
        val available = activeChannels()

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

    private fun togglePlayback() {
        val exoPlayer = player ?: return

        if (exoPlayer.isPlaying) {
            exoPlayer.pause()

            Toast.makeText(
                this,
                "Yayın duraklatıldı",
                Toast.LENGTH_SHORT
            ).show()

        } else {
            exoPlayer.play()

            Toast.makeText(
                this,
                "Yayın devam ediyor",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showSideChannels() {
        val screen = videoScreen ?: return

        if (channelPanel != null) return

        val available = activeChannels()

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 24, 16, 24)
            setBackgroundColor(0xEE192638.toInt())
        }

        val width = dp(280)

        screen.addView(
            panel,
            FrameLayout.LayoutParams(
                width,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
        )

        channelPanel = panel

        val heading = makeText("KANALLAR")
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

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {
                    if (channelPanel == null) {
                        togglePlayback()
                        return true
                    }
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

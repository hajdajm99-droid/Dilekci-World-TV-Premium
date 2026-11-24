package com.dilekci.worldtv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.dilekci.worldtv.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.Collator
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    private val world = "🌍 Dünya TV"
    private val trial = "🧪 Dünya TV Deneme"
    private val iptv = "📡 IPTV"

    private var player: ExoPlayer? = null
    private var videoScreen: FrameLayout? = null
    private var channelPanel: LinearLayout? = null
    private var channelList: View? = null
    private var channelColumn: LinearLayout? = null

    private var currentCategory = world
    private var currentCountry = "DE"
    private var currentChannel = 0

    private var loading = false
    private var loaded = false
    private var iptvLoading = false
    private var playbackConfirmed = false

    private val iptvFileRequest = 501

    private data class Channel(
        val name: String,
        val url: String,
        val country: String,
        val hls: Boolean = false
    )

    private val categories = listOf(
        "🇹🇷 Türkiye",
        world,
        trial,
        "🎬 Sinema",
        "📺 Diziler",
        "🌿 Belgeseller",
        "⚽ Spor",
        "📰 Haber",
        "⭐ Favoriler",
        iptv
    )

    private val channels = mutableListOf(
        Channel(
            "DW English",
            "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
            "DE"
        ),
        Channel(
            "France 24 English",
            "https://live.france24.com/hls/live/2037218/F24_EN_HI_HLS/master_2300.m3u8",
            "FR"
        ),
        Channel(
            "Sky News",
            "https://linear901-oo-hls0-prd-gtm.delivery.skycdp.com/17501/sde-fast-skynews/master.m3u8",
            "GB"
        ),
        Channel(
            "CBS News 24/7",
            "https://cbsn-us.cbsnstream.cbsnews.com/out/v1/55a8648e8f134e82a470f83d562deeca/master.m3u8",
            "US"
        ),
        Channel(
            "Savoir Média",
            "https://hls.savoir.media/live/stream.m3u8",
            "CA"
        ),
        Channel(
            "ABC News Australia",
            "https://abc-news-dmd-streams-1.akamaized.net/out/v1/abc83881886746b0802dc3e7ca2bc792/index.m3u8",
            "AU"
        ),
        Channel(
            "NHK WORLD-JAPAN",
            "https://media-tyo.hls.nhkworld.jp/hls/w/live/master.m3u8",
            "JP"
        ),
        Channel(
            "Arirang TV",
            "https://amdlive-ch01-ctnd-com.akamaized.net/arirang_1ch/smil:arirang_1ch.smil/playlist.m3u8",
            "KR"
        ),
        Channel(
            "Canal Once",
            "https://vivo.canaloncelive.tv/securepkgr3/oncemexico/playlist.m3u8",
            "MX"
        ),
        Channel(
            "TVR International",
            "https://tvr-international.lg.mncdn.com/tvrinternational/smil:tvrinternational.smil/playlist.m3u8",
            "RO"
        ),
        Channel(
            "SKAI TV",
            "https://skai-live-back.siliconweb.com/media/cambria4/index.m3u8",
            "GR"
        ),
        Channel(
            "oe24 TV",
            "https://oe24.mdn.ors.at/live/eds/oe24tv_hd/hls_nodrm/oe24tv_hd-nodrm.m3u8",
            "AT"
        ),
        Channel(
            "W24",
            "https://ms01.w24.at/W24/smil:liveevent.smil/playlist.m3u8",
            "AT"
        ),
        Channel(
            "Tirol TV",
            "https://streaming14.huberwebmedia.at/LiveApp/streams/livestream.m3u8",
            "AT"
        )
    )

    private val trialChannels = mutableListOf<Channel>()
    private val savedChannels = mutableListOf<Channel>()
    private val iptvChannels = mutableListOf<Channel>()

    private val turkish = Locale("tr", "TR")
    private val countryCodes = Locale.getISOCountries().toSet()

    private val allowedChannels = mapOf(
        "TR" to setOf(
            "TRT Haber", "TRT World",
            "TRT Belgesel", "TRT EBA TV"
        ),
        "DE" to setOf(
            "DW English", "DW Deutsch",
            "DW Español", "tagesschau24"
        ),
        "FR" to setOf(
            "France 24 English",
            "France 24 French",
            "France 24 Arabic",
            "France 24 Spanish",
            "Franceinfo"
        ),
        "GB" to setOf("Sky News", "BBC News"),
        "US" to setOf(
            "CBS News", "CBS News 24/7",
            "ABC News Live", "NBC News NOW",
            "NASA TV", "NASA+", "PBS NewsHour"
        ),
        "CA" to setOf(
            "Savoir Média", "CBC News",
            "CBC News Explore"
        ),
        "AU" to setOf(
            "ABC News", "ABC News Australia"
        ),
        "JP" to setOf(
            "NHK WORLD-JAPAN", "NHK World Japan"
        ),
        "KR" to setOf(
            "Arirang TV", "Arirang World", "KBS World"
        ),
        "AT" to setOf("oe24 TV", "W24", "Tirol TV"),
        "CH" to setOf("SRF info"),
        "IT" to setOf("Rai News 24"),
        "ES" to setOf("Canal 24 Horas", "24h"),
        "PT" to setOf("RTP Notícias", "RTP Internacional"),
        "MX" to setOf("Canal Once", "Canal 22"),
        "RO" to setOf("TVR International", "TVR Info"),
        "GR" to setOf("ERT News", "ERT World"),
        "IN" to setOf("DD News", "DD India"),
        "SG" to setOf("CNA", "Channel NewsAsia"),
        "NZ" to setOf("RNZ"),
        "BR" to setOf("TV Brasil", "TV Cultura"),
        "AR" to setOf("Canal Encuentro"),
        "CL" to setOf("24 Horas"),
        "FI" to setOf("Yle TV1"),
        "NO" to setOf("NRK1"),
        "SE" to setOf("SVT24"),
        "NL" to setOf("NPO Nieuws"),
        "BE" to setOf("VRT NWS"),
        "ZA" to setOf("SABC News"),
        "KE" to setOf("KBC Channel 1"),
        "ID" to setOf("TVRI World"),
        "TH" to setOf("Thai PBS"),
        "MY" to setOf("Bernama TV")
    )

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(
            this, message, Toast.LENGTH_SHORT
        ).show()
    }

    private fun countryName(code: String): String {
        return when (code) {
            "US" -> "ABD"
            "GB" -> "İngiltere"
            "KR" -> "Güney Kore"
            else -> Locale("", code).getDisplayCountry(turkish)
        }
    }

    private fun flag(code: String): String {
        if (code !in countryCodes) return "🌍"

        return code.map {
            String(
                Character.toChars(
                    0x1F1E6 + it.code - 'A'.code
                )
            )
        }.joinToString("")
    }

    private fun sameChannel(a: Channel, b: Channel): Boolean {
        return a.url == b.url ||
            (a.country == b.country &&
                a.name.equals(b.name, ignoreCase = true))
    }

    private fun loadSavedChannels() {
        try {
            val prefs = getSharedPreferences(
                "world_tv_saved", MODE_PRIVATE
            )

            val array = JSONArray(
                prefs.getString("channels", "[]") ?: "[]"
            )

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                val channel = Channel(
                    item.getString("name"),
                    item.getString("url"),
                    item.getString("country")
                )

                if (
                    channel.country in countryCodes &&
                    channel.url.startsWith("https://") &&
                    channels.none { sameChannel(it, channel) }
                ) {
                    channels.add(channel)
                    savedChannels.add(channel)
                }
            }
        } catch (e: Exception) {
            toast("Kayıtlı kanallar okunamadı")
        }
    }

    private fun saveChannel(channel: Channel) {
        if (channels.any { sameChannel(it, channel) }) {
            toast("Bu kanal zaten kayıtlı")
            return
        }

        val array = JSONArray()

        (savedChannels + channel).forEach {
            array.put(
                JSONObject()
                    .put("name", it.name)
                    .put("url", it.url)
                    .put("country", it.country)
            )
        }

        val success = getSharedPreferences(
            "world_tv_saved", MODE_PRIVATE
        ).edit()
            .putString("channels", array.toString())
            .commit()

        if (success) {
            savedChannels.add(channel)
            channels.add(channel)

            toast(
                "${flag(channel.country)} " +
                "${channel.name} Dünya TV'ye eklendi"
            )
        } else {
            toast("Kanal kaydedilemedi")
        }
    }

    private fun confirmSaveChannel() {

        if (currentCategory == iptv) {
            toast("Bu kanal IPTV listesinde kayıtlı")
            return
        }

        if (currentCategory != trial) {
            toast("Bu kanal zaten Dünya TV bölümünde")
            return
        }

        val channel = activeChannels()
            .getOrNull(currentChannel) ?: return

        if (!playbackConfirmed) {
            toast("Önce yayının açılmasını bekleyin")
            return
        }

        if (channels.any { sameChannel(it, channel) }) {
            toast("Bu kanal zaten kayıtlı")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Dünya TV'ye ekle")
            .setMessage(
                "${flag(channel.country)} " +
                "${countryName(channel.country)}\n\n" +
                "${channel.name}\n\n" +
                "Yayının çalıştığını ve kullanımına " +
                "izin verildiğini doğruluyor musunuz?"
            )
            .setPositiveButton("Kaydet") { _, _ ->
                saveChannel(channel)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun findCountry(group: String): String? {

        val aliases = mapOf(
            "United States" to "US",
            "United Kingdom" to "GB",
            "South Korea" to "KR",
            "Turkey" to "TR",
            "Türkiye" to "TR",
            "Czech Republic" to "CZ"
        )

        for (part in group.split(";")) {

            val name = part.trim()

            aliases.entries.firstOrNull {
                it.key.equals(name, ignoreCase = true)
            }?.let {
                return it.value
            }

            val code = countryCodes.firstOrNull {
                it.equals(name, ignoreCase = true) ||
                    Locale("", it)
                        .getDisplayCountry(Locale.ENGLISH)
                        .equals(name, ignoreCase = true) ||
                    countryName(it)
                        .equals(name, ignoreCase = true)
            }

            if (code != null) return code
        }

        return null
    }

    private fun normalizeName(name: String): String {
        return name
            .replace(Regex("""\s*\(\d+p\)\s*"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun parseM3U(text: String): List<Channel> {

        val result = mutableListOf<Channel>()

        var name = ""
        var country: String? = null

        val groupRegex = Regex("group-title=\"([^\"]*)\"")
        val nameRegex = Regex("tvg-name=\"([^\"]*)\"")

        text.lineSequence().forEach { raw ->

            val line = raw.trim()

            if (line.startsWith("#EXTINF:")) {

                val group = groupRegex.find(line)
                    ?.groupValues?.get(1) ?: ""

                country = findCountry(group)

                name = normalizeName(
                    nameRegex.find(line)
                        ?.groupValues?.get(1)
                        ?.takeIf { it.isNotBlank() }
                        ?: line.substringAfterLast(",")
                )

            } else if (
                line.startsWith("https://") ||
                line.startsWith("http://")
            ) {

                val code = country

                val allowed = allowedChannels[code]?.any {
                    it.equals(name, ignoreCase = true)
                } == true

                if (
                    code != null &&
                    allowed &&
                    line.startsWith("https://")
                ) {
                    result.add(Channel(name, line, code))
                }

                name = ""
                country = null
            }
        }

        return result.distinctBy {
            "${it.country}|${it.url}"
        }
    }

    private fun loadTrialChannels() {

        if (loading || loaded) return

        loading = true

        binding.contentDescription.text =
            "Deneme kanalları yükleniyor..."

        Thread {
            try {
                val connection = URL(
                    "https://iptv-org.github.io/iptv/index.country.m3u"
                ).openConnection()

                connection.connectTimeout = 20000
                connection.readTimeout = 30000

                val text = connection.getInputStream()
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val parsed = parseM3U(text)

                runOnUiThread {

                    if (isDestroyed || isFinishing) {
                        return@runOnUiThread
                    }

                    trialChannels.clear()
                    trialChannels.addAll(parsed)

                    loading = false
                    loaded = parsed.isNotEmpty()

                    if (
                        currentCategory == trial &&
                        videoScreen == null
                    ) {
                        removeChannelList()
                        showWorldTV()
                    }

                    if (parsed.isEmpty()) {
                        toast("Uygun deneme kanalı bulunamadı")
                    } else {
                        toast("${parsed.size} deneme kanalı bulundu")
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    if (isDestroyed || isFinishing) {
                        return@runOnUiThread
                    }

                    loading = false

                    if (currentCategory == trial) {
                        binding.contentDescription.text =
                            "Liste yüklenemedi. " +
                            "Deneme bölümünü tekrar seçin."
                    }
                }
            }
        }.start()
    }

    private fun sourceChannels(): List<Channel> {
        return when (currentCategory) {
            world -> channels
            trial -> trialChannels
            iptv -> iptvChannels
            else -> emptyList()
        }
    }

    private fun channelsForCountry(country: String): List<Channel> {
        return sourceChannels().filter {
            it.country == country
        }
    }

    private fun visibleCountries(): List<String> {

        val collator = Collator.getInstance(turkish)

        return sourceChannels()
            .map { it.country }
            .distinct()
            .sortedWith { a, b ->
                collator.compare(
                    countryName(a), countryName(b)
                )
            }
    }

    private fun activeChannels(): List<Channel> {
        return if (currentCategory == iptv) {
            iptvChannels
        } else {
            channelsForCountry(currentCountry)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSavedChannels()
        loadIptvChannels()

        binding.sectionContainer.removeAllViews()

        binding.primaryAction.visibility = View.GONE
        binding.secondaryAction.visibility = View.GONE

        categories.forEach { category ->

            val item = makeItem(category, 17f)

            item.setOnClickListener {
                selectCategory(category)
            }

            binding.sectionContainer.addView(item)
        }

        selectCategory(world)

        binding.sectionContainer
            .getChildAt(1)?.requestFocus()
    }

    private fun selectCategory(category: String) {

        closeVideo()
        removeChannelList()

        currentCategory = category
        binding.sectionTitle.text = category

        when (category) {

            world, trial -> {

                showWorldTV()

                if (category == trial && !loaded) {
                    loadTrialChannels()
                }
            }

            iptv -> showIptv()

            else -> {

                binding.contentTitle.text = category

                binding.contentDescription.text =
                    if (category == "⭐ Favoriler") {
                        "Favoriler sonraki aşamada eklenecek."
                    } else {
                        "Bu kategoriye henüz yayın eklenmedi."
                    }
            }
        }
    }

    private fun showWorldTV() {

        binding.contentTitle.text =
            if (currentCategory == trial)
                "DÜNYA TV DENEME"
            else
                "DÜNYA TV"

        binding.contentDescription.text =
            if (currentCategory == trial) {

                if (loading) {
                    "Kanallar yükleniyor..."
                } else {
                    "${trialChannels.size} deneme kanalı. " +
                    "Kaydetmek için OK tuşuna uzun basın."
                }

            } else {
                "${channels.size} kanal • " +
                "${visibleCountries().size} ülke"
            }

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
            LinearLayout.LayoutParams(0, dp(360), 1f)
        )

        container.addView(
            channelsScroll,
            LinearLayout.LayoutParams(0, dp(360), 1.5f)
        )

        parent.addView(container)

        channelList = container
        channelColumn = channelsList

        val available = visibleCountries()

        available.forEach { code ->

            val item = makeItem(
                "${flag(code)} ${countryName(code)}"
            )

            item.setOnClickListener {
                currentCountry = code
                showCountryChannels()
            }

            countriesList.addView(item)
        }

        if (available.isNotEmpty()) {

            if (currentCountry !in available) {
                currentCountry = available.first()
            }

            showCountryChannels()

        } else {
            channelsList.addView(
                makeText("Henüz kanal bulunamadı.")
            )
        }
    }

    private fun showCountryChannels() {

        val column = channelColumn ?: return

        column.removeAllViews()

        column.addView(
            makeText("${countryName(currentCountry)} KANALLARI")
        )

        channelsForCountry(currentCountry)
            .forEachIndexed { index, channel ->

                val item = makeItem(
                    "${index + 1}. ${channel.name}"
                )

                item.setOnClickListener {
                    openVideo(index)
                }

                column.addView(item)
            }
    }

    private fun makeText(value: String): TextView {

        return TextView(this).apply {
            text = value
            textSize = 19f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(12, 16, 12, 16)
        }
    }

    private fun makeItem(
        value: String,
        size: Float = 19f
    ): TextView {

        return makeText(value).apply {
            textSize = size
            isFocusable = true
            isClickable = true
            setPadding(14, 18, 14, 18)

            setBackgroundResource(
                R.drawable.nav_item_background
            )
        }
    }

    private fun removeChannelList() {

        channelList?.let {
            (it.parent as? LinearLayout)?.removeView(it)
        }

        channelList = null
        channelColumn = null
    }

    private fun openVideo(index: Int) {

        if (index !in activeChannels().indices) return

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

        val exoPlayer = ExoPlayer.Builder(this).build()

        player = exoPlayer
        videoView.player = exoPlayer

        exoPlayer.addListener(
            object : Player.Listener {

                override fun onIsPlayingChanged(
                    isPlaying: Boolean
                ) {
                    if (isPlaying) {
                        playbackConfirmed = true
                    }
                }

                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    playbackConfirmed = false
                    toast("Yayın açılamadı")
                }
            }
        )

        playChannel(index)
        screen.requestFocus()
    }

    private fun playChannel(index: Int) {

        val channel = activeChannels()
            .getOrNull(index) ?: return

        currentChannel = index
        playbackConfirmed = false

        try {

            val media = MediaItem.Builder()
                .setUri(channel.url)

            if (channel.hls) {
                media.setMimeType(MimeTypes.APPLICATION_M3U8)
            }

            player?.apply {

                stop()
                clearMediaItems()
                setMediaItem(media.build())
                prepare()
                playWhenReady = true
            }

            toast("${index + 1}. ${channel.name} açılıyor")

        } catch (e: Exception) {
            toast("Kanal başlatılamadı")
        }
    }

    private fun togglePlayback() {

        val exoPlayer = player ?: return

        if (exoPlayer.playWhenReady) {
            exoPlayer.pause()
            toast("Yayın duraklatıldı")
        } else {
            exoPlayer.play()
            toast("Yayın devam ediyor")
        }
    }

    private fun showSideChannels() {

        val screen = videoScreen ?: return

        if (channelPanel != null) return

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 24, 16, 24)
            setBackgroundColor(0xEE192638.toInt())
        }

        screen.addView(
            panel,
            FrameLayout.LayoutParams(
                dp(300),
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
        )

        channelPanel = panel

        panel.addView(makeText("KANALLAR"))

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            addView(list)
        }

        panel.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        activeChannels().forEachIndexed { index, channel ->

            val item = makeItem(
                "${index + 1}. ${channel.name}"
            )

            item.setOnClickListener {
                playChannel(index)
                hideSideChannels()
            }

            list.addView(item)
        }

        list.getChildAt(currentChannel)?.requestFocus()
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
        playbackConfirmed = false
    }

    // IPTV KAYIT SİSTEMİ

    private fun loadIptvChannels() {

        iptvChannels.clear()

        try {

            val prefs = getSharedPreferences(
                "iptv_saved", MODE_PRIVATE
            )

            val array = JSONArray(
                prefs.getString("channels", "[]") ?: "[]"
            )

            for (i in 0 until array.length()) {

                val item = array.getJSONObject(i)

                iptvChannels.add(
                    Channel(
                        item.getString("name"),
                        item.getString("url"),
                        "IPTV",
                        item.optBoolean("hls", false)
                    )
                )
            }

        } catch (e: Exception) {
            toast("IPTV kayıtları okunamadı")
        }
    }

    private fun saveIptvChannels(items: List<Channel>) {

        if (items.isEmpty()) {
            toast("IPTV kanalı bulunamadı")
            return
        }

        val merged = (iptvChannels + items)
            .distinctBy { it.url }

        val array = JSONArray()

        merged.forEach {

            array.put(
                JSONObject()
                    .put("name", it.name)
                    .put("url", it.url)
                    .put("hls", it.hls)
            )
        }

        val success = getSharedPreferences(
            "iptv_saved", MODE_PRIVATE
        ).edit()
            .putString("channels", array.toString())
            .commit()

        if (!success) {
            toast("IPTV listesi kaydedilemedi")
            return
        }

        iptvChannels.clear()
        iptvChannels.addAll(merged)

        if (
            currentCategory == iptv &&
            videoScreen == null
        ) {
            removeChannelList()
            showIptv()
        }

        toast("${iptvChannels.size} IPTV kanalı kayıtlı")
    }

    private fun parseIptv(
        text: String,
        base: String? = null
    ): List<Channel> {

        val result = mutableListOf<Channel>()

        var name = ""

        if (
            text.contains("#EXT-X-TARGETDURATION") ||
            text.contains("#EXT-X-STREAM-INF")
        ) {

            if (base != null) {
                return listOf(
                    Channel("HLS Yayını", base, "IPTV", true)
                )
            }

            return emptyList()
        }

        text.lineSequence().forEach { raw ->

            val line = raw.trim()
                .removePrefix("\uFEFF")

            if (line.startsWith("#EXTINF:")) {

                name = line.substringAfterLast(",").trim()

            } else if (
                line.isNotBlank() &&
                !line.startsWith("#")
            ) {

                val url = try {

                    if (base != null) {
                        URL(URL(base), line).toString()
                    } else {
                        line
                    }

                } catch (e: Exception) {
                    line
                }

                if (
                    url.startsWith("http://") ||
                    url.startsWith("https://")
                ) {

                    result.add(
                        Channel(
                            name.ifBlank {
                                "Kanal ${result.size + 1}"
                            },
                            url,
                            "IPTV",
                            url.substringBefore("?")
                                .endsWith(".m3u8", true)
                        )
                    )
                }

                name = ""
            }
        }

        return result.distinctBy { it.url }
    }

    private fun importIptvUrl(url: String) {

        if (
            !url.startsWith("http://") &&
            !url.startsWith("https://")
        ) {
            toast("Geçerli bir HTTP veya HTTPS adresi girin")
            return
        }

        if (iptvLoading) {
            toast("Liste zaten yükleniyor")
            return
        }

        iptvLoading = true

        toast("IPTV listesi yükleniyor...")

        Thread {
            try {

                val connection = URL(url).openConnection()

                connection.connectTimeout = 20000
                connection.readTimeout = 30000

                val text = connection.getInputStream()
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val items = parseIptv(
                    text,
                    connection.url.toString()
                )

                runOnUiThread {

                    iptvLoading = false

                    if (!isDestroyed && !isFinishing) {
                        saveIptvChannels(items)
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    iptvLoading = false

                    if (!isDestroyed && !isFinishing) {
                        toast("IPTV bağlantısı kurulamadı")
                    }
                }
            }
        }.start()
    }

    private fun iptvInput(hintText: String): EditText {

        return EditText(this).apply {
            hint = hintText
            setSingleLine(true)
            textSize = 18f
        }
    }

    private fun showM3uDialog() {

        val input = iptvInput("M3U veya M3U8 URL")

        input.inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_VARIATION_URI

        AlertDialog.Builder(this)
            .setTitle("M3U URL Ekle")
            .setView(input)
            .setPositiveButton("Bağlan") { _, _ ->

                importIptvUrl(
                    input.text.toString().trim()
                )
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showXtreamDialog() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }

        val server = iptvInput("Sunucu URL")
        val username = iptvInput("Kullanıcı adı")
        val password = iptvInput("Şifre")

        server.inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_VARIATION_URI

        password.inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        layout.addView(server)
        layout.addView(username)
        layout.addView(password)

        AlertDialog.Builder(this)
            .setTitle("Xtream Codes")
            .setView(layout)
            .setPositiveButton("Bağlan") { _, _ ->

                val host = server.text.toString()
                    .trim()
                    .trimEnd('/')

                val user = username.text.toString().trim()
                val pass = password.text.toString()

                if (
                    host.isBlank() ||
                    user.isBlank() ||
                    pass.isBlank()
                ) {

                    toast("Tüm alanları doldurun")

                } else {

                    val u = URLEncoder.encode(user, "UTF-8")
                    val p = URLEncoder.encode(pass, "UTF-8")

                    val url =
                        "$host/get.php?username=$u" +
                        "&password=$p&type=m3u_plus" +
                        "&output=m3u8"

                    importIptvUrl(url)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun selectIptvFile() {

        try {

            val intent = Intent(
                Intent.ACTION_OPEN_DOCUMENT
            ).apply {

                addCategory(Intent.CATEGORY_OPENABLE)

                type = "*/*"
            }

            startActivityForResult(
                intent, iptvFileRequest
            )

        } catch (e: Exception) {
            toast("Cihazda dosya seçici bulunamadı")
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode, resultCode, data
        )

        if (
            requestCode != iptvFileRequest ||
            resultCode != RESULT_OK
        ) return

        val uri = data?.data ?: return

        if (iptvLoading) {
            toast("Liste zaten yükleniyor")
            return
        }

        iptvLoading = true

        Thread {

            try {

                val text = contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: throw Exception()

                val items = parseIptv(text)

                runOnUiThread {

                    iptvLoading = false

                    if (!isDestroyed && !isFinishing) {
                        saveIptvChannels(items)
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    iptvLoading = false

                    if (!isDestroyed && !isFinishing) {
                        toast("IPTV dosyası okunamadı")
                    }
                }
            }
        }.start()
    }

    private fun showIptv() {

        binding.contentTitle.text = "IPTV"

        binding.contentDescription.text =
            "${iptvChannels.size} kayıtlı IPTV kanalı"

        val parent =
            binding.contentDescription.parent as LinearLayout

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, 0)
        }

        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val menuScroll = ScrollView(this).apply {
            addView(menu)
        }

        val listScroll = ScrollView(this).apply {
            addView(list)
        }

        container.addView(
            menuScroll,
            LinearLayout.LayoutParams(0, dp(360), 1f)
        )

        container.addView(
            listScroll,
            LinearLayout.LayoutParams(0, dp(360), 1.5f)
        )

        val xtream = makeItem("🔑 Xtream Codes")

        xtream.setOnClickListener {
            showXtreamDialog()
        }

        val m3u = makeItem("🔗 M3U URL")

        m3u.setOnClickListener {
            showM3uDialog()
        }

        val file = makeItem("📂 M3U/M3U8 Dosyası")

        file.setOnClickListener {
            selectIptvFile()
        }

        menu.addView(xtream)
        menu.addView(m3u)
        menu.addView(file)

        val clear = makeItem("🗑 IPTV Listesini Temizle")

        clear.setOnClickListener {

            if (iptvLoading) {
                toast("Önce yüklemenin tamamlanmasını bekleyin")
            } else {

                AlertDialog.Builder(this)
                    .setTitle("IPTV listesini sil")
                    .setMessage(
                        "Dünya TV kanalları silinmeyecek."
                    )
                    .setPositiveButton("Sil") { _, _ ->

                        val success = getSharedPreferences(
                            "iptv_saved", MODE_PRIVATE
                        ).edit()
                            .remove("channels")
                            .commit()

                        if (success) {

                            iptvChannels.clear()

                            removeChannelList()
                            showIptv()

                            toast("IPTV listesi temizlendi")

                        } else {
                            toast("Liste silinemedi")
                        }
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            }
        }

        menu.addView(clear)

        list.addView(
            makeText("IPTV KANALLARI")
        )

        iptvChannels.forEachIndexed { index, channel ->

            val item = makeItem(
                "${index + 1}. ${channel.name}"
            )

            item.setOnClickListener {
                openVideo(index)
            }

            list.addView(item)
        }

        if (iptvChannels.isEmpty()) {

            list.addView(
                makeText("Henüz IPTV kanalı eklenmedi.")
            )
        }

        parent.addView(container)

        channelList = container
        channelColumn = list
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        if (videoScreen != null) {

            val isOk =
                event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                event.keyCode == KeyEvent.KEYCODE_ENTER

            if (isOk && channelPanel == null) {

                if (event.action == KeyEvent.ACTION_DOWN) {
                    return true
                }

                if (event.action == KeyEvent.ACTION_UP) {

                    if (!event.isCanceled) {

                        val duration =
                            event.eventTime - event.downTime

                        if (duration >= 700) {
                            confirmSaveChannel()
                        } else {
                            togglePlayback()
                        }
                    }

                    return true
                }
            }

            if (event.action == KeyEvent.ACTION_DOWN) {

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
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {

        closeVideo()

        super.onDestroy()
    }
}
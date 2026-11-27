package com.dilekci.worldtv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.dilekci.worldtv.databinding.ActivityMainBinding
import org.json.*
import java.net.URL
import java.text.Collator
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private val world = "🌍 Dünya TV"
    private val trial = "🧪 Dünya TV Deneme"
    private val iptv = "📡 IPTV"
    private val settings = "⚙️ Ayarlar"
    private var group = "Tüm Kanallar"
    private var replacement: String? = null
    private val categories = listOf("🇹🇷 Türkiye", world, "🎬 Sinema", "📺 Diziler", "🌿 Belgeseller", "⚽ Spor", "📰 Haber", "⭐ Favoriler", iptv, settings)
    private data class Channel(val name:String,val url:String,val country:String,val hls:Boolean=false,val group:String="Genel")
    private enum class Source(val key:String,val title:String) {
        XTREAM("xtream_channels","Xtream Codes"), URL("url_channels","M3U URL"), FILE("file_channels","M3U Dosyası"), LEGACY("channels","Eski IPTV Kayıtları")
    }
    private val lists = Source.values().associateWith { mutableListOf<Channel>() }
    private data class FileList(val id:String,val name:String,val channels:List<Channel>)
    private val files = mutableListOf<FileList>()
    private data class Connection(val id:String,val name:String,val source:Source,val channels:List<Channel>)
    private val connections=mutableListOf<Connection>()
    private var selectedConnection:String?=null
    private var connectionsReadable=true
    private var selectedFile:String? = null
    private var filesReadable = true
    private var source = Source.FILE
    private var category = world
    private var country = "DE"
    private var current = 0
    private var player:ExoPlayer? = null
    private var screen:FrameLayout? = null
    private var panel:LinearLayout? = null
    private var content:LinearLayout? = null
    private var right:LinearLayout? = null
    private var playing = emptyList<Channel>()
    private var confirmed = false
    private var busy = false
    private var trialBusy = false
    private var trialRefreshed = false
    private val retryHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var retryCount = 0
    private val tr = Locale("tr","TR")
    private val codes = Locale.getISOCountries().toSet()
    private val saved = mutableListOf<Channel>()
    private val trials = mutableListOf<Channel>()
    private val channels = mutableListOf(
        Channel("DW English","https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8","DE"),
        Channel("France 24 English","https://live.france24.com/hls/live/2037218/F24_EN_HI_HLS/master_2300.m3u8","FR"),
        Channel("Sky News","https://linear901-oo-hls0-prd-gtm.delivery.skycdp.com/17501/sde-fast-skynews/master.m3u8","GB"),
        Channel("CBS News 24/7","https://cbsn-us.cbsnstream.cbsnews.com/out/v1/55a8648e8f134e82a470f83d562deeca/master.m3u8","US"),
        Channel("Savoir Média","https://hls.savoir.media/live/stream.m3u8","CA"),
        Channel("ABC News Australia","https://abc-news-dmd-streams-1.akamaized.net/out/v1/abc83881886746b0802dc3e7ca2bc792/index.m3u8","AU"),
        Channel("NHK WORLD-JAPAN","https://media-tyo.hls.nhkworld.jp/hls/w/live/master.m3u8","JP"),
        Channel("Arirang TV","https://amdlive-ch01-ctnd-com.akamaized.net/arirang_1ch/smil:arirang_1ch.smil/playlist.m3u8","KR"),
        Channel("Canal Once","https://vivo.canaloncelive.tv/securepkgr3/oncemexico/playlist.m3u8","MX"),
        Channel("TVR International","https://tvr-international.lg.mncdn.com/tvrinternational/smil:tvrinternational.smil/playlist.m3u8","RO"),
        Channel("SKAI TV","https://skai-live-back.siliconweb.com/media/cambria4/index.m3u8","GR"),
        Channel("oe24 TV","https://oe24.mdn.ors.at/live/eds/oe24tv_hd/hls_nodrm/oe24tv_hd-nodrm.m3u8","AT"),
        Channel("W24","https://ms01.w24.at/W24/smil:liveevent.smil/playlist.m3u8","AT"),
        Channel("Tirol TV","https://streaming14.huberwebmedia.at/LiveApp/streams/livestream.m3u8","AT")
    )
    private val iptvTrial = "🧪 IPTV Deneme"
    private var importedWorld: List<Channel> = emptyList()
    private var importedIptv: List<Channel> = emptyList()
    private var trialQuery = ""
    private var trialCountry = "ALL"
    private fun worldTrials(): List<Channel> = dbList("world", includeCache=true)
    private fun searchTrial() {
        val e=input("Kanal adı"); e.setText(trialQuery)
        AlertDialog.Builder(this).setTitle("Kanal ara").setView(e).setPositiveButton("Ara") { _,_ -> trialQuery=e.text.toString().trim(); renderRight() }.setNegativeButton("İptal",null).show()
    }
    private fun clearTrial(isWorld:Boolean) {
        if(busy) { toast("Liste yükleniyor"); return }
        val key=if(isWorld) "world" else "iptv"
        AlertDialog.Builder(this).setMessage("Yüklenen deneme kanalları silinsin mi? Kalıcı kanallar korunur.").setPositiveButton("Sil") { _,_ ->
            if(clearDb(key)) { refreshDb(); render() }
        }.setNegativeButton("İptal",null).show()
    }
    private val allowed = mapOf(
        "TR" to "TRT Haber|TRT World|TRT Belgesel|TRT EBA TV", "DE" to "DW English|DW Deutsch|DW Español|tagesschau24", "FR" to "France 24 English|France 24 French|France 24 Arabic|France 24 Spanish|Franceinfo", "GB" to "Sky News|BBC News", "US" to "CBS News|CBS News 24/7|ABC News Live|NBC News NOW|NASA TV|NASA+|PBS NewsHour", "CA" to "Savoir Média|CBC News|CBC News Explore", "AU" to "ABC News|ABC News Australia", "JP" to "NHK WORLD-JAPAN|NHK World Japan", "KR" to "Arirang TV|Arirang World|KBS World", "AT" to "oe24 TV|W24|Tirol TV", "CH" to "SRF info", "IT" to "Rai News 24", "ES" to "Canal 24 Horas|24h", "PT" to "RTP Notícias|RTP Internacional", "MX" to "Canal Once|Canal 22", "RO" to "TVR International|TVR Info", "GR" to "ERT News|ERT World", "IN" to "DD News|DD India", "SG" to "CNA|Channel NewsAsia", "NZ" to "RNZ", "BR" to "TV Brasil|TV Cultura", "AR" to "Canal Encuentro", "CL" to "24 Horas", "FI" to "Yle TV1", "NO" to "NRK1", "SE" to "SVT24", "NL" to "NPO Nieuws", "BE" to "VRT NWS", "ZA" to "SABC News", "KE" to "KBC Channel 1", "ID" to "TVRI World", "TH" to "Thai PBS", "MY" to "Bernama TV"
    )
    private fun dp(n:Int) = (n*resources.displayMetrics.density).toInt()
    private fun toast(s:String) = Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun countryName(c:String) = when(c) { "ZZ"->"Ülkesi Belirtilmemiş"; "US"->"ABD"; "GB"->"İngiltere"; "KR"->"Güney Kore"; else->Locale("",c).getDisplayCountry(tr) }
    private fun flag(c:String) = if(c !in codes) "🌍" else c.map { String(Character.toChars(0x1F1E6+it.code-'A'.code)) }.joinToString("")
    private fun same(a:Channel,b:Channel) = a.url==b.url || (a.country==b.country && a.name.equals(b.name,true))
    private fun read(pref:String,key:String,defaultCountry:String):List<Channel> {
        val result=mutableListOf<Channel>()
        try {
            val a=JSONArray(getSharedPreferences(pref,MODE_PRIVATE).getString(key,"[]") ?: "[]")
            for(i in 0 until a.length()) { val o=a.getJSONObject(i); result.add(Channel(o.getString("name"),o.getString("url"),o.optString("country",defaultCountry),o.optBoolean("hls",false),o.optString("group","Genel"))) }
        } catch(e:Exception) { toast("Kayıt okunamadı") }
        return result
    }
    private fun write(pref:String,key:String,items:List<Channel>):Boolean {
        val a=JSONArray(); items.forEach { a.put(JSONObject().put("name",it.name).put("url",it.url).put("country",it.country).put("hls",it.hls).put("group",it.group)) }
        return getSharedPreferences(pref,MODE_PRIVATE).edit().putString(key,a.toString()).commit()
    }
    override fun onCreate(state:Bundle?) {
        super.onCreate(state); binding=ActivityMainBinding.inflate(layoutInflater); setContentView(binding.root)
        read("world_tv_saved","channels","").filter { it.country in codes && it.url.startsWith("https://") }.forEach { c -> if(channels.none { same(it,c) }) { channels.add(c); saved.add(c) } }
        Source.values().filter { it!=Source.LEGACY }.forEach { lists.getValue(it).addAll(read("iptv_saved",it.key,"IPTV")) }
        loadConnections()
        migrateDb("world", "ZZ")
        trials.addAll(read("world_trial_cache","channels","ZZ")); syncCache()
        migrateDb("iptv", "IPTV"); refreshDb()
        loadFiles()
        source=connections.firstOrNull()?.source ?: Source.FILE
        binding.sectionContainer.removeAllViews(); binding.primaryAction.visibility=View.GONE; binding.secondaryAction.visibility=View.GONE
        categories.forEach { c -> binding.sectionContainer.addView(item(c,17f) { select(c) }) }
        select(world); binding.sectionContainer.getChildAt(1)?.requestFocus()
    }
    private fun text(s:String)=TextView(this).apply { text=s; textSize=19f; setTextColor(-1); setPadding(dp(8),dp(8),dp(8),dp(8)) }
    private fun item(s:String,size:Float=19f,action:()->Unit)=text(s).apply { textSize=size; isFocusable=true; isClickable=true; setBackgroundResource(R.drawable.nav_item_background); setOnClickListener { action() } }
    private fun select(c:String) { closeVideo(); category=c; trialQuery=""; if(c==trial) trialCountry="ALL"; render(); if(c==trial && !trialRefreshed) loadTrials() }
    private fun render() {
        content?.let { (it.parent as? LinearLayout)?.removeView(it) }; content=null; right=null
        binding.sectionTitle.text=category; binding.contentTitle.text=category
        if(category!=world && category!=trial && category!=iptv && category!=iptvTrial && category!=settings) { binding.contentDescription.text="Bu kategoriye henüz yayın eklenmedi."; return }
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,dp(16),0,0) }
        (binding.contentDescription.parent as LinearLayout).addView(row,LinearLayout.LayoutParams(-1,0,1f)); content=row
        if(category==settings) { renderSettings(row); return }
        val left=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; val r=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        row.addView(ScrollView(this).apply { addView(left) },LinearLayout.LayoutParams(0,-1,1f))
        row.addView(r,LinearLayout.LayoutParams(0,-1,1.5f)); right=r
        if(category==iptvTrial) {
            binding.contentDescription.text="${importedIptv.size} kanal • Kaydetmek için OK uzun basın"
            left.addView(item("M3U URL ile Deneme",16f) { m3uTrial() })
            left.addView(item("📂 Dosya yükle",16f) { pickFile(503) })
            left.addView(item("🔍 Kanal ara",16f) { searchTrial() })
            left.addView(item("Deneme listesini temizle",16f) { clearTrial(false) })
            left.addView(item("⚙️ Ayarlar",16f) { select(settings) })
        } else if(category==iptv) {
            val f=files.firstOrNull { it.id==selectedFile }
            binding.contentTitle.text=if(source==Source.FILE) f?.name ?: "IPTV" else connections.firstOrNull { it.id==selectedConnection }?.name ?: source.title
            val all=allIptv(); binding.contentDescription.text="${all.size} kanal"
            left.addView(text("KATEGORİLER"))
            val groups=listOf("Tüm Kanallar")+all.map { it.group }.distinct().filter { it!="Tüm Kanallar" }.sorted()
            if(group !in groups) group="Tüm Kanallar"
            left.addView(item("⚙️ Liste seç",16f) { select(settings) })
            groups.forEach { g -> left.addView(item(g,16f) { group=g; renderRight() }) }
        } else {
            val all=if(category==trial) worldTrials() else channels; val collator=Collator.getInstance(tr)
            val countries=(if(category==trial) dbCountries() else all.map { it.country }.distinct()).sortedWith { a,b -> collator.compare(countryName(a),countryName(b)) }
            binding.contentDescription.text=if(trialBusy && category==trial) "${all.size} kanal • Liste güncelleniyor..." else "${all.size} kanal • ${countries.size} ülke" + if(category==trial) " • Kaydetmek için OK uzun basın" else ""
            if(category==trial) {
                left.addView(item("📂 Dosya yükle",16f) { pickFile(502) })
                left.addView(item("⚙️ Ayarlar",16f) { select(settings) })
                left.addView(item("Tüm Kanallar",16f) { trialCountry="ALL"; trialQuery=""; renderRight() })
                left.addView(item("🔍 Kanal ara",16f) { searchTrial() })
                left.addView(item("Deneme listesini temizle",16f) { clearTrial(true) })
            }
            if(category==trial) {
                if(trialCountry !in countries) trialCountry="ALL"
                countries.forEach { c -> left.addView(item("${flag(c)} ${countryName(c)}") { trialCountry=c; trialQuery=""; renderRight() }) }
            } else {
                if(country !in countries) country=countries.firstOrNull() ?: "DE"
                countries.forEach { c -> left.addView(item("${flag(c)} ${countryName(c)}") { country=c; renderRight() }) }
            }
        }
        renderRight()
    }
    private fun allIptv():List<Channel> = if(source==Source.FILE) files.firstOrNull { it.id==selectedFile }?.channels ?: emptyList() else connections.firstOrNull { it.id==selectedConnection && it.source==source }?.channels ?: emptyList()
    private fun active():List<Channel> = when(category) {
        iptvTrial -> dbList("iptv", query=trialQuery)
        trial -> dbList("world", query=trialQuery, countryFilter=if(trialQuery.isBlank() && trialCountry!="ALL") trialCountry else null, includeCache=true)
        iptv -> allIptv().let { if(group=="Tüm Kanallar") it else it.filter { c->c.group==group } }
        world -> channels.filter { it.country==country }
        else -> emptyList()
    }
    private fun channelList(a:List<Channel>, action:(Int)->Unit)=ListView(this).apply {
        adapter=object:BaseAdapter() {
            override fun getCount()=a.size
            override fun getItem(position:Int)=a[position]
            override fun getItemId(position:Int)=position.toLong()
            override fun getView(position:Int,convertView:View?,parent:ViewGroup?):View {
                val v=convertView as? TextView ?: this@MainActivity.text("")
                val c=a[position]
                v.text="${position+1}. ${c.name}"; return v
            }
        }
        setOnItemClickListener { _,_,i,_->action(i) }
    }
    private fun renderRight() {
        val r=right ?: return; r.removeAllViews(); r.addView(text(if(category==iptvTrial || (category==trial && trialQuery.isNotBlank())) "DENEME KANALLARI" else if(category==iptv) "KANAL LİSTESİ" else if(category==trial) { if(trialCountry=="ALL") "Tüm Kanallar" else countryName(trialCountry) } else countryName(country)))
        val a=active(); if(a.isEmpty()) r.addView(text("Henüz kanal bulunamadı."))
        else r.addView(channelList(a) { openVideo(it,a) },LinearLayout.LayoutParams(-1,0,1f))
    }
    private fun renderSettings(row:LinearLayout) {
        binding.contentTitle.text="IPTV BAĞLANTI AYARLARI"; binding.contentDescription.text="Yüklenen listeleri yönet"
        val l=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; val r=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        row.addView(ScrollView(this).apply { addView(l) },LinearLayout.LayoutParams(0,-1,1f)); row.addView(ScrollView(this).apply { addView(r) },LinearLayout.LayoutParams(0,-1,1.5f))
        l.addView(item("Dünya TV Deneme",16f) { select(trial) })
        l.addView(item("IPTV Deneme",16f) { select(iptvTrial) })
        r.addView(text("YÜKLENEN LİSTELER"))
        connections.forEach { c -> r.addView(item("${c.source.title}: ${c.name}",16f) {
            AlertDialog.Builder(this).setTitle(c.name).setItems(arrayOf("Aç","Sil")) { _,i -> when(i) {
                0 -> { source=c.source; selectedConnection=c.id; group="Tüm Kanallar"; select(iptv) }
                1 -> AlertDialog.Builder(this).setMessage("Bu bağlantı silinsin mi?").setPositiveButton("Sil") { _,_ -> persistConnections(connections.filter { it.id!=c.id }) }.setNegativeButton("İptal",null).show()
            } }.show()
        }) }
        files.forEach { f -> r.addView(item(f.name,16f) {
            AlertDialog.Builder(this).setTitle(f.name).setItems(arrayOf("Aç","Sil")) { _,i -> when(i) {
                0->{ source=Source.FILE; selectedFile=f.id; group="Tüm Kanallar"; select(iptv) }
                1->{ AlertDialog.Builder(this).setMessage("Liste silinsin mi? Orijinal dosya korunur.").setPositiveButton("Sil") { _,_-> if(!busy) persistFiles(files.filter { it.id!=f.id }) }.setNegativeButton("İptal",null).show() }
            } }.show()
        }) }
    }
    private fun persistFiles(items:List<FileList>) {
        if(busy) return; busy=true
        Thread { val ok=try { storeFiles(items) } catch(e:Exception) { false }; runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) { if(ok) { files.clear(); files.addAll(items); if(screen==null) render() } else toast("Kayıt başarısız") } } }.start()
    }
    private fun openVideo(i:Int,displayed:List<Channel>) {
        val a=displayed; if(i !in a.indices) return; closeVideo(); playing=a
        val f=FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()); isFocusable=true; isFocusableInTouchMode=true }; screen=f
        addContentView(f,FrameLayout.LayoutParams(-1,-1)); val pv=PlayerView(this).apply { useController=false }; f.addView(pv,FrameLayout.LayoutParams(-1,-1))
        val loadControl=androidx.media3.exoplayer.DefaultLoadControl.Builder().setBufferDurationsMs(20000,50000,2000,5000).setTargetBufferBytes(24*1024*1024).setPrioritizeTimeOverSizeThresholds(false).build()
        val p=ExoPlayer.Builder(this).setLoadControl(loadControl).build(); player=p; pv.player=p
        p.addListener(object:Player.Listener { override fun onIsPlayingChanged(isPlaying:Boolean) { if(isPlaying) confirmed=true }; override fun onPlayerError(error:PlaybackException) { confirmed=false
            if(error.errorCode in 2000..2002 && retryCount<2 && p.playWhenReady) {
                retryCount++
                retryHandler.postDelayed({ if(player===p && p.playWhenReady) p.prepare() }, 2500L*retryCount)
            } else toast("Yayın açılamadı")
        } })
        play(i); f.requestFocus()
    }
    private fun play(i:Int) {
        val c=playing.getOrNull(i) ?: return; current=i; confirmed=false; retryCount=0; retryHandler.removeCallbacksAndMessages(null)
        val m=MediaItem.Builder().setUri(c.url); if(c.hls) m.setMimeType(MimeTypes.APPLICATION_M3U8)
        try { player?.apply { stop(); clearMediaItems(); setMediaItem(m.build()); prepare(); playWhenReady=true } } catch(e:Exception) { toast("Yayın açılamadı") }
    }
    private fun side() {
        val f=screen ?: return; if(panel!=null) return
        val p=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xEE192638.toInt()); setPadding(dp(8),dp(12),dp(8),dp(12)) }; panel=p
        f.addView(p,FrameLayout.LayoutParams(dp(300),-1,Gravity.START)); p.addView(text(if(category==iptv && source==Source.FILE) files.firstOrNull { it.id==selectedFile }?.name ?: "KANALLAR" else if(category==iptv) connections.firstOrNull { it.id==selectedConnection }?.name ?: source.title else "KANALLAR"))
        val lv=channelList(playing) { i->play(i); hideSide() }; p.addView(lv,LinearLayout.LayoutParams(-1,0,1f)); lv.requestFocus(); lv.setSelection(current)
    }
    private fun hideSide() { panel?.let { (it.parent as? FrameLayout)?.removeView(it) }; panel=null; screen?.requestFocus() }
    private fun closeVideo() { retryHandler.removeCallbacksAndMessages(null); hideSide(); player?.release(); player=null; screen?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }; screen=null; playing=emptyList(); confirmed=false }
    private fun saveTrial() {
        if(category!=trial && category!=iptvTrial) { toast("Kanal zaten listede"); return }
        val c=playing.getOrNull(current) ?: return
        if(!confirmed) { toast("Önce yayının açılmasını bekleyin"); return }
        val isWorld=category==trial
        if(isWorld) {
            if(!c.url.startsWith("https://")) { toast("Dünya TV için HTTPS gerekli"); return }
            chooseWorldCountry(c); return
        }
        if(files.any { f -> f.channels.any { it.url==c.url } } || connections.any { f -> f.channels.any { it.url==c.url } }) { toast("Kanal zaten kayıtlı"); return }
        AlertDialog.Builder(this).setTitle(if(isWorld) "Dünya TV'ye ekle" else "IPTV'ye ekle").setMessage("${c.name}: Yayının çalıştığını ve kullanımına izin verildiğini doğruluyor musunuz?").setPositiveButton("Kaydet") { _,_ ->
            run {
                val id="tested-iptv-v1"
                val old=files.firstOrNull { it.id==id }
                persistFiles(files.filter { it.id!=id }+FileList(id,"Test Edilen IPTV Kanalları",(old?.channels ?: emptyList())+c))
            }
        }.setNegativeButton("İptal",null).show()
    }
    private fun chooseWorldCountry(c:Channel) {
        if(channels.any { it.url==c.url }) { toast("Kanal zaten kayıtlı"); return }
        val collator=Collator.getInstance(tr)
        val options=codes.sortedWith { a,b -> collator.compare(countryName(a),countryName(b)) }
        AlertDialog.Builder(this).setTitle("${c.name}: Ülke seçin")
            .setItems(options.map { "${flag(it)} ${countryName(it)}" }.toTypedArray()) { _,i ->
                val chosen=c.copy(country=options[i])
                if(channels.any { same(it,chosen) }) toast("Kanal zaten kayıtlı")
                else AlertDialog.Builder(this).setTitle("Dünya TV'ye ekle")
                    .setMessage("${chosen.name} → ${countryName(chosen.country)}. Yayının çalıştığını ve kullanımına izin verildiğini doğruluyor musunuz?")
                    .setPositiveButton("Kaydet") { _,_ ->
                        if(channels.any { same(it,chosen) }) toast("Kanal zaten kayıtlı")
                        else if(write("world_tv_saved","channels",saved+chosen)) { saved.add(chosen); channels.add(chosen); toast("Kanal kaydedildi") }
                        else toast("Kayıt başarısız")
                    }.setNegativeButton("İptal",null).show()
            }.setNegativeButton("İptal",null).show()
    }
    private class ListLimit: java.io.IOException()
    private fun parse(reader:java.io.BufferedReader,base:String?=null,requireHeader:Boolean=false):List<Channel> {
        val result=ArrayList<Channel>(); val seen=HashSet<String>()
        var name=""; var g="Genel"; var first=true; var total=0L
        val gr=Regex("group-title=\"([^\"]*)\"",RegexOption.IGNORE_CASE)
        val root=base?.let { URL(it) }
        while(true) {
            val raw=reader.readLine() ?: break
            total+=raw.length.toLong()+1
            if(total>64L*1024*1024) throw ListLimit()
            val line=raw.trim().removePrefix("\uFEFF").trim()
            if(line.isEmpty()) continue
            if(first) { first=false; if(requireHeader && !line.startsWith("#EXTM3U",true)) return emptyList() }
            if(line.startsWith("#EXT-X-TARGETDURATION") || line.startsWith("#EXT-X-STREAM-INF")) return if(base!=null) listOf(Channel("HLS Yayını",base,"IPTV",true)) else emptyList()
            if(line.startsWith("#EXTINF:")) {
                name=line.substringAfterLast(',').trim(); g=gr.find(line)?.groupValues?.get(1)?.ifBlank { "Genel" } ?: "Genel"
            } else if(!line.startsWith("#")) {
                val u=try { if(root!=null) URL(root,line).toString() else line } catch(e:Exception) { line }
                if((u.startsWith("https://") || u.startsWith("http://")) && seen.add(u)) {
                    if(result.size>=50000) throw ListLimit()
                    result.add(Channel(name.ifBlank { "Kanal ${result.size+1}" },u,"IPTV",u.substringBefore('?').endsWith(".m3u8",true),g))
                }
                name=""; g="Genel"
            }
        }
        return result
    }
    private fun mergeChannels(previous:List<Channel>,items:List<Channel>,countryCode:String?=null):List<Channel> {
        val result=ArrayList<Channel>(); val seen=HashSet<String>()
        for(c in previous) if(seen.add(c.url)) result.add(c)
        for(c in items) if(seen.add(c.url)) {
            if(result.size>=50000) throw ListLimit()
            result.add(if(countryCode!=null && c.country!=countryCode) c.copy(country=countryCode) else c)
        }
        return result
    }
    private fun loadConnections() {
        val prefs=getSharedPreferences("iptv_saved",MODE_PRIVATE)
        try {
            val raw=prefs.getString("connections_v1",null)
            if(raw==null) {
                listOf(Source.URL,Source.XTREAM).forEach { kind ->
                    val old=lists.getValue(kind)
                    if(old.isNotEmpty()) connections.add(Connection("old-${kind.name}","Mevcut ${kind.title}",kind,old.toList()))
                }
            } else {
                val arr=JSONArray(raw)
                for(i in 0 until arr.length()) {
                    val o=arr.getJSONObject(i); val entries=o.getJSONArray("channels")
                    val items=mutableListOf<Channel>()
                    for(j in 0 until entries.length()) {
                        val c=entries.getJSONObject(j)
                        items.add(Channel(c.getString("name"),c.getString("url"),"IPTV",c.optBoolean("hls",false),c.optString("group","Genel")))
                    }
                    connections.add(Connection(o.getString("id"),o.getString("name"),Source.valueOf(o.getString("source")),items))
                }
            }
            selectedConnection=connections.firstOrNull()?.id
        } catch(e:Exception) { connectionsReadable=false; toast("Bağlantı kayıtları okunamadı; kayıtlar korunuyor") }
    }
    private fun storeConnections(items:List<Connection>):Boolean {
        if(!connectionsReadable) return false
        val arr=JSONArray()
        items.forEach { c ->
            val entries=JSONArray()
            c.channels.forEach { ch -> entries.put(JSONObject().put("name",ch.name).put("url",ch.url).put("hls",ch.hls).put("group",ch.group)) }
            arr.put(JSONObject().put("id",c.id).put("name",c.name).put("source",c.source.name).put("channels",entries))
        }
        return getSharedPreferences("iptv_saved",MODE_PRIVATE).edit().putString("connections_v1",arr.toString()).commit()
    }
    private fun persistConnections(items:List<Connection>) {
        if(busy) { toast("Liste yükleniyor"); return }; busy=true
        Thread {
            val ok=try { storeConnections(items) } catch(e:Exception) { false }
            runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) {
                if(ok) { connections.clear(); connections.addAll(items); if(connections.none { it.id==selectedConnection }) selectedConnection=null; if(screen==null) render(); toast("Listeler kaydedildi") }
                else toast("Kayıt başarısız; eski listeler korundu")
            } }
        }.start()
    }
    private fun readLimited(reader:java.io.Reader):String {
        val result=StringBuilder(); val buffer=CharArray(8192)
        while(true) {
            val n=reader.read(buffer); if(n<0) break
            if(result.length+n>12*1024*1024) throw java.io.IOException("Liste boyutu sınırı")
            result.append(buffer,0,n)
        }
        return result.toString()
    }
    private class HttpFailure(val status:Int):java.io.IOException()
    private fun fetch(u:String):Pair<String,String> {
        return try { fetchOnce(u) } catch(e:java.net.SocketTimeoutException) { fetchOnce(u) }
    }
    private fun fetchOnce(u:String):Pair<String,String> {
        val c=URL(u).openConnection() as java.net.HttpURLConnection
        try {
            c.connectTimeout=30000; c.readTimeout=90000
            c.instanceFollowRedirects=true
            c.setRequestProperty("User-Agent","DilekciWorldTV/1.0")
            val status=c.responseCode
            if(status !in 200..299) throw HttpFailure(status)
            val body=c.inputStream.bufferedReader(Charsets.UTF_8).use { readLimited(it) }
            return body to c.url.toString()
        } finally { c.disconnect() }
    }
    private fun fetchPlaylist(u:String):List<Channel> {
        val c=URL(u).openConnection() as java.net.HttpURLConnection
        try {
            c.connectTimeout=30000; c.readTimeout=90000
            c.instanceFollowRedirects=true
            c.setRequestProperty("User-Agent","DilekciWorldTV/1.0")
            val status=c.responseCode
            if(status !in 200..299) throw HttpFailure(status)
            return c.inputStream.bufferedReader(Charsets.UTF_8).use { parse(it,c.url.toString(),true) }
        } finally { c.disconnect() }
    }
    private fun connectionError(e:Exception):String = when(e) {
        is ListLimit -> "Liste güvenli boyut sınırını aşıyor; eski kayıtlar korundu"
        is HttpFailure -> "Sunucu yanıtı: HTTP ${e.status}"
        is java.net.UnknownHostException -> "Sunucu adresi bulunamadı (DNS)"
        is java.net.SocketTimeoutException -> "Bağlantı zaman aşımına uğradı"
        is javax.net.ssl.SSLException -> "HTTPS güvenlik bağlantısı başarısız"
        is java.net.ConnectException -> "Sunucuya bağlantı kurulamadı"
        is java.net.MalformedURLException -> "Sunucu adresi geçersiz"
        else -> "Liste okunamadı (${e.javaClass.simpleName})"
    }
    private fun input(h:String)=EditText(this).apply { hint=h; setSingleLine(true); textSize=18f }
    private fun importTrialUrl(u:String) {
        if(!u.startsWith("https://") && !u.startsWith("http://")) { toast("Geçerli URL girin"); return }
        if(busy) { toast("Liste yükleniyor"); return }; busy=true
        val progress=AlertDialog.Builder(this).setTitle("Deneme listesi yükleniyor").setMessage("Lütfen bekleyin...").setCancelable(false).create(); progress.show()
        Thread {
            val message=try {
                val c=URL(u).openConnection() as java.net.HttpURLConnection
                try {
                    c.connectTimeout=30000; c.readTimeout=90000; c.instanceFollowRedirects=true
                    c.setRequestProperty("User-Agent","DilekciWorldTV/1.0")
                    val status=c.responseCode; if(status !in 200..299) throw HttpFailure(status)
                    val n=c.inputStream.bufferedReader(Charsets.UTF_8).use { importDb("iptv",it,c.url.toString()) }
                    "$n yeni kanal eklendi"
                } finally { c.disconnect() }
            } catch(e:Exception) { connectionError(e) }
            runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) { progress.dismiss(); refreshDb(); select(iptvTrial); toast(message) } }
        }.start()
    }
    private fun m3uTrial() {
        val e=input("M3U veya M3U8 URL")
        AlertDialog.Builder(this).setTitle("M3U URL ile Deneme").setView(e).setPositiveButton("Bağlan") { _,_ -> importTrialUrl(e.text.toString().trim()) }.setNegativeButton("İptal",null).show()
    }
    private fun pickFile(request:Int=501) { try { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="*/*" },request) } catch(e:Exception) { toast("Dosya seçici bulunamadı") } }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data); if(requestCode !in 501..503) return; if(resultCode!=RESULT_OK) { replacement=null; return }; val uri=data?.data ?: return; if(busy) { toast("Liste yükleniyor"); return }; busy=true
        Thread { try { val filename=contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c -> if(c.moveToFirst()) c.getString(0) else null } ?: "M3U dosyası"
            if(requestCode==501) {
                val a=contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { parse(it) } ?: error("Dosya okunamadı")
                runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) saveFile(filename,a) }
            } else {
                val key=if(requestCode==502) "world" else "iptv"
                val n=contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { importDb(key,it,null) } ?: error("Dosya okunamadı")
                runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) { refreshDb(); select(if(requestCode==502) trial else iptvTrial); toast("$n yeni kanal: $filename") } }
            } } catch(e:Exception) { runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) toast(connectionError(e)) } } }.start()
    }
    private fun loadFiles() {
        val prefs=getSharedPreferences("iptv_saved",MODE_PRIVATE)
        try {
            val a=JSONArray(prefs.getString("named_files_v1","[]") ?: "[]")
            val loaded=mutableListOf<FileList>()
            for(i in 0 until a.length()) {
                val o=a.getJSONObject(i); val entries=o.getJSONArray("channels")
                val items=mutableListOf<Channel>()
                for(j in 0 until entries.length()) {
                    val c=entries.getJSONObject(j)
                    items.add(Channel(c.getString("name"),c.getString("url"),"IPTV",c.optBoolean("hls",false),c.optString("group","Genel")))
                }
                loaded.add(FileList(o.getString("id"),o.getString("name"),items))
            }
            files.addAll(loaded)
            val old=lists.getValue(Source.FILE)
            if(!prefs.getBoolean("named_files_migrated",false) && old.isNotEmpty()) {
                val migrated=files+FileList("legacy-file","Eski M3U dosya kayıtları",old.toList())
                if(storeFiles(migrated)) { files.clear(); files.addAll(migrated) }
                else { files.add(migrated.last()); filesReadable=false; toast("Eski dosya kayıtları korunuyor; kayıt yapılamadı") }
            }
        } catch(e:Exception) { filesReadable=false; toast("Dosya kayıtları okunamadı; eski kayıtlar silinmedi") }
    }
    private fun storeFiles(items:List<FileList>):Boolean {
        if(!filesReadable) return false
        val a=JSONArray()
        items.forEach { f ->
            val entries=JSONArray()
            f.channels.forEach { c -> entries.put(JSONObject().put("name",c.name).put("url",c.url).put("hls",c.hls).put("group",c.group)) }
            a.put(JSONObject().put("id",f.id).put("name",f.name).put("channels",entries))
        }
        return getSharedPreferences("iptv_saved",MODE_PRIVATE).edit().putString("named_files_v1",a.toString()).putBoolean("named_files_migrated",true).commit()
    }
    private fun saveFile(name:String,items:List<Channel>) {
        val id=replacement; replacement=null
        if(items.isEmpty()) { toast("Dosyada desteklenen kanal bulunamadı"); return }
        val f=FileList(id ?: java.util.UUID.randomUUID().toString(),name,items)
        persistFiles(files.filter { it.id!=f.id }+f)
    }
    private fun loadTrials() {
        if(trialBusy) return; trialBusy=true; binding.contentDescription.text="${worldTrials().size} kanal • Liste güncelleniyor..."
        Thread { try {
            val body=fetch("https://iptv-org.github.io/iptv/index.country.m3u").first; val result=mutableListOf<Channel>(); var name=""; var code:String?=null
            val aliases=mapOf("United States" to "US","United Kingdom" to "GB","South Korea" to "KR","Turkey" to "TR","Türkiye" to "TR","Czech Republic" to "CZ")
            val countryLookup=mutableMapOf<String,String>()
            codes.forEach { c -> listOf(c,Locale("",c).getDisplayCountry(Locale.ENGLISH),countryName(c)).forEach { countryLookup[it.lowercase(Locale.ROOT)]=c } }
            aliases.forEach { (n,c) -> countryLookup[n.lowercase(Locale.ROOT)]=c }
            val gr=Regex("group-title=\"([^\"]*)\""); val nr=Regex("tvg-name=\"([^\"]*)\"")
            body.lineSequence().forEach { raw -> val line=raw.trim(); if(line.startsWith("#EXTINF:")) { val g=gr.find(line)?.groupValues?.get(1) ?: ""; code=g.split(';').mapNotNull { part -> val n=part.trim(); countryLookup[n.lowercase(Locale.ROOT)] }.firstOrNull(); name=(nr.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: line.substringAfterLast(',')).replace(Regex("""\s*\(\d+p\)\s*""")," ").replace(Regex("""\s+""")," ").trim() } else if(line.startsWith("http://") || line.startsWith("https://")) { val c=code; if(c!=null && line.startsWith("https://") && allowed[c]?.split('|')?.any { it.equals(name,true) }==true) result.add(Channel(name,line,c)); code=null; name="" } }
            val fresh=result.distinctBy { it.country+"|"+it.url }
            if(fresh.isNotEmpty()) write("world_trial_cache","channels",fresh)
            runOnUiThread { trialBusy=false; if(!isDestroyed && !isFinishing) { if(fresh.isNotEmpty()) { trials.clear(); trials.addAll(fresh); if(screen==null) syncCache(); trialRefreshed=true }; if(category==trial && screen==null) render() } }
        } catch(e:Exception) { runOnUiThread { trialBusy=false; if(!isDestroyed && !isFinishing && category==trial) binding.contentDescription.text="Liste yüklenemedi. Tekrar seçin." } } }.start()
    }
    override fun dispatchKeyEvent(event:KeyEvent):Boolean {
        if(screen!=null) {
            val ok=event.keyCode==KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode==KeyEvent.KEYCODE_ENTER
            if(ok && panel==null) { if(event.action==KeyEvent.ACTION_UP && !event.isCanceled) { if(event.eventTime-event.downTime>=700) { saveTrial() } else player?.let { if(it.playWhenReady) it.pause() else it.play() } }; return true }
            if(event.action==KeyEvent.ACTION_DOWN) when(event.keyCode) { KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT -> { side(); return true }; KeyEvent.KEYCODE_BACK -> { if(panel!=null) hideSide() else { closeVideo(); render(); binding.sectionContainer.getChildAt(categories.indexOf(if(category==iptvTrial || category==trial) settings else category))?.requestFocus() }; return true } }
        }; if(screen==null && category==iptv && source==Source.FILE && selectedFile!=null && event.keyCode==KeyEvent.KEYCODE_BACK && event.action==KeyEvent.ACTION_DOWN) { selectedFile=null; renderRight(); right?.getChildAt(1)?.requestFocus(); return true }; return super.dispatchKeyEvent(event)
    }
    override fun onDestroy() { closeVideo(); super.onDestroy() }

    private val dbHelper by lazy {
        object: android.database.sqlite.SQLiteOpenHelper(this, "dilekci_trial_v2.db", null, 1) {
            override fun onCreate(db:android.database.sqlite.SQLiteDatabase) {
                db.execSQL("CREATE TABLE ch(id INTEGER PRIMARY KEY AUTOINCREMENT, bucket TEXT NOT NULL, name TEXT NOT NULL, url TEXT NOT NULL, country TEXT NOT NULL, hls INTEGER NOT NULL, grp TEXT NOT NULL, UNIQUE(bucket,url))")
                db.execSQL("CREATE INDEX ch_bucket_country ON ch(bucket,country,id)")
                db.execSQL("CREATE TABLE migrated(bucket TEXT PRIMARY KEY)")
            }
            override fun onUpgrade(db:android.database.sqlite.SQLiteDatabase,oldVersion:Int,newVersion:Int) { error("Unsupported database version") }
        }
    }
    private val db get() = dbHelper.writableDatabase
    private fun insertDb(bucket:String,c:Channel):Boolean {
        val v=android.content.ContentValues().apply { put("bucket",bucket); put("name",c.name); put("url",c.url); put("country",c.country); put("hls",if(c.hls) 1 else 0); put("grp",c.group) }
        return db.insertWithOnConflict("ch",null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)!=-1L
    }
    private fun migrateDb(bucket:String,defaultCountry:String) {
        try {
            if(db.rawQuery("SELECT 1 FROM migrated WHERE bucket=?",arrayOf(bucket)).use { it.moveToFirst() }) return
            val raw=getSharedPreferences("trial_imports",MODE_PRIVATE).getString(bucket,"[]") ?: "[]"
            db.beginTransaction()
            try {
                android.util.JsonReader(java.io.StringReader(raw)).use { r ->
                    r.beginArray(); while(r.hasNext()) {
                        var name=""; var url=""; var country=defaultCountry; var hls=false; var grp="Genel"
                        r.beginObject(); while(r.hasNext()) when(r.nextName()) {
                            "name" -> name=r.nextString(); "url" -> url=r.nextString(); "country" -> country=r.nextString(); "hls" -> hls=r.nextBoolean(); "group" -> grp=r.nextString(); else -> r.skipValue()
                        }; r.endObject(); if(url.isNotBlank()) insertDb(bucket,Channel(name,url,country,hls,grp))
                    }; r.endArray()
                }
                db.execSQL("INSERT INTO migrated(bucket) VALUES(?)",arrayOf<Any>(bucket)); db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        } catch(e:Exception) { toast("Eski deneme kayıtları aktarılamadı; orijinal kayıtlar korundu") }
    }
    private fun syncCache() {
        db.beginTransaction(); try { db.delete("ch","bucket=?",arrayOf("cache")); trials.forEach { insertDb("cache",it) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }
    private fun clearDb(bucket:String):Boolean = try { db.delete("ch","bucket=?",arrayOf(bucket)); true } catch(e:Exception) { toast("Liste silinemedi"); false }
    private fun refreshDb() { importedWorld=dbList("world"); importedIptv=dbList("iptv") }
    private fun dbCountries():List<String> = db.rawQuery("SELECT DISTINCT country FROM ch WHERE bucket IN ('world','cache')",null).use { c -> val out=mutableListOf<String>(); while(c.moveToNext()) out.add(c.getString(0)); out }
    private fun dbList(bucket:String,query:String="",countryFilter:String?=null,includeCache:Boolean=false):List<Channel> {
        val args=mutableListOf(bucket)
        var where=if(includeCache) "(bucket=? OR (bucket='cache' AND NOT EXISTS (SELECT 1 FROM ch AS w WHERE w.bucket='world' AND w.url=ch.url)))" else "bucket=?"
        if(countryFilter!=null) { where+=" AND country=?"; args.add(countryFilter) }
        if(query.isNotBlank()) { where+=" AND instr(lower(name),lower(?))>0"; args.add(query) }
        val ids=db.rawQuery("SELECT id FROM ch WHERE $where ORDER BY CASE WHEN bucket='cache' THEN 1 ELSE 0 END,id",args.toTypedArray()).use { c -> LongArray(c.count).also { a -> var i=0; while(c.moveToNext()) a[i++]=c.getLong(0) } }
        return object:AbstractList<Channel>() {
            override val size:Int get()=ids.size
            private val cache=android.util.LruCache<Long,Channel>(200)
            override fun get(index:Int):Channel {
                val id=ids[index]; cache.get(id)?.let { return it }
                return db.rawQuery("SELECT name,url,country,hls,grp FROM ch WHERE id=?",arrayOf(id.toString())).use { c ->
                    if(!c.moveToFirst()) throw IndexOutOfBoundsException("Channel removed")
                    Channel(c.getString(0),c.getString(1),c.getString(2),c.getInt(3)!=0,c.getString(4)).also { cache.put(id,it) }
                }
            }
        }
    }
    private fun importDb(bucket:String,reader:java.io.BufferedReader,base:String?):Int {
        val root=base?.let { URL(it) }; val country=if(bucket=="world") "ZZ" else "IPTV"
        val gr=Regex("group-title=\"([^\"]*)\"",RegexOption.IGNORE_CASE)
        var name=""; var grp="Genel"; var first=true; var total=0L; var added=0; var count=0
        db.beginTransaction()
        try {
            count=db.rawQuery("SELECT count(*) FROM ch WHERE bucket=?",arrayOf(bucket)).use { it.moveToFirst(); it.getInt(0) }
            while(true) {
                val raw=reader.readLine() ?: break; total+=raw.length+1L
                if(total>256L*1024*1024 || raw.length>65536) throw ListLimit()
                val line=raw.trim().removePrefix("\uFEFF").trim(); if(line.isEmpty()) continue
                if(first) { first=false; if(base!=null && !line.startsWith("#EXTM3U",true)) error("M3U header") }
                if(line.startsWith("#EXT-X-TARGETDURATION") || line.startsWith("#EXT-X-STREAM-INF")) {
                    if(base==null || added>0) error("HLS playlist")
                    if(insertDb(bucket,Channel("HLS Yayını",base,country,true))) { added++; count++ }; if(count>150000) throw ListLimit(); break
                }
                if(line.startsWith("#EXTINF:")) { name=line.substringAfterLast(',').trim(); grp=gr.find(line)?.groupValues?.get(1)?.ifBlank { "Genel" } ?: "Genel" }
                else if(!line.startsWith("#")) {
                    val u=try { if(root!=null) URL(root,line).toString() else line } catch(e:Exception) { line }
                    if(u.startsWith("https://") || u.startsWith("http://")) {
                        if(insertDb(bucket,Channel(name.ifBlank { "Kanal ${count+1}" },u,country,u.substringBefore('?').endsWith(".m3u8",true),grp))) { count++; added++; if(count>150000) throw ListLimit() }
                    }; name=""; grp="Genel"
                }
            }
            db.setTransactionSuccessful(); return added
        } finally { db.endTransaction() }
    }

}

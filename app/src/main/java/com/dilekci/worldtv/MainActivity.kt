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
import java.net.URLEncoder
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
    private val categories = listOf("🇹🇷 Türkiye", world, trial, "🎬 Sinema", "📺 Diziler", "🌿 Belgeseller", "⚽ Spor", "📰 Haber", "⭐ Favoriler", iptv, settings)
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
    private var source = Source.XTREAM
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
    private val allowed = mapOf(
        "TR" to "TRT Haber|TRT World|TRT Belgesel|TRT EBA TV", "DE" to "DW English|DW Deutsch|DW Español|tagesschau24", "FR" to "France 24 English|France 24 French|France 24 Arabic|France 24 Spanish|Franceinfo", "GB" to "Sky News|BBC News", "US" to "CBS News|CBS News 24/7|ABC News Live|NBC News NOW|NASA TV|NASA+|PBS NewsHour", "CA" to "Savoir Média|CBC News|CBC News Explore", "AU" to "ABC News|ABC News Australia", "JP" to "NHK WORLD-JAPAN|NHK World Japan", "KR" to "Arirang TV|Arirang World|KBS World", "AT" to "oe24 TV|W24|Tirol TV", "CH" to "SRF info", "IT" to "Rai News 24", "ES" to "Canal 24 Horas|24h", "PT" to "RTP Notícias|RTP Internacional", "MX" to "Canal Once|Canal 22", "RO" to "TVR International|TVR Info", "GR" to "ERT News|ERT World", "IN" to "DD News|DD India", "SG" to "CNA|Channel NewsAsia", "NZ" to "RNZ", "BR" to "TV Brasil|TV Cultura", "AR" to "Canal Encuentro", "CL" to "24 Horas", "FI" to "Yle TV1", "NO" to "NRK1", "SE" to "SVT24", "NL" to "NPO Nieuws", "BE" to "VRT NWS", "ZA" to "SABC News", "KE" to "KBC Channel 1", "ID" to "TVRI World", "TH" to "Thai PBS", "MY" to "Bernama TV"
    )
    private fun dp(n:Int) = (n*resources.displayMetrics.density).toInt()
    private fun toast(s:String) = Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun countryName(c:String) = when(c) { "US"->"ABD"; "GB"->"İngiltere"; "KR"->"Güney Kore"; else->Locale("",c).getDisplayCountry(tr) }
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
        Source.values().filter { it!=Source.LEGACY && it!=Source.FILE }.forEach { lists.getValue(it).addAll(read("iptv_saved",it.key,"IPTV")) }
        loadConnections()
        loadFiles()
        if(filesReadable && storeFiles(files.filter { it.id!="legacy-file" })) { files.removeAll { it.id=="legacy-file" }; getSharedPreferences("iptv_saved",MODE_PRIVATE).edit().remove("channels").remove("file_channels").apply() }
        source=connections.firstOrNull()?.source ?: Source.XTREAM
        binding.sectionContainer.removeAllViews(); binding.primaryAction.visibility=View.GONE; binding.secondaryAction.visibility=View.GONE
        categories.forEach { c -> binding.sectionContainer.addView(item(c,17f) { select(c) }) }
        select(world); binding.sectionContainer.getChildAt(1)?.requestFocus()
    }
    private fun text(s:String)=TextView(this).apply { text=s; textSize=19f; setTextColor(-1); setPadding(dp(8),dp(8),dp(8),dp(8)) }
    private fun item(s:String,size:Float=19f,action:()->Unit)=text(s).apply { textSize=size; isFocusable=true; isClickable=true; setBackgroundResource(R.drawable.nav_item_background); setOnClickListener { action() } }
    private fun select(c:String) { closeVideo(); category=c; render(); if(c==trial && trials.isEmpty()) loadTrials() }
    private fun render() {
        content?.let { (it.parent as? LinearLayout)?.removeView(it) }; content=null; right=null
        binding.sectionTitle.text=category; binding.contentTitle.text=category
        if(category!=world && category!=trial && category!=iptv && category!=settings) { binding.contentDescription.text="Bu kategoriye henüz yayın eklenmedi."; return }
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,dp(16),0,0) }
        (binding.contentDescription.parent as LinearLayout).addView(row,LinearLayout.LayoutParams(-1,0,1f)); content=row
        if(category==settings) { renderSettings(row); return }
        val left=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; val r=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        row.addView(ScrollView(this).apply { addView(left) },LinearLayout.LayoutParams(0,-1,1f))
        row.addView(r,LinearLayout.LayoutParams(0,-1,1.5f)); right=r
        if(category==iptv) {
            val f=files.firstOrNull { it.id==selectedFile }
            binding.contentTitle.text=if(source==Source.FILE) f?.name ?: "IPTV" else connections.firstOrNull { it.id==selectedConnection }?.name ?: source.title
            val all=allIptv(); binding.contentDescription.text="${all.size} kanal"
            left.addView(text("KATEGORİLER"))
            val groups=listOf("Tüm Kanallar")+all.map { it.group }.distinct().filter { it!="Tüm Kanallar" }.sorted()
            if(group !in groups) group="Tüm Kanallar"
            left.addView(item("⚙️ Liste seç",16f) { select(settings) })
            groups.forEach { g -> left.addView(item(g,16f) { group=g; renderRight() }) }
        } else {
            val all=if(category==trial) trials else channels; val collator=Collator.getInstance(tr)
            val countries=all.map { it.country }.distinct().sortedWith { a,b -> collator.compare(countryName(a),countryName(b)) }
            binding.contentDescription.text=if(trialBusy && category==trial) "Kanallar yükleniyor..." else "${all.size} kanal • ${countries.size} ülke" + if(category==trial) " • Kaydetmek için OK uzun basın" else ""
            if(country !in countries) country=countries.firstOrNull() ?: "DE"
            countries.forEach { c -> left.addView(item("${flag(c)} ${countryName(c)}") { country=c; renderRight() }) }
        }
        renderRight()
    }
    private fun allIptv():List<Channel> = if(source==Source.FILE) files.firstOrNull { it.id==selectedFile }?.channels ?: emptyList() else connections.firstOrNull { it.id==selectedConnection && it.source==source }?.channels ?: emptyList()
    private fun active():List<Channel> = when(category) { iptv->allIptv().let { if(group=="Tüm Kanallar") it else it.filter { c->c.group==group } }; world->channels.filter { it.country==country }; trial->trials.filter { it.country==country }; else->emptyList() }
    private fun channelList(a:List<Channel>, action:(Int)->Unit)=ListView(this).apply {
        adapter=object:BaseAdapter() {
            override fun getCount()=a.size
            override fun getItem(position:Int)=a[position]
            override fun getItemId(position:Int)=position.toLong()
            override fun getView(position:Int,convertView:View?,parent:ViewGroup?):View {
                val v=convertView as? TextView ?: this@MainActivity.text("")
                v.text="${position+1}. ${a[position].name}"; return v
            }
        }
        setOnItemClickListener { _,_,i,_->action(i) }
    }
    private fun renderRight() {
        val r=right ?: return; r.removeAllViews(); r.addView(text(if(category==iptv) "KANAL LİSTESİ" else countryName(country)))
        val a=active(); if(a.isEmpty()) r.addView(text("Henüz kanal bulunamadı."))
        else r.addView(channelList(a) { openVideo(it) },LinearLayout.LayoutParams(-1,0,1f))
    }
    private fun renderSettings(row:LinearLayout) {
        binding.contentTitle.text="IPTV BAĞLANTI AYARLARI"; binding.contentDescription.text="Yüklenen listeleri yönet"
        val l=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; val r=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        row.addView(ScrollView(this).apply { addView(l) },LinearLayout.LayoutParams(0,-1,1f)); row.addView(ScrollView(this).apply { addView(r) },LinearLayout.LayoutParams(0,-1,1.5f))
        l.addView(item("Xtream Codes Ekle",16f) { xtream() }); l.addView(item("M3U / M3U8 URL Ekle",16f) { m3u() }); l.addView(item("Dosyadan Yükle",16f) { pickFile() }); l.addView(item("MAG / Stalker Portal",16f) { toast("Portal bağlantısı henüz aktif değil") })
        r.addView(text("YÜKLENEN LİSTELER"))
        connections.forEach { c -> r.addView(item("${c.source.title}: ${c.name}",16f) {
            AlertDialog.Builder(this).setTitle(c.name).setItems(arrayOf("Aç","Değiştir","Sil")) { _,i -> when(i) {
                0 -> { source=c.source; selectedConnection=c.id; group="Tüm Kanallar"; select(iptv) }
                1 -> { if(busy) toast("Liste yükleniyor") else if(c.source==Source.URL) m3u(c) else xtream(c) }
                2 -> AlertDialog.Builder(this).setMessage("Bu bağlantı silinsin mi?").setPositiveButton("Sil") { _,_ -> persistConnections(connections.filter { it.id!=c.id }) }.setNegativeButton("İptal",null).show()
            } }.show()
        }) }
        files.forEach { f -> r.addView(item(f.name,16f) {
            AlertDialog.Builder(this).setTitle(f.name).setItems(arrayOf("Aç","Değiştir","Sil")) { _,i -> when(i) {
                0->{ source=Source.FILE; selectedFile=f.id; group="Tüm Kanallar"; select(iptv) }
                1->{ if(!busy) { replacement=f.id; pickFile() } }
                2->{ AlertDialog.Builder(this).setMessage("Liste silinsin mi? Orijinal dosya korunur.").setPositiveButton("Sil") { _,_-> if(!busy) persistFiles(files.filter { it.id!=f.id }) }.setNegativeButton("İptal",null).show() }
            } }.show()
        }) }
    }
    private fun persistFiles(items:List<FileList>) {
        if(busy) return; busy=true
        Thread { val ok=try { storeFiles(items) } catch(e:Exception) { false }; runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) { if(ok) { files.clear(); files.addAll(items); if(screen==null) render() } else toast("Kayıt başarısız") } } }.start()
    }
    private fun openVideo(i:Int) {
        val a=active().toList(); if(i !in a.indices) return; closeVideo(); playing=a
        val f=FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()); isFocusable=true; isFocusableInTouchMode=true }; screen=f
        addContentView(f,FrameLayout.LayoutParams(-1,-1)); val pv=PlayerView(this).apply { useController=false }; f.addView(pv,FrameLayout.LayoutParams(-1,-1))
        val p=ExoPlayer.Builder(this).build(); player=p; pv.player=p
        p.addListener(object:Player.Listener { override fun onIsPlayingChanged(isPlaying:Boolean) { if(isPlaying) confirmed=true }; override fun onPlayerError(error:PlaybackException) { confirmed=false; toast("Yayın açılamadı") } })
        play(i); f.requestFocus()
    }
    private fun play(i:Int) {
        val c=playing.getOrNull(i) ?: return; current=i; confirmed=false
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
    private fun closeVideo() { hideSide(); player?.release(); player=null; screen?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }; screen=null; playing=emptyList(); confirmed=false }
    private fun saveTrial() {
        if(category!=trial) { toast("Kanal zaten listede"); return }; val c=playing.getOrNull(current) ?: return
        if(!confirmed) { toast("Önce yayının açılmasını bekleyin"); return }; if(channels.any { same(it,c) }) { toast("Kanal zaten kayıtlı"); return }
        AlertDialog.Builder(this).setTitle("Dünya TV'ye ekle").setMessage("${c.name}: Yayının çalıştığını ve kullanımına izin verildiğini doğruluyor musunuz?").setPositiveButton("Kaydet") { _,_ -> if(write("world_tv_saved","channels",saved+c)) { saved.add(c); channels.add(c); toast("Kanal kaydedildi") } else toast("Kayıt başarısız") }.setNegativeButton("İptal",null).show()
    }
    private fun parse(body:String,base:String?=null):List<Channel> {
        if(body.contains("#EXT-X-TARGETDURATION") || body.contains("#EXT-X-STREAM-INF")) return if(base!=null) listOf(Channel("HLS Yayını",base,"IPTV",true)) else emptyList()
        val result=mutableListOf<Channel>(); var name=""; var g="Genel"; val gr=Regex("group-title=\"([^\"]*)\"",RegexOption.IGNORE_CASE)
        body.lineSequence().forEach { raw -> val line=raw.trim().removePrefix("\uFEFF"); if(line.startsWith("#EXTINF:")) { name=line.substringAfterLast(',').trim(); g=gr.find(line)?.groupValues?.get(1)?.ifBlank { "Genel" } ?: "Genel" } else if(line.isNotBlank() && !line.startsWith("#")) { val u=try { if(base!=null) URL(URL(base),line).toString() else line } catch(e:Exception) { line }; if(u.startsWith("https://") || u.startsWith("http://")) result.add(Channel(name.ifBlank { "Kanal ${result.size+1}" },u,"IPTV",u.substringBefore('?').endsWith(".m3u8",true),g)); name=""; g="Genel" } }
        return result.distinctBy { it.url }
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
    private fun fetch(u:String):Pair<String,String> { val c=URL(u).openConnection(); c.connectTimeout=20000; c.readTimeout=30000; val body=c.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }; return body to c.url.toString() }
    private fun importUrl(u:String,s:Source,name:String,id:String?=null) {
        if(!u.startsWith("https://") && !u.startsWith("http://")) { toast("Geçerli URL girin"); return }
        if(busy) { toast("Liste yükleniyor"); return }; busy=true
        val key=id ?: java.util.UUID.randomUUID().toString()
        val previous=connections.toList()
        val progress=AlertDialog.Builder(this).setTitle("Liste yükleniyor").setMessage("Lütfen bekleyin...").setCancelable(false).create()
        progress.show()
        Thread {
            var updated:List<Connection>?=null
            val message=try {
                val (body,base)=fetch(u); val a=parse(body,base)
                if(a.isEmpty()) "IPTV kanalı bulunamadı" else {
                    val next=previous.filter { it.id!=key }+Connection(key,name,s,a)
                    if(storeConnections(next)) { updated=next; "${a.size} kanal kaydedildi" } else "Liste kaydedilemedi"
                }
            } catch(e:Exception) { "Bağlantı kurulamadı veya liste okunamadı" }
            runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) {
                progress.dismiss()
                updated?.let { connections.clear(); connections.addAll(it); source=s; selectedConnection=key; group="Tüm Kanallar"; if(screen==null) render() }
                toast(message)
            } }
        }.start()
    }
    private fun input(h:String)=EditText(this).apply { hint=h; setSingleLine(true); textSize=18f }
    private fun m3u(old:Connection?=null) {
        val l=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(12),dp(20),dp(12)) }
        val n=input("Liste adı"); n.setText(old?.name ?: "M3U ${connections.count { it.source==Source.URL }+1}")
        val e=input("M3U veya M3U8 URL"); l.addView(n); l.addView(e)
        AlertDialog.Builder(this).setTitle("M3U URL").setView(l).setPositiveButton("Bağlan") { _,_ -> importUrl(e.text.toString().trim(),Source.URL,n.text.toString().trim().ifBlank { "M3U URL" },old?.id) }.setNegativeButton("İptal",null).show()
    }
    private fun xtream(old:Connection?=null) {
        val l=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(12),dp(20),dp(12)) }
        val n=input("Liste adı"); n.setText(old?.name ?: "Xtream ${connections.count { it.source==Source.XTREAM }+1}")
        val h=input("Sunucu URL"); val u=input("Kullanıcı adı"); val p=input("Şifre"); p.inputType=129; l.addView(n); l.addView(h); l.addView(u); l.addView(p)
        AlertDialog.Builder(this).setTitle("Xtream Codes").setView(l).setPositiveButton("Bağlan") { _,_ -> val host=h.text.toString().trim().trimEnd('/'); val user=u.text.toString().trim(); val pass=p.text.toString(); if(host.isBlank() || user.isBlank() || pass.isBlank()) toast("Tüm alanları doldurun") else importUrl("$host/get.php?username=${URLEncoder.encode(user,"UTF-8")}&password=${URLEncoder.encode(pass,"UTF-8")}&type=m3u_plus&output=m3u8",Source.XTREAM,n.text.toString().trim().ifBlank { "Xtream Codes" },old?.id) }.setNegativeButton("İptal",null).show()
    }
    private fun pickFile() { try { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="*/*" },501) } catch(e:Exception) { toast("Dosya seçici bulunamadı") } }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data); if(requestCode!=501) return; if(resultCode!=RESULT_OK) { replacement=null; return }; val uri=data?.data ?: return; if(busy) { toast("Liste yükleniyor"); return }; busy=true
        Thread { try { val filename=contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c -> if(c.moveToFirst()) c.getString(0) else null } ?: "M3U dosyası"
            val body=contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: error("Dosya okunamadı"); val a=parse(body); runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) saveFile(filename,a) } } catch(e:Exception) { runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) toast("Dosya okunamadı") } } }.start()
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
        if(trialBusy) return; trialBusy=true; binding.contentDescription.text="Kanallar yükleniyor..."
        Thread { try {
            val body=fetch("https://iptv-org.github.io/iptv/index.country.m3u").first; val result=mutableListOf<Channel>(); var name=""; var code:String?=null
            val aliases=mapOf("United States" to "US","United Kingdom" to "GB","South Korea" to "KR","Turkey" to "TR","Türkiye" to "TR","Czech Republic" to "CZ")
            val gr=Regex("group-title=\"([^\"]*)\""); val nr=Regex("tvg-name=\"([^\"]*)\"")
            body.lineSequence().forEach { raw -> val line=raw.trim(); if(line.startsWith("#EXTINF:")) { val g=gr.find(line)?.groupValues?.get(1) ?: ""; code=g.split(';').mapNotNull { part -> val n=part.trim(); aliases.entries.firstOrNull { it.key.equals(n,true) }?.value ?: codes.firstOrNull { it.equals(n,true) || Locale("",it).getDisplayCountry(Locale.ENGLISH).equals(n,true) || countryName(it).equals(n,true) } }.firstOrNull(); name=(nr.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: line.substringAfterLast(',')).replace(Regex("""\s*\(\d+p\)\s*""")," ").replace(Regex("""\s+""")," ").trim() } else if(line.startsWith("http://") || line.startsWith("https://")) { val c=code; if(c!=null && line.startsWith("https://") && allowed[c]?.split('|')?.any { it.equals(name,true) }==true) result.add(Channel(name,line,c)); code=null; name="" } }
            runOnUiThread { trialBusy=false; if(!isDestroyed && !isFinishing) { trials.clear(); trials.addAll(result.distinctBy { it.country+"|"+it.url }); if(category==trial && screen==null) render() } }
        } catch(e:Exception) { runOnUiThread { trialBusy=false; if(!isDestroyed && !isFinishing && category==trial) binding.contentDescription.text="Liste yüklenemedi. Tekrar seçin." } } }.start()
    }
    override fun dispatchKeyEvent(event:KeyEvent):Boolean {
        if(screen!=null) {
            val ok=event.keyCode==KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode==KeyEvent.KEYCODE_ENTER
            if(ok && panel==null) { if(event.action==KeyEvent.ACTION_UP && !event.isCanceled) { if(event.eventTime-event.downTime>=700) saveTrial() else player?.let { if(it.playWhenReady) it.pause() else it.play() } }; return true }
            if(event.action==KeyEvent.ACTION_DOWN) when(event.keyCode) { KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT -> { side(); return true }; KeyEvent.KEYCODE_BACK -> { if(panel!=null) hideSide() else { closeVideo(); render(); binding.sectionContainer.getChildAt(categories.indexOf(category))?.requestFocus() }; return true } }
        }; if(screen==null && category==iptv && source==Source.FILE && selectedFile!=null && event.keyCode==KeyEvent.KEYCODE_BACK && event.action==KeyEvent.ACTION_DOWN) { selectedFile=null; renderRight(); right?.getChildAt(1)?.requestFocus(); return true }; return super.dispatchKeyEvent(event)
    }
    override fun onDestroy() { closeVideo(); super.onDestroy() }
}

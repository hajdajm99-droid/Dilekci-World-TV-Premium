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
    private val categories = listOf("🇹🇷 Türkiye", world, trial, "🎬 Sinema", "📺 Diziler", "🌿 Belgeseller", "⚽ Spor", "📰 Haber", "⭐ Favoriler", iptv)
    private data class Channel(val name:String,val url:String,val country:String,val hls:Boolean=false)
    private enum class Source(val key:String,val title:String) {
        XTREAM("xtream_channels","Xtream Codes"), URL("url_channels","M3U URL"), FILE("file_channels","M3U Dosyası"), LEGACY("channels","Eski IPTV Kayıtları")
    }
    private val lists = Source.values().associateWith { mutableListOf<Channel>() }
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
            for(i in 0 until a.length()) { val o=a.getJSONObject(i); result.add(Channel(o.getString("name"),o.getString("url"),o.optString("country",defaultCountry),o.optBoolean("hls",false))) }
        } catch(e:Exception) { toast("Kayıt okunamadı") }
        return result
    }
    private fun write(pref:String,key:String,items:List<Channel>):Boolean {
        val a=JSONArray(); items.forEach { a.put(JSONObject().put("name",it.name).put("url",it.url).put("country",it.country).put("hls",it.hls)) }
        return getSharedPreferences(pref,MODE_PRIVATE).edit().putString(key,a.toString()).commit()
    }
    override fun onCreate(state:Bundle?) {
        super.onCreate(state); binding=ActivityMainBinding.inflate(layoutInflater); setContentView(binding.root)
        read("world_tv_saved","channels","").filter { it.country in codes && it.url.startsWith("https://") }.forEach { c -> if(channels.none { same(it,c) }) { channels.add(c); saved.add(c) } }
        Source.values().forEach { lists.getValue(it).addAll(read("iptv_saved",it.key,"IPTV")) }
        source=Source.values().firstOrNull { lists.getValue(it).isNotEmpty() } ?: Source.XTREAM
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
        if(category!=world && category!=trial && category!=iptv) { binding.contentDescription.text="Bu kategoriye henüz yayın eklenmedi."; return }
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,dp(16),0,0) }
        val left=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; val r=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        row.addView(ScrollView(this).apply { addView(left) },LinearLayout.LayoutParams(0,dp(360),1f))
        row.addView(ScrollView(this).apply { addView(r) },LinearLayout.LayoutParams(0,dp(360),1.5f))
        (binding.contentDescription.parent as LinearLayout).addView(row); content=row; right=r
        if(category==iptv) {
            binding.contentTitle.text="IPTV - LİSTELERİM"; binding.contentDescription.text="${lists.values.sumOf { it.size }} kayıtlı kanal"
            Source.values().forEach { s -> left.addView(item("${s.title} (${lists.getValue(s).size})",16f) { source=s; renderRight() }) }
        } else {
            val all=if(category==trial) trials else channels; val collator=Collator.getInstance(tr)
            val countries=all.map { it.country }.distinct().sortedWith { a,b -> collator.compare(countryName(a),countryName(b)) }
            binding.contentDescription.text=if(trialBusy && category==trial) "Kanallar yükleniyor..." else "${all.size} kanal • ${countries.size} ülke" + if(category==trial) " • Kaydetmek için OK uzun basın" else ""
            if(country !in countries) country=countries.firstOrNull() ?: "DE"
            countries.forEach { c -> left.addView(item("${flag(c)} ${countryName(c)}") { country=c; renderRight() }) }
        }
        renderRight()
    }
    private fun active():List<Channel> = when(category) { iptv->lists.getValue(source); world->channels.filter { it.country==country }; trial->trials.filter { it.country==country }; else->emptyList() }
    private fun renderRight() {
        val r=right ?: return; r.removeAllViews(); r.addView(text(if(category==iptv) source.title else countryName(country)))
        if(category==iptv && source!=Source.LEGACY) r.addView(item("➕ Liste ekle",17f) { when(source) { Source.XTREAM->xtream(); Source.URL->m3u(); Source.FILE->pickFile(); else->Unit } })
        val a=active(); if(a.isEmpty()) r.addView(text("Henüz kanal bulunamadı."))
        a.forEachIndexed { i,c -> r.addView(item("${i+1}. ${c.name}") { openVideo(i) }) }
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
        f.addView(p,FrameLayout.LayoutParams(dp(300),-1,Gravity.START)); p.addView(text(if(category==iptv) source.title else "KANALLAR"))
        val l=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; p.addView(ScrollView(this).apply { addView(l) },LinearLayout.LayoutParams(-1,0,1f))
        playing.forEachIndexed { i,c -> l.addView(item("${i+1}. ${c.name}") { play(i); hideSide() }) }; l.getChildAt(current)?.requestFocus()
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
        val result=mutableListOf<Channel>(); var name=""
        body.lineSequence().forEach { raw -> val line=raw.trim().removePrefix("\uFEFF"); if(line.startsWith("#EXTINF:")) name=line.substringAfterLast(',').trim() else if(line.isNotBlank() && !line.startsWith("#")) { val u=try { if(base!=null) URL(URL(base),line).toString() else line } catch(e:Exception) { line }; if(u.startsWith("https://") || u.startsWith("http://")) result.add(Channel(name.ifBlank { "Kanal ${result.size+1}" },u,"IPTV",u.substringBefore('?').endsWith(".m3u8",true))); name="" } }
        return result.distinctBy { it.url }
    }
    private fun save(s:Source,a:List<Channel>) {
        if(a.isEmpty()) { toast("IPTV kanalı bulunamadı"); return }; val target=lists.getValue(s); val merged=(target+a).distinctBy { it.url }
        if(!write("iptv_saved",s.key,merged)) { toast("Liste kaydedilemedi"); return }; target.clear(); target.addAll(merged)
        if(category==iptv && screen==null) { source=s; render() }; toast("${s.title}: ${target.size} kanal")
    }
    private fun fetch(u:String):Pair<String,String> { val c=URL(u).openConnection(); c.connectTimeout=20000; c.readTimeout=30000; val body=c.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }; return body to c.url.toString() }
    private fun importUrl(u:String,s:Source) {
        if(!u.startsWith("https://") && !u.startsWith("http://")) { toast("Geçerli URL girin"); return }; if(busy) { toast("Liste yükleniyor"); return }; busy=true
        Thread { try { val (body,base)=fetch(u); val a=parse(body,base); runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) save(s,a) } } catch(e:Exception) { runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) toast("Bağlantı kurulamadı") } } }.start()
    }
    private fun input(h:String)=EditText(this).apply { hint=h; setSingleLine(true); textSize=18f }
    private fun m3u() { val e=input("M3U veya M3U8 URL"); AlertDialog.Builder(this).setTitle("M3U URL").setView(e).setPositiveButton("Bağlan") { _,_ -> importUrl(e.text.toString().trim(),Source.URL) }.setNegativeButton("İptal",null).show() }
    private fun xtream() {
        val l=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(12),dp(20),dp(12)) }; val h=input("Sunucu URL"); val u=input("Kullanıcı adı"); val p=input("Şifre"); p.inputType=129; l.addView(h); l.addView(u); l.addView(p)
        AlertDialog.Builder(this).setTitle("Xtream Codes").setView(l).setPositiveButton("Bağlan") { _,_ -> val host=h.text.toString().trim().trimEnd('/'); val user=u.text.toString().trim(); val pass=p.text.toString(); if(host.isBlank() || user.isBlank() || pass.isBlank()) toast("Tüm alanları doldurun") else importUrl("$host/get.php?username=${URLEncoder.encode(user,"UTF-8")}&password=${URLEncoder.encode(pass,"UTF-8")}&type=m3u_plus&output=m3u8",Source.XTREAM) }.setNegativeButton("İptal",null).show()
    }
    private fun pickFile() { try { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="*/*" },501) } catch(e:Exception) { toast("Dosya seçici bulunamadı") } }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data); if(requestCode!=501 || resultCode!=RESULT_OK) return; val uri=data?.data ?: return; if(busy) { toast("Liste yükleniyor"); return }; busy=true
        Thread { try { val body=contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: error("Dosya okunamadı"); val a=parse(body); runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) save(Source.FILE,a) } } catch(e:Exception) { runOnUiThread { busy=false; if(!isDestroyed && !isFinishing) toast("Dosya okunamadı") } } }.start()
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
        }; return super.dispatchKeyEvent(event)
    }
    override fun onDestroy() { closeVideo(); super.onDestroy() }
}

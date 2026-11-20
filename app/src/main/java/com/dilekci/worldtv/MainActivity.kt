package com.dilekci.worldtv

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.dilekci.worldtv.databinding.ActivityMainBinding

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding

    private val sections = listOf(
        "Ana Sayfa", "Canlı TV", "Filmler", "Diziler",
        "Belgeseller", "Spor", "Haber", "Ayarlar"
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

        binding.primaryAction.setOnClickListener { selectSection("Canlı TV") }
        binding.secondaryAction.setOnClickListener { selectSection("Filmler") }
    }

    private fun selectSection(section: String) {
        binding.sectionTitle.text = section
        binding.contentTitle.text = when (section) {
            "Ana Sayfa" -> "Dünyanın ekranı"
            "Canlı TV" -> "Canlı TV"
            "Filmler" -> "Filmler"
            "Diziler" -> "Diziler"
            "Belgeseller" -> "Belgeseller"
            "Spor" -> "Spor"
            "Haber" -> "Haber"
            "Ayarlar" -> "Ayarlar"
            else -> section
        }
        binding.contentDescription.text = when (section) {
            "Canlı TV" -> "Kendi yayın listenizi eklediğinizde kanallarınız burada görünecek."
            "Ayarlar" -> "Xtream Codes, M3U ve uygulama tercihleri sonraki geliştirme aşamasında eklenecek."
            else -> "Bu bölüm için içerik kaynağı henüz eklenmedi."
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (binding.sectionTitle.text != "Ana Sayfa") {
                selectSection("Ana Sayfa")
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

package com.openbridge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.openbridge.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PrefsManager(this)

        binding.btnBack.setOnClickListener { finish() }

        // Initial state
        binding.checkAutoOpen.isChecked = prefs.autoOpen
        binding.editDefaultMime.setText(prefs.defaultMime)

        // Listeners
        binding.checkAutoOpen.setOnCheckedChangeListener { _, isChecked ->
            prefs.autoOpen = isChecked
        }
        
        // We'll save the text when the user leaves or we could add a save button
    }

    override fun onPause() {
        super.onPause()
        prefs.defaultMime = binding.editDefaultMime.text.toString()
    }
}

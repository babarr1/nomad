package com.example.nomad // Added missing package declaration

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.nomad.R

// Renamed from PhenomenaActivityActivity to PhenomenaActivity
class PhenomenaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phenomena)
    }
}
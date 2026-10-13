/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.ui.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.ui.theme.KenkoTheme

/**
 * Target of the Health Connect rationale intent. Health Connect finishes this page
 * after showing the app's privacy policy link; a minimal static screen is enough to
 * satisfy the platform contract.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KenkoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(text = getString(R.string.label_health_connect_rationale))
                    }
                }
            }
        }
    }
}

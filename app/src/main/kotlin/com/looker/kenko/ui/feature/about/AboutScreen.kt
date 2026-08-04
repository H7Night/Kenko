/*
 * Copyright (C) 2025 LooKeR & Contributors
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.looker.kenko.ui.feature.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.looker.kenko.BuildConfig
import com.looker.kenko.R
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.KenkoBorderWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackPress: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.label_about)) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = KenkoBorderWidth,
            )
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.ic_app_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/H7Night/Kenko"))
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        // No browser installed — silently ignore
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                modifier = Modifier.padding(horizontal = 32.dp),
            ) {
                Text(text = stringResource(R.string.label_view_source_on_github))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

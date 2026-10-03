package com.mi.explorer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mi.explorer.ui.theme.MiOrange

enum class MiTab {
    RECENT,
    STORAGE
}

@Composable
fun MiTopHeader(
    selectedTab: MiTab,
    onTabSelected: (MiTab) -> Unit,
    onSearchClick: () -> Unit,
    onCleanerClick: () -> Unit,
    onFtpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Xiaomi MIUI styled Tab Selector (Recent vs Storage)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiTabPill(
                    text = "Recent",
                    isSelected = selectedTab == MiTab.RECENT,
                    onClick = { onTabSelected(MiTab.RECENT) },
                    modifier = Modifier.testTag("tab_recent")
                )
                MiTabPill(
                    text = "Storage",
                    isSelected = selectedTab == MiTab.STORAGE,
                    onClick = { onTabSelected(MiTab.STORAGE) },
                    modifier = Modifier.testTag("tab_storage")
                )
            }

            // Action icons: Cleaner, FTP / Transfer, Search
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCleanerClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("header_cleaner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Cleaner",
                        tint = MiOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onFtpClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("header_ftp_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Transfer to PC (FTP)",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("header_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiTabPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "pillBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MiOrange else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pillText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = textColor
        )
    }
}

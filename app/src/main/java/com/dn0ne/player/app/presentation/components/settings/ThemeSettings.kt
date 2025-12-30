package com.dn0ne.player.app.presentation.components.settings

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhonelinkSetup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastForEach
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.settings.Theme.Appearance
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.dn0ne.player.core.data.Settings

@Composable
fun ThemeSettings(
    settings: Settings,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }

    ColumnWithCollapsibleTopBar(
        topBarContent = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = context.resources.getString(R.string.back)
                )
            }

            Text(
                text = context.resources.getString(R.string.theme),
                fontSize = lerp(
                    MaterialTheme.typography.titleLarge.fontSize,
                    MaterialTheme.typography.displaySmall.fontSize,
                    collapseFraction
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            )
        },
        collapseFraction = {
            collapseFraction = it
        },
        contentPadding = PaddingValues(horizontal = 28.dp),
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        val isDarkTheme = isSystemInDarkTheme()
        val selectedAppearance by settings.appearance.collectAsState()
        val appearanceOptions = remember {
            listOf(
                AppearanceOption(
                    title = context.resources.getString(R.string.appearance_system),
                    onSelection = {
                        settings.updateAppearance(Appearance.System)
                    },
                    appearance = Appearance.System,
                    icon = Icons.Rounded.PhonelinkSetup,
                    containerColor = if (isDarkTheme) Color.Black else Color.White,
                    contentColor = if (isDarkTheme) Color.White else Color.Black
                ),
                AppearanceOption(
                    title = context.resources.getString(R.string.appearance_light),
                    onSelection = {
                        settings.updateAppearance(Appearance.Light)
                    },
                    appearance = Appearance.Light,
                    icon = Icons.Rounded.LightMode,
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                AppearanceOption(
                    title = context.resources.getString(R.string.appearance_dark),
                    onSelection = {
                        settings.updateAppearance(Appearance.Dark)
                    },
                    appearance = Appearance.Dark,
                    icon = Icons.Rounded.DarkMode,
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
            )
        }

        SettingOptionsRow(
            title = context.resources.getString(R.string.appearance),
            options = appearanceOptions,
            modifier = Modifier.fillMaxWidth()
        ) { option ->
            Column(
                modifier = Modifier
                    .clip(ShapeDefaults.Large)
                    .clickable {
                        option.onSelection()
                    }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isSelected by remember {
                    derivedStateOf {
                        selectedAppearance == option.appearance
                    }
                }
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(color = option.containerColor)
                        .border(
                            width = animateDpAsState(
                                targetValue = if (isSelected) 2.dp else (-1).dp,
                                label = ""
                            ).value,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = option.contentColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        val amoledDarkTheme by settings.amoledDarkTheme.collectAsState()
        if (selectedAppearance != Appearance.Light) {
            SettingSwitch(
                title = context.resources.getString(R.string.black_theme),
                supportingText = context.resources.getString(R.string.black_theme_explain),
                icon = Icons.Rounded.Contrast,
                isChecked = amoledDarkTheme,
                onCheckedChange = {
                    settings.updateAmoledDarkTheme(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val useDynamicColor by settings.useDynamicColor.collectAsState()
            SettingSwitch(
                title = context.resources.getString(R.string.use_system_key_colors),
                supportingText = context.resources.getString(R.string.use_system_key_colors_explain),
                icon = Icons.Rounded.AutoAwesome,
                isChecked = useDynamicColor,
                onCheckedChange = {
                    settings.updateUseDynamicColor(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

class AppearanceOption(
    title: String,
    onSelection: () -> Unit,
    val appearance: Appearance,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
) : SettingOption(title, onSelection)

object Theme {
    enum class Appearance {
        System,
        Light,
        Dark
    }
}

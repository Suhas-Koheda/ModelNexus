package dev.haas.modelhub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haas.modelhub.model.Tab

@Composable
fun TabSwitcher(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit
) {
    val isLight = !isSystemDark()
    val containerColor = if (isLight) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
    val selectedColor = if (isLight) Color.White else Color(0xFF2C2C2E)
    val textColor = if (isLight) Color(0xFF1E1E1E) else Color.White
    val unselectedColor = if (isLight) Color(0xFF8E8E93) else Color(0xFF98989D)

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(containerColor)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Tab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.95f,
                animationSpec = tween(150),
                label = "scale"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) selectedColor else Color.Transparent,
                animationSpec = tween(150),
                label = "bg"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(RoundedCornerShape(7.dp))
                    .background(bgColor)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = if (isSelected) textColor else unselectedColor
                )
            }
        }
    }
}

private fun isSystemDark(): Boolean {
    return androidx.compose.ui.graphics.Color(0f, 0f, 0f).isDark()
}

private fun Color.isDark(): Boolean {
    val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
    return luminance < 0.5
}

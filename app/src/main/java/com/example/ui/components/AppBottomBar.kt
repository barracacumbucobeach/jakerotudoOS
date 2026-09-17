package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JakeroTheme
import com.example.ui.viewmodel.AppTab

@Composable
fun AppBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onNovaOrdemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        // Floating Action Button: "+ Nova ordem de serviço"
        FloatingActionButton(
            onClick = onNovaOrdemClick,
            containerColor = palette.brandLime,
            contentColor = palette.brandLimeText,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(bottom = 6.dp)
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = palette.brandLimeText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nova ordem de serviço",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.brandLimeText
                )
            }
        }

        // Barra de 4 Abas
        NavigationBar(
            containerColor = palette.surfaceBackground,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val icon = when (tab) {
                    AppTab.INICIO -> Icons.Default.Home
                    AppTab.ORDENS -> Icons.Default.Assignment
                    AppTab.CLIENTES -> Icons.Default.People
                    AppTab.FINANCEIRO -> Icons.Default.Paid
                }

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = palette.brandLimeText,
                        selectedTextColor = palette.brandTeal,
                        indicatorColor = palette.brandLime,
                        unselectedIconColor = palette.textSecondaryColor,
                        unselectedTextColor = palette.textSecondaryColor
                    )
                )
            }
        }
    }
}

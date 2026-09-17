package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.sync.SyncState
import com.example.ui.theme.JakeroTheme

@Composable
fun AppHeader(
    tituloTela: String,
    syncState: SyncState,
    numeroAvisos: Int,
    temAvisoUrgente: Boolean,
    onSyncClick: () -> Unit,
    onAvisosClick: () -> Unit,
    onConfigClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

    Surface(
        color = palette.surfaceBackground,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo circular e títulos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, palette.brandLime, CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.jakero_logo),
                            contentDescription = "Logo Jakero Tudo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "JAKERO TUDO",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = tituloTela,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondaryColor
                        )
                    }
                }

                // Indicador de Sincronização, Sino de Avisos e Configurações
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Indicador de Sync
                    SyncBadge(
                        syncState = syncState,
                        onClick = onSyncClick
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Sino de Avisos com Badge
                    IconButton(onClick = onAvisosClick) {
                        BadgedBox(
                            badge = {
                                if (numeroAvisos > 0) {
                                    Badge(
                                        containerColor = if (temAvisoUrgente) Color(0xFFD32F2F) else Color(0xFF0288D1),
                                        contentColor = Color.White
                                    ) {
                                        Text(text = "$numeroAvisos", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Avisos e Lembretes",
                                tint = if (numeroAvisos > 0 && temAvisoUrgente) Color(0xFFD32F2F) else palette.textColor
                            )
                        }
                    }

                    // Configurações
                    IconButton(onClick = onConfigClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Configurações",
                            tint = palette.textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncBadge(
    syncState: SyncState,
    onClick: () -> Unit
) {
    val palette = JakeroTheme.palette

    val (bgColor, textColor, label) = when (syncState) {
        is SyncState.Sincronizado -> Triple(
            Color(0x224CAF50),
            Color(0xFF2E7D32),
            "Sincronizado"
        )
        is SyncState.Enviando -> Triple(
            Color(0x220288D1),
            Color(0xFF0288D1),
            "Enviando…"
        )
        is SyncState.Offline -> {
            if (syncState.pendentes > 0) {
                Triple(
                    Color(0x33E65100),
                    Color(0xFFE65100),
                    "${syncState.pendentes} pendente(s)"
                )
            } else {
                Triple(
                    Color(0x229E9E9E),
                    palette.textSecondaryColor,
                    "Offline"
                )
            }
        }
        is SyncState.Desconectado -> Triple(
            Color(0x229E9E9E),
            palette.textSecondaryColor,
            "Desconectado"
        )
    }

    val transition = rememberInfiniteTransition(label = "sync_rotate")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation"
    )

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (syncState is SyncState.Enviando) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier
                        .size(13.dp)
                        .rotate(angle)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

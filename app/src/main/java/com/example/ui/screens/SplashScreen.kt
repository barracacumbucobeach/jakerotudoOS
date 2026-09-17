package com.example.ui.screens

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JakeroLime
import com.example.ui.theme.JakeroTeal
import com.example.ui.theme.JakeroTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette

    val scaleAnim = remember { Animatable(0.4f) }
    val alphaAnim = remember { Animatable(0f) }
    val blurAnim = remember { Animatable(20f) }
    val titleAlphaAnim = remember { Animatable(0f) }
    val lineProgressAnim = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    // Anel girando
    val infiniteTransition = rememberInfiniteTransition(label = "ring_spin")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LaunchedEffect(Unit) {
        // 1. Escala e entrada com desfoque
        scaleAnim.animateTo(1.0f, tween(900))
        alphaAnim.animateTo(1.0f, tween(700))
        blurAnim.animateTo(0f, tween(700))

        // 2. Nome surgindo
        titleAlphaAnim.animateTo(1.0f, tween(600))

        // 3. Linha verde crescendo
        lineProgressAnim.animateTo(1.0f, tween(1000))

        // Espera para totalizar ~3.8 a 4 segundos
        delay(1200)

        // Fade out
        screenAlpha.animateTo(0f, tween(500))
        onFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(palette.surfaceBackground)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onFinished()
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Container da Logo com Anel Girando ao redor
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Anel gradiente girando
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .rotate(ringRotation)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    JakeroLime,
                                    JakeroTeal,
                                    Color.Transparent,
                                    JakeroLime
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Logo com entrada em escala e desfoque
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(scaleAnim.value)
                        .alpha(alphaAnim.value)
                        .blur(blurAnim.value.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, JakeroLime, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.jakero_logo),
                        contentDescription = "Logo Jakero Tudo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(120.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nome surgindo
            Text(
                text = "JAKERO TUDO",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textColor,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(titleAlphaAnim.value)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ordens de Serviço",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textSecondaryColor,
                modifier = Modifier.alpha(titleAlphaAnim.value)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Linha verde crescendo
            Box(
                modifier = Modifier
                    .width(180.dp * lineProgressAnim.value)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(JakeroLime)
            )
        }
    }
}

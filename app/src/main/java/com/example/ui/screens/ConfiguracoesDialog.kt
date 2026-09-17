package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Configuracoes
import com.example.data.model.FrequenciaLembrete
import com.example.data.model.TemaApp
import com.example.ui.theme.JakeroTheme

@Composable
fun ConfiguracoesDialog(
    config: Configuracoes,
    onDismiss: () -> Unit,
    onTemaChange: (TemaApp) -> Unit,
    onFrequenciaChange: (FrequenciaLembrete) -> Unit,
    onAntecedenciaChange: (Int) -> Unit,
    onSupabaseSave: (String, String) -> Unit,
    onExportarBackup: () -> Unit,
    onImportarBackup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = JakeroTheme.palette
    val context = LocalContext.current

    var supabaseUrl by remember { mutableStateOf(config.supabaseUrl) }
    var supabaseKey by remember { mutableStateOf(config.supabaseAnonKey) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: ""
                if (json.isNotBlank()) {
                    onImportarBackup(json)
                    Toast.makeText(context, "Backup importado com sucesso!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao ler arquivo de backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surfaceBackground,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Topo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = palette.brandTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configurações",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textColor
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    // 1. Seletor de Tema
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "TEMA VISUAL",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TemaCard("Claro", Color(0xFFEFEEE9), Color(0xFF231F1C), config.tema == TemaApp.CLARO, Modifier.weight(1f)) {
                                    onTemaChange(TemaApp.CLARO)
                                }
                                TemaCard("Escuro", Color(0xFF0F0D0C), Color(0xFF8EE600), config.tema == TemaApp.ESCURO, Modifier.weight(1f)) {
                                    onTemaChange(TemaApp.ESCURO)
                                }
                                TemaCard("Jakero", Color(0xFFE6F3D2), Color(0xFF1B7A85), config.tema == TemaApp.JAKERO, Modifier.weight(1f)) {
                                    onTemaChange(TemaApp.JAKERO)
                                }
                            }
                        }
                    }

                    // 2. Lembretes e Cobranças
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "FREQUÊNCIA DOS LEMBRETES",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FrequenciaLembrete.values().forEach { freq ->
                                    val isSelected = config.frequenciaLembrete == freq
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onFrequenciaChange(freq) },
                                        label = { Text(freq.label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = palette.brandLime,
                                            selectedLabelColor = palette.brandLimeText
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "AVISAR VENCIMENTO COM ANTECEDÊNCIA",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(0, 1, 3, 7).forEach { dias ->
                                    val isSelected = config.antecedenciaVencimento == dias
                                    val label = if (dias == 0) "No dia" else "$dias dia(s)"
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onAntecedenciaChange(dias) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = palette.brandLime,
                                            selectedLabelColor = palette.brandLimeText
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Backup de Dados
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "BACKUP LOCAL (JSON)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = onExportarBackup,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Exportar", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch("application/json") },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Importar", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // 4. Sincronização Supabase
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "SINCRONIZAÇÃO EM NUVEM (SUPABASE)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textSecondaryColor,
                                    letterSpacing = 0.5.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = palette.brandTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            OutlinedTextField(
                                value = supabaseUrl,
                                onValueChange = { supabaseUrl = it },
                                label = { Text("Supabase Project URL") },
                                placeholder = { Text("https://xyz.supabase.co") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = palette.cardBackground,
                                    unfocusedContainerColor = palette.cardBackground,
                                    focusedBorderColor = palette.brandLime,
                                    unfocusedBorderColor = palette.borderColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = supabaseKey,
                                onValueChange = { supabaseKey = it },
                                label = { Text("Supabase Anon Key") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = palette.cardBackground,
                                    unfocusedContainerColor = palette.cardBackground,
                                    focusedBorderColor = palette.brandLime,
                                    unfocusedBorderColor = palette.borderColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    onSupabaseSave(supabaseUrl, supabaseKey)
                                    Toast.makeText(context, "Configurações salvas. Sincronizando…", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.brandLime,
                                    contentColor = palette.brandLimeText
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Salvar e Sincronizar Agora", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemaCard(
    nome: String,
    bgSample: Color,
    accentSample: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = JakeroTheme.palette

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.cardBackground,
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) palette.brandLime else palette.borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(bgSample)
                    .border(1.dp, accentSample, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nome,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) palette.brandTeal else palette.textColor
            )
        }
    }
}

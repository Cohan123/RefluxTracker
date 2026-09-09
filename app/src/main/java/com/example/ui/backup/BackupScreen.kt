package com.example.ui.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RefluxViewModel
import com.example.ui.components.HighDensityCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BotanicalCardBorder
import com.example.ui.theme.BotanicalCream
import com.example.ui.theme.BotanicalForest
import com.example.ui.theme.BotanicalMint
import com.example.ui.theme.BotanicalSage
import com.example.ui.theme.BotanicalSurface
import com.example.ui.theme.BotanicalTextMuted
import com.example.ui.theme.BotanicalTextPrimary
import com.example.ui.theme.BotanicalTextSecondary
import com.example.ui.theme.TriggerHighRed
import com.example.ui.theme.TriggerHighRedBg
import com.example.ui.theme.TriggerLowGreen
import com.example.ui.theme.TriggerLowGreenBg
import com.example.util.CsvBackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: RefluxViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var csvPastedText by remember { mutableStateOf("") }
    var validationResult by remember { mutableStateOf<CsvBackupManager.ValidationResult?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    // File Picker für CSV-Dateien
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().use { it.readText() }
                    csvPastedText = content
                    validationResult = viewModel.validateCsvData(content)
                    statusMessage = "Datei geladen. Bitte prüfe die Vorschau und klicke auf 'Daten importieren'."
                    isSuccessStatus = true
                }
            } catch (e: Exception) {
                statusMessage = "Fehler beim Lesen der Datei: ${e.localizedMessage}"
                isSuccessStatus = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CSV Datensicherung",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BotanicalTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = BotanicalTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BotanicalCream)
            )
        },
        containerColor = BotanicalCream
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status-Meldung (wenn vorhanden)
            if (statusMessage != null) {
                item {
                    val bg = if (isSuccessStatus) TriggerLowGreenBg else TriggerHighRedBg
                    val tint = if (isSuccessStatus) TriggerLowGreen else TriggerHighRed
                    val icon = if (isSuccessStatus) Icons.Default.CheckCircle else Icons.Default.WarningAmber

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        color = bg
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMessage.orEmpty(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = tint
                            )
                        }
                    }
                }
            }

            // 1. Export-Bereich
            item {
                SectionHeader(
                    title = "Daten exportieren (Backup)",
                    subtitle = "Sichere alle Mahlzeiten, Lebensmittel, Symptome und Maßnahmen als CSV"
                )

                HighDensityCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Vollständiger Datenexport",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BotanicalTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Erzeugt eine standardisierte CSV-Datei mit allen gespeicherten Daten (inkl. Mehrfachsymptomen, Auslösern und Linderungsmaßnahmen), die du teilen, speichern oder archivieren kannst.",
                            fontSize = 12.sp,
                            color = BotanicalTextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        try {
                                            val csv = viewModel.exportCsvData()
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(Intent.EXTRA_SUBJECT, "RefluxTrack_Backup_${System.currentTimeMillis()}.csv")
                                                putExtra(Intent.EXTRA_TEXT, csv)
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "RefluxTrack CSV exportieren")
                                            context.startActivity(shareIntent)
                                            statusMessage = "Export erfolgreich gestartet."
                                            isSuccessStatus = true
                                        } catch (e: Exception) {
                                            statusMessage = "Export fehlgeschlagen: ${e.localizedMessage}"
                                            isSuccessStatus = false
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BotanicalForest),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("export_csv_share_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CSV teilen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        try {
                                            val csv = viewModel.exportCsvData()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("RefluxTrack CSV", csv)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "CSV in Zwischenablage kopiert!", Toast.LENGTH_SHORT).show()
                                            statusMessage = "CSV-Inhalt in Zwischenablage kopiert (${csv.lines().size} Zeilen)."
                                            isSuccessStatus = true
                                        } catch (e: Exception) {
                                            statusMessage = "Kopieren fehlgeschlagen: ${e.localizedMessage}"
                                            isSuccessStatus = false
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("export_csv_copy_button")
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kopieren", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 2. Import-Bereich
            item {
                SectionHeader(
                    title = "Daten importieren (Wiederherstellung)",
                    subtitle = "Lade ein vorheriges Backup oder füge CSV-Text direkt ein"
                )

                HighDensityCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "CSV-Datei auswählen oder Text einfügen",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BotanicalTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vorhandene Daten bleiben erhalten. Neue Mahlzeiten und Symptome werden nahtlos hinzugefügt.",
                            fontSize = 12.sp,
                            color = BotanicalTextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Datei laden Button
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("select_csv_file_button")
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV-Datei vom Speicher auswählen", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Oder CSV-Text direkt hier einfügen:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BotanicalTextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = csvPastedText,
                            onValueChange = {
                                csvPastedText = it
                                if (it.isNotBlank()) {
                                    validationResult = viewModel.validateCsvData(it)
                                } else {
                                    validationResult = null
                                }
                            },
                            placeholder = {
                                Text(
                                    "Typ,Zeitstempel,Art,Lebensmittel/Symptome,Staerke,Dauer,Ausloeser,Massnahmen,Notiz\nMEAL,1700000000000,Mittagessen,\"Reis, Huehnchen\",MITTEL,,,,\"Notiz\"",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = BotanicalTextMuted
                                )
                            },
                            minLines = 4,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BotanicalSurface,
                                unfocusedContainerColor = BotanicalSurface,
                                focusedBorderColor = BotanicalForest,
                                unfocusedBorderColor = BotanicalCardBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("csv_import_textfield")
                        )

                        // Validierungsergebnis
                        validationResult?.let { res ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp)),
                                color = if (res.isValid) BotanicalSage.copy(alpha = 0.15f) else TriggerHighRedBg
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Gefundene Einträge: ca. ${res.estimatedMeals} Mahlzeiten, ${res.estimatedSymptoms} Symptome (${res.rowCount} Zeilen)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (res.isValid) BotanicalForest else TriggerHighRed
                                    )
                                    res.errorMessage?.let { msg ->
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Hinweis / Fehler: $msg",
                                            fontSize = 11.sp,
                                            color = TriggerHighRed
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Import Ausführen Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    try {
                                        val result = viewModel.importCsvData(csvPastedText)
                                        if (result.totalImported > 0 || !result.hasErrors) {
                                            statusMessage = "Erfolgreich importiert: ${result.mealsImported} Mahlzeiten, ${result.symptomsImported} Symptome (${result.duplicatesSkipped} Dubletten übersprungen)."
                                            isSuccessStatus = !result.hasErrors
                                            if (!result.hasErrors) {
                                                csvPastedText = ""
                                                validationResult = null
                                            }
                                        } else {
                                            statusMessage = "Import-Fehler: ${result.errors.joinToString(", ")}"
                                            isSuccessStatus = false
                                        }
                                    } catch (e: Exception) {
                                        statusMessage = "Unerwarteter Fehler: ${e.localizedMessage}"
                                        isSuccessStatus = false
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BotanicalForest),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isProcessing && csvPastedText.isNotBlank() && (validationResult?.isValid == true),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("execute_import_button")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    color = BotanicalCream,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Daten importieren", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Info Card
            item {
                HighDensityCard(backgroundColor = BotanicalSurface) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BotanicalSage, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Datenschutz: Alle Exporte und Importe finden lokal auf deinem Gerät statt. Es werden keine Daten an externe Server übertragen.",
                            fontSize = 11.sp,
                            color = BotanicalTextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

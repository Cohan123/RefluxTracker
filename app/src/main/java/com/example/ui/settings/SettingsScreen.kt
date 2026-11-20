package com.example.ui.settings

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.importer.CsvImportSummary
import com.example.reminder.ReminderScheduler
import com.example.reminder.ReminderSettings
import com.example.ui.RefluxViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen with three features:
 *  - Daily reminder toggle + time picker
 *  - CSV export of all locally-stored data
 *  - CSV/HTML import of a backup
 *
 * Stays as simple as possible — no account, no cloud, no telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: RefluxViewModel,
    onNavigateBack: () -> Unit,
    onRequestCsvExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings = remember { ReminderSettings(context) }
    var reminderEnabled by remember { mutableStateOf(settings.isEnabled()) }
    val initialTime = settings.getReminderTime()
    var hour by remember { mutableStateOf(initialTime?.hourOfDay ?: 19) }
    var minute by remember { mutableStateOf(initialTime?.minute ?: 30) }

    var isImporting by remember { mutableStateOf(false) }
    var importDialog by remember { mutableStateOf<ImportDialogContent?>(null) }

    val storedReportName by viewModel.doctorReportUserName.collectAsStateWithLifecycle()
    var reportNameInput by remember(storedReportName) { mutableStateOf(storedReportName) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val pdfFileName = remember {
        "Arztbericht_" +
            SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(Date()) + ".pdf"
    }

    val pdfSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            isGeneratingPdf = true
            viewModel.saveDoctorReportTo(
                context = context,
                uri = uri,
                onComplete = {
                    isGeneratingPdf = false
                    Toast.makeText(context, "Arztbericht gespeichert.", Toast.LENGTH_LONG).show()
                },
                onError = { throwable ->
                    isGeneratingPdf = false
                    Toast.makeText(
                        context,
                        "Speichern fehlgeschlagen: ${throwable.message ?: "unbekannter Fehler"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    fun shareDoctorReport() {
        isGeneratingPdf = true
        viewModel.createDoctorReport(
            context = context,
            onReady = { file ->
                isGeneratingPdf = false
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileprovider",
                        file
                    )
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        putExtra(
                            android.content.Intent.EXTRA_SUBJECT,
                            "GERD / Reflux – Symptom- und Verlaufsbericht"
                        )
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = android.content.Intent
                        .createChooser(send, "Arztbericht teilen")
                        .apply {
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    context.startActivity(chooser)
                } catch (t: Throwable) {
                    Toast.makeText(
                        context,
                        "Teilen fehlgeschlagen: ${t.message ?: "unbekannter Fehler"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onError = { throwable ->
                isGeneratingPdf = false
                Toast.makeText(
                    context,
                    "Bericht fehlgeschlagen: ${throwable.message ?: "unbekannter Fehler"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            viewModel.importCsv(
                context = context,
                uri = uri,
                onComplete = { summary ->
                    isImporting = false
                    importDialog = ImportDialogContent(
                        title = "Import abgeschlossen",
                        message = importSuccessMessage(summary)
                    )
                },
                onError = { throwable ->
                    isImporting = false
                    importDialog = ImportDialogContent(
                        title = "Import fehlgeschlagen",
                        message = throwable.message?.takeIf { it.isNotBlank() }
                            ?: "Die Datei konnte nicht importiert werden."
                    )
                }
            )
        }
    }

    fun enableReminder() {
        settings.enable(hour, minute)
        ReminderScheduler.schedule(context, hour, minute)
        reminderEnabled = true
    }

    fun disableReminder() {
        settings.disable()
        ReminderScheduler.cancel(context)
        reminderEnabled = false
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enableReminder()
        } else {
            // Keep the switch off rather than pretending the reminder works.
            reminderEnabled = false
            Toast.makeText(
                context,
                "Ohne Benachrichtigungsberechtigung kann die Erinnerung nicht angezeigt werden.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val onToggleReminder: (Boolean) -> Unit = { newVal ->
        if (newVal) {
            val needsPermission = android.os.Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                // Stay off until the permission result is known.
                reminderEnabled = false
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                enableReminder()
            }
        } else {
            disableReminder()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Reminder card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "Tägliche Erinnerung",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (reminderEnabled) {
                                    "Aktiv um %02d:%02d".format(hour, minute)
                                } else {
                                    "Ausgeschaltet"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = onToggleReminder,
                            modifier = Modifier.testTag("reminder_switch")
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        hour = h
                                        minute = m
                                        settings.enable(h, m)
                                        ReminderScheduler.schedule(context, h, m)
                                    },
                                    hour,
                                    minute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.testTag("reminder_time_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zeit ändern: %02d:%02d".format(hour, minute))
                        }
                    }
                }
            }

            // Doctor report card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doctor_report_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Arztbericht (PDF)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Erzeugt einen A4-Bericht für den aktuell in der Auswertung " +
                            "gewählten Zeitraum. Fehlende Angaben werden nicht geschätzt, " +
                            "sondern als nicht erfasst ausgewiesen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reportNameInput,
                        onValueChange = {
                            reportNameInput = it
                            viewModel.setDoctorReportUserName(it)
                        },
                        label = { Text("Name (optional, nur im PDF)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doctor_report_name_field")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { pdfSaveLauncher.launch(pdfFileName) },
                            enabled = !isGeneratingPdf,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_doctor_report_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isGeneratingPdf) "Erzeuge …" else "PDF speichern")
                        }
                        OutlinedButton(
                            onClick = { shareDoctorReport() },
                            enabled = !isGeneratingPdf,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_doctor_report_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PDF teilen")
                        }
                    }
                }
            }

            // Export card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Daten exportieren",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Erzeugt eine CSV-Datei mit allen erfassten Mahlzeiten und Symptomen. " +
                            "Die Datei wird über die Teilen-Funktion deines Geräts geteilt — " +
                            "du entscheidest, wohin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestCsvExport,
                        modifier = Modifier.testTag("export_csv_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CSV exportieren")
                    }
                }
            }

            // Import card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Daten importieren",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Liest eine CSV- oder HTML-Sicherung ein (z. B. RefluxTrack_Backup_*.csv). " +
                            "Lebensmittel, Tätigkeiten und Maßnahmen werden bei Bedarf ergänzt; " +
                            "bereits vorhandene Einträge werden nicht doppelt angelegt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        enabled = !isImporting,
                        modifier = Modifier.testTag("import_csv_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isImporting) "Importiere …" else "CSV importieren")
                    }
                }
            }
        }
    }

    importDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { importDialog = null },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                TextButton(onClick = { importDialog = null }) {
                    Text("OK")
                }
            }
        )
    }
}

private data class ImportDialogContent(val title: String, val message: String)

private fun importSuccessMessage(summary: CsvImportSummary): String {
    val builder = StringBuilder()
    builder.append("${summary.mealsImported} Mahlzeiten und ")
        .append("${summary.symptomsImported} Symptome importiert.")
    if (summary.duplicatesSkipped > 0) {
        builder.append("\n\nBereits vorhandene Einträge übersprungen: ")
            .append(summary.duplicatesSkipped)
    }
    if (summary.skippedRows > 0) {
        builder.append("\nNicht lesbare Zeilen: ").append(summary.skippedRows)
    }
    if (summary.warnings.isNotEmpty()) {
        builder.append("\n\nHinweise:")
        summary.warnings.take(5).forEach { warning ->
            builder.append("\n• ").append(warning)
        }
        if (summary.warnings.size > 5) {
            builder.append("\n… und ").append(summary.warnings.size - 5).append(" weitere.")
        }
    }
    return builder.toString()
}

# RefluxTracker

Persönlicher Reflux- und GERD-Tracker für Android. Lokal, ohne Cloud, ohne Tracker — alle Daten bleiben auf dem Gerät.

## Zweck

Hilft, persönliche Auslöser für Reflux / Sodbrennen zu erkennen, indem man Mahlzeiten und Symptome im Alltag erfasst und die App anschließend Korrelationen sichtbar macht.

## Features

- **Mahlzeiten erfassen** — Zeitpunkt (auch rückwirkend), Mahlzeit-Typ (Frühstück, Mittag-, Abendessen, Snack, Getränk, Sonstiges), Portionsgröße, Notiz, zugeordnete Lebensmittel; nach dem Speichern springt die Tagesansicht automatisch auf das gespeicherte Datum
- **Lebensmittel-Auswahl über der Tastatur** — suchbare, scrollbare Auswahl mit eigener Suche; die Eingabezeile bleibt über der Bildschirmtastatur sichtbar (`adjustResize` + `imePadding`)
- **Gewohnheiten** — deterministisch aus der Mahlzeitenhistorie erkannt (mind. 3 ähnliche Mahlzeiten an mind. 3 Tagen); Vorschläge bei der Eingabe, Verwaltung mit Umbenennen, Lebensmittelauswahl, Häufigkeit, typischer Uhrzeit, letzter Nutzung und Löschen
- **Lebensmittel-Kategorien** — manuell zuweisbare Analysemerkmale (Fettig, Scharf, Sauer, Tomate, Zitrus, Schokolade, Kaffee/Koffein, Alkohol, Kohlensäure, Zwiebel/Knoblauch, Süß, Deftig, Frittiert, Stark würzig, Schwarz-/Grüntee, Milchprodukt, Sehr heiß, Große Portion), Mehrfachauswahl pro Lebensmittel; „Groß“ auf Mahlzeitenebene zählt als große Portion
- **Mahlzeiten bearbeiten** — bestehende Einträge inkl. Datum/Uhrzeit, Typ, Portion und Lebensmittelauswahl ändern
- **Symptome erfassen** — Zeitpunkt (auch rückwirkend), mehrere Symptomarten gleichzeitig, Intensität (0–10), Dauer, Notiz sowie beliebig viele Tätigkeiten/Auslöser und Maßnahmen (Kataloge erweiterbar)
- **Symptome bearbeiten** — bestehende Einträge inkl. Datum/Uhrzeit, Mehrfachauswahl und Katalogdetails ändern
- **Timeline** — Tagesansicht aller Mahlzeiten und Symptome in chronologischer Reihenfolge; Einträge aufklappen für alle Symptomarten, Tätigkeiten, Maßnahmen und Notizen; direkt bearbeiten oder löschen
- **Dashboard** — Tages-Zähler, Durchschnitts-Intensität, Top-Trigger der letzten 14 Tage
- **Analyse** — Statistische Trigger-Analyse mit symptomspezifischen Zeitfenstern (z. B. Sodbrennen 15 Min.–3 Std., Husten 1–8 Std.) sowie festen Auswertungsfenstern über 7, 14, 30 oder 365 Tage inkl. Portionsgewichtung, Tagesverlauf, Tätigkeiten und Maßnahmen; alle ausgewählten Symptomarten fließen ein. Zusätzlich: Kategorie-Korrelationen/Trigger-Matrix, Tageszeit-Heatmap, täglicher GERD-Burden-Verlauf mit Spitzen und data-getriebenen Hinweisen
- **Arztbericht (PDF)** — A4-Bericht für den gewählten Zeitraum mit Symptomübersicht, dokumentierten Medikamenteneinträgen, Verlaufsvergleich, beobachteten Zusammenhängen und festem Haftungshinweis; speichern über die Systemdateiauswahl oder direkt teilen; fehlende Werte werden nie geschätzt
- **CSV-Export** — alle Symptomarten sowie Tätigkeiten und Maßnahmen pro Eintrag
- **CSV-Import** — Sicherungen im alten (`TYP;DATUM;…`) oder aktuellen Exportformat inkl. HTML-Weiterleitung einlesen; fehlende Katalogeinträge werden ergänzt, Duplikate übersprungen
- **Tägliche Erinnerung** — optionale lokale Benachrichtigung; bleibt auch nach Neustart oder App-Update erhalten
- **Lebensmittel** — Vordefinierte Liste (deutsche Bezeichnungen), erweiterbar um eigene Einträge
- **Local-only** — Keine Cloud, kein Account, kein Tracking, keine Netzwerkberechtigung; nur die optionalen Benachrichtigungs-/Boot-Berechtigungen für die Erinnerung

## Architektur

- **Sprache:** Kotlin 2.2
- **UI:** Jetpack Compose + Material 3
- **Persistenz:** Room (SQLite), standardmäßig mit SQLCipher verschlüsselt; für ein reines Debug-Build ohne Verschlüsselung: `-PenableSqlcipher=false`
- **DI:** Keine — explizite Factories (`RefluxRepository.create`, `RefluxViewModel.provideFactory`)
- **Build:** Gradle (Kotlin DSL) mit Version Catalog (`gradle/libs.versions.toml`)
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36 (Android 16)

```
app/src/main/java/com/example/
├── MainActivity.kt
├── analysis/         # Trigger-Engine, Fixed-Window-Analytics, HabitDetector
├── data/
│   ├── local/        # Room: AppDatabase, DAOs, Migrationen, InitialFoodData
│   ├── model/        # Entities, FoodCategories, MealTypes, SymptomTypes
│   └── repository/   # RefluxRepository, TimelineEntry
├── export/           # CsvExporter, RefluxDataExport
├── importer/         # CsvImporter, RefluxDataImport
├── reminder/         # tägliche Erinnerung, Boot-Restore, Notification
├── report/           # DoctorReportBuilder, DoctorReportPdf, ReportSettings
├── ui/
│   ├── RefluxViewModel.kt
│   ├── analysis/     # AnalysisScreen (Verlauf, Heatmap, Trigger-Matrix)
│   ├── components/   # Wiederverwendbare Compose-Komponenten
│   ├── dashboard/    # DashboardScreen
│   ├── food/         # FoodsScreen
│   ├── habit/        # HabitsScreen
│   ├── meal/         # AddMealScreen (Tastatur-sichere Auswahl)
│   ├── navigation/   # RefluxNavHost
│   ├── settings/     # SettingsScreen (Erinnerung, CSV, PDF)
│   ├── symptom/      # AddSymptomScreen
│   ├── theme/        # Color, Type, Theme
│   └── timeline/     # TimelineScreen
└── util/             # DateTimeUtils
```

### Analyse-Service (feste Zeiträume)

`RefluxAnalyticsService` ist ein reines, testbares Modul für Auswertungen über 7, 14, 30 oder 365 Tage. Es bekommt Mahlzeiten inkl. Lebensmitteln sowie Symptome inkl. Tätigkeiten/Maßnahmen und liefert ein `AnalyticsSummary`. Die Analyse-Seite bindet den Service über `RefluxViewModel.analyticsSummary` an und bietet einen Zeitraum-Umschalter für die vier Fenster. Beispiel:

```kotlin
val summary = RefluxAnalyticsService.getAnalyticsSummary(
    meals = repository.allMealsWithFoods.first(),
    symptoms = repository.allSymptomsWithDetails.first(),
    timeWindowDays = AnalyticsWindow.SEVEN_DAYS.days
)
```

- **Zeitfenster-Matching:** Ein Symptom zählt für eine Mahlzeit, wenn es 30 Minuten bis 4 Stunden nach der Mahlzeit beginnt (beide Grenzen inklusive).
- **Überlappende Mahlzeiten:** Ein Symptom kann mehreren Mahlzeiten zugeordnet werden; die Wahrscheinlichkeit wird pro Konsumereignis berechnet, nicht pro eindeutigem Symptom.
- **Portionsgewichtung:** `Klein` = 0,8, `Normal`/leer = 1,0, `Groß` = 1,25. Numerische Portionswerte werden als relative Gewichte verwendet. Die gewichtete Trigger-Wahrscheinlichkeit ist die Summe der Portionsgewichte der Mahlzeiten mit passendem Symptom geteilt durch die Summe aller Portionsgewichte des Lebensmittels.
- **Metriken:** gewichtete und rohe Trigger-Wahrscheinlichkeit, durchschnittliche Latenz bis zum ersten Symptom, durchschnittliche Symptomstärke, aktivste Tätigkeiten und ein Vergleich von Maßnahmen gegen Symptome ohne Maßnahme (Stärke und Dauer).
- **Top-Listen:** Top-3-Trigger mit einem vertrauensadjustierten Score, häufigste Tätigkeiten und wirksamste Maßnahmen. Bei Maßnahmen werden belastbare Gruppen (mindestens 3 Beobachtungen mit und ohne Maßnahme) bevorzugt.
- **Tageswerte:** Die durchschnittliche Symptomstärke wird nach lokalem Kalendertag gruppiert; Tage ohne Symptome erscheinen nicht als `null`-Einträge.
- **Täglicher GERD-Burden (0–100):** `60 % · (Ø Intensität / 10) + 40 % · (min(Symptomzahl, 4) / 4)`. Ein dokumentierter Tag ohne Symptome hat Burden 0; ein Tag **ohne jeden Eintrag** ist `null` und wird nie als symptomfrei gewertet. Spitze = Burden ≥ `max(Mittelwert + Standardabweichung, Mittelwert + 10)` über die dokumentierten Tage.
- **Verlauf:** lineare Tendenz über dokumentierte Tage (mind. 3, inkl. dokumentierter symptomfreier Tage, ohne fehlende Tage). Der Halbzeitvergleich mittelt die erste und zweite Hälfte des Fensters getrennt über dokumentierte Tage; `Veränderung % = (zweite − erste) / erste · 100`, negativ = Besserung.
- **Tageszeit-Heatmap:** 24 Stunden-Buckets mit Anzahl, Intensitätssumme und Ø Intensität. Das stärkste zusammenhängende 5-Stunden-Fenster basiert auf starken Symptomen (Intensität ≥ 6), sofern mindestens 3 davon vorliegen, sonst auf allen Symptomen; ein Hinweistext erscheint erst ab 3 Beobachtungen und mindestens 50 % Anteil im Fenster.
- **Kategorie-Korrelationen / Trigger-Matrix:** Vergleich je Mahlzeit mit vs. ohne Faktor im 30-Min.–4-Std.-Fenster. Relative Veränderung nur bei mindestens 3 Mahlzeiten je Gruppe und Basiswahrscheinlichkeit ≥ 5 %; sonst nur Prozentpunkte. Datenqualität richtet sich nach der kleineren Gruppe. Fehlende Kategorien zählen weder positiv noch negativ. Ausgewertet werden alle Kategorien aus `FoodCategories` (inkl. große Portion) und die Verhaltensfaktoren „Abendmahlzeit ab 18 Uhr“ / „späte Mahlzeit ab 21 Uhr“. Zusammenhänge sind ausdrücklich keine Kausalität.
- **Nicht auswertbar:** Stress, Schlafqualität, Abstand zum Schlafbeginn und Medikamenteneinnahme pro Mahlzeit werden als „nicht erfasst“ ausgewiesen, statt einen Effekt zu behaupten.
- **Grenzfälle:** Leere Datenmengen liefern eine gültige leere Zusammenfassung. Fehlende Dauern werden bei Dauer-Mittelwerten ignoriert. Die Ergebnisse sind beschreibend und keine medizinische Kausalaussage.

Die Logik ist in `app/src/main/java/com/example/analysis/RefluxAnalyticsService.kt` implementiert und durch `RefluxAnalyticsServiceTest`, `RefluxAnalyticsExtensionsTest` sowie `AnalysisScreenTest` abgedeckt.

### Gewohnheiten

`HabitDetector` ist deterministisch und lokal:

- Mahlzeiten werden chronologisch verarbeitet und zu Clustern gruppiert, wenn Mahlzeitentyp, Jaccard-Ähnlichkeit der Lebensmittelnamen (≥ 0,6) und zeitlicher Abstand (≤ 180 Min., zirkulär) passen.
- Ein Habit braucht mindestens 3 Vorkommen an mindestens 3 verschiedenen lokalen Tagen.
- Die Lebensmittelliste enthält nur Namen, die in mindestens 60 % der Cluster-Mahlzeiten vorkommen; die typische Uhrzeit ist der zirkuläre Mittelwert.
- Die Signatur enthält zusätzlich einen 3-Stunden-Zeitblock der typischen Uhrzeit. Dadurch bleiben gleiche Lebensmittel zu unterschiedlichen Tageszeiten zwei getrennte Gewohnheiten und die UI-Liste hat garantiert eindeutige Schlüssel. Overrides aus älteren Versionen (ohne Zeitblock) werden weiterhin erkannt.
- Nur Abweichungen (Name, Lebensmittelauswahl, gelöscht) werden in `habit_overrides` gespeichert. Die Mahlzeitenhistorie bleibt unverändert; Vorschläge werden bei jeder Eingabe neu berechnet.

### Arztbericht (PDF)

`DoctorReportBuilder` erzeugt ein reines Datenmodell aus demselben `AnalyticsSummary` wie die Analyse-Seite; `DoctorReportPdf` rendert es mit Androids `PdfDocument` auf DIN A4 (595 × 842 pt) und paginiert Inhalte, statt sie abzuschneiden.

- **Kopf:** Titel, optionaler Name aus den Einstellungen, Auswertungszeitraum, Erstellungsdatum.
- **A. Symptome:** Anzahl, Tage mit Sodbrennen, Nächte mit Reflux (22–6 Uhr), Ø Intensität, symptomfreie/dokumentierte Tage, häufigstes 5-Stunden-Fenster, Tageszeit-Heatmap.
- **B. Therapie/Medikamente:** nur dokumentierte Einnahmen (Maßnahme mit „Medikament“ im Namen), dokumentierte Präparate, Einnahmezeiten, Zeitraum und dokumentierte Häufigkeit. Dosierung, geplante Häufigkeit und Einnahmetreue bleiben „nicht erfasst/nicht berechenbar“, weil kein Einnahmeplan modelliert ist.
- **C. Verlauf:** Burden-Chart mit Lücken, Spitzen, lineare Tendenz sowie Halbzeitvergleich über dokumentierte Tage.
- **D. Beobachtete Zusammenhänge:** belastbare Faktoren (mind. 3 Mahlzeiten je Gruppe) mit relativer Veränderung, Mit/Ohne-Wahrscheinlichkeit und Datenqualität; darunter die data-getriebenen Hinweise.
- **E. GerdQ:** Das Datenmodell enthält keinen GerdQ; der Abschnitt weist das explizit aus und schätzt keine Werte.
- **Fußzeile auf jeder Seite:** „Dieser Bericht dokumentiert subjektive Symptome und beobachtete statistische Zusammenhänge. Er stellt keine medizinische Diagnose dar.“
- **Speichern/Teilen:** `ActivityResultContracts.CreateDocument("application/pdf")` bzw. `FileProvider` (`cacheDir/exports/`); die Datei verlässt das Gerät nur durch eine bewusste Nutzeraktion.

## Build & Run

Voraussetzungen: JDK 17+, Android SDK mit Platform 36 und Build-Tools 36.0.0.

```bash
# Debug-Build
./gradlew :app:assembleDebug

# Debug-APK installieren (Gerät via USB angeschlossen)
./gradlew :app:installDebug

# Unit-Tests
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug
```

`local.properties` muss den Pfad zum Android-SDK enthalten (`sdk.dir=/Pfad/zu/Android/sdk`).

## Datenbank-Migrationen

- Das Room-Schema wird nach `app/schemas/` exportiert (`exportSchema = true`).
- Jede Schemaänderung braucht: Version erhöhen, `Migration(n, n+1)` in `AppDatabase` registrieren und das neue Schema-JSON mitcommitten.
- Es gibt bewusst **kein** `fallbackToDestructiveMigration`: Eine fehlende Migration schlägt beim Öffnen laut fehl, statt Daten still zu löschen.

### Schema-Version 2 (Multi-Select)

- `symptoms.symptomTypesJson` enthält alle gewählten Symptomarten als JSON-Liste; `symptoms.symptomType` bleibt die **primäre** Art, damit Analyse und CSV-Export kompatibel bleiben.
- `activities` und `remedies` sind Kataloge für Tätigkeiten und Maßnahmen. Eigene Einträge werden mit `isCustom = true` gespeichert und beim App-Start über `RefluxRepository.ensureDefaultCatalogs()` ergänzt.
- `symptom_activity_cross_ref` / `symptom_remedy_cross_ref` verknüpfen einen Eintrag mit beliebig vielen Aktivitäten bzw. Maßnahmen. Löschen erfolgt über `ON DELETE CASCADE`.
- `MIGRATION_1_2` legt die neuen Tabellen an und befüllt `symptomTypesJson` aus dem bisherigen `symptomType`. `Migration1To2Test` baut dafür eine echte v1-Datenbank auf und prüft Daten­erhalt, Backfill und die neuen DAOs.

### Schema-Version 3 (Kategorien und Gewohnheiten)

- `foods.categories` speichert die manuell zugewiesenen Analyse-Kategorien als kommagetrennte stabile Schlüssel (`fat`, `spicy`, …, `large_portion`). Die Anzeige-Labels kommen aus `FoodCategories`; die Auswertung leitet **keine** Kategorie aus einem Namen ab.
- `habit_overrides` speichert ausschließlich Nutzeranpassungen (`signature`, `customName`, `customFoods`, `isDeleted`). Die Habits selbst werden aus den Mahlzeiten berechnet.
- `MIGRATION_2_3` fügt beide Spalten/Tabellen hinzu und befüllt `foods.categories` nur für exakt benannte Lebensmittel des mitgelieferten Startkatalogs, sofern die Spalte noch leer ist. Bestehende, nicht kategorisierte Lebensmittel bleiben unverändert gültig.
- `Migration2To3Test` baut eine echte v2-Datenbank auf und prüft Daten­erhalt, Backfill, leere Kategorien bei eigenen Lebensmitteln sowie die neuen DAOs.

### Bugfixes 2026-11-23

- **Gewohnheiten-Verwaltung:** Gleiche Lebensmittel zu unterschiedlichen Tageszeiten erzeugten zwei Habit-Cluster mit identischer Signatur. Die doppelten Schlüssel ließen `LazyColumn` beim Öffnen und nach dem Löschen abstürzen. Die Signatur enthält jetzt einen 3-Stunden-Zeitblock; Kollisionen werden vor der Ausgabe zusammengeführt. Overrides im alten Format bleiben gültig.
- **Lebensmittel-Kategorien:** Süß, Deftig, Frittiert, Stark würzig, Schwarz-/Grüntee, Milchprodukt und Sehr heiß ergänzt. Bestehende Schlüssel, gespeicherte Kategorien und die Auswertungslogik bleiben unverändert; die Chips erscheinen automatisch in allen Kategorie-Auswahlen.

### Änderungen dieser Ausbaustufe

Geänderte/neue Dateien (Auszug):

- Datenbank: `data/local/Migrations.kt`, `AppDatabase.kt`, `FoodDao.kt`, `HabitOverrideDao.kt`, `InitialFoodData.kt`, `schemas/.../3.json`, `data/model/Entities.kt`, `FoodCategories.kt`, `data/repository/RefluxRepository.kt`
- Analyse: `analysis/RefluxAnalyticsService.kt`, `analysis/HabitDetector.kt`
- UI/Logik: `ui/RefluxViewModel.kt`, `ui/meal/AddMealScreen.kt`, `ui/food/FoodsScreen.kt`, `ui/habit/HabitsScreen.kt`, `ui/analysis/AnalysisScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/navigation/RefluxNavHost.kt`, `util/DateTimeUtils.kt`, `AndroidManifest.xml`
- Bericht: `report/DoctorReportBuilder.kt`, `report/DoctorReportPdf.kt`, `report/ReportSettings.kt`
- Tests: `FoodCategoriesTest`, `DateTimeUtilsLocalDayTest`, `HabitDetectorTest`, `HabitsScreenTest`, `RefluxAnalyticsExtensionsTest`, `DoctorReportBuilderTest`, `Migration2To3Test`, `Migration1To2Test` (v3-Erwartung), `SettingsReportTest`, `RefluxViewModelDateTest`, `AnalysisScreenTest` (neue Karten)

Tests: `./gradlew :app:testDebugUnitTest` → 137 Tests, 0 Fehler, 1 übersprungen (SQLCipher-Nativtest unter Robolectric). Lint und `assembleDebug` laufen erfolgreich.

Offene Punkte:

- Package-Rename `com.example` → `com.dp.refluxtracker`
- GerdQ-Erfassung (der Bericht weist den Abschnitt bis dahin als „keine Daten“ aus)
- Medikamentenplan (Dosis, geplante Häufigkeit) für eine echte Einnahmetreue-Berechnung
- PDF-Layout auf weiteren Gerätegrößen/Sprachen visuell prüfen; `PdfDocument` ist unter Robolectric nicht testbar

### CSV-Import

- Unterstützt das aktuelle Exportformat (`# MAHLZEITEN` / `# SYMPTOME`, Komma-getrennt) und alte Semicolon-Backups (`TYP;DATUM;UHRZEIT;TIMESTAMP_MS;…`).
- Android-Bluetooth-Weiterleitungen, die die CSV in eine kleine HTML-Seite einbetten, werden automatisch entpackt.
- Der Import läuft in einer einzigen Room-Transaktion. Bereits vorhandene Einträge mit gleichem Zeitpunkt (minutengenau) und Inhalt werden übersprungen, damit ein Backup nicht versehentlich doppelt importiert wird.
- Unbekannte Lebensmittel werden mit der Kategorie `Import`, unbekannte Tätigkeiten/Maßnahmen als eigene Katalogeinträge angelegt.
- Alte Symptomnamen werden auf die aktuellen Kategorien abgebildet (z. B. „Reizhusten“ → „Husten“, „Druckgefühl / Druck in der Brust“ → „Druckgefühl“). Unbekannte Namen bleiben unverändert erhalten.
- Alte Mahlzeitentypen werden übernommen; `GETRAENK` wird zu „Getränk“, `MITTEL` zu „Normal“ und `KLEIN` zu „Klein“.

## Datenschutz

- **Keine Netzwerk-Berechtigung** im Manifest
- **Kein Auto-Backup** der Datenbank in Google Drive (`allowBackup="false"`, plus explizite Excludes in `backup_rules.xml` und `data_extraction_rules.xml`)
- **Keine Analytics / Crash-Reporting / Firebase**
- DB-Name: `refluxtrack_database` (Room)
- Bei Deinstallation der App werden alle Daten gelöscht

## Roadmap

Offen (kein fester Zeitplan):

- **Package-Rename** `com.example` → `com.dp.refluxtracker` (Quellcode-Migration)

Bereits umgesetzt:

- SQLCipher-Verschlüsselung, Backup-Regeln und DB-Excludes
- CSV-Export und -Import inklusive Legacy-Format und HTML-Weiterleitungen
- Tägliche Erinnerung inklusive Boot-/Update-Wiederherstellung
- Symptom-spezifische Analysefenster und Filterung von Paaren ohne Symptomereignis
- Fixed-Window-Analytics-Service für 7/14/30/365 Tage inkl. Portionsgewichtung, Tätigkeiten- und Maßnahmen-Auswertung sowie UI-Zeitraumumschalter
- Tastatur-sichere Lebensmittelauswahl, rückwirkende Tagesansicht, Gewohnheiten, Lebensmittel-Kategorien
- Kategorie-Korrelationen/Trigger-Matrix, Tageszeit-Heatmap, GERD-Burden-Verlauf und PDF-Arztbericht

## Lizenz

Privatprojekt, kein öffentlicher Vertrieb.

# RefluxTracker

Persönlicher Reflux- und GERD-Tracker für Android. Lokal, ohne Cloud, ohne Tracker — alle Daten bleiben auf dem Gerät.

## Zweck

Hilft, persönliche Auslöser für Reflux / Sodbrennen zu erkennen, indem man Mahlzeiten und Symptome im Alltag erfasst und die App anschließend Korrelationen sichtbar macht.

## Features

- **Mahlzeiten erfassen** — Zeitpunkt, Mahlzeit-Typ (Frühstück, Mittag-, Abendessen, Snack), Portionsgröße, Notiz, zugeordnete Lebensmittel
- **Symptome erfassen** — Zeitpunkt, Typ (Sodbrennen, Säurerückfluss, Aufstoßen, Räuspern, Husten, Halsbrennen, Druckgefühl, Übelkeit, Blähungen, Schlafprobleme), Intensität (0–10), Dauer, Notiz
- **Timeline** — Tagesansicht aller Mahlzeiten und Symptome in chronologischer Reihenfolge
- **Dashboard** — Tages-Zähler, Durchschnitts-Intensität, Top-Trigger der letzten 14 Tage
- **Analyse** — Statistische Trigger-Analyse: welche Lebensmittel wie oft mit Symptomen korrelieren
- **Lebensmittel** — Vordefinierte Liste (deutsche Bezeichnungen), erweiterbar um eigene Einträge
- **Local-only** — Keine Cloud, kein Account, kein Tracking, keine Berechtigungen außer lokalem Speicher

## Architektur

- **Sprache:** Kotlin 2.2
- **UI:** Jetpack Compose + Material 3
- **Persistenz:** Room (SQLite)
- **DI:** Keine — explizite Factories (`RefluxRepository.create`, `RefluxViewModel.provideFactory`)
- **Build:** Gradle (Kotlin DSL) mit Version Catalog (`gradle/libs.versions.toml`)
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36 (Android 16)

```
app/src/main/java/com/example/
├── MainActivity.kt
├── analysis/         # Trigger-Korrelations-Engine
├── data/
│   ├── local/        # Room: AppDatabase, DAOs, InitialFoodData
│   ├── model/        # Entities, MealTypes, SymptomTypes
│   └── repository/   # RefluxRepository, TimelineEntry
├── ui/
│   ├── RefluxViewModel.kt
│   ├── analysis/     # AnalysisScreen
│   ├── components/   # Wiederverwendbare Compose-Komponenten
│   ├── dashboard/    # DashboardScreen
│   ├── food/         # FoodsScreen
│   ├── meal/         # AddMealScreen
│   ├── navigation/   # RefluxNavHost
│   ├── symptom/      # AddSymptomScreen
│   ├── theme/        # Color, Type, Theme
│   └── timeline/     # TimelineScreen
└── util/             # DateTimeUtils
```

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

## Datenschutz

- **Keine Netzwerk-Berechtigung** im Manifest
- **Kein Auto-Backup** der Datenbank in Google Drive (`allowBackup="false"`, plus explizite Excludes in `backup_rules.xml` und `data_extraction_rules.xml`)
- **Keine Analytics / Crash-Reporting / Firebase**
- DB-Name: `refluxtrack_database` (Room)
- Bei Deinstallation der App werden alle Daten gelöscht

## Roadmap

Geplant (kein fester Zeitplan):

- **Backup-Regeln:** schon umgesetzt (DB-Excludes) — weitergehende SQLCipher-Verschlüsselung als Option
- **CSV-Export** der eigenen Daten
- **Reminders** für Mahlzeiten-Erfassung
- **Verbesserte Analyse-Engine** — symptom-spezifische Zeitfenster, robustere Tests
- **Package-Rename** `com.example` → `com.dp.refluxtracker` (Quellcode-Migration)

## Lizenz

Privatprojekt, kein öffentlicher Vertrieb.

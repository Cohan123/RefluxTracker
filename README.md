# RefluxTracker

Persönlicher Reflux- und GERD-Tracker für Android – hilfreich zur Identifizierung individueller Trigger anhand von Mahlzeiten- und Symptomprotokollen.

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4)
![Android](https://img.shields.io/badge/Android-minSdk%2026-3DDC84)
![Room](https://img.shields.io/badge/Room-2.7.0-003B57)

## Features

- **Mahlzeiten erfassen**: Typ (Frühstück/Mittagessen/Abendessen/Snack/Getränk), Portionsgröße, Uhrzeit, Lebensmittel aus Datenbank + Freitext, Notizen
- **Symptome protokollieren**: Typ (Sodbrennen, saures Aufstoßen, Magendruck, Halsschmerzen, Heiserkeit, Reizhusten u.a.), Intensität 1-10, Dauer, Zeitpunkt, Tätigkeiten/Situationen, ergriffene Gegenmaßnahmen
- **Lebensmitteldatenbank**: ~100 vorbefüllte Lebensmittel, eigene Einträge, Kategorien, Portionsgrößen
- **Timeline**: Chronologische Übersicht aller Mahlzeiten und Symptome, filterbar, mit Zeitabstand
- **Dashboard**: Tagesübersicht, letzte Mahlzeiten/Symptome, schnelle Navigation
- **Analyse – Trigger-Erkennung** (`TriggerAnalysisEngine`): Korrelation zwischen Lebensmitteln und Symptomen in Zeitfenster (z. B. 4 h), Trefferquote, Ø Intensität & Ø Delay, Likelihood-Einstufung (Hoch/Mittel/Gering/Unbekannt)
- **CSV Backup & Restore**: Export/Import aller Daten (`CsvBackupManager`), inkl. BackupScreen
- **Offline-first**: Room-Datenbank (`RefluxDatabase`), keine Cloud-Abhängigkeit

## Screenshots

> `app/src/test/screenshots/greeting.png` enthält einen Compose-Preview-Screenshot.

## Projektstruktur

```
app/src/main/java/com/example/
├── MainActivity.kt
├── analysis/TriggerAnalysisEngine.kt
├── data/local/
│   ├── AppDatabase.kt / RefluxDatabase.kt / RefluxDao.kt
│   ├── FoodDao.kt / MealDao.kt / SymptomDao.kt
│   ├── InitialFoodData.kt
│   └── RefluxDao.kt
├── data/model/Entities.kt
├── data/repository/RefluxRepository.kt
├── ui/
│   ├── RefluxViewModel.kt
│   ├── navigation/RefluxNavHost.kt
│   ├── dashboard/DashboardScreen.kt
│   ├── meal/AddMealScreen.kt
│   ├── symptom/AddSymptomScreen.kt
│   ├── timeline/TimelineScreen.kt
│   ├── analysis/AnalysisScreen.kt
│   ├── food/FoodsScreen.kt
│   ├── backup/BackupScreen.kt
│   └── components/
├── util/CsvBackupManager.kt / DateTimeUtils.kt
└── ui/theme/
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3 + Navigation Compose
- **DB**: Room 2.7.0 + KSP (`2.2.10-1.0.29`)
- **Async**: Kotlin Coroutines 1.10.2
- **Build**: Android Gradle Plugin 9.1.1, Gradle 9.3.1, compileSdk 36, minSdk 26, targetSdk 35, Java 17

## APK Download

Die fertige APK liegt als **`RefluxTracker.apk`** im Repository-Root (Debug-Build, 17 MB):

- Direkt-Download: [`RefluxTracker.apk`](./RefluxTracker.apk)
- Auf Gerät installieren: `adb install RefluxTracker.apk` oder per Datei-Manager sideloaden (Unbekannte Quellen erlauben)

> Hinweis: Dies ist ein Debug-signierter Build. Für Play-Store-Release eigenes Keystore und `release`-Build verwenden.

## Lokal bauen

**Voraussetzungen**: Android Studio, Android SDK (Platform 36, Build-Tools 36), JDK 17

```bash
git clone https://github.com/Cohan123/RefluxTracker.git
cd RefluxTracker

# SDK-Pfad setzen (falls nicht automatisch)
echo "sdk.dir=/pfad/zum/Android/Sdk" > local.properties

# Debug-APK bauen (ergibt app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug

# Release-APK (unsigniert/minify off)
./gradlew assembleRelease

# Umbenennen wie im Repo
cp app/build/outputs/apk/debug/app-debug.apk RefluxTracker.apk
```

Alternative mit System-Gradle (wie in diesem Repo gebaut):
```bash
export JAVA_HOME=/pfad/zu/jdk17
export ANDROID_HOME=/pfad/zu/Android/Sdk
gradle assembleDebug
```

## Tests

```bash
./gradlew test                 # Unit-Tests (u. a. TriggerAnalysisEngineTest)
./gradlew connectedAndroidTest # Instrumentation (Gerät/Emulator erforderlich)
```

## Konfiguration

- `.env.example` zeigt Platzhalter für `GEMINI_API_KEY` (falls Gemini-Features genutzt werden). Für den reinen Tracker nicht erforderlich.
- `metadata.json` und `public/assets/` stammen aus dem AI-Studio-Export.

## Lizenz

Kein explizites Lizenzfile – bei Bedarf MIT/Apache-2.0 ergänzen.

---

Erstellt via AI Studio (App-ID `83d21a6a-885a-44b2-9454-8bea72242145`), weiterentwickelt als native Android-App.

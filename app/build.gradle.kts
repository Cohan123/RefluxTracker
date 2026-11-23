import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.dp.refluxtracker"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.dp.refluxtracker"
    minSdk = 24
    targetSdk = 36
    versionCode = 2
    versionName = "1.0.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Only ship native libraries for the ABIs real devices use today. SQLCipher's
    // libsqlcipher.so is ~5-7 MB per ABI; without an explicit filter AGP would
    // either strip them or include all four and balloon the APK by ~20 MB.
    ndk {
      abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
    }
  }

  signingConfigs {
    // Debug signing config: the AGP-provided "debug" config already points at
    // ~/.android/debug.keystore, but we want the checked-in debug.keystore so
    // debug builds are reproducible across machines without any Android-Studio
    // first-launch state. Overwrite its properties instead of re-declaring.
    getByName("debug") {
      storeFile = rootProject.file("debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    // Release: keystore + passwords must be supplied via env vars (KEYSTORE_PATH,
    // STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD). The release build type wires this
    // in only when KEYSTORE_PATH is present, so a missing release keystore does
    // not block local debug builds.
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")
      if (keystorePath != null) {
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Only attach a release signing config if a keystore was actually configured
      // (i.e. the KEYSTORE_PATH env var was set). Without this, a missing release
      // keystore would block local debug builds.
      signingConfig = if (signingConfigs.findByName("release")?.storeFile != null) {
        signingConfigs.getByName("release")
      } else {
        signingConfigs.getByName("debug")
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = false
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }

  packaging {
    resources {
      excludes += setOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "/META-INF/DEPENDENCIES",
        "/META-INF/LICENSE*",
        "/META-INF/NOTICE*"
      )
    }
  }
}

ksp {
  // Export the Room schema of every version so migrations can be reviewed and
  // tested (MigrationTestHelper). Commit the generated JSON together with the
  // migration that produced it.
  arg("room.schemaLocation", "${projectDir}/schemas")
}

dependencies {
  // AndroidX core
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.navigation.compose)

  // Room (local persistence)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  "ksp"(libs.androidx.room.compiler)

  // SQLCipher (optional local encryption of the Room database)
  // Disable for plaintext SQLite: ./gradlew :app:assembleDebug -PenableSqlcipher=false
  val enableSqlcipher = (project.findProperty("enableSqlcipher") ?: "true").toString().toBoolean()
  if (enableSqlcipher) {
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
  }

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)

  // Unit tests
  testImplementation(libs.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.compose.ui.test.junit4)

  // Instrumented tests
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.runner)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

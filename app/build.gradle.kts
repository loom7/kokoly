// SPDX-License-Identifier: GPL-3.0-or-later
// AGP 9 bringt Kotlin eingebaut mit; org.jetbrains.kotlin.android obendrauf ist ein Fehler.
plugins {
    id("com.android.application")
}

android {
    namespace = "de.tilly.kokoly.tts"
    compileSdk = 37

    defaultConfig {
        // ADR-0011: die applicationId ist die Engine-Identität — sie wandert nie.
        applicationId = "de.tilly.kokoly.tts"
        // ADR-0008: minSdk 26 — Voice-API + rangeStart ohne Verzweigungen; ORT verlangt >=24.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Nutzer-Entscheid F5: Geräte, die 300-MiB-Modelle tragen, sind arm64.
            abiFilters += "arm64-v8a"
        }

        externalNativeBuild {
            cmake {
                // Nur der JNI-Wrapper wird gebaut — espeak-ng haengt als
                // statische Bibliothek daran (FetchContent, Tag 1.52.0).
                targets += "kokoly_jni"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }

    // Die zugeschnittene voices-Datei ist unkomprimiert im APK, damit sie per
    // Random-Access gelesen werden kann (522 KB je Stimme, lazy).
    androidResources {
        noCompress += "bin"
    }

    signingConfigs {
        // Release-Signatur NUR aus der Umgebung (CI-Secrets bzw. lokale
        // Shell) — Schlüsselmaterial liegt nie im Repo. Ohne die Variablen
        // bleibt das Release unsigniert; der Release-Workflow lädt es dann
        // nicht hoch.
        create("release") {
            System.getenv("KOKOLY_KEYSTORE")?.takeIf { it.isNotBlank() }?.let {
                storeFile = file(it)
                storePassword = System.getenv("KOKOLY_KEYSTORE_PASS")
                keyAlias = System.getenv("KOKOLY_KEY_ALIAS")
                keyPassword = System.getenv("KOKOLY_KEY_PASS")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile != null }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // 1.28.0: die auf GENAU diesem Zielgerät (SM-F971B/SM8850) im Nova-Projekt
    // bewiesene Version. 1.23.2 (ADR-0008-Erstwahl) stürzt dort mit SIGILL in
    // libonnxruntime ab — Instruktions-Dispatch-Falle des sehr neuen SoC.
    // ADR-0008-Nachtrag: docs/adr/, Messbeleg in docs/erkenntnisse.md.
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.28.0")

    testImplementation("junit:junit:4.13.2")
    // org.json ist auf dem Gerät Teil der Plattform; für JVM-Tests kommt es als Bibliothek.
    testImplementation("org.json:json:20240303")

    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

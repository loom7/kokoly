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
        versionName = "0.1.0-m0"

        ndk {
            // Nutzer-Entscheid F5: Geräte, die 300-MiB-Modelle tragen, sind arm64.
            abiFilters += "arm64-v8a"
        }
    }

    // Die zugeschnittene voices-Datei ist unkomprimiert im APK, damit sie per
    // Random-Access gelesen werden kann (522 KB je Stimme, lazy).
    androidResources {
        noCompress += "bin"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // ADR-0008: 1.23.2 gepinnt — erste 16-KB-Page-kompatible Version bei noch
    // 19,3 MiB arm64; neuere Versionen sind deutlich gewachsen.
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.23.2")

    testImplementation("junit:junit:4.13.2")
}

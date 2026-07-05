plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    // Diubah sesuai identitas project Auto Clicker RYCL
    namespace = "com.rycl.autoclicker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rycl.autoclicker"
        minSdk = 26 // Android 8.0 (Oreo) ke atas untuk kestabilan overlay & background services
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.jks") 
            storePassword = System.getenv("RELEASE_STORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true // Diaktifkan agar kode di-minify & aman
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core Android & Kotlin KTX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // UI Layout Standar (XML, Material, CardView yang kita pakai di activity_main)
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")

    // Google ML Kit Text Recognition (Otak deteksi angka pintar di layar)
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // Coroutines untuk proses deteksi asinkronous biar ga lag/ghost touch
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Pengujian (Testing)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

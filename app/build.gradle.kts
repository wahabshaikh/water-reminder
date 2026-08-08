import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Sippy is signed with a checked-in hobby keystore so that every build — yours,
// a friend's, or CI's — produces an APK Android accepts as an *upgrade* of the
// previous one. Set SIPPY_KEYSTORE / SIPPY_KEYSTORE_PASSWORD / SIPPY_KEY_ALIAS /
// SIPPY_KEY_PASSWORD (env or gradle properties) to sign with your own key instead.
val keystorePath = providers.environmentVariable("SIPPY_KEYSTORE").orNull
    ?: rootProject.file("keystore/sippy.jks").takeIf { it.exists() }?.absolutePath

android {
    namespace = "com.sippy.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sippy.app"
        // API 26 lets us use java.time directly, without library desugaring.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("sippy") {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("SIPPY_KEYSTORE_PASSWORD").orNull ?: "sippysippy"
                keyAlias = providers.environmentVariable("SIPPY_KEY_ALIAS").orNull ?: "sippy"
                keyPassword = providers.environmentVariable("SIPPY_KEY_PASSWORD").orNull ?: "sippysippy"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("sippy")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}

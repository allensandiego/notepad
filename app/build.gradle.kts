import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val bugsnagApiKey = localProperties.getProperty("bugsnag.api.key") ?: "YOUR_BUGSNAG_API_KEY"
val admobAppId = localProperties.getProperty("admob.app_id") ?: "YOUR_ADMOB_APP_ID"
val admobBannerId = localProperties.getProperty("admob.banner_ad_unit_id") ?: "YOUR_ADMOB_BANNER_ID"
val admobNativeId = localProperties.getProperty("admob.native_ad_unit_id") ?: "YOUR_ADMOB_NATIVE_ID"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.allensandiego.notepad"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.allensandiego.notepad"
        minSdk = 26
        targetSdk = 36
        versionCode = 18
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["bugsnagApiKey"] = bugsnagApiKey
        manifestPlaceholders["admobAppId"] = admobAppId

        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$admobBannerId\"")
        buildConfigField("String", "ADMOB_NATIVE_AD_UNIT_ID", "\"$admobNativeId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Bugsnag Crash Reporting
    implementation(libs.bugsnag.android)

    // Ktor HTTP Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)



    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // BugSnag
    implementation(libs.bugsnag.android)
    implementation(libs.bugsnag.android.performance)

    // AdMob
    implementation(libs.play.services.ads)
}
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

fun configuredValue(name: String): String? =
    providers.gradleProperty(name).orNull ?: providers.environmentVariable(name).orNull

val defaultVersionCode = 1005
val defaultVersionName = "0.8.3"
val overrideVersionCode = providers.gradleProperty("versionCodeOverride").orNull?.toIntOrNull()
val overrideVersionName = providers.gradleProperty("versionNameOverride").orNull

val bundledStoreFile = file("keystore/redcode-release.jks")
val bundledStorePassword = "redcode-release"
val bundledKeyAlias = "redcode"
val bundledKeyPassword = "redcode-release"

val releaseStorePath = configuredValue("REDCODE_RELEASE_STORE_FILE")
    ?: bundledStoreFile.absolutePath.takeIf { bundledStoreFile.isFile }
val releaseStorePassword = configuredValue("KEYSTORE_PASSWORD") ?: configuredValue("REDCODE_RELEASE_STORE_PASSWORD") ?: bundledStorePassword
val releaseKeyAlias = configuredValue("KEY_ALIAS") ?: configuredValue("REDCODE_RELEASE_KEY_ALIAS") ?: bundledKeyAlias
val releaseKeyPassword = configuredValue("KEY_PASSWORD") ?: configuredValue("REDCODE_RELEASE_KEY_PASSWORD") ?: bundledKeyPassword

android {
    namespace = "com.redcode.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.redcode.app"
        minSdk = 26
        targetSdk = 34
        versionCode = overrideVersionCode ?: defaultVersionCode
        versionName = overrideVersionName ?: defaultVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }

        buildConfigField("String", "OPENCODE_VERSION", "\"2.3.2\"")
        buildConfigField("String", "DEFAULT_SERVER_PORT", "\"49374\"")
        buildConfigField("String", "DEFAULT_SERVER_HOST", "\"127.0.0.1\"")
        buildConfigField("String", "UBUNTU_ROOTFS_URL", "\"https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.4-base-arm64.tar.gz\"")
        buildConfigField("String", "UBUNTU_ROOTFS_SHA256", "\"e48adafaa53f19f648da1894afe02b8347b4f8d821747ae3eae5181be96476c9\"")
    }

    signingConfigs {
        create("release") {
            if (releaseStorePath != null) {
                storeFile = file(releaseStorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
        getByName("debug") {
            if (releaseStorePath != null) {
                storeFile = file(releaseStorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.3")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // OkHttp & Retrofit & SSE
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Commons Compress for RootFS tar.gz extraction
    implementation("org.apache.commons:commons-compress:1.26.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

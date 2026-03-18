plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}
android {
    namespace = "com.meshapp.meshchat"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.meshapp.meshchat"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        python {
            buildPython("python3")
            pip {
                install("rns==0.6.7")
                install("lxmf==0.4.4")
                install("pyserial")
            }
        }
        ndk { abiFilters("armeabi-v7a", "arm64-v8a") }
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}

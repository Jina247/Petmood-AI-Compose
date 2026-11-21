import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Code shared by the Android and iOS apps: API client, models, session storage, repositories.
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  android {
    namespace = "com.example.shared"
    compileSdk = 36
    minSdk = 24
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
  }

  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.client.content.negotiation)
      implementation(libs.ktor.serialization.kotlinx.json)
      api(libs.kotlinx.serialization.json)
      api(libs.multiplatform.settings)
    }
    androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
    iosMain.dependencies { implementation(libs.ktor.client.darwin) }
  }
}

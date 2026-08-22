import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appVersionCode = providers.gradleProperty("APP_VERSION_CODE")
  .orElse("1")
  .get()
  .toIntOrNull()
  ?: error("APP_VERSION_CODE must be a positive integer")
require(appVersionCode > 0) { "APP_VERSION_CODE must be positive" }
val appVersionName = providers.gradleProperty("APP_VERSION_NAME")
  .orElse("1.0.0")
  .get()
  .trim()
require(appVersionName.isNotBlank()) { "APP_VERSION_NAME must not be blank" }

val releaseKeystorePath = providers.environmentVariable("RELEASE_KEYSTORE_PATH")
  .orNull
  ?.trim()
  .orEmpty()
val releaseStorePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD")
  .orNull
val releaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS")
  .orNull
  ?.trim()
  .orEmpty()
val releaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD")
  .orNull
val releaseSigningConfigured = releaseKeystorePath.isNotBlank() &&
  !releaseStorePassword.isNullOrBlank() &&
  releaseKeyAlias.isNotBlank() &&
  !releaseKeyPassword.isNullOrBlank()
val releaseSigningRequired = providers.gradleProperty("REQUIRE_RELEASE_SIGNING")
  .orElse("false")
  .get()
  .toBoolean()
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.generateKotlin", "true")
  arg("room.incremental", "true")
}

android {
  namespace = "com.aistudio.shreeshyamstore.pqwzkb"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.shreeshyamstore.pqwzkb"
    minSdk = 24
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      if (releaseSigningConfigured) {
        storeFile = file(releaseKeystorePath)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      buildConfigField("String", "BUILD_ENVIRONMENT", "\"production\"")
      buildConfigField("Boolean", "CLOUD_SYNC_ENABLED", "true")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (releaseSigningConfigured) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      buildConfigField("String", "BUILD_ENVIRONMENT", "\"debug\"")
      buildConfigField("Boolean", "CLOUD_SYNC_ENABLED", "false")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

val releaseMinifyEnabled = android.buildTypes.getByName("release").isMinifyEnabled
val releaseBuildEnvironment = android.buildTypes.getByName("release")
  .buildConfigFields["BUILD_ENVIRONMENT"]?.value
val releaseCloudSyncFlag = android.buildTypes.getByName("release")
  .buildConfigFields["CLOUD_SYNC_ENABLED"]?.value
val debugBuildEnvironment = android.buildTypes.getByName("debug")
  .buildConfigFields["BUILD_ENVIRONMENT"]?.value
val debugCloudSyncFlag = android.buildTypes.getByName("debug")
  .buildConfigFields["CLOUD_SYNC_ENABLED"]?.value

abstract class VerifyReleaseConfigurationTask : DefaultTask() {
  @get:Input
  abstract val versionCode: Property<Int>

  @get:Input
  abstract val versionName: Property<String>

  @get:Input
  abstract val minifyEnabled: Property<Boolean>

  @get:Input
  abstract val releaseEnvironment: Property<String>

  @get:Input
  abstract val releaseCloudSyncEnabled: Property<String>

  @get:Input
  abstract val debugEnvironment: Property<String>

  @get:Input
  abstract val debugCloudSyncEnabled: Property<String>

  @get:Input
  abstract val signingRequired: Property<Boolean>

  @get:Input
  abstract val signingConfigured: Property<Boolean>

  @get:Input
  abstract val keystorePath: Property<String>

  @TaskAction
  fun verify() {
    check(versionCode.get() > 0) { "Release versionCode must be positive" }
    check(versionName.get().isNotBlank()) { "Release versionName must not be blank" }
    check(minifyEnabled.get()) { "Release minification must remain enabled" }
    check(releaseEnvironment.get() == "\"production\"") {
      "Release must be marked as production"
    }
    check(releaseCloudSyncEnabled.get() == "true") {
      "Release cloud sync must be enabled"
    }
    check(debugEnvironment.get() == "\"debug\"") {
      "Debug must be marked as debug"
    }
    check(debugCloudSyncEnabled.get() == "false") {
      "Debug cloud sync must be disabled"
    }
    if (signingRequired.get()) {
      check(signingConfigured.get()) {
        "Release signing is required but RELEASE_KEYSTORE_PATH and release secret variables are incomplete"
      }
      check(File(keystorePath.get()).isFile) {
        "Configured RELEASE_KEYSTORE_PATH does not point to a keystore file"
      }
    }
  }
}

tasks.register<VerifyReleaseConfigurationTask>("verifyReleaseConfiguration") {
  group = "verification"
  description = "Verifies release signing, version, minification, and debug isolation invariants."
  versionCode.set(appVersionCode)
  versionName.set(appVersionName)
  minifyEnabled.set(releaseMinifyEnabled)
  releaseEnvironment.set(releaseBuildEnvironment)
  releaseCloudSyncEnabled.set(releaseCloudSyncFlag)
  debugEnvironment.set(debugBuildEnvironment)
  debugCloudSyncEnabled.set(debugCloudSyncFlag)
  signingRequired.set(releaseSigningRequired)
  signingConfigured.set(releaseSigningConfigured)
  keystorePath.set(releaseKeystorePath)
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

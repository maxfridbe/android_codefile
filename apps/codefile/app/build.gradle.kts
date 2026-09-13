plugins {
    id("com.android.application")
}

// Injected by build.sh; null only for builds outside the build system,
// which then fall back to the default debug signing.
val sharedKeystore = findProperty("appKeystoreFile") as String?

android {
    namespace = "com.TechHurts.CodeFile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.TechHurts.CodeFile"
        minSdk = 24
        targetSdk = 34
        // Injected by build.sh from git (commit count / git describe);
        // the defaults only apply to builds outside the build system.
        versionCode = (findProperty("appVersionCode") as String?)?.toInt() ?: 9751003
        versionName = findProperty("appVersionName") as String? ?: "v26.0912.003"
    }

    if (sharedKeystore != null) {
        signingConfigs.create("shared") {
            storeFile = file(sharedKeystore)
            storePassword = findProperty("appKeystorePassword") as String?
            keyAlias = findProperty("appKeyAlias") as String? ?: "androidbase"
            keyPassword = findProperty("appKeyPassword") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (sharedKeystore != null) {
                signingConfig = signingConfigs.getByName("shared")
            }
        }
        debug {
            // Same key for debug so signatures never change between machines
            // and `adb install -r` upgrades always work.
            if (sharedKeystore != null) {
                signingConfig = signingConfigs.getByName("shared")
            }
        }
    }

    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("io.github.Rosemoe.sora-editor:editor:0.23.4")
    implementation("io.github.Rosemoe.sora-editor:language-textmate:0.23.4")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.window:window-java:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
}

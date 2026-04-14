plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.zjgsu.moveup"
    // Using a stable compileSdk version. 35 is stable. 
    // The error with 36.1 might be due to it being a preview/experimental SDK.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zjgsu.moveup"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

// Move the toolchain configuration to the top level or via the android extension correctly
// Actually, AGP 8.0+ supports toolchains via compileOptions in a specific way, 
// but often setting it via the java extension at the top level works for the project.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.amap.api:3dmap:latest.integration")
}
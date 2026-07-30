@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}


android {
    namespace = "com.ravenhub.app"
    compileSdk = 37

    val moduleProp = file("../../module.prop")
    var mVersionCode = 1
    var mVersionName = "1.0"
    if (moduleProp.exists()) {
        moduleProp.readLines().forEach { line ->
            if (line.startsWith("versionCode=")) {
                mVersionCode = line.substringAfter("=").trim().toIntOrNull() ?: 1
            } else if (line.startsWith("version=")) {
                mVersionName = line.substringAfter("=").trim()
            }
        }
    }

    defaultConfig {
        applicationId = "com.ravenhub.app"
        minSdk = 29
        targetSdk = 35
        versionCode = mVersionCode
        versionName = mVersionName
        vectorDrawables.useSupportLibrary = true
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
        ndk {
            abiFilters.addAll(setOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("ravencore.jks")
            storePassword = "azenith"
            keyAlias = "azenith_key"
            keyPassword = "azenith"
        }
    }
    
    androidResources {
        generateLocaleConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            signingConfig = signingConfigs.getByName("release")
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), 
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols.clear()
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.version"
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin-tooling-metadata.json"
        }
    }

    tasks.withType<PackageAndroidArtifact> {
        doLast {
            outputs.files.files.forEach { file ->
                if (file.isDirectory) {
                    file.walkTopDown().filter { it.extension == "apk" }.forEach { apk ->
                        apk.setReadable(true, false)
                        apk.setWritable(true, false)
                    }
                }
            }
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        checkDependencies = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.core)

    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material.kolor)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.me.zhanghai.android.appiconloader.coil)
    implementation(libs.io.coil.kt.coil.compose)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.io)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.yalantis.ucrop)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.jna)
    implementation(libs.androidx.transition)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.ansi.library)
    implementation(libs.ansi.library.ktx)
    implementation(libs.coil.gif)
    implementation(libs.compose.markdown)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}

plugins {
    id("com.android.application")
}

android {
    namespace = "com.blackberrykeyboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.blackberrykeyboard"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-Wall", "-O2")
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
}

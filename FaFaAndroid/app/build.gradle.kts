import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.jwd.fafa"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.jwd.fafa"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-M00"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 单一配置文件 app/fafa.properties（见文件头注释），缺失时使用下方默认值。
        // 注意：Properties.load(InputStream) 按 ISO-8859-1 解析，这里显式用 UTF-8 读取，
        // 否则中文注释/中文字面值会变成乱码。
        val cfg = Properties().apply {
            val f = project.file("fafa.properties")
            if (f.exists()) {
                f.reader(Charsets.UTF_8).use { load(it) }
            }
        }
        buildConfigField("String", "BASE_URL", "\"${cfg.getProperty("baseUrl", "http://10.0.2.2:8080")}\"")
        buildConfigField("int", "CONNECT_TIMEOUT_SECONDS", cfg.getProperty("connectTimeoutSeconds", "10"))
        buildConfigField("int", "READ_TIMEOUT_SECONDS", cfg.getProperty("readTimeoutSeconds", "30"))
        buildConfigField("boolean", "ENABLE_HTTP_LOG", cfg.getProperty("enableHttpLogging", "true"))
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
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

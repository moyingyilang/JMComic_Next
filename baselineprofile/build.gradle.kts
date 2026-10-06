plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.jmnext.baselineprofile"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // 测试模块镜像 app 的 flavor 维度，否则解析 :app 时 full/lite 会产生变体歧义
    flavorDimensions += "edition"
    productFlavors {
        create("full")
        create("lite")
    }
    targetProjectPath = ":app"
    // 被测变体：release（真实用户拿到的包）；插件会据此选择收集 profile 的目标
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.4.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
}


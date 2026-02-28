plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jmcomic_next.lyqs" // 只改这行！替换成你的包名
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jmcomic_next.lyqs" // 和上面包名一致
        minSdk = 21
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

    // 仅保留DataBinding/ViewBinding，项目必需
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    // Java编译基础配置
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // 保留kotlinOptions仅做jvmTarget配置，警告忽略即可
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

// 仅保留项目必需依赖，无任何多余
dependencies {
    // 基础AndroidX：指定兼容33的版本，拒绝自动升级
    implementation("androidx.core:core-ktx:1.9.0") // 适配33，无SDK36要求
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.0")
    implementation("com.github.bumptech.glide:glide:4.15.0")

    // 布局核心控件：均为兼容33的稳定版
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // 网络+图片加载：无SDK版本限制
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // 测试依赖（可选）
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

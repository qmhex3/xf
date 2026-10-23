plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.frameout.core"
    compileSdk = 37
    defaultConfig { minSdk = 24; consumerProguardFiles("consumer-rules.pro"); testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests.isReturnDefaultValues = true }
}

androidComponents {
    onVariants { variant ->
        variant.androidTest?.sources?.assets?.addStaticSourceDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":codecpack"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.security.crypto)
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.paging)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.heifwriter)
    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.mlkit.objectdetection)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.pose.detection)
    implementation(libs.tesseract4android)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

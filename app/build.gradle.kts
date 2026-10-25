plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val expectedCert = (System.getenv("CERT_SHA256") ?: providers.gradleProperty("certSha256").orNull ?: "")
    .lowercase().filter { it in "0123456789abcdef" }

val storeBuild = providers.gradleProperty("storeBuild").orElse("false").get() == "true"
val grandfather = providers.gradleProperty("grandfatherExisting").orElse("true").get() == "true"
val paywallCutoffMs = providers.gradleProperty("paywallCutoffMs").orNull
    ?: System.currentTimeMillis().toString()
val revenueCatDebugKey = providers.gradleProperty("revenueCatTestKey")
    .orElse("test_sIwyqhzVezorxUrlHNemPlSBEDQ").get()
val revenueCatReleaseKey = System.getenv("REVENUECAT_API_KEY")
    ?: providers.gradleProperty("revenueCatApiKey").orElse("").get()
val revenueCatPaywall = providers.gradleProperty("revenueCatPaywall").orElse("true").get() == "true"
val appProjectPath = project.path

val adsEnabled = providers.gradleProperty("adsEnabled").orElse("false").get() == "true"
val adsDebugEea = providers.gradleProperty("adsDebugEea").orElse("false").get() == "true"
val adsTestDevice = providers.gradleProperty("adsTestDevice").orElse("").get()
val testAdIds = mapOf(
    "app" to "ca-app-pub-3940256099942544~3347511713",
    "banner" to "ca-app-pub-3940256099942544/9214589741",
    "interstitial" to "ca-app-pub-3940256099942544/1033173712",
    "rewarded" to "ca-app-pub-3940256099942544/5224354917"
)

fun adValue(env: String, prop: String): String =
    System.getenv(env) ?: providers.gradleProperty(prop).orElse("").get()

val liveAdIds = mapOf(
    "app" to adValue("ADMOB_APP_ID", "admobAppId"),
    "banner" to adValue("ADMOB_BANNER_ID", "admobBannerId"),
    "interstitial" to adValue("ADMOB_INTERSTITIAL_ID", "admobInterstitialId"),
    "rewarded" to adValue("ADMOB_REWARDED_ID", "admobRewardedId")
)

gradle.taskGraph.whenReady {
    val shipping = allTasks.any { it.path.startsWith("$appProjectPath:") && Regex("(assemble|bundle|package)Release.*").matches(it.name) }
    val adIdsBad = liveAdIds.values.any { it.isBlank() || it.contains("3940256099942544") }
    if (shipping && adsEnabled && adIdsBad) {
        throw GradleException(
            "Release builds with -PadsEnabled=true need your real AdMob IDs: admobAppId, admobBannerId, " +
                "admobInterstitialId, admobRewardedId (or ADMOB_* environment variables). Google test IDs are not allowed in release."
        )
    }
    if (shipping && storeBuild && !revenueCatReleaseKey.startsWith("goog_")) {
        throw GradleException(
            "Store release builds need the RevenueCat Google Play key: pass -PrevenueCatApiKey=goog_... " +
                "(or set REVENUECAT_API_KEY). A blank or test_ key would crash the app."
        )
    }
}

android {
    namespace = "com.frameout.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.frameout.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 24
        versionName = "1.0.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "PLAY_BILLING", storeBuild.toString())
        buildConfigField("boolean", "GRANDFATHER", grandfather.toString())
        buildConfigField("long", "PAYWALL_CUTOFF_MS", "${paywallCutoffMs}L")
        buildConfigField("String", "REVENUECAT_ENTITLEMENT_ID", "\"frame_out_pro\"")
        buildConfigField("boolean", "REVENUECAT_PAYWALL", revenueCatPaywall.toString())
        buildConfigField("boolean", "ADS_ENABLED", adsEnabled.toString())
        buildConfigField("boolean", "ADS_DEBUG_EEA", adsDebugEea.toString())
        buildConfigField("String", "ADS_TEST_DEVICE", "\"$adsTestDevice\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${testAdIds.getValue("banner")}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${testAdIds.getValue("interstitial")}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${testAdIds.getValue("rewarded")}\"")
        manifestPlaceholders["admobAppId"] = testAdIds.getValue("app")
    }

    signingConfigs {
        create("release") {
            val ks = file("release.keystore")
            if (ks.exists()) {
                storeFile = ks
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: "frameout"
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            buildConfigField("String", "CERT_SHA256", "\"$expectedCert\"")
            buildConfigField("String", "REVENUECAT_API_KEY", "\"$revenueCatReleaseKey\"")
            if (adsEnabled) {
                buildConfigField("String", "ADMOB_BANNER_ID", "\"${liveAdIds.getValue("banner")}\"")
                buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${liveAdIds.getValue("interstitial")}\"")
                buildConfigField("String", "ADMOB_REWARDED_ID", "\"${liveAdIds.getValue("rewarded")}\"")
                manifestPlaceholders["admobAppId"] = liveAdIds.getValue("app")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val ks = file("release.keystore")
            if (ks.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            buildConfigField("String", "CERT_SHA256", "\"\"")
            buildConfigField("String", "REVENUECAT_API_KEY", "\"$revenueCatDebugKey\"")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        localeFilters += listOf("en", "ar", "tr", "ru", "fa", "ur", "fr", "pt", "hi", "ko", "ja", "zh", "de", "ro", "es", "uk", "it", "nb", "ms")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/*.version"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin-tooling-metadata.json"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp"
                }
                create("pixel6Api29") {
                    device = "Pixel 6"
                    apiLevel = 29
                    systemImageSource = "aosp"
                }
            }
        }
    }

    splits {
        abi {
            isEnable = !storeBuild
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }
}

androidComponents {
    onVariants { variant ->
        val dir = "$projectDir/src/" + if (adsEnabled) "adsOn/java" else "adsOff/java"
        (variant.sources.kotlin ?: variant.sources.java)?.addStaticSourceDirectory(dir)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":engine"))
    implementation(project(":designsystem"))
    implementation(project(":codecpack"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.revenuecat.purchases)
    implementation(libs.revenuecat.purchases.ui)
    if (adsEnabled) {
        implementation(libs.admob)
        implementation(libs.ump)
    }

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

val releaseKeystore = file("release.keystore")
val allowUnsignedRelease = providers.gradleProperty("allowUnsignedRelease").orElse("false")

val requireReleaseSigning = tasks.register("requireReleaseSigning") {
    val keystoreExists = releaseKeystore.exists()
    val allowed = allowUnsignedRelease.get() == "true"
    val certValid = expectedCert.length == 64
    doLast {
        if (!certValid && !allowed) {
            throw GradleException("CERT_SHA256 is missing or not 64 hex chars: the release signer check would be silently disabled. Set CERT_SHA256 or pass -PallowUnsignedRelease=true.")
        }
        if (!keystoreExists && !allowed) {
            throw GradleException("release.keystore is missing: refusing to build an unsigned release APK. Pass -PallowUnsignedRelease=true to build one on purpose.")
        }
    }
}

tasks.matching { it.name == "packageRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(requireReleaseSigning)
}

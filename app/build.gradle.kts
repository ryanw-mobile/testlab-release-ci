import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import java.io.FileInputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinter)
}

// Configuration
val productApkName = "cidemo"
val productNamespace = "com.rwmobi.githubcidemo"
val isRunningOnCI = System.getenv("CI") == "true"

android {
    namespace = productNamespace

    setupSdkVersionsFromVersionCatalog()
    setupSigningAndBuildTypes()
    setupPackagingResourcesDeduplication()

    defaultConfig {
        applicationId = "com.rwmobi.githubcidemo"

//        testInstrumentationRunner = "$productNamespace.ui.CustomTestRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        animationsDisabled = true

        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        managedDevices {
            allDevices {
                create<ManagedVirtualDevice>("pixel2Api34") {
                    device = "Pixel 2"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))
    testImplementation(libs.junit)
    androidTestImplementation(kotlin("test"))
    androidTestImplementation(kotlin("test-common"))
    androidTestImplementation(kotlin("test-annotations-common"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks {
    // copyBaselineProfileAfterBuild()
    check { dependsOn("detekt") }
    preBuild { dependsOn("formatKotlin") }
}

detekt { parallel = true }

kover {
    useJacoco()
    reports.filters.excludes {
        packages(
            "$productNamespace.ui.*",
            "$productNamespace.di*",
            "$productNamespace.data.repository.demodata*",
        )

        classes(
            "dagger.hilt.internal.aggregatedroot.codegen.*",
            "hilt_aggregated_deps.*",
            "$productNamespace.*.Hilt_*",
            "$productNamespace.*.*_Factory*",
            "$productNamespace.*.*_HiltModules*",
            "$productNamespace.*.*Module_*",
            "$productNamespace.*.*MembersInjector*",
            "$productNamespace.*.*_Impl*",
            "$productNamespace.ComposableSingletons*",
            "$productNamespace.BuildConfig*",
            "$productNamespace.*.Fake*",
            "$productNamespace.*.previewparameter*",
            "$productNamespace.app.ComposableSingletons*",
            "$productNamespace.Application*",
            "*Fragment",
            "*Fragment\$*",
            "*Activity",
            "*Activity\$*",
            "*.databinding.*",
            "*.BuildConfig",
            "*.DebugUtil",
        )
    }
}

// Gradle Build Utilities
private fun BaseAppModuleExtension.setupSdkVersionsFromVersionCatalog() {
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
    }
}

private fun BaseAppModuleExtension.setupPackagingResourcesDeduplication() {
    packaging.resources {
        excludes.addAll(
            listOf(
                "META-INF/*.md",
                "META-INF/proguard/*",
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/LICENSE-notice.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.properties",
                "/*.properties",
            ),
        )
    }
}

private fun BaseAppModuleExtension.setupSigningAndBuildTypes() {
    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())

    signingConfigs {
        create("releaseSigningConfig") {
            // Only initialise the signing config when a Release or Bundle task is being executed.
            // This prevents Gradle sync or debug builds from attempting to load the keystore,
            // which could fail if the keystore or environment variables are not available.
            // SigningConfig itself is only wired to the 'release' build type, so this guard avoids unnecessary setup.
            val isReleaseBuild =
                gradle.startParameter.taskNames.any { it.contains("Release") || it.contains("Bundle") }
            if (isReleaseBuild || isRunningOnCI) {
                val keystorePropertiesFile = file("../../keystore.properties")

                if (isRunningOnCI || !keystorePropertiesFile.exists()) {
                    println("Signing Config: using environment variables")
                    keyAlias = System.getenv("CI_ANDROID_KEYSTORE_ALIAS")
                    keyPassword = System.getenv("CI_ANDROID_KEYSTORE_PRIVATE_KEY_PASSWORD")
                    storeFile = file(System.getenv("KEYSTORE_LOCATION"))
                    storePassword = System.getenv("CI_ANDROID_KEYSTORE_PASSWORD")
                } else {
                    println("Signing Config: using keystore properties")
                    val properties = Properties()
                    InputStreamReader(
                        FileInputStream(keystorePropertiesFile),
                        Charsets.UTF_8,
                    ).use { reader ->
                        properties.load(reader)
                    }

                    keyAlias = properties.getProperty("alias")
                    keyPassword = properties.getProperty("pass")
                    storeFile = file(properties.getProperty("store"))
                    storePassword = properties.getProperty("storePass")
                }
            } else {
                println("Signing Config not created for non-release builds.")
            }
        }
    }

    defaultConfig {
        // Bundle output filename
        setProperty("archivesBaseName", "$productApkName-$versionName-$timestamp")
    }

    buildTypes {
        fun setOutputFileName() {
            applicationVariants.all {
                outputs
                    .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
                    .forEach { output ->
                        val outputFileName = "$productApkName-$name-$versionName-$timestamp.apk"
                        output.outputFileName = outputFileName
                    }
            }
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isDebuggable = true
            setOutputFileName()
        }

        create("demo") {
            matchingFallbacks += listOf("debug")
            applicationIdSuffix = ".demo"
            isMinifyEnabled = false
            isDebuggable = true
            setOutputFileName()
        }

        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            isDebuggable = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                ),
            )
            signingConfig = signingConfigs.getByName("releaseSigningConfig")
            setOutputFileName()
        }
    }
}

private fun TaskContainerScope.copyBaselineProfileAfterBuild() {
    afterEvaluate {
        named("generateReleaseBaselineProfile") {
            doLast {
                val outputFile =
                    File(
                        "$projectDir/src/release/generated/baselineProfiles/baseline-prof.txt",
                    )
                val destinationDir = File("$projectDir/src/main")
                destinationDir.mkdirs()
                val destinationFile = File(destinationDir, outputFile.name)
                println("Moving file $outputFile to $destinationDir")
                outputFile.renameTo(destinationFile)
            }
        }
    }
}

plugins {
    id("com.android.application")
}

val releaseStoreFile = System.getenv("JIJAU_STORE_FILE")
val releaseStorePassword = System.getenv("JIJAU_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("JIJAU_KEY_ALIAS")
val releaseKeyPassword = System.getenv("JIJAU_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.udyamsuite.jijau"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.udyamsuite.jijau"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
}

configurations.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlin:kotlin-stdlib:1.8.22",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.22",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22"
    )
}

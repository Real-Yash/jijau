plugins {
    id("com.android.application")
}

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
}

dependencies {
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}

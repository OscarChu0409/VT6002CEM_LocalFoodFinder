buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}

plugins {
    id("com.android.application") // Only declare this once
    id("com.google.gms.google-services")
}

android {
    // Your existing configuration
}

dependencies {
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-database:20.3.0")
    
    // Other dependencies
}

// Make sure you have the google-services.json file in your app directory
// Download it from Firebase console: Project settings > Your apps > google-services.json



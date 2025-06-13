plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Add this line
}

android {
    // Your existing configuration
}

dependencies {
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-database:20.3.0")
    
    // Other dependencies
    // ...
}
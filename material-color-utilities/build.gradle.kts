plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly("com.google.errorprone:error_prone_core:2.35.1")
    implementation(libs.annotation)
}
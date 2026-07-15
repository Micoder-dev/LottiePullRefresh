import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

// On JitPack the whole Gradle Module Metadata graph (the root `.module` and its per-platform
// redirects) must live under a single group that JitPack actually serves, otherwise KMP variant
// resolution breaks. JitPack sets the `JITPACK` env var, so we switch the group to
// `com.github.<user>.<repo>` there and keep the Maven Central group everywhere else.
val isJitpack = System.getenv("JITPACK").toBoolean()
group = if (isJitpack) "com.github.Micoder-dev.LottiePullRefresh" else "io.github.micoder"
version = System.getenv("VERSION")?.takeIf { it.isNotBlank() } ?: "0.1.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Compottie targets iosArm64 + iosSimulatorArm64 only (no iosX64 / Intel simulator).
    // JitPack builds on Linux and cannot compile Apple targets, so skip them there — the JitPack
    // artifact is Android-only; the full multiplatform build (incl. iOS) is published from macOS.
    if (!isJitpack) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "LottiePullRefresh"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            api(libs.compottie)
        }
    }
}

android {
    namespace = "io.github.micoder.lottiepullrefresh"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)

    coordinates(group.toString(), "lottie-pullrefresh", version.toString())

    pom {
        name.set("Lottie Pull Refresh")
        description.set("A Compose Multiplatform pull-to-refresh with a Lottie animation as the refresh indicator (Android & iOS).")
        inceptionYear.set("2026")
        url.set("https://github.com/Micoder-dev/LottiePullRefresh")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("Micoder-dev")
                name.set("Micoder-dev")
                url.set("https://github.com/Micoder-dev")
            }
        }
        scm {
            url.set("https://github.com/Micoder-dev/LottiePullRefresh")
            connection.set("scm:git:git://github.com/Micoder-dev/LottiePullRefresh.git")
            developerConnection.set("scm:git:ssh://git@github.com/Micoder-dev/LottiePullRefresh.git")
        }
    }
}

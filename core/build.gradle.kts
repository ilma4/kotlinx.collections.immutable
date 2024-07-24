import kotlinx.team.infra.mavenPublicationsPom

plugins {
    id("kotlin-multiplatform")
    `maven-publish`
}

base {
    archivesBaseName = "kotlinx-collections-immutable" // doesn't work
}

mavenPublicationsPom {
    description.set("Kotlin Immutable Collections multiplatform library")
}

kotlin {
    applyDefaultHierarchyTemplate()
    explicitApi()

    // According to https://kotlinlang.org/docs/native-target-support.html
    // Tier 1

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }


    sourceSets.all {
        kotlin.setSrcDirs(listOf("$name/src"))
        resources.setSrcDirs(listOf("$name/resources"))
        languageSettings.apply {
            //            progressiveMode = true
            optIn("kotlin.RequiresOptIn")
        }
    }

    sourceSets {
        val commonMain by getting {
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.google.guava:guava-testlib:18.0")
                implementation("com.code-intelligence:jazzer-api:0.22.1")
                implementation("com.code-intelligence:jazzer-junit:0.16.1")
                implementation("org.junit.jupiter:junit-jupiter:5.9.2")
                implementation(kotlin("reflect"))
            }
        }
    }
}

tasks {
    named("jvmTest", Test::class) {
//        maxHeapSize = "4096m"
        maxHeapSize = "8192m"
//        maxHeapSize = "16384m"
//        maxHeapSize = "32768m"
        useJUnitPlatform()
        testLogging.showStandardStreams = true
    }
}

with(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(rootProject)) {
    nodeVersion = "21.0.0-v8-canary202309167e82ab1fa2"
    nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
}

// Drop this when node js version become stable
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}
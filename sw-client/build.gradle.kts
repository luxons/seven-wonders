plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        browser() // necessary for local dependency from JS UI module
    }
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api(project(":sw-common-model"))
                api("org.hildan.krossbow:krossbow-stomp-kxserialization:1.1.5")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("text-encoding", "0.7.0")) // required by krossbow, because required by kotlinx-io
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

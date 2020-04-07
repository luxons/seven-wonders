plugins {
    val kotlinVersion = "1.3.71"
    kotlin("js") version kotlinVersion apply false
    kotlin("jvm") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion apply false
    id("org.jlleitschuh.gradle.ktlint") version "9.1.1" apply false
}

subprojects {
    repositories {
        // while jcenter sync is broken for krossbow
        maven(url = "https://dl.bintray.com/joffrey-bion/maven")
        jcenter()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
        kotlinOptions.freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
    }

    afterEvaluate {
        // The import ordering expected by ktlint is alphabetical, which doesn't match IDEA's formatter.
        // Since it is not configurable, we have to disable the rule.
        // https://github.com/pinterest/ktlint/issues/527
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            disabledRules.set(setOf("import-ordering", "no-wildcard-imports"))
        }
    }
}

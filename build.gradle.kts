plugins {
    alias(libs.plugins.kotlinMultiplatform)
}


kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }

    jvm {}

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.wgpu4k)
                implementation(libs.coroutines)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}


object Platform {
    val os: String
        get() = System.getProperty("os.name").let { name ->
            when {
                arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } -> "linux"
                arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> "macos"
                arrayOf("Windows").any { name.startsWith(it) } -> "windows"
                else -> error("Unrecognized or unsupported operating system.")
            }
        }
}

tasks.register<JavaExec>("runJvm") {
    group = "run"
    mainClass = "MainKt"
    if (Platform.os == "macos") {
        jvmArgs(
            "-XstartOnFirstThread",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--enable-native-access=ALL-UNNAMED"
        )
    } else {
        jvmArgs(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--enable-native-access=ALL-UNNAMED"
        )
    }
    classpath = sourceSets["main"].runtimeClasspath
}


group = "example"
version = "0.0.0-SNAPSHOT"

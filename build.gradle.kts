plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

repositories {
    google()
    mavenCentral()
    //wgpu4k snapshot & preview repository
    maven("https://gitlab.com/api/v4/projects/25805863/packages/maven")
}


kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }

    jvm {}

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.bundles.commonMain)
                implementation(libs.lwjgl.stb)
                runtimeOnly(libs.lwjgl.stb.get().module.toString()) {
                    artifact {
                        classifier = "natives-${Platform.os}"
                    }
                }
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

tasks.withType<JavaExec> {
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
}


group = "example"
version = "0.0.0-SNAPSHOT"

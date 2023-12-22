import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.Executable
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce

plugins {
    kotlin("multiplatform") version "1.9.20"
}

group = "com.github.tarcv"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
//        jvmToolchain(19)
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        browser {
            commonWebpackConfig {
                mode = KotlinWebpackConfig.Mode.PRODUCTION
                sourceMaps = false
                cssSupport {
                    enabled.set(false)
                }
            }
        }
        useCommonJs()
        binaries.executable()
        compilations.createExecutable("comparable")
        compilations.createExecutable("optimal")
    }
    sourceSets {
        named("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.seleniumhq.selenium:selenium-java:4.12.1")
            }
        }
    }
}
val downloadNodeBinaries = providers.environmentVariable("NIX")
    .map { it.isNullOrEmpty() }
    .orElse(true)
rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.the<NodeJsRootExtension>().download = downloadNodeBinaries.get()
}

tasks.named<Copy>("jvmTestProcessResources") {
    from(tasks.named<KotlinWebpack>("executeProductionWebpackComparable").map { it.outputDirectory })
    from(tasks.named<KotlinWebpack>("executeProductionWebpackOptimal").map { it.outputDirectory })
    from(tasks.named<KotlinWebpack>("jsBrowserProductionWebpack").map { it.outputDirectory })
}

fun NamedDomainObjectContainer<out KotlinJsCompilation>.createExecutable(name: String) {
    create(name) {
        val compilation = this
/*        tasks.create(
            "executeProductionWebpack${compilation.name.forceCapitalized()}",
            KotlinWebpack::class.java,
            compilation
        ).apply {
            outputDirectory.set(
                project.layout.buildDirectory
                    .dir(project.provider { "distribution${compilation.name.forceCapitalized()}" })
            )

            dependsOn(compilation.compileTaskProvider)
            inputFilesDirectory.set(
                compilation.compileTaskProvider.flatMap { it.destinationDirectory }
            )

            mode = KotlinWebpackConfig.Mode.PRODUCTION
            sourceMaps = false
            esModules.set(false)
            entryModuleName.set(
                compilation.compileTaskProvider.flatMap { it.compilerOptions.moduleName }
            )
            mainOutputFileName.set("${compilation.name}.js")
        }*/
        binaries
            .executable(this)
            .filter { binary -> binary.mode == KotlinJsBinaryMode.PRODUCTION }
            .forEach { binary ->
                binary as Executable
                tasks.create(
                    "executeProductionWebpack${compilation.name.forceCapitalized()}",
                    KotlinWebpack::class.java,
                    compilation
                ).apply {
                    outputDirectory.set(
                        project.layout.buildDirectory
                            .dir(project.provider { "distribution${compilation.name.forceCapitalized()}" })
                    )

                    dependsOn(binary.linkSyncTask)
                    inputFilesDirectory.fileProvider(
                        binary.linkSyncTask.flatMap {
                            it.destinationDirectory
                        }
                    )

                    mode = KotlinWebpackConfig.Mode.PRODUCTION
                    sourceMaps = false
                    esModules.set(false)
                    entryModuleName.set(binary.linkTask.flatMap { it.compilerOptions.moduleName })
                    mainOutputFileName.set("${compilation.name}.js")
                }
                KotlinJsDce
            }
    }
}

fun String.forceCapitalized() = lowercase().capitalized()
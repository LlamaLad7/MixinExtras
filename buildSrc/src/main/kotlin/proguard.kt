import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import proguard.gradle.ProGuardTask
import java.io.File

fun Project.createProGuardTask(name: String, input: File, output: File, configFile: String): ProGuardTask {
    return tasks.create<ProGuardTask>(name) {
        inputs.file(input)
        outputs.files(output)

        doFirst {
            configurations.getByName("library").resolve().forEach {
                libraryjars(it)
            }
        }

        val javaToolchains = this@createProGuardTask.extensions.getByType<JavaToolchainService>()

        val java8 = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        }.get()

        libraryjars(java8.metadata.installationPath.file("jre/lib/rt.jar"))

        injars(input)
        outjars(output)
        configuration(file(configFile))
    }
}
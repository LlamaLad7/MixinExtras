import gradle.kotlin.dsl.accessors._c5ed198e191f30f39ee46d0abbbd888f.javaToolchains
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.create
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

        val java8 = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        }.get()

        libraryjars(java8.metadata.installationPath.file("jre/lib/rt.jar"))

        injars(input)
        outjars(output)
        configuration(file(configFile))
    }
}
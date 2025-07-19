import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.gradle.plugin.tasks.AbstractJReleaserTask
import org.jreleaser.model.Active
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer
import org.gradle.kotlin.dsl.*

fun Project.configureJReleaser(stagingDir: Directory) {
    apply(plugin = "org.jreleaser")
    apply(plugin = "maven-publish")

    tasks.withType<AbstractJReleaserTask> release@{
        for (proj in allprojects) {
            proj.tasks.withType<PublishToMavenRepository> stage@{
                this@release.mustRunAfter(this@stage)
            }
        }
        outputs.upToDateWhen { false }
    }

    extensions.configure<JReleaserExtension> {
        release {
            github {
                skipRelease = true
            }
        }
        deploy {
            maven {
                mavenCentral {
                    create("sonatype") {
                        active = Active.ALWAYS
                        url = "https://central.sonatype.com/api/v1/publisher"
                        stagingRepository(stagingDir.asFile.absolutePath)
                        stage = MavenCentralMavenDeployer.Stage.UPLOAD
                        sign = false
                        verifyUrl = "https://repo1.maven.org/maven2/{{path}}/{{filename}}"
                    }
                }
            }

            upload {
                active = Active.ALWAYS
            }
        }
    }
}

fun Project.registerUploadTask(vararg subprojects: Project) = tasks.registering {
    dependsOn(subprojects.map { it.tasks.named("publishAllPublicationsToStagingRepository") })
    dependsOn(":jreleaserDeploy")
}

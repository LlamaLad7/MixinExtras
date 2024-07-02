import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningExtension

fun Project.configurePublishing(artifactName: String, setup: MavenPublication.() -> Unit) {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    val isLocal = (properties["isLocal"] as? String).toBoolean()

    extensions.configure<PublishingExtension> {
        if (isLocal) {
            repositories {
                maven {
                    name = "Sonatype"
                    url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    authentication {
                        credentials {
                            username = properties["ossrhUsername"] as String
                            password = properties["ossrhPassword"] as String
                        }
                    }
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "io.github.llamalad7"
                artifactId = "mixinextras-$artifactName"

                setup()

                pom {
                    name = "MixinExtras"
                    description = "Companion library to Mixin with lots of features to improve the compatibility and concision of your mixins!"
                    url = "https://github.com/LlamaLad7/MixinExtras"
                    licenses {
                        license {
                            name = "MIT License"
                            url = "http://www.opensource.org/licenses/mit-license.php"
                        }
                    }
                    developers {
                        developer {
                            name = "LlamaLad7"
                            url = "https://github.com/LlamaLad7"
                        }
                    }
                    scm {
                        connection = "scm:git:git://github.com/LlamaLad7/MixinExtras.git"
                        developerConnection = "scm:git:git://github.com/LlamaLad7/MixinExtras.git"
                        url = "https://github.com/LlamaLad7/MixinExtras/tree/master"
                    }
                    withXml {
                        val node = asNode().appendNode("dependencies")
                        configurations.getByName("api").dependencies.forEach {
                            val dep = node.appendNode("dependency")
                            dep.appendNode("groupId", it.group)
                            dep.appendNode("artifactId", it.name)
                            dep.appendNode("version", it.version)
                            dep.appendNode("scope", "runtime")
                        }
                    }
                }
            }
        }
    }
    if (isLocal) {
        extensions.configure<SigningExtension> {
            sign(extensions.getByType<PublishingExtension>().publications["maven"])
        }
    }
}

val isLocal = (properties["isLocal"] as? String).toBoolean()

val inclusions = listOf(
    project(":mixin-versions").subprojects.toList()
).flatten()

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    val proguardFile: File by rootProject.extra
    val proguardJar = rootProject.tasks.getByName("proguardJar")

    if (project.name != "forge") {
        dependencies {
            compileOnly(rootProject)
        }

        tasks.named<Jar>("jar") {
            from(zipTree(proguardFile))
        }

        tasks.named<Jar>("sourcesJar") {
            from(rootProject.sourceSets.main.get().allSource)
            inclusions.forEach {
                from(it.sourceSets.main.get().allSource)
            }
        }

        tasks.named<Javadoc>("javadoc") {
            classpath += rootProject.configurations.compileClasspath.get()
            source(rootProject.sourceSets.main.get().java)
            inclusions.forEach {
                source(it.sourceSets.main.get().java)
            }
        }
    }

    val moduleName = name

    tasks.withType<Jar> {
        archiveBaseName.set("mixinextras-$moduleName")
    }

    tasks.named("jar") {
        dependsOn(proguardJar)
    }

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
                artifactId = "mixinextras-$moduleName"

                artifact(tasks.getByName("jar"))
                artifact(tasks.getByName("sourcesJar"))
                artifact(tasks.getByName("javadocJar"))

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
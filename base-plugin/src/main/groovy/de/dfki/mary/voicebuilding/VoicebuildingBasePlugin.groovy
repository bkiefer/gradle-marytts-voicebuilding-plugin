package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin

class VoicebuildingBasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply JavaPlugin
        project.plugins.apply MavenPlugin

        project.extensions.create 'voice', VoiceExtension
        project.voice.extensions.create 'license', VoiceLicenseExtension

        project.ext {
            maryttsVersion = '5.1.1'
        }

        project.repositories {
            jcenter()
        }

        project.dependencies {
            compile(group: 'de.dfki.mary', name: 'marytts-runtime', version: project.maryttsVersion) {
                exclude module: 'freetts'
                exclude module: 'freetts-en_us'
                exclude module: 'freetts-de'
            }
            testCompile group: 'junit', name: 'junit', version: '4.12'
        }

        project.task('generateSource', type: GenerateSource) {
            destDir = project.file("$project.buildDir/generatedSrc")
            project.sourceSets.main.java.srcDirs += "$destDir/main/java"
            project.sourceSets.test.java.srcDirs += "$destDir/test/java"
            project.compileJava.dependsOn it
            project.compileTestJava.dependsOn it
        }

        project.task('generateVoiceConfig', type: GenerateVoiceConfig) {
            project.afterEvaluate {
                destFile = project.file("$project.sourceSets.main.output.resourcesDir/marytts/voice/$project.voice.nameCamelCase/voice.config")
            }
            project.processResources.dependsOn it
        }

        project.task('generateServiceLoader', type: GenerateServiceLoader) {
            destFile = project.file("$project.sourceSets.main.output.resourcesDir/META-INF/services/marytts.config.MaryConfig")
            project.processResources.dependsOn it
        }

        project.task('generatePom', type: GeneratePom) {
            project.afterEvaluate {
                destFile = project.file("${project.sourceSets.main.output.resourcesDir}/META-INF/maven/${project.group.replace('.', '/')}/voice-$project.voice.name/pom.xml")
            }
            project.jar.dependsOn it
        }

        project.task('generatePomProperties', type: GeneratePomProperties) {
            project.afterEvaluate {
                destFile = project.file("${project.sourceSets.main.output.resourcesDir}/META-INF/maven/${project.group.replace '.', '/'}/voice-$project.voice.name/pom.properties")
            }
            project.jar.dependsOn it
        }
    }
}

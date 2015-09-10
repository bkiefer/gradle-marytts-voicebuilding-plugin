package de.dfki.mary.voicebuilding.data

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    def dataDependencyName = 'cmu_time_awb'
    def dataDependency = "org.festvox:$dataDependencyName::ldom@tar.bz2"

    @BeforeSuite
    void setup() {
        def projectDir = new File(System.properties.testProjectDir)
        projectDir.mkdirs()
        gradle = GradleRunner.create().withProjectDir(projectDir)
        buildFile = new File(projectDir, 'build.gradle')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")

        // Add the logic under test to the test build
        buildFile << """
        buildscript {
            dependencies {
                classpath files($pluginClasspath)
            }
        }

        apply plugin: 'de.dfki.mary.voicebuilding-data'

        repositories {
            flatDir dirs: "$projectDir.parent/testKitGradleHome"
        }

        dependencies {
            data "$dataDependency"
        }

        task testConfigurations << {
            assert configurations.data
        }

        task testSourceSets << {
            assert sourceSets.data
        }

        task testDependencies << {
            assert configurations.data.dependencies.find { it.name == "$dataDependencyName" }
        }

        processDataResources {
            from configurations.data
            filesMatching '*.tar.bz2', { tarFileDetails ->
                copy {
                    from tarTree(tarFileDetails.file)
                    into destinationDir
                    include '**/wav/*.wav', '**/lab/*.lab', '**/etc/*.data'
                    eachFile {
                        it.path = 'unpacked/' + it.name
                    }
                    includeEmptyDirs = false
                }
                tarFileDetails.exclude()
            }

            doLast {
                // extract text prompts
                fileTree(destinationDir).include('**/*.data').each { dataFile ->
                     dataFile.eachLine { line ->
                         def m = line =~ /\\( (?<utt>.+) "(?<text>.+)" \\)/
                         if (m.matches()) {
                             file("\$destinationDir/unpacked/\${m.group('utt')}.txt").text = m.group('text')
                         }
                     }
                 }
            }
        }

        task testProcessDataResources {
            dependsOn processDataResources
            doLast {
                assert fileTree(sourceSets.data.output.resourcesDir).include('**/*.wav').files
                assert fileTree(sourceSets.data.output.resourcesDir).include('**/*.lab').files
                assert fileTree(sourceSets.data.output.resourcesDir).include('**/*.txt').files
            }
        }
        """
    }

    @Test
    void testBuild() {
        def result = gradle.build()
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testConfigurations() {
        def result = gradle.withArguments('testConfigurations').build()
        assert result.task(':testConfigurations').outcome == SUCCESS
    }

    @Test
    void testSourceSets() {
        def result = gradle.withArguments('testSourceSets').build()
        assert result.task(':testSourceSets').outcome == SUCCESS
    }

    @Test
    void testDependencies() {
        def result = gradle.withArguments('testDependencies').build()
        assert result.task(':testDependencies').outcome == SUCCESS
    }

    @Test
    void testProcessDataResources() {
        def result = gradle.withArguments('testProcessDataResources').build()
        assert result.task(':processDataResources').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':testProcessDataResources').outcome == SUCCESS
    }
}
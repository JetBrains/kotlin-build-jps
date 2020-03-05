package common.buildtypes

import common.vcsRoots.KotlinVcsRoots
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2019_2.ReuseBuilds
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

class BuildIntegrityTest(
        id: String,
        buildStepName: String,
        kotlinVcsRoots: KotlinVcsRoots,
        buildNumberFrom: BuildType,
        pluginVersion: String,
        pluginDownloadUrl: String
) : BuildType({
    this.id(id)
    this.name = buildStepName

    buildNumberPattern = "%build.number.default%"

    artifactRules = """
        jps/.idea => jps/idea.zip
        jps/out => jps/out.zip
        jps/dist => jps/dist.zip
        jpsBuildPlugin/local/system/log => idea_logs.zip
""".trimIndent()

    params {
        jdkRequirements()
        param("requirement.jdk9", "%env.JDK_19%")
        param("build.number.default", "%dep.${buildNumberFrom.idValue}.build.number%")

        param("gradleParameters", "--info --full-stacktrace")

        param("env.ide_plugins", "java,gradle,org.jetbrains.kotlin:$pluginVersion")
    }

    vcs {
        root(kotlinVcsRoots.mainVcsRoot, "+:. => jps")
        root(
                kotlinVcsRoots.jpsCompilerVcsRoot
                        ?: error("JPS compiler vcs root is expected for BuildIntegrity configuration"),
                "+:. => jpsBuildPlugin"
        )

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradleStep(name = "Check buildSrc", tasks = "checkBuild") {
            workingDir = "jps"
            buildFile = "buildSrc/build.gradle.kts"
        }

        gradleStep(name = "Check build", tasks = "checkBuild") {
            workingDir = "jps"
        }

        // enable jps build
        script {
            name = "enable jps build"
            scriptContent = """
                rem() {
                  @echo off
                  cd jps
                  echo jpsBuild=true> local.properties
                  echo -------------------------------------
                  type local.properties
                  echo -------------------------------------
                  exit 0
                }

                cd jps
                echo "jpsBuild=true" > local.properties
                echo -------------------------------------
                cat local.properties
                echo -------------------------------------
            """.trimIndent()
        }

        step {
            name = "Download kotlin plugin"
            type = "MRPP_DownloadFile"
            param("system.url", pluginDownloadUrl)
            param("system.dest.dir", "kotlin-plugin")
            param("system.clean.dest.dir", "false")
        }
        maven {
            name = "Publish Kotlin IDE plugin to local repository"
            goals = "install:install-file"
            pomLocation = ""
            runnerArgs = """
                -Dfile=kotlin-plugin/kotlin-plugin-$pluginVersion.zip
                 -DgroupId=com.jetbrains.plugins -DartifactId=org.jetbrains.kotlin -Dversion=$pluginVersion -Dpackaging=zip
            """.trimIndent()
            localRepoScope = MavenBuildStep.RepositoryScope.MAVEN_DEFAULT
            mavenVersion = bundled_3_5()
        }

        // build with jps
        gradleStep(name = "Import Kotlin project and build with JPS", tasks = "runIde") {
            workingDir = "jpsBuildPlugin"
            buildFile = "build.gradle"
        }
    }

    dependencies {
        dependency(buildNumberFrom) {
            snapshot {
                onDependencyFailure = FailureAction.ADD_PROBLEM
                onDependencyCancel = FailureAction.FAIL_TO_START
                reuseBuilds = ReuseBuilds.SUCCESSFUL
            }
        }
    }


    requirements {
        noLessThan("teamcity.agent.work.dir.freeSpaceMb", 1000.toString())
        noLessThan("teamcity.agent.hardware.memorySizeMb", 7500.toString())
    }

    features {
        freeDiskSpace {
            requiredSpace = "5gb"
            failBuild = true
        }
        feature {
            type = "perfmon"
        }
        swabra {
            lockingProcesses = Swabra.LockingProcessPolicy.KILL
        }
    }

    failureConditions {
        executionTimeoutMin = 60
    }
})
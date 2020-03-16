package common

import common.buildtypes.*
import common.settings.ProjectSettings
import common.vcsRoots.KotlinVcsRoots
import jetbrains.buildServer.configs.kotlin.v2019_2.*

const val defaultTeamCityVersion = "2019.2"

// This class describes all configurations necessary for a particular branch
// The recommended usage is to enumerate them in setupKotlinProject
sealed class Configuration<T: BuildType> {
    abstract val name: String
    lateinit var build: T

    class BuildNumber : Configuration<BuildType>() {
        override val name = "Build number"
    }

    class BuildIntegrityTest : Configuration<common.buildtypes.BuildIntegrityTest>() {
        override val name = "Test build integrity"
    }
}

class BranchConfiguration(
        val version: String,                   // base version
        val releaseBranch: String = version,   // branch in VCS
        val remoteRunBranches: List<String>,             // release status: DEV, EAP, RELEASE
        val monitoredBranches: List<String> = emptyList()
) {
    val versionPrefix: String = "$version-pub"
}

fun setupAllConfigurations(
        kotlinVcsRoots: KotlinVcsRoots,
        configurations: List<Configuration<*>>,
        projectSettings: ProjectSettings
): List<Configuration<*>> {
    val buildNumber = configurations.single { it is Configuration.BuildNumber }

    for (configuration in configurations) {
        when (configuration) {
            is Configuration.BuildNumber -> (configuration as Configuration.BuildNumber).build = BuildNumber(
                    kotlinVcsRoots = kotlinVcsRoots
            )

            is Configuration.BuildIntegrityTest -> configuration.build = BuildIntegrityTest(
                    id = "BuildIntegrityTest",
                    buildStepName = configuration.name,
                    kotlinVcsRoots = kotlinVcsRoots,
                    buildNumberFrom = buildNumber.build,
                    pluginDownloadUrl = projectSettings.INTEGRITY_BUILD_PLUGIN_DOWNLOAD_URL
            )
        }
    }

    return configurations
}

fun ParametrizedWithType.setupJDKParams() {
    param("projectJDK", "%env.JDK_18_x64%")
    param("env.JAVA_HOME", "%projectJDK%")
    param("env.JDK_16", "%env.JDK_16_x64%")
    param("env.JDK_17", "%env.JDK_17_x64%")
    param("env.JDK_18", "%env.JDK_18_x64%")
    param("env.JDK_9", "%env.JDK_9_x64%")
}

fun ParametrizedWithType.setupGradleParameters() {
    param(
            "globalGradleParameters",
            listOfNotNull("--info --full-stacktrace").joinToString(separator = " ")
    )
}

fun Project.setupKotlinProject(
        kotlinVcsRoots: KotlinVcsRoots,
        branchConfiguration: BranchConfiguration,
        configurations: List<Configuration<*>>
) {
    with (kotlinVcsRoots) {
        roots.add(mainVcsRoot)
        if (jpsCompilerVcsRoot != null) {
            roots.add(jpsCompilerVcsRoot)
        }
    }

    params {
        setupJDKParams()
        setupGradleParameters()

        text("teamcity.activeVcsBranch.age.days", "5", display = ParameterDisplay.HIDDEN)
        param("build.number.prefix", branchConfiguration.versionPrefix)
    }

    buildTypes += configurations.map { it.build }

    cleanup {
        artifacts(days = 7, builds = 1, artifactPatterns = listOf("+:**/*", "+:.teamcity/logs/**").joinToString("\n"))
    }
}

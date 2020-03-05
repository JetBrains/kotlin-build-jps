package common.buildtypes

import common.vcsRoots.KotlinVcsRoots
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.GradleBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ant
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

fun ParametrizedWithType.jdkRequirements() {
    text("requirement.jdk16", "%env.JDK_16%", display = ParameterDisplay.HIDDEN)
    text("requirement.jdk17", "%env.JDK_17%", display = ParameterDisplay.HIDDEN)
    text("requirement.jdk18", "%env.JDK_18%", display = ParameterDisplay.HIDDEN)
}

// common build steps
fun BuildSteps.gradleStep(
        name: String,
        tasks: String,
        parameters: String? = null,
        buildFile: String = "build.gradle.kts",
        workingDir: String = KotlinVcsRoots.MAIN_VCS_ROOT_PATH,
        setup: GradleBuildStep.() -> Unit = {}
) {
    gradle {
        this.name = name
        this.tasks = tasks
        useGradleWrapper = true
        gradleParams = "%gradleParameters%" + (parameters?.let { " $it" } ?: "")
        jdkHome = "%projectJDK%"
        this.buildFile = buildFile
        this.workingDir = workingDir
        this.setup()
    }
}
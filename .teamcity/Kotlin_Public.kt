@file:Suppress("ConstantConditionIf")

import common.*
import common.settings.ProjectSettings
import common.vcsRoots.KotlinVcsRoots
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

@Suppress("ClassName")
object Kotlin_Public : Project({
    val projectSettings = ProjectSettings(
            INTEGRITY_BUILD_PLUGIN_DOWNLOAD_URL_PATTERN = "https://teamcity.jetbrains.com/guestAuth/repository/download" +
                    "/Kotlin_KotlinPublic_CompilerAndPlugin_193" +
                    "/{version}" +
                    "/kotlin-plugin-{version}-IJ2019.3-Community-1.zip"
    )

    val buildNumber = Configuration.BuildNumber()

    val branchConfiguration = BranchConfiguration(
            version = projectSettings.VERSION,
            releaseBranch = "master",
            remoteRunBranches = emptyList()
    )

    val kotlinVcsRoots = KotlinVcsRoots.create(
            branchConfiguration,
            customBranchSpec = """
                +:refs/heads/(master)
                +:refs/heads/(1.*)
                +:refs/heads/(prr/*)
                +:refs/tags/(build-*)
                """.trimIndent()
    )

    kotlinVcsRoots.mainVcsRoot.useTagsAsBranches = true

    val configurations = setupAllConfigurations(
            kotlinVcsRoots,
            configurations = listOf(buildNumber, Configuration.BuildIntegrityTest()),
            projectSettings = projectSettings
    )

    setupKotlinProject(
            kotlinVcsRoots = kotlinVcsRoots,
            branchConfiguration = branchConfiguration,
            configurations = configurations
    )

    description = "Build integrity test playground"
})

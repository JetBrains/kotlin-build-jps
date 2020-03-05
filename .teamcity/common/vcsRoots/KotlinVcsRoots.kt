package common.vcsRoots

import common.BranchConfiguration
import common.buildtypes.idValue
import jetbrains.buildServer.configs.kotlin.v2019_2.VcsSettings
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import java.io.File

class KotlinVcsRoots(
        val mainVcsRoot: GitVcsRoot,
        val jpsCompilerVcsRoot: GitVcsRoot? = null
) {
    fun applyTo(vcsSettings: VcsSettings, withKotlinUltimate: Boolean = false, withKotlinNative: Boolean = false) {
        vcsSettings.root(mainVcsRoot, "+:. => $KOTLIN_CHECKOUT_DIR")
    }

    companion object {
        const val KOTLIN_CHECKOUT_DIR = "kotlin"

        val MAIN_VCS_ROOT_PATH = checkoutDirFullPath(KOTLIN_CHECKOUT_DIR)

        private val KOTLIN_VCS_TEMPLATE = GitVcsRoot {
            id("Kotlin")
            url = "https://github.com/JetBrains/kotlin.git"
        }

        private val JPS_VCS_TEMPLATE = GitVcsRoot {
            id("JpsBuildTool")
            url = "https://github.com/JetBrains/idea-gradle-jps-build-app.git"
        }

        fun create(
                branchConfiguration: BranchConfiguration,
                customBranchSpec: String?
        ): KotlinVcsRoots {
            val mainVcsRoot: GitVcsRoot = KOTLIN_VCS_TEMPLATE.setupVcsRoot(
                    releaseBranch = branchConfiguration.releaseBranch,
                    remoteRunBranches = branchConfiguration.remoteRunBranches,
                    monitoredBranches = branchConfiguration.monitoredBranches,
                    customBranchSpec = customBranchSpec,
                    authMethod = GitVcsRoot.AuthMethod.Anonymous()
            )

            val jpsCompilerVcsRoot: GitVcsRoot = JPS_VCS_TEMPLATE.setupVcsRoot(
                    releaseBranch = "idea-193",
                    authMethod = GitVcsRoot.AuthMethod.Anonymous()
            )

            return KotlinVcsRoots(mainVcsRoot, jpsCompilerVcsRoot)
        }

        private fun GitVcsRoot.setupVcsRoot(
                releaseBranch: String,
                authMethod: GitVcsRoot.AuthMethod,
                remoteRunBranches: List<String>? = null,
                monitoredBranches: List<String>? = null,
                customBranchSpec: String? = null
        ) = this.apply {
            this.name = idValue
            this.branch = releaseBranch
            this.branchSpec = customBranchSpec
                    ?: listOfNotNull(listOf(releaseBranch), remoteRunBranches, monitoredBranches)
                            .flatten()
                            .toBranchSpec()
            this.authMethod = authMethod
        }

        private fun checkoutDirFullPath(vararg subDir: String): String =
                subDir.fold(File("%teamcity.build.checkoutDir%"), ::File).invariantSeparatorsPath
    }
}

fun List<String>.toBranchSpec() =
        joinToString("\n") { "+:refs/heads/($it)" }
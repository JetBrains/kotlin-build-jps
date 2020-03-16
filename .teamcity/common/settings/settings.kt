package common.settings

// Set of common defaults for project parameters
class ProjectSettings(
        val VERSION: String = "1.4.0",

        private val INTEGRITY_BUILD_VERSION: String = "1.4.0-dev-1075",
        private val INTEGRITY_BUILD_PLUGIN_DOWNLOAD_URL_PATTERN: String =
                "https://buildserver.labs.intellij.net/guestAuth/repository/download" +
                        "/Kotlin_KotlinDev_CompilerAndPlugin_193" +
                        "/{version}" +
                        "/kotlin-plugin-{version}-IJ2019.3-1.zip",
        val INTEGRITY_BUILD_PLUGIN_DOWNLOAD_URL: String = INTEGRITY_BUILD_PLUGIN_DOWNLOAD_URL_PATTERN
                .replace("{version}", INTEGRITY_BUILD_VERSION)
)
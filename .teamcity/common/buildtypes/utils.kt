package common.buildtypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildTypeSettings
import jetbrains.buildServer.configs.kotlin.v2019_2.VcsRoot

val BuildTypeSettings.idValue get() = id!!.value
val VcsRoot.idValue get() = id!!.value


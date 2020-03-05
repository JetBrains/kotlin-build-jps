package common.buildtypes

import common.vcsRoots.KotlinVcsRoots
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.RelativeId

class BuildNumber(kotlinVcsRoots: KotlinVcsRoots) : BuildType({
    id = ID
    name = "Build number"

    vcs {
        kotlinVcsRoots.applyTo(this)

        checkoutMode = CheckoutMode.MANUAL
        cleanCheckout = true
    }

    buildNumberPattern = "%chain.build.number%"

    params {
        param("chain.build.number", "%build.number.prefix%-%build.counter%" )
    }
}) {
    companion object {
        val ID = RelativeId("BuildNumber")
    }
}

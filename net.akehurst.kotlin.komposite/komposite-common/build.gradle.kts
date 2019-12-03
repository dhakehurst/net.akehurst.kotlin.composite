plugins {
    id("net.akehurst.kotlin.kt2ts") version "1.2.0"
}

val version_kotlinx:String by project
val version_agl:String by project

dependencies {

    commonMainApi(project(":komposite-api"))
    commonMainImplementation(project(":komposite-processor"))
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")

    commonMainImplementation(kotlin("reflect"))


    //because InteliJ can't find it at runtime !!!
    commonTestImplementation("net.akehurst.language:agl-processor:${version_agl}")
}

kt2ts {
    jvmTargetName.set("jvm8")
    classPatterns.set(listOf(
            "net.akehurst.kotlin.komposite.common.*"
    ))
}

plugins {
    id("net.akehurst.kotlin.kt2ts") version "1.0.0"
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

val tsdDir ="${buildDir}/tmp/jsJar/ts"

kotlin {
    sourceSets {
        val jsMain by getting {
            resources.srcDir("${tsdDir}")
        }
    }
}

kt2ts {
    outputDirectory.set(file(tsdDir))
    localJvmName.set("jvm8")
    modulesConfigurationName.set("jvm8RuntimeClasspath")
    classPatterns.set(listOf(
            "net.akehurst.kotlin.komposite.common.*"
    ))
}

tasks.getByName("generateTypescriptDefinitionFile").dependsOn("jvm8MainClasses")
tasks.getByName("jsJar").dependsOn("generateTypescriptDefinitionFile")
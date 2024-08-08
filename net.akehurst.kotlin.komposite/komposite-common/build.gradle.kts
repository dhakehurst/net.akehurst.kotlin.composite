plugins {
    //id("net.akehurst.kotlin.gradle.plugin.exportPublic") version("1.3.0")
    //id("net.akehurst.kotlinx.kotlinx-reflect-gradle-plugin") version ("1.4.1")
}

val version_kotlinx:String by project
val version_agl:String by project

dependencies {

    "commonMainApi"(project(":komposite-api"))
    "commonMainImplementation"(project(":komposite-processor"))
    commonMainApi(libs.nal.agl.processor)
    commonMainImplementation(libs.nak.kotlinx.reflect)
    commonMainImplementation(libs.nak.kotlinx.collections)

    "commonMainImplementation"(kotlin("reflect"))

}



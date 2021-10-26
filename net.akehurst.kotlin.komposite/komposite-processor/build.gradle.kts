plugins {
    //id("net.akehurst.kotlin.gradle.plugin.exportPublic") version("1.3.0")
    //id("net.akehurst.kotlinx.kotlinx-reflect-gradle-plugin") version ("1.4.1")
}


val version_agl:String by project
val version_kotlinx:String by project

dependencies {

    "commonMainApi"(project(":komposite-api"))

    "commonMainImplementation"("net.akehurst.language:agl-processor:${version_agl}")
    "commonMainImplementation"("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")

}

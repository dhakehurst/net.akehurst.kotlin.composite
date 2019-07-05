val version_kxreflect:String by project
val version_agl:String by project

dependencies {

    commonMainApi(project(":komposite-api"))
    commonMainImplementation(project(":komposite-processor"))
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kxreflect")

    commonMainImplementation(kotlin("reflect"))


    //because InteliJ can't find it at runtime !!!
    commonTestImplementation("net.akehurst.language:agl-processor:${version_agl}")
}
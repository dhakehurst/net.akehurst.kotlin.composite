val version_agl:String by project
val version_kxreflect:String by project

dependencies {

    commonMainApi(project(":komposite-api"))

    commonMainImplementation("net.akehurst.language:agl-processor:${version_agl}")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kxreflect")

}
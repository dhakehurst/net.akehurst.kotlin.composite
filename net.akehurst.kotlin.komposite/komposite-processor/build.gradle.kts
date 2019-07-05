val version_agl:String by project

dependencies {

    commonMainApi(project(":komposite-api"))

    commonMainImplementation("net.akehurst.language:agl-processor:${version_agl}")

}
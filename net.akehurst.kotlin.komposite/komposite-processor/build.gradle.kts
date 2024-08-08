
dependencies {

    "commonMainApi"(project(":komposite-api"))
    //"commonMainApi"("net.akehurst.language:type-model:${version_agl}")

    commonMainApi(libs.nal.agl.processor)
    commonMainImplementation(libs.nak.kotlinx.reflect)

}

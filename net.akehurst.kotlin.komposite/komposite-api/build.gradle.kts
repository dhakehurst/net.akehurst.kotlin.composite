
dependencies {

}

kt2ts {
    jvmTargetName.set("jvm8")
    classPatterns.set(listOf(
            "net.akehurst.kotlin.komposite.api.*"
    ))
}

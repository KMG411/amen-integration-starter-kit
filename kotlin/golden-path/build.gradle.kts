plugins { kotlin("jvm"); application }
kotlin { jvmToolchain(17) }
dependencies { implementation(project(":amen-client")) }
application { mainClass.set("GoldenPathKt") }

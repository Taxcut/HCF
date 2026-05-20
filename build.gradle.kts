plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.testrank.hcf"
version = "1.0"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("com.lunarclient:apollo-api:1.2.5")
    compileOnly("com.lunarclient:apollo-extra-adventure4:1.2.5")

    implementation("org.mongodb:mongodb-driver-reactivestreams:5.2.1")
    implementation("io.lettuce:lettuce-core:6.5.1.RELEASE")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("org.slf4j:slf4j-nop:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("io.lettuce", "com.testrank.hcf.libs.lettuce")
    relocate("io.netty", "com.testrank.hcf.libs.netty")
    relocate("com.mongodb", "com.testrank.hcf.libs.mongodb")
    relocate("com.google.gson", "com.testrank.hcf.libs.gson")
    relocate("com.fasterxml.jackson", "com.testrank.hcf.libs.jackson")
    relocate("org.slf4j", "com.testrank.hcf.libs.slf4j")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

plugins {
    id("java")
}

group = "com.berkan"
version = "1.0.0"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Minecraft Paper API
    compileOnly("io.papermc.paper:paper-api:26.3.build.230-stable")

    // Test kütüphaneleri
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("org.xerial:sqlite-jdbc:3.54.1.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.test {
    useJUnitPlatform()
}




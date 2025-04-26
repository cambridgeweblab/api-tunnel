plugins {
    java
    id("org.springframework.boot") version "2.7.18" apply false
    id("io.spring.dependency-management") version "1.0.11.RELEASE" apply false
}

val isCiServer = System.getenv().containsKey("CI")

allprojects {
    group = "ucles.weblab"
    version = "2.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencies {
        // Common dependencies can be defined here
    }

    tasks.withType<Test> {
        useJUnit()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

ext {
    set("commonJavaVersion", "2.0.0-SNAPSHOT")
    set("websocketVersion", "1.1")
}

// Profile equivalent for Shippable CI
if (isCiServer) {
    subprojects {
        tasks.withType<Test> {
            reports.html.required.set(false)
            reports.junitXml.required.set(true)
            reports.junitXml.outputLocation.set(file("../shippable/testresults"))
        }
    }
}

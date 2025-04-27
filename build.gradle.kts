import io.spring.gradle.dependencymanagement.DependencyManagementPlugin

plugins {
    java
    id("org.springframework.boot") version "2.5.11" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

val isCiServer = System.getenv().containsKey("CI")

allprojects {
    group = "ucles.weblab"
    version = "2.5.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

subprojects {
    apply(plugin = "java")
    apply<DependencyManagementPlugin>()
    apply(plugin = "io.spring.dependency-management")

    java {
        // Spring Boot 2.5.x supports Java 8-17
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        toolchain {
            // Will need to install one locally e.g. via IntelliJ
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    dependencyManagement {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        }
    }

    dependencies {
        // Common dependencies can be defined here
        compileOnly("org.jspecify:jspecify:1.0.0")
        compileOnly("org.projectlombok:lombok")
        testImplementation("junit:junit")
        annotationProcessor("org.projectlombok:lombok")
        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")
    }

    tasks.withType<Test> {
        useJUnit()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
        testCompileOnly {
            extendsFrom(configurations.testAnnotationProcessor.get())
        }
    }
}

// Override Spring Boot default versions
extra["lombok.version"] = "1.18.38"

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

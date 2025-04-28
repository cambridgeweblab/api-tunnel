plugins {
    java
}

dependencies {
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-websocket")
    implementation("javax.websocket:javax.websocket-api:${rootProject.extra["websocketVersion"]}")

    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    compileOnly("javax.servlet:javax.servlet-api")
    compileOnly("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

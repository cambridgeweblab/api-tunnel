plugins {
    java
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-websocket")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    compileOnly("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

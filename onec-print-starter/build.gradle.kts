plugins {
    `java-library`
}

dependencies {
    api(project(":onec-framework"))
    implementation(project(":onec-framework-starter"))

    implementation("org.springframework.boot:spring-boot-autoconfigure:3.4.4")
    implementation("org.springframework:spring-context:6.2.5")

    api("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
    api("org.xhtmlrenderer:flying-saucer-pdf-openpdf:9.4.0")

    compileOnly("org.springframework.boot:spring-boot-configuration-processor:3.4.4")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.4.4")

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

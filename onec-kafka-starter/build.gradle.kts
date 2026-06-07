plugins {
    `java-library`
}

dependencies {
    api(project(":onec-framework"))
    implementation(project(":onec-framework-starter"))

    implementation("org.springframework.boot:spring-boot-autoconfigure:3.4.4")
    implementation("org.springframework:spring-web:6.2.5")
    implementation("org.springframework.kafka:spring-kafka:3.3.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")

    compileOnly("org.springframework.boot:spring-boot-configuration-processor:3.4.4")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.4.4")
}

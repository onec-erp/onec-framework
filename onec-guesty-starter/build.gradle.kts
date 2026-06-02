plugins {
    `java-library`
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "onec-guesty-starter"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/onec-erp/onec-framework")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    api(project(":onec-framework"))
    implementation(project(":onec-framework-starter"))

    implementation("org.springframework.boot:spring-boot-autoconfigure:3.4.4")

    // RestClient transport against the Guesty Open API.
    api("org.springframework:spring-web:6.2.5")
    api("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")

    compileOnly("org.springframework.boot:spring-boot-configuration-processor:3.4.4")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.4.4")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

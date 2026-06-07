plugins {
    java
    id("org.springframework.boot") version "3.4.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    // Publishes the open-source modules to Maven Central (Central Portal). Applied only to the
    // published subprojects below; the host (CENTRAL_PORTAL) and signing are driven by the
    // SONATYPE_HOST / RELEASE_SIGNING_ENABLED properties in gradle.properties.
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

// The version a build publishes. Release pipelines pass an explicit version via the
// `releaseVersion` Gradle property or the `RELEASE_VERSION` env var. When neither is set
// (the common case for a local `publishToMavenLocal`), default to a `-SNAPSHOT` version so a
// local build can never collide with — and silently shadow — a released tag in mavenLocal.
// See issue #31: a bare `0.1.0` default meant a HEAD build published to mavenLocal masked the
// genuinely older released `0.1.0`, breaking consumers that later switched the same coordinate
// back to the registry.
val releaseVersion = providers.gradleProperty("releaseVersion")
    .orElse(providers.environmentVariable("RELEASE_VERSION"))
    .orElse("0.1.0-SNAPSHOT")

allprojects {
    // Maven coordinate group. The artifacts publish to Maven Central under the GitHub-org-verified
    // `io.github.onec-erp` namespace (no domain ownership required). NOTE: this is the *publish
    // coordinate* only — the Java packages stay `com.onec.*` and the Gradle plugin id stays
    // `com.onec.desktop`. Consumers write `io.github.onec-erp:onec-framework-starter:<ver>` but still
    // `import com.onec...`.
    group = "io.github.onec-erp"
    version = releaseVersion.get()

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// ---------------------------------------------------------------------------
// Maven Central publishing
// ---------------------------------------------------------------------------
// One convention for every published open-source module instead of a per-module `publishing {}`
// block. The vanniktech plugin auto-creates the sources + javadoc jars, signs the artifacts
// (RELEASE_SIGNING_ENABLED, skipped for -SNAPSHOT) and targets the Central Portal
// (SONATYPE_HOST=CENTRAL_PORTAL) — see gradle.properties. Here we only supply the shared POM
// metadata Central requires; the per-module name/description comes from the map below.
//
// `example` is not published; the desktop Gradle plugin lives in a separate included build and is
// released on its own.
val publishedModules = mapOf(
    "onec-framework" to "Core domain model, JDBI persistence, and entity-change events for the onec ERP toolkit.",
    "onec-framework-starter" to "Spring Boot autoconfiguration for the onec core: datasource, JDBC repositories, and JobRunr background jobs.",
    "onec-ui-starter" to "Server-driven admin UI starter for onec — bundles the frontend and its Spring MVC endpoints.",
    "onec-auth-starter" to "Authentication starter for onec: in-memory and OIDC / OAuth2 (Keycloak, Zitadel) single sign-on.",
    "onec-mcp-starter" to "Model Context Protocol (MCP) server starter exposing onec query and command services to AI agents.",
    "onec-kafka-starter" to "Kafka integration starter publishing onec entity-change events to topics.",
    "onec-print-starter" to "PDF / printing starter for onec using Thymeleaf templates and Flying Saucer.",
    "onec-mail-starter" to "Email starter for onec: SMTP and HTTP dispatch with Thymeleaf-templated bodies.",
    "onec-desktop-starter" to "Desktop (Tauri) packaging starter bundling the onec shell for native app builds.",
)

configure(subprojects.filter { it.name in publishedModules.keys }) {
    apply(plugin = "com.vanniktech.maven.publish")

    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        pom {
            name.set(project.name)
            description.set(publishedModules.getValue(project.name))
            inceptionYear.set("2025")
            url.set("https://github.com/onec-erp/onec-framework")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("onec-erp")
                    name.set("onec-erp")
                    url.set("https://github.com/onec-erp")
                }
            }
            scm {
                url.set("https://github.com/onec-erp/onec-framework")
                connection.set("scm:git:https://github.com/onec-erp/onec-framework.git")
                developerConnection.set("scm:git:ssh://git@github.com/onec-erp/onec-framework.git")
            }
        }
    }
}

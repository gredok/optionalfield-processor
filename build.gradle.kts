plugins {
    id("java")
    id("maven-publish")
    id("signing")
}

group = "com.gredok"
version = "1.2.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Jackson
    implementation("tools.jackson.core:jackson-databind:3.0.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:3.0-rc5")
    // Jakarta Validation
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    testImplementation("org.glassfish:jakarta.el:4.0.2")

    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    // Add the processor to the test classpath
    testAnnotationProcessor(project(":"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.gredok"
            artifactId = "optionalfield-processor"
            version = version

            pom {
                name = "OptionalField Processor"
                description = "Java annotation processor for handling PATCH requests — distinguishes between absent fields and fields explicitly set to null."
                url = "https://github.com/gredok/optionalfield-processor"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "gredok"
                        name = "Jozef Greznár"
                        url = "https://github.com/gredok"
                    }
                }
                scm {
                    url = "https://github.com/gredok/optionalfield-processor"
                    connection = "scm:git:https://github.com/gredok/optionalfield-processor.git"
                    developerConnection = "scm:git:ssh://git@github.com/gredok/optionalfield-processor.git"
                }
            }
        }
    }
    repositories {
        // Remote publishing to Maven Central goes through the nmcp plugin (see settings.gradle.kts),
        // which uploads this publication via the Central Portal Publisher API. `publishToMavenLocal`
        // still works locally without any credentials.
        mavenLocal()
    }
}

signing {
    // Only sign when key material is available (CI). Leaves `./gradlew publishToMavenLocal`
    // working locally without a GPG key configured. Central rejects unsigned releases.
    val signingKey = (findProperty("signingKey") as String?) ?: System.getenv("SIGNING_KEY")
    val signingPassword = (findProperty("signingPassword") as String?) ?: System.getenv("SIGNING_PASSWORD")
    isRequired = signingKey != null && signingPassword != null
    if (isRequired) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}

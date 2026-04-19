plugins {
    id("java")
    id("maven-publish")
}

group = "io.optionalfield"
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "io.optionalfield"
            artifactId = "optionalfield-processor"
            version = version

            pom {
                name = "OptionalField Processor"
                description = "Java annotation processor for handling PATCH requests — distinguishes between absent fields and fields explicitly set to null."
                url = "https://github.com/jgreznar/optionalfield-processor"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/jgreznar/optionalfield-processor")
            credentials {
                username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
                password = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("java")
    id("org.openapi.generator") version "7.11.0"
    `maven-publish`
}

group = "io.kryptance"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
//    implementation("com.squareup.okio:okio:2.10.0")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("io.gsonfire:gson-fire:1.9.0")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kryptance/sftpgo-client")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "sftpgo-client"
        }
    }
}

val generatedDir = layout.buildDirectory.file("generated/src/main/java")
tasks.named("openApiGenerate") {
    outputs.cacheIf { false }
}

openApiGenerate {
    generatorName.set("java")
    library.set("okhttp-gson")
    // inputSpec.set("$projectDir/src/main/resources/openapi.yaml")
    inputSpec.set(layout.projectDirectory.file("src/main/resources/openapi.yaml").asFile.path)
    outputDir.set(generatedDir.get().asFile.path)
    apiPackage.set("io.kryptance.sftpgo.api")
    modelPackage.set("io.kryptance.sftpgo.model")
    invokerPackage.set("io.kryptance.sftpgo.invoker")

    generateApiTests.set(false)
    generateModelTests.set(false)

    additionalProperties.set(
        mapOf(
            "openApiNullable" to "false"
        )
    )
}



tasks.withType(GenerateTask::class.java) {
    finalizedBy("addSetImport")
}

val processedDir = layout.buildDirectory.file("processed/src/main/java")

val addSetImport by tasks.registering(Copy::class) {
    dependsOn("openApiGenerate")
    from(generatedDir)
    into(processedDir)
    include("**/*.java")
    // Use a custom filter action: read the entire file and perform a multi‑line replacement.
    eachFile {
        // Read the original file content.
        val originalContent = file.readText()
        if (originalContent.contains("Set<") && !originalContent.contains("import java.util.Set;")) {
            // Insert the import after the package declaration.
            val modifiedContent = originalContent.replaceFirst(
                Regex("^(package\\s+[^;]+;\\s*)", RegexOption.MULTILINE),
                "$1\nimport java.util.Set;\n"
            )
            file.writeText(modifiedContent)
            logger.info("Added import to: ${file.path}")
        }
    }
    // Declare the input and output directories for caching
    inputs.dir(generatedDir)
    outputs.dir(processedDir)
    onlyIf {
        // check if openApiGenerate task has been executed
        generatedDir.get().asFile.exists()
    }
}
tasks.named("compileJava") {
    dependsOn("openApiGenerate", "addSetImport")
}

// Add generated sources to the main source set.
sourceSets["main"].java.srcDir(processedDir)

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}


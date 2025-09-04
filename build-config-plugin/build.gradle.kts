plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    id("com.gradle.plugin-publish") version "1.2.1"
}

repositories {
    // Use Maven Central for resolving dependencies
    mavenCentral()
}

dependencies {
    // Use JUnit test framework for unit tests
    testImplementation("junit:junit:4.13")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation(gradleTestKit())
}

group = "io.github.e-hai"
version = "1.0.1"

gradlePlugin {
    website = "https://github.com/e-hai/AppConfigPlugin"
    vcsUrl = "https://github.com/e-hai/AppConfigPlugin"
    // Define the plugin
    val greeting by plugins.creating {
        id = "io.github.e-hai.config"
        displayName = "Plugin for KMP BuildConfig of Gradle plugins"
        description = "A plugin that helps you create BuildConfig class"
        tags = listOf("BuildConfig", "kmp", "local")
        implementationClass = "com.plugin.config.BuildConfigPlugin"
    }
}


// Add a source set and a task for a functional test suite
val functionalTest by sourceSets.creating
gradlePlugin.testSourceSets(functionalTest)

configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
}

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTestTask)
}

package com.plugin.config;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;


public class BuildConfigPluginTest {
    @Test
    public void pluginRegistersATask() {
        // Create a test project and apply the plugin
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("com.plugin.config");

        // Verify the result
        assertNotNull(project.getTasks().findByName("generateBuildConfig"));
    }
}

package com.plugin.config;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

import org.gradle.testkit.runner.TaskOutcome;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class BuildConfigPluginFunctionalTest {


    @Test
    public void canRunTaskWithBasicConfiguration() throws IOException {
        File projectDir = new File("build/functionalTest");
        Files.createDirectories(projectDir.toPath());
        // 创建 local.properties 文件
        writeString(new File(projectDir, "local.properties"),
                "HOST=https://api.example.com\n" +
                        "API_KEY=test-key-123\n" +
                        "DEBUG_MODE=true");

        // 创建 settings.gradle
        writeString(new File(projectDir, "settings.gradle"),
                "rootProject.name = 'test-project'");

        // 创建 build.gradle
        writeString(new File(projectDir, "build.gradle"),
                "plugins {\n" +
                        "    id('io.github.e-hai.config')\n" +
                        "}\n\n" +
                        "buildConfig {\n" +
                        "    packageName('com.demo.example')\n" +
                        "    className('BuildConfig')\n" +
                        "    propertiesFile('local.properties')\n" +
                        "    field('HOST')\n" +
                        "    field('API_KEY')\n" +
                        "}");

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateBuildConfig", "--rerun-tasks")
                .forwardOutput()
                .build();

        // 验证任务执行成功
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateBuildConfig").getOutcome());

        // 验证生成的文件存在
        File generatedFile = new File(projectDir, "build/buildconfig/com/demo/example/BuildConfig.kt");
        assertTrue(String.valueOf(generatedFile.exists()), true);

        // 验证文件内容
        String content = readFileContent(generatedFile);
        assertAll("Generated file content validation",
                () -> assertTrue(String.valueOf(content.contains("package com.demo.example")), true),
                () -> assertTrue(String.valueOf(content.contains("object BuildConfig")), true)
        );
    }


    private void writeString(File file, String content) {
        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + file.getAbsolutePath(), e);
        }
    }

    private String readFileContent(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getAbsolutePath(), e);
        }
    }
}

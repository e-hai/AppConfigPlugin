package com.plugin.config;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.Internal;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ListProperty;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;

public class BuildConfigPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // 创建插件扩展
        BuildConfigExtension extension = project.getExtensions().create("buildConfig", BuildConfigExtension.class);

        // 创建生成BuildConfig的任务
        project.getTasks().register("generateBuildConfig", GenerateBuildConfigTask.class, task -> {
            setupTask(project, task, extension);
        });
    }

    private void setupTask(Project project, GenerateBuildConfigTask task, BuildConfigExtension extension) {
        // 设置输入文件（从扩展配置获取）
        task.getPropertiesFile().set(extension.getPropertiesFile());

        // 设置输出文件
        task.getPackageName().set(extension.getPackageName());
        task.getClassName().set(extension.getClassName());
        task.getConfigFields().set(extension.getConfigFields());

        // 动态设置输出文件路径
        task.getOutputFile().set(project.getLayout().getBuildDirectory().file(
                extension.getPackageName().map(pkg -> extension.getClassName().map(className -> {
                    String packagePath = pkg.replace(".", "/");
                    return "buildConfig/" + packagePath + "/" + className + ".kt" ;
                }).get()).get()
        ));

        // 设置任务描述和分组
        task.setDescription("Generates BuildConfig file from properties");
        task.setGroup("build setup");

    }

    // 插件扩展类
    public static abstract class BuildConfigExtension {
        // 包名
        public abstract Property<String> getPackageName();

        // 类名
        public abstract Property<String> getClassName();

        // Properties文件路径
        public abstract RegularFileProperty getPropertiesFile();

        // 要从properties文件读取的字段列表
        public abstract ListProperty<String> getConfigFields();

        public BuildConfigExtension() {
            // 设置默认值
            getPackageName().convention("com.example.app");
            getClassName().convention("BuildConfig");
        }

        // DSL方法：设置包名
        public void packageName(String packageName) {
            getPackageName().set(packageName);
        }

        // DSL方法：设置类名
        public void className(String className) {
            getClassName().set(className);
        }

        // DSL方法：设置properties文件路径
        public void propertiesFile(Object file) {
            if (file instanceof String) {
                getPropertiesFile().set(new File((String) file));
            } else if (file instanceof File) {
                getPropertiesFile().set((File) file);
            }
        }

        // DSL方法：添加配置字段
        public void field(String fieldName) {
            getConfigFields().add(fieldName);
        }

        // DSL方法：批量添加字段
        public void fields(String... fieldNames) {
            for (String fieldName : fieldNames) {
                getConfigFields().add(fieldName);
            }
        }

        // DSL方法：批量添加字段（List）
        public void fields(List<String> fieldNames) {
            getConfigFields().addAll(fieldNames);
        }
    }

    @CacheableTask
    public static abstract class GenerateBuildConfigTask extends DefaultTask {

        @InputFile
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract RegularFileProperty getPropertiesFile();

        @OutputFile
        public abstract RegularFileProperty getOutputFile();

        @Internal
        public abstract Property<String> getPackageName();

        @Internal
        public abstract Property<String> getClassName();

        @Internal
        public abstract ListProperty<String> getConfigFields();

        @TaskAction
        public void generateBuildConfig() {
            File propertiesFile = getPropertiesFile().get().getAsFile();
            File outputFile = getOutputFile().get().getAsFile();

            // 验证输入文件存在
            if (!propertiesFile.exists()) {
                throw new RuntimeException("Properties file not found: " + propertiesFile.getAbsolutePath());
            }

            try {
                // 确保输出目录存在
                File outputDir = outputFile.getParentFile();

                if (outputDir.exists() && outputDir.isDirectory()) {
                    File[] files = outputDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            file.delete(); // 删除文件
                        }
                    }
                }


                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    throw new RuntimeException("Failed to create output directory: " + outputDir.getAbsolutePath());
                }

                // 加载属性
                Properties properties = loadProperties(propertiesFile);

                // 生成BuildConfig内容
                String buildConfigContent = generateBuildConfigContent(properties);

                // 写入文件
                writeContentToFile(outputFile, buildConfigContent);

                getLogger().info("Successfully generated {} at: {}",
                        getClassName().get() + ".kt", outputFile.getAbsolutePath());

            } catch (Exception e) {
                throw new RuntimeException("Failed to generate " + getClassName().get() + ".kt", e);
            }
        }

        private Properties loadProperties(File propertiesFile) throws IOException {
            Properties properties = new Properties();
            try (InputStream inputStream = Files.newInputStream(propertiesFile.toPath())) {
                properties.load(inputStream);
            }
            return properties;
        }

        private String generateBuildConfigContent(Properties properties) {
            List<String> configFields = getConfigFields().get();


            StringBuilder configBuilder = new StringBuilder();
            configBuilder.append("package ").append(getPackageName().get()).append("\n\n")
                    .append("/**\n")
                    .append(" * Auto-generated build configuration\n")
                    .append(" * DO NOT EDIT MANUALLY\n")
                    .append(" */\n")
                    .append("object ").append(getClassName().get()).append(" {\n");

            // 生成每个配置字段
            for (String fieldName : configFields) {
                String value = properties.getProperty(fieldName, "");

                if (value.trim().isEmpty()) {
                    getLogger().warn("{} is empty or not set in properties file", fieldName);
                }

                configBuilder.append("    const val ").append(fieldName)
                        .append(": String = \"").append(value).append("\"\n");
            }

            configBuilder.append("}\n");

            return configBuilder.toString();
        }


        private void writeContentToFile(File file, String content) throws IOException {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    file.toPath(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(content);
            }
        }
    }
}
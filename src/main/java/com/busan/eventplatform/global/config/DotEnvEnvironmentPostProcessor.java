//package com.busan.eventplatform.global.config;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.LinkedHashMap;
//import java.util.regex.Pattern;
//import org.springframework.boot.EnvironmentPostProcessor;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
//import org.springframework.core.Ordered;
//import org.springframework.core.env.ConfigurableEnvironment;
//import org.springframework.core.env.StandardEnvironment;
//import org.springframework.core.env.SystemEnvironmentPropertySource;
//
///** Loads the working directory's optional .env before profile selection. */
//public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
//    private static final Pattern ASSIGNMENT = Pattern.compile("^\\s*(?:export\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s*=(.*)$");
//
//    @Override
//    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
//        load(Path.of(".env"), environment);
//    }
//
//    static void load(Path file, ConfigurableEnvironment environment) {
//        if (!Files.exists(file)) {
//            return;
//        }
//        var values = new LinkedHashMap<String, Object>();
//        try {
//            String contents = Files.readString(file, StandardCharsets.UTF_8).replaceFirst("^\\uFEFF", "");
//            for (String line : contents.split("\\R")) {
//                var match = ASSIGNMENT.matcher(line);
//                if (!match.matches()) {
//                    continue;
//                }
//                String value = match.group(2).strip();
//                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
//                        || (value.startsWith("'") && value.endsWith("'")))) {
//                    value = value.substring(1, value.length() - 1);
//                }
//                values.put(match.group(1), value);
//            }
//        } catch (IOException exception) {
//            throw new IllegalStateException("Cannot read the project's .env file", exception);
//        }
//        var source = new SystemEnvironmentPropertySource("projectDotEnv", values);
//        var sources = environment.getPropertySources();
//        if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
//            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, source);
//        } else {
//            sources.addLast(source);
//        }
//    }
//
//    @Override
//    public int getOrder() {
//        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
//    }
//}

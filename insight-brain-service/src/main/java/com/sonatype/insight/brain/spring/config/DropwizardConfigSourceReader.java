/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public final class DropwizardConfigSourceReader
{
  private final ObjectMapper yamlMapper;

  public DropwizardConfigSourceReader() {
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
    this.yamlMapper.findAndRegisterModules();
    this.yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public Map<String, Object> readConfigMap(File configFile) throws IOException {
    String rawConfig = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
    String substitutedConfig = createEnvironmentVariableSubstitutor().replace(rawConfig);

    @SuppressWarnings("unchecked")
    Map<String, Object> configMap = yamlMapper.readValue(substitutedConfig, Map.class);
    return configMap;
  }

  private StringSubstitutor createEnvironmentVariableSubstitutor() {
    StringSubstitutor substitutor = new StringSubstitutor(System::getenv);
    substitutor.setEnableUndefinedVariableException(false);
    substitutor.setEnableSubstitutionInVariables(true);
    return substitutor;
  }

  public <T> T convertValue(Object value, Class<T> valueType) {
    return yamlMapper.convertValue(value, valueType);
  }

  <T> T convertValueStrict(Object value, Class<T> valueType) {
    ObjectMapper strictMapper = yamlMapper.copy();
    strictMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    return strictMapper.convertValue(value, valueType);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ci;

import java.util.List;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.clm.dto.model.ci.config.DownloadConfig;
import com.sonatype.clm.dto.model.ci.config.JavaAnalysisConfig;
import com.sonatype.clm.dto.model.ci.config.JavaScriptAnalysisConfig;
import com.sonatype.clm.dto.model.ci.config.ProxyConfig;
import com.sonatype.clm.dto.model.ci.config.ReachabilityConfig;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * Utility class for validating CI integration configuration DTOs.
 * Provides reusable validation methods for strings, lists, and nested configuration objects.
 *
 * @since 1.201
 */
public final class CiConfigurationValidator
{
  private CiConfigurationValidator() {
    // Utility class - prevent instantiation
  }

  /**
   * Validates the main CI configuration DTO.
   *
   * @param config the configuration to validate
   * @throws BadRequestException if validation fails
   */
  public static void validateConfiguration(final ApiCiConfigurationDto config) {
    if (config == null) {
      throw new BadRequestException("Configuration cannot be null");
    }

    // Validate string fields are non-empty
    validateParameterPriority(config.getParameterPriority());
    validateNonEmptyString(config.getResultFile(), "resultFile");
    validateNonEmptyString(config.getSarifFile(), "sarifFile");

    // Validate string lists contain non-empty strings
    validateNonEmptyStringList(config.getScanPatterns(), "scanPatterns");
    validateNonEmptyStringList(config.getModuleExcludes(), "moduleExcludes");
    validateNonEmptyStringList(config.getAdvancedProperties(), "advancedProperties");

    // Validate nested objects
    validateProxyConfiguration(config.getProxy());
    validateDownloadConfiguration(config.getDownload());
    validateReachabilityConfiguration(config.getReachability());
  }

  /**
   * Validates proxy configuration.
   *
   * @param proxy the proxy configuration to validate
   * @throws BadRequestException if validation fails
   */
  public static void validateProxyConfiguration(final ProxyConfig proxy) {
    if (proxy == null) {
      return;
    }

    validateNonEmptyString(proxy.getHost(), "proxy.host");
  }

  /**
   * Validates download configuration.
   *
   * @param download the download configuration to validate
   * @throws BadRequestException if validation fails
   */
  public static void validateDownloadConfiguration(final DownloadConfig download) {
    if (download == null) {
      return;
    }

    validateNonEmptyString(download.getIqCliUrl(), "downloadConfig.iqCliUrl");
    validateNonEmptyString(download.getIqCliVersion(), "downloadConfig.iqCliVersion");
  }

  /**
   * Validates reachability configuration including nested Java and JavaScript analysis configs.
   *
   * @param reachability the reachability configuration to validate
   * @throws BadRequestException if validation fails
   */
  public static void validateReachabilityConfiguration(final ReachabilityConfig reachability) {
    if (reachability == null) {
      return;
    }

    validateJavaAnalysisConfig(reachability.getJavaAnalysis());
    validateJavaScriptAnalysisConfig(reachability.getJavaScriptAnalysis());
  }

  /**
   * Validates Java analysis configuration.
   *
   * @param javaAnalysis the Java analysis configuration to validate
   * @throws BadRequestException if validation fails
   */
  public static void validateJavaAnalysisConfig(final JavaAnalysisConfig javaAnalysis) {
    if (javaAnalysis == null) {
      return;
    }

    validateEntrypointStrategy(javaAnalysis.getEntrypointStrategy());
    validateNonEmptyStringList(javaAnalysis.getNamespaces(), "reachability.javaAnalysis.namespaces");
  }

  /**
   * Validates JavaScript analysis configuration.
   *
   * @param jsAnalysis the JavaScript analysis configuration to validate
   * @throws BadRequestException if validation fails
   */
  public static void validateJavaScriptAnalysisConfig(final JavaScriptAnalysisConfig jsAnalysis) {
    if (jsAnalysis == null) {
      return;
    }

    validateNonEmptyString(jsAnalysis.getProjectRoot(), "reachability.javaScriptAnalysis.projectRoot");
    validateNonEmptyString(jsAnalysis.getNodeJsExecutable(), "reachability.javaScriptAnalysis.nodeJsExecutable");

    validateNonEmptyStringList(jsAnalysis.getJsSources(), "reachability.javaScriptAnalysis.jsSources");
    validateNonEmptyStringList(jsAnalysis.getJsExcludes(), "reachability.javaScriptAnalysis.jsExcludes");
  }

  /**
   * Validates that the parameter priority is one of the allowed values.
   *
   * @param parameterPriority the parameter priority to validate
   * @throws BadRequestException if validation fails
   */
  public static void validateParameterPriority(final String parameterPriority) {
    if (parameterPriority == null) {
      return;
    }

    if (parameterPriority.trim().isEmpty()) {
      throw new BadRequestException("parameterPriority cannot be empty");
    }

    if (!parameterPriority.equals("CI") && !parameterPriority.equals("API")) {
      throw new BadRequestException("parameterPriority must be either 'CI' or 'API'");
    }
  }

  /**
   * Validates that the entrypoint strategy is one of the allowed values.
   *
   * @param entrypointStrategy the entrypoint strategy to validate
   * @throws BadRequestException if validation fails
   */
  public static void validateEntrypointStrategy(final String entrypointStrategy) {
    if (entrypointStrategy == null) {
      return;
    }

    if (entrypointStrategy.trim().isEmpty()) {
      throw new BadRequestException("reachability.javaAnalysis.entrypointStrategy cannot be empty");
    }

    if (!entrypointStrategy.equals("CONCRETE") &&
        !entrypointStrategy.equals("PUBLIC_CONCRETE") &&
        !entrypointStrategy.equals("ACCESSIBLE_CONCRETE") &&
        !entrypointStrategy.equals("ALL") &&
        !entrypointStrategy.equals("JAVA_MAIN"))
    {
      throw new BadRequestException(
          "reachability.javaAnalysis.entrypointStrategy must be one of: " +
              "CONCRETE, PUBLIC_CONCRETE, ACCESSIBLE_CONCRETE, ALL, JAVA_MAIN");
    }
  }

  /**
   * Validates that a string field is non-empty if provided.
   *
   * @param value the string value to validate
   * @param fieldName the field name for error messages
   * @throws BadRequestException if the string is empty or whitespace-only
   */
  public static void validateNonEmptyString(final String value, final String fieldName) {
    if (value != null && value.trim().isEmpty()) {
      throw new BadRequestException(fieldName + " cannot be empty");
    }
  }

  /**
   * Validates that a string list contains no null or empty values.
   *
   * @param values the list of strings to validate
   * @param fieldName the field name for error messages
   * @throws BadRequestException if any item is null or empty
   */
  public static void validateNonEmptyStringList(final List<String> values, final String fieldName) {
    if (values != null) {
      for (int i = 0; i < values.size(); i++) {
        String value = values.get(i);
        if (value == null || value.trim().isEmpty()) {
          throw new BadRequestException(fieldName + "[" + i + "] cannot be null or empty");
        }
      }
    }
  }
}

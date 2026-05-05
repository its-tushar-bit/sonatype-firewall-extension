/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ci;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationResponseDto;
import com.sonatype.clm.dto.model.ci.config.DownloadConfig;
import com.sonatype.clm.dto.model.ci.config.JavaAnalysisConfig;
import com.sonatype.clm.dto.model.ci.config.JavaScriptAnalysisConfig;
import com.sonatype.clm.dto.model.ci.config.ProxyConfig;
import com.sonatype.clm.dto.model.ci.config.ReachabilityConfig;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for CiConfigurationService business logic.
 *
 * @since 1.201
 */
public class CiConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private CiConfigurationService service;

  @Inject
  private CiIntegrationsConfigDao ciConfigDao;

  @Inject
  private ObjectMapper objectMapper;

  @Test
  public void testGetDirectConfiguration() throws Exception {
    // Given: Root org and child org both have configurations
    ApiCiConfigurationDto rootConfig = new ApiCiConfigurationDto();
    rootConfig.setParameterPriority("CI");
    rootConfig.setEnableDebugLogging(true);
    saveConfig(ROOT_ORGANIZATION_ID, ORGANIZATION, rootConfig);

    Organization childOrg = tempEntity.newOrganization(ROOT_ORGANIZATION_ID);
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("API");
    config.setFailBuildOnPolicyWarnings(true);
    config.setScanPatterns(Arrays.asList("**/*.jar", "**/*.war"));
    saveConfig(childOrg.getId(), ORGANIZATION, config);

    // When: Requesting direct configuration (no inheritance)
    ApiCiConfigurationResponseDto result = service.getConfiguration(ORGANIZATION, childOrg.getId(), true);

    // Then: Only child org's config is returned, not inherited from the root
    assertThat(result).isNotNull();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getParameterPriority()).isEqualTo("API");
    assertThat(result.getData().getFailBuildOnPolicyWarnings()).isTrue();
    assertThat(result.getData().getScanPatterns()).containsExactly("**/*.jar", "**/*.war");
    assertThat(result.getData().getEnableDebugLogging()).isNull();
    assertThat(result.getSource()).isNull();

    // Cleanup
    service.deleteConfiguration(ORGANIZATION, ROOT_ORGANIZATION_ID);
    service.deleteConfiguration(ORGANIZATION, childOrg.getId());
  }

  @Test
  public void testGetMergedConfiguration() throws Exception {
    // Given: Hierarchical configuration at root, child org, and application levels
    Organization childOrg = tempEntity.newOrganization(ROOT_ORGANIZATION_ID);
    Application app = tempEntity.newApplication("myApp", childOrg.getId());

    ApiCiConfigurationDto rootConfig = new ApiCiConfigurationDto();
    rootConfig.setParameterPriority("API");
    rootConfig.setFailBuildOnPolicyWarnings(true);
    rootConfig.setEnableDebugLogging(false);
    saveConfig(ROOT_ORGANIZATION_ID, ORGANIZATION, rootConfig);

    ApiCiConfigurationDto childConfig = new ApiCiConfigurationDto();
    childConfig.setFailBuildOnPolicyWarnings(false);
    childConfig.setScanPatterns(List.of("**/*.jar"));
    saveConfig(childOrg.getId(), ORGANIZATION, childConfig);

    ApiCiConfigurationDto appConfig = new ApiCiConfigurationDto();
    appConfig.setEnableDebugLogging(true);
    saveConfig(app.getId(), APPLICATION, appConfig);

    // When: Requesting merged configuration
    ApiCiConfigurationResponseDto result = service.getConfiguration(APPLICATION, app.getId(), false);

    // Then: Configuration is merged with lower levels taking precedence
    assertThat(result).isNotNull();
    assertThat(result.getData()).isNotNull();

    // Verify merged values
    assertThat(result.getData().getParameterPriority()).isEqualTo("API");
    assertThat(result.getData().getFailBuildOnPolicyWarnings()).isFalse();
    assertThat(result.getData().getEnableDebugLogging()).isTrue();
    assertThat(result.getData().getScanPatterns()).containsExactly("**/*.jar");

    // Verify provenance tracking
    assertThat(result.getSource()).isNotEmpty();
    assertThat(result.getSource()).containsEntry("parameterPriority", ROOT_ORGANIZATION_ID);
    assertThat(result.getSource()).containsEntry("failBuildOnPolicyWarnings", childOrg.getId());
    assertThat(result.getSource()).containsEntry("enableDebugLogging", app.getPublicId());
    assertThat(result.getSource()).containsEntry("scanPatterns", childOrg.getId());

    // Cleanup
    service.deleteConfiguration(ORGANIZATION, ROOT_ORGANIZATION_ID);
    service.deleteConfiguration(ORGANIZATION, childOrg.getId());
    service.deleteConfiguration(APPLICATION, app.getId());
  }

  @Test
  public void testGetConfiguration_notFound() {
    // Given: Organization with no configuration
    String id = tempEntity.newOrganization()
        .getId();

    // When/Then: Requesting configuration throws NotFoundException
    assertThatThrownBy(() -> service.getConfiguration(ORGANIZATION, id, true))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("not found");
  }

  @Test
  public void testSetConfiguration() throws Exception {
    // Given: Valid CI configuration for an organization
    Organization org = tempEntity.newOrganization();

    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    config.setFailBuildOnPolicyWarnings(true);
    config.setScanPatterns(Arrays.asList("**/*.jar", "**/*.war"));

    // When: Setting configuration
    ApiCiConfigurationDto result = service.setConfiguration(ORGANIZATION, org.getId(), config);

    // Then: Configuration is saved and returned correctly
    assertThat(result).isNotNull();
    assertThat(result.getParameterPriority()).isEqualTo("CI");
    assertThat(result.getFailBuildOnPolicyWarnings()).isTrue();
    assertThat(result.getScanPatterns()).containsExactly("**/*.jar", "**/*.war");

    // Verify persisted to database
    assertConfigInDatabase(org.getId());

    // Cleanup
    service.deleteConfiguration(ORGANIZATION, org.getId());
  }

  @Test
  public void testSetConfiguration_updateExisting() throws Exception {
    // Given: Organization with existing configuration
    Organization org = tempEntity.newOrganization();

    ApiCiConfigurationDto initialConfig = createConfig("APP");
    saveConfig(org.getId(), ORGANIZATION, initialConfig);

    // When: Updating with a new configuration
    ApiCiConfigurationDto newConfig = new ApiCiConfigurationDto();
    newConfig.setParameterPriority("API");
    newConfig.setFailBuildOnPolicyWarnings(true);
    service.setConfiguration(ORGANIZATION, org.getId(), newConfig);

    // Then: Configuration is updated in the database
    CiIntegrationsConfig savedConfig = ciConfigDao.findByOwner(ORGANIZATION.toString(), org.getId()).orElse(null);
    assertThat(savedConfig).isNotNull();
    String savedJson = savedConfig.getConfigurationJson();
    assertThat(savedJson)
        .contains("API")
        .contains("failBuildOnPolicyWarnings");

    // Cleanup
    service.deleteConfiguration(ORGANIZATION, org.getId());
  }

  @Test
  public void testSetConfiguration_application() throws Exception {
    // Given: Valid CI configuration for an application
    Application app = tempEntity.newApplicationWithParent();

    ApiCiConfigurationDto config = createConfig("CI");

    // When: Setting application configuration
    ApiCiConfigurationDto result = service.setConfiguration(APPLICATION, app.getId(), config);

    // Then: Configuration is saved successfully
    assertThat(result).isNotNull();
    assertThat(result.getParameterPriority()).isEqualTo("CI");

    // Cleanup
    service.deleteConfiguration(APPLICATION, app.getId());
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    // Given: Organization with existing configuration
    Organization org = tempEntity.newOrganization();

    ApiCiConfigurationDto config = createConfig("CI");
    saveConfig(org.getId(), ORGANIZATION, config);

    // When: Deleting configuration
    service.deleteConfiguration(ORGANIZATION, org.getId());

    // Then: Configuration is deleted successfully
    assertThat(ciConfigDao.findByOwner(ORGANIZATION.toString(), org.getId())).isEmpty();
  }

  @Test
  public void testDeleteConfiguration_notFound() {
    // Given: Organization without configuration
    String id = tempEntity.newOrganization()
        .getId();

    // When/Then: Attempting to delete a non-existent configuration throws NotFoundException
    assertThatThrownBy(() -> service.deleteConfiguration(ORGANIZATION, id))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("not found");
  }

  private void saveConfig(String ownerId, OwnerType ownerType, ApiCiConfigurationDto config) throws Exception {
    ciConfigDao.save(new CiIntegrationsConfig(
        ownerId, ownerType.toString(), objectMapper.writeValueAsString(config)));
  }

  private ApiCiConfigurationDto createConfig(String parameterPriority) {
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority(parameterPriority);
    return config;
  }

  private void assertConfigInDatabase(String ownerId) {
    CiIntegrationsConfig savedConfig = ciConfigDao.findByOwner(OwnerType.ORGANIZATION.toString(), ownerId).orElse(null);
    assertThat(savedConfig).isNotNull();
  }

  // Tests for overrideConfiguration method with deep merge behavior

  @Test
  public void testOverrideConfiguration_proxyDeepMerge() {
    // Given: Base has a proxy with a host, override has a proxy with a different host
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    ProxyConfig baseProxy = new ProxyConfig();
    baseProxy.setHost("base.com");
    base.setProxy(baseProxy);

    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    ProxyConfig overrideProxy = new ProxyConfig();
    overrideProxy.setHost("override.com");
    override.setProxy(overrideProxy);

    Map<String, String> source = new HashMap<>();

    // When: Applying override
    service.overrideConfiguration(base, override, "org-123", source);

    // Then: Proxy host is overridden
    assertThat(base.getProxy()).isNotNull();
    assertThat(base.getProxy().getHost()).isEqualTo("override.com");

    // Provenance tracks nested field
    assertThat(source).containsEntry("proxy.host", "org-123");
  }

  @Test
  public void testOverrideConfiguration_proxyNullDoesNotOverride() {
    // Given: Base has a proxy with a host, override has a proxy with null host
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    ProxyConfig baseProxy = new ProxyConfig();
    baseProxy.setHost("base.com");
    base.setProxy(baseProxy);

    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    ProxyConfig overrideProxy = new ProxyConfig();
    // host is null
    override.setProxy(overrideProxy);

    Map<String, String> source = new HashMap<>();

    // When: Applying override with null field
    service.overrideConfiguration(base, override, "org-123", source);

    // Then: Base host is preserved (not overridden by null)
    assertThat(base.getProxy()).isNotNull();
    assertThat(base.getProxy().getHost()).isEqualTo("base.com");

    // No provenance entry for the null field
    assertThat(source).doesNotContainKey("proxy.host");
  }

  @Test
  public void testOverrideConfiguration_createsMissingNestedObjects() {
    // Given: Base has no proxy, override has a proxy with host
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();

    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    ProxyConfig overrideProxy = new ProxyConfig();
    overrideProxy.setHost("override.com");
    override.setProxy(overrideProxy);

    Map<String, String> source = new HashMap<>();

    // When: Applying override
    service.overrideConfiguration(base, override, "org-111", source);

    // Then: Proxy is created in base
    assertThat(base.getProxy()).isNotNull();
    assertThat(base.getProxy().getHost()).isEqualTo("override.com");
    assertThat(source).containsEntry("proxy.host", "org-111");
  }

  @Test
  public void testOverrideConfiguration_nullOverrideObjectIgnored() {
    // Given: Base has a proxy, override has a null proxy
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    ProxyConfig baseProxy = new ProxyConfig();
    baseProxy.setHost("base.com");
    base.setProxy(baseProxy);

    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    // proxy is null

    Map<String, String> source = new HashMap<>();

    // When: Applying override with a null nested object
    service.overrideConfiguration(base, override, "org-222", source);

    // Then: Base proxy is unchanged
    assertThat(base.getProxy()).isNotNull();
    assertThat(base.getProxy().getHost()).isEqualTo("base.com");
    assertThat(source).isEmpty();
  }

  @Test
  public void testOverrideConfiguration_downloadDeepMerge() {
    // Given: Base has download with URL, override has download with a version
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    DownloadConfig baseDownload = new DownloadConfig();
    baseDownload.setIqCliUrl("https://base.com/cli");
    base.setDownload(baseDownload);

    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    DownloadConfig overrideDownload = new DownloadConfig();
    overrideDownload.setIqCliVersion("2.0.0");
    override.setDownload(overrideDownload);

    Map<String, String> source = new HashMap<>();

    // When: Applying override
    service.overrideConfiguration(base, override, "org-456", source);

    // Then: Both fields are present (deep merge)
    assertThat(base.getDownload()).isNotNull();
    assertThat(base.getDownload().getIqCliUrl()).isEqualTo("https://base.com/cli");
    assertThat(base.getDownload().getIqCliVersion()).isEqualTo("2.0.0");

    // Provenance tracks individual nested fields
    assertThat(source)
        .containsEntry("download.iqCliVersion", "org-456")
        .doesNotContainKey("download.iqCliUrl");
  }

  @Test
  public void testOverrideConfiguration_reachabilityTwoLevelDeepMerge() {
    // Given: Base has reachability with Java analysis entrypoint strategy
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    ReachabilityConfig baseReachability = new ReachabilityConfig();
    JavaAnalysisConfig baseJava = new JavaAnalysisConfig();
    baseJava.setEntrypointStrategy("ALL");
    baseReachability.setJavaAnalysis(baseJava);
    base.setReachability(baseReachability);

    // Override has reachability with Java analysis namespaces
    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    ReachabilityConfig overrideReachability = new ReachabilityConfig();
    JavaAnalysisConfig overrideJava = new JavaAnalysisConfig();
    overrideJava.setNamespaces(Arrays.asList("com.example", "com.test"));
    overrideReachability.setJavaAnalysis(overrideJava);
    override.setReachability(overrideReachability);

    Map<String, String> source = new HashMap<>();

    // When: Applying override
    service.overrideConfiguration(base, override, "org-789", source);

    // Then: Both fields are present (2-level deep merge)
    assertThat(base.getReachability()).isNotNull();
    assertThat(base.getReachability().getJavaAnalysis()).isNotNull();
    assertThat(base.getReachability().getJavaAnalysis().getEntrypointStrategy()).isEqualTo("ALL");
    assertThat(base.getReachability().getJavaAnalysis().getNamespaces())
        .containsExactly("com.example", "com.test");

    // Provenance tracks 2-level nested field
    assertThat(source)
        .containsEntry("reachability.javaAnalysis.namespaces", "org-789")
        .doesNotContainKey("reachability.javaAnalysis.entrypointStrategy");
  }

  @Test
  public void testOverrideConfiguration_multipleNestedObjectsIndependently() {
    // Given: Base has a proxy and download, override modifies both
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    ProxyConfig baseProxy = new ProxyConfig();
    baseProxy.setHost("base-proxy.com");
    base.setProxy(baseProxy);
    DownloadConfig baseDownload = new DownloadConfig();
    baseDownload.setIqCliUrl("https://base.com/cli");
    base.setDownload(baseDownload);

    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    ProxyConfig overrideProxy = new ProxyConfig();
    overrideProxy.setHost("override-proxy.com");
    override.setProxy(overrideProxy);
    DownloadConfig overrideDownload = new DownloadConfig();
    overrideDownload.setIqCliVersion("3.0.0");
    override.setDownload(overrideDownload);

    Map<String, String> source = new HashMap<>();

    // When: Applying override
    service.overrideConfiguration(base, override, "org-333", source);

    // Then: Both nested objects are merged independently
    assertThat(base.getProxy().getHost()).isEqualTo("override-proxy.com");
    assertThat(base.getDownload().getIqCliUrl()).isEqualTo("https://base.com/cli");
    assertThat(base.getDownload().getIqCliVersion()).isEqualTo("3.0.0");

    // Provenance tracks fields from both objects
    assertThat(source)
        .containsEntry("proxy.host", "org-333")
        .containsEntry("download.iqCliVersion", "org-333")
        .doesNotContainKey("download.iqCliUrl");
  }

  @Test
  public void testOverrideConfiguration_javaScriptAnalysisDeepMerge() {
    // Given: Base has reachability with JS analysis project root
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    ReachabilityConfig baseReachability = new ReachabilityConfig();
    JavaScriptAnalysisConfig baseJs = new JavaScriptAnalysisConfig();
    baseJs.setProjectRoot("/base/project");
    baseJs.setJsSources(List.of("src/**/*.js"));
    baseReachability.setJavaScriptAnalysis(baseJs);
    base.setReachability(baseReachability);

    // Override has reachability with JS analysis node executable
    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    ReachabilityConfig overrideReachability = new ReachabilityConfig();
    JavaScriptAnalysisConfig overrideJs = new JavaScriptAnalysisConfig();
    overrideJs.setNodeJsExecutable("/usr/bin/node");
    overrideReachability.setJavaScriptAnalysis(overrideJs);
    override.setReachability(overrideReachability);

    Map<String, String> source = new HashMap<>();

    // When: Applying override
    service.overrideConfiguration(base, override, "org-555", source);

    // Then: Both JS fields are present (deep merge)
    assertThat(base.getReachability()).isNotNull();
    assertThat(base.getReachability().getJavaScriptAnalysis()).isNotNull();
    assertThat(base.getReachability().getJavaScriptAnalysis().getProjectRoot()).isEqualTo("/base/project");
    assertThat(base.getReachability().getJavaScriptAnalysis().getNodeJsExecutable()).isEqualTo("/usr/bin/node");
    assertThat(base.getReachability().getJavaScriptAnalysis().getJsSources()).containsExactly("src/**/*.js");

    // Provenance tracks 2-level nested field
    assertThat(source)
        .containsEntry("reachability.javaScriptAnalysis.nodeJsExecutable", "org-555")
        .doesNotContainKey("reachability.javaScriptAnalysis.projectRoot");
  }

  @Test
  public void testOverrideConfiguration_reachabilityFailOnError() {
    // Given: Base has reachability with failOnError=false
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    ReachabilityConfig baseReachability = new ReachabilityConfig();
    baseReachability.setFailOnError(false);
    base.setReachability(baseReachability);

    // Override has reachability with failOnError=true only
    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    ReachabilityConfig overrideReachability = new ReachabilityConfig();
    overrideReachability.setFailOnError(true);
    override.setReachability(overrideReachability);

    Map<String, String> source = new HashMap<>();

    // When: Applying override
    service.overrideConfiguration(base, override, "org-fail", source);

    // Then: failOnError is overridden
    assertThat(base.getReachability().getFailOnError()).isTrue();

    // Provenance tracks failOnError field
    assertThat(source)
        .containsEntry("reachability.failOnError", "org-fail");
  }

  @Test
  public void testOverrideConfiguration_javaAnalysisEnabledField() {
    // Given: Base has Java analysis with enabled=true and namespaces
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    ReachabilityConfig baseReachability = new ReachabilityConfig();
    JavaAnalysisConfig baseJava = new JavaAnalysisConfig();
    baseJava.setEnabled(true);
    baseReachability.setJavaAnalysis(baseJava);
    base.setReachability(baseReachability);

    // Override has Java analysis with enabled=false only
    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    ReachabilityConfig overrideReachability = new ReachabilityConfig();
    JavaAnalysisConfig overrideJava = new JavaAnalysisConfig();
    overrideJava.setEnabled(false);
    overrideReachability.setJavaAnalysis(overrideJava);
    override.setReachability(overrideReachability);

    Map<String, String> source = new HashMap<>();

    // When: Applying override
    service.overrideConfiguration(base, override, "org-enabled", source);

    // Then: enabled is overridden
    assertThat(base.getReachability().getJavaAnalysis().getEnabled()).isFalse();

    // Provenance tracks enabled field
    assertThat(source)
        .containsEntry("reachability.javaAnalysis.enabled", "org-enabled");
  }

  @Test
  public void testOverrideConfiguration_javaScriptAnalysisEnabledField() {
    // Given: Base has JS analysis with enabled=false
    ApiCiConfigurationDto base = new ApiCiConfigurationDto();
    ReachabilityConfig baseReachability = new ReachabilityConfig();
    JavaScriptAnalysisConfig baseJs = new JavaScriptAnalysisConfig();
    baseJs.setEnabled(false);
    baseReachability.setJavaScriptAnalysis(baseJs);
    base.setReachability(baseReachability);

    // Override has JS analysis with enabled=true only
    ApiCiConfigurationDto override = new ApiCiConfigurationDto();
    ReachabilityConfig overrideReachability = new ReachabilityConfig();
    JavaScriptAnalysisConfig overrideJs = new JavaScriptAnalysisConfig();
    overrideJs.setEnabled(true);
    overrideReachability.setJavaScriptAnalysis(overrideJs);
    override.setReachability(overrideReachability);

    Map<String, String> source = new HashMap<>();

    // When: Applying override
    service.overrideConfiguration(base, override, "org-js-enabled", source);

    // Then: enabled is overridden
    assertThat(base.getReachability().getJavaScriptAnalysis().getEnabled()).isTrue();

    // Provenance tracks enabled field
    assertThat(source)
        .containsEntry("reachability.javaScriptAnalysis.enabled", "org-js-enabled");
  }

  @Test
  public void testSetConfiguration_validParameterPriority() {
    Organization org = tempEntity.newOrganization();

    // No exception should be thrown for valid values
    ApiCiConfigurationDto configCI = new ApiCiConfigurationDto();
    configCI.setParameterPriority("CI");
    service.setConfiguration(ORGANIZATION, org.getId(), configCI);

    ApiCiConfigurationDto configAPI = new ApiCiConfigurationDto();
    configAPI.setParameterPriority("API");
    service.setConfiguration(ORGANIZATION, org.getId(), configAPI);

    ApiCiConfigurationDto configNull = new ApiCiConfigurationDto();
    configNull.setParameterPriority(null);
    service.setConfiguration(ORGANIZATION, org.getId(), configNull);

    // Cleanup
    service.deleteConfiguration(ORGANIZATION, org.getId());
  }

  @Test
  public void testSetConfiguration_invalidParameterPriority() {
    Organization org = tempEntity.newOrganization();

    ApiCiConfigurationDto configEmpty = new ApiCiConfigurationDto();
    configEmpty.setParameterPriority("");
    assertThatThrownBy(() -> service.setConfiguration(ORGANIZATION, org.getId(), configEmpty))
        .hasMessage("parameterPriority cannot be empty");

    ApiCiConfigurationDto configWhitespace = new ApiCiConfigurationDto();
    configWhitespace.setParameterPriority("   ");
    assertThatThrownBy(() -> service.setConfiguration(ORGANIZATION, org.getId(), configWhitespace))
        .hasMessage("parameterPriority cannot be empty");

    ApiCiConfigurationDto configInvalid = new ApiCiConfigurationDto();
    configInvalid.setParameterPriority("INVALID");
    assertThatThrownBy(() -> service.setConfiguration(ORGANIZATION, org.getId(), configInvalid))
        .hasMessage("parameterPriority must be either 'CI' or 'API'");

    ApiCiConfigurationDto configLowercase = new ApiCiConfigurationDto();
    configLowercase.setParameterPriority("ci");
    assertThatThrownBy(() -> service.setConfiguration(ORGANIZATION, org.getId(), configLowercase))
        .hasMessage("parameterPriority must be either 'CI' or 'API'");
  }
}

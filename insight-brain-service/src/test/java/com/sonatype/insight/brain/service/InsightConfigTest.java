/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class InsightConfigTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(InsightConfig.class);

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void testClusterDirectory() {
    InsightConfig config = new InsightConfig();
    config.setSonatypeWork(tempDir.getRoot().getAbsolutePath());
    assertThat(config.getClusterDirectory()).isEqualTo(config.getSonatypeWork());
    assertThat(config.isValidClusterDirectory()).isTrue();
    assertThat(config.isClusterDirectorySetByUser()).isFalse();

    config.setClusterDirectory(config.getSonatypeWork().getPath());
    assertThat(config.isValidClusterDirectory()).isFalse();
    assertThat(config.isClusterDirectorySetByUser()).isTrue();

    config.setClusterDirectory(tempDir.getRoot().getAbsolutePath() + "/cluster-directory");
    assertThat(config.isValidClusterDirectory()).isTrue();
    assertThat(config.isClusterDirectorySetByUser()).isTrue();
  }

  @Test
  public void testFeatures() {
    InsightConfig config = new InsightConfig();

    // test unspecified feature is enabled
    assertThat(config.getFeatures()).isNull();
    assertThat(config.isFeatureEnabled(Feature.PR_COMMENTING)).isTrue();

    // test feature is enabled, when feature flag is set to true
    Map<String, Boolean> features = new HashMap<>();
    features.put("featureOne", true);
    config.setFeatures(features);
    assertThat(config.getFeatures()).isNotNull();
    assertThat(config.isFeatureEnabled("featureOne")).isTrue();

    // test feature is disabled, when feature flag is set to false
    features.put("featureOne", false);
    assertThat(config.getFeatures()).isNotNull();
    assertThat(config.isFeatureEnabled("featureOne")).isFalse();
  }

  /**
   * @deprecated The tested method is deprecated.
   */
  @Test
  @Deprecated
  public void testSetAnonymousClientAccessAllowed() {
    InsightConfig config = new InsightConfig();

    config.setAnonymousClientAccessAllowed(true);

    assertThat(logOutput).atWarnLevel()
        .contains("The support for anonymous client access was removed in Nexus IQ Server 72. "
            + "The anonymousClientAccessAllowed configuration option should be removed from the config yml file.");
  }

  /**
   * @deprecated The tested method is deprecated.
   */
  @Test
  @Deprecated
  public void testsetShowRootOrganization() {
    InsightConfig config = new InsightConfig();

    config.setShowRootOrganization(true);

    assertThat(logOutput).atWarnLevel()
        .contains("The support for hiding the root organization was removed in Nexus IQ Server 98. "
            + "The showRootOrganization configuration option should be removed from the config yml file.");
  }

  /**
   * @deprecated The tested method is deprecated.
   */
  @Test
  @Deprecated
  public void testSetConsentToUpgradeToVersion_1_45() {
    InsightConfig config = new InsightConfig();

    config.setConsentToUpgradeToVersion_1_45(true);

    assertThat(logOutput).atWarnLevel().contains("The consentToUpgradeToVersion_1_45 configuration option is " +
        "obsolete and can be removed from the config yml file.");
  }

  @Test
  public void testGetClusterDirectory() {
    InsightConfig insightConfig = new InsightConfig();
    assertThat(insightConfig.getClusterDirectory()).isEqualTo(insightConfig.getSonatypeWork());

    String clusterDirectory = "someDirectory";
    insightConfig.setClusterDirectory(clusterDirectory);
    assertThat(insightConfig.getClusterDirectory()).isEqualTo(new File(clusterDirectory));
  }

  @Test
  public void testIsClusterDirectorySetByUser() {
    InsightConfig insightConfig = new InsightConfig();
    assertThat(insightConfig.isClusterDirectorySetByUser()).isFalse();

    assertThat(insightConfig.getSonatypeWork()).isNotNull();
    insightConfig.setClusterDirectory(insightConfig.getSonatypeWork().getPath());
    assertThat(insightConfig.isClusterDirectorySetByUser()).isTrue();

    insightConfig.setClusterDirectory("someDirectory");
    assertThat(insightConfig.isClusterDirectorySetByUser()).isTrue();
  }

  @Test
  public void testDropwizardWebConfig_SetsHstsConfigCorrectly() {
    InsightConfig config = new InsightConfig();

    // verify the defaults
    assertThat(config.getWebConfiguration()).isNotNull();
    assertThat(config.getWebConfiguration().getHstsHeaderFactory()).isNotNull();
    assertThat(config.getWebConfiguration().getHstsHeaderFactory().isEnabled()).isTrue();
    assertThat(config.getWebConfiguration().getHstsHeaderFactory().getMaxAge()).isEqualTo(Duration.days(365));
    assertThat(config.getWebConfiguration().getHstsHeaderFactory().isIncludeSubDomains()).isTrue();
  }

  @Test
  public void testInsightConfigIsFrozen() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    objectMapper.registerModule(new Jdk8Module());

    InsightConfig insightConfig = new InsightConfig();
    JsonNode allJsonNode = objectMapper.valueToTree(insightConfig);
    Set<String> allFieldNames = new TreeSet<>();
    getAllFieldNames("", allJsonNode, allFieldNames);
    Set<String> inheritedFieldNames = new TreeSet<>();
    Configuration configuration = new Configuration();
    JsonNode inheritedJsonNode = objectMapper.valueToTree(configuration);
    getAllFieldNames("", inheritedJsonNode, inheritedFieldNames);
    Set<String> insightFieldNames = new TreeSet<>(allFieldNames);
    insightFieldNames.removeAll(inheritedFieldNames);

    assertThat(insightFieldNames)
        .as(
            "InsightConfig should not be changed except for migrating values from config.yml to the database." +
                " Any new configuration should be added to the database," +
                " see https://github.com/sonatype/insight-brain#adding-configuration for more information.")
        .containsExactly(
            "additionalDBParams",
            "advancedSearchCSVExportDelimiter",
            "baseUrl",
            "blockBackslashInPath",
            "blockNonAsciiInPath",
            "blockSemicolonInPath",
            "cdnUrl",
            "clusterDirectory",
            "connectTimeoutInSeconds",
            "createSampleData",
            "cspEnabled",
            "csrfProtection",
            "database",
            "dbBackupDir",
            "dbCacheSizePercent",
            "defaultBranchMonitoring",
            "enableDefaultPasswordWarning",
            "eventBus",
            "exitOnFatalError",
            "externalHyperlinksAllowed",
            "features",
            "forceBaseUrl",
            "hdsUrl",
            "importReferencePoliciesFromHDS",
            "jira",
            "licenseFile",
            "licenseLegalHdsRequestLimit",
            "mail",
            "matcherConfiguration",
            "maxAdvancedSearchClauseCount",
            "maxApplicationsToQueryOnDashboard",
            "needsAcknowledgementOfInitialDashboardFilter",
            "policyMonitoringHour",
            "proxy",
            "pullRequestMonitoringIntervalInSeconds",
            "releaseGraphCacheSize",
            "reportTimeoutInSeconds",
            "reverseProxyAuthentication",
            "socketTimeoutInSeconds",
            "sonatypeWork",
            "sourceControl",
            "support",
            "systemAllowlist",
            "userAgentSuffix",
            "web",
            "web.content-type-options",
            "web.content-type-options.enabled",
            "web.cors",
            "web.csp",
            "web.frame-options",
            "web.frame-options.enabled",
            "web.frame-options.option",
            "web.frame-options.origin",
            "web.headers",
            "web.hsts",
            "web.hsts.enabled",
            "web.hsts.includeSubDomains",
            "web.hsts.maxAge",
            "web.hsts.preload",
            "web.uriPath",
            "web.xss-protection",
            "web.xss-protection.block",
            "web.xss-protection.enabled",
            "web.xss-protection.on",
            "webhookSecretPassphrase");
  }

  private void getAllFieldNames(String name, JsonNode jsonNode, Set<String> fieldNames) {
    if (jsonNode.isObject()) {
      Iterator<Entry<String, JsonNode>> fields = jsonNode.fields();
      fields.forEachRemaining(field -> {
        String childName = name + field.getKey();
        fieldNames.add(childName);
        getAllFieldNames(childName + ".", field.getValue(), fieldNames);
      });
    }
    else if (jsonNode.isArray()) {
      jsonNode.forEach(node -> getAllFieldNames(name, node, fieldNames));
    }
  }
}

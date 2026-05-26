/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.test.LogOutput;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

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

    assertThat(config.getFeatures()).isNull();
    assertThat(config.isFeatureEnabled(Feature.PR_COMMENTING)).isTrue();

    Map<String, Boolean> features = new HashMap<>();
    features.put("featureOne", true);
    config.setFeatures(features);
    assertThat(config.getFeatures()).isNotNull();
    assertThat(config.isFeatureEnabled("featureOne")).isTrue();

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

    assertThat(logOutput).atWarnLevel()
        .contains("The consentToUpgradeToVersion_1_45 configuration option is obsolete and can be removed from the "
            + "config yml file.");
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
  public void testHstsConfigDefaults() {
    InsightConfig config = new InsightConfig();
    assertThat(config.getHstsConfig().isEnabled()).isTrue();
    assertThat(config.getHstsConfig().getMaxAgeSeconds()).isEqualTo(365L * 24 * 60 * 60);
    assertThat(config.getHstsConfig().isIncludeSubDomains()).isTrue();
    assertThat(config.getHstsConfig().isPreload()).isFalse();
  }

  @Test
  public void testFrameOptionsConfigDefaults() {
    InsightConfig config = new InsightConfig();
    assertThat(config.getFrameOptionsConfig().isEnabled()).isFalse();
    assertThat(config.getFrameOptionsConfig().buildHeaderValue()).isEqualTo("DENY");
  }

  @Test
  public void testGetApplicationConnectorPorts() {
    InsightConfig config = new InsightConfig();
    assertThat(config.getApplicationConnectorPorts()).isEqualTo("8070");
  }

  @Test
  public void testStorageConfig_default() {
    InsightConfig config = new InsightConfig();

    assertThat(config.isValidStorageConfig()).isTrue();
    assertThat(logOutput).atErrorLevel().isEmpty();
  }

  @Test
  public void testStorageConfig_null() {
    InsightConfig config = new InsightConfig();
    config.setStorage(null);

    assertThat(config.isValidStorageConfig()).isTrue();
    assertThat(logOutput).atErrorLevel().isEmpty();
  }

  @Test
  public void testStorageConfig_fileType_valid() {
    InsightConfig config = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.FILE);
    config.setStorage(storageConfig);

    assertThat(config.isValidStorageConfig()).isTrue();
    assertThat(logOutput).atErrorLevel().isEmpty();
  }

  @Test
  public void testStorageConfig_s3Type_valid() {
    InsightConfig config = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.S3);
    S3DataStoreConfig s3 = new S3DataStoreConfig();
    s3.setBucketName("bucket");
    s3.setRegion("us-east-1");
    s3.setObjectKeyPrefix("prefix/");
    storageConfig.setS3Config(s3);
    config.setStorage(storageConfig);

    assertThat(config.isValidStorageConfig()).isTrue();
    assertThat(logOutput).atErrorLevel().isEmpty();
  }

  @Test
  public void testStorageConfig_s3Type_missingS3Config() {
    InsightConfig config = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.S3);
    config.setStorage(storageConfig);

    assertThat(config.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: s3Config is required when the data store type is S3.");
  }

  @Test
  public void testStorageConfig_s3Type_missingBucket() {
    InsightConfig config = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.S3);
    S3DataStoreConfig s3 = new S3DataStoreConfig();
    s3.setRegion("us-east-1");
    storageConfig.setS3Config(s3);
    config.setStorage(storageConfig);

    assertThat(config.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: Property 'bucketName' must be provided and non-empty.");
  }

  @Test
  public void testStorageConfig_s3Type_missingRegion() {
    InsightConfig config = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.S3);
    S3DataStoreConfig s3 = new S3DataStoreConfig();
    s3.setBucketName("bucket");
    storageConfig.setS3Config(s3);
    config.setStorage(storageConfig);

    assertThat(config.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: Property 'region' must be provided and non-empty.");
  }

  @Test
  public void testStorageConfig_s3Type_invalidObjectKeyPrefix() {
    InsightConfig config = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.S3);
    S3DataStoreConfig s3 = new S3DataStoreConfig();
    s3.setBucketName("bucket");
    s3.setRegion("us-east-1");
    s3.setObjectKeyPrefix("invalid prefix!@#");
    storageConfig.setS3Config(s3);
    config.setStorage(storageConfig);

    assertThat(config.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: Property 'objectKeyPrefix' does not match the expected regex pattern "
            + S3DataStoreConfig.S3_KEY_PREFIX);
  }

  @Test
  public void testStorageConfig_s3Type_invalidServerSideEncryption() {
    S3DataStoreConfig s3DataStoreConfig = new S3DataStoreConfig();
    s3DataStoreConfig.setBucketName("bucket");
    s3DataStoreConfig.setRegion("us-east-1");
    s3DataStoreConfig.setServerSideEncryption("doesNotExist");

    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.S3);
    storageConfig.setS3Config(s3DataStoreConfig);

    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setStorage(storageConfig);

    assertThat(insightConfig.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: Property 'serverSideEncryption' with value 'doesNotExist' "
            + "does not correspond to a known server side encryption algorithm.");
  }

  @Test
  public void testStorageConfig_s3Type_validServerSideEncryption() {
    for (ServerSideEncryption serverSideEncryption : ServerSideEncryption.knownValues()) {
      S3DataStoreConfig s3DataStoreConfig = new S3DataStoreConfig();
      s3DataStoreConfig.setBucketName("bucket");
      s3DataStoreConfig.setRegion("us-east-1");
      s3DataStoreConfig.setServerSideEncryption(serverSideEncryption.toString());

      StorageConfig storageConfig = new StorageConfig();
      storageConfig.setType(DataStoreType.S3);
      storageConfig.setS3Config(s3DataStoreConfig);

      InsightConfig insightConfig = new InsightConfig();
      insightConfig.setStorage(storageConfig);

      assertThat(insightConfig.isValidStorageConfig()).isTrue();
      assertThat(ServerSideEncryption.fromValue(s3DataStoreConfig.getServerSideEncryption())).isEqualTo(
          serverSideEncryption);
    }
  }

  @Test
  public void testStorageConfig_hybridType_valid() {
    InsightConfig insightConfig = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.HYBRID);

    S3DataStoreConfig s3Config = new S3DataStoreConfig();
    s3Config.setBucketName("bucket");
    s3Config.setRegion("us-east-1");
    s3Config.setObjectKeyPrefix("prefix/");
    storageConfig.setS3Config(s3Config);

    HybridDataStoreConfig hybridConfig = new HybridDataStoreConfig();
    hybridConfig.setTypes(new LinkedHashSet<>(List.of(DataStoreType.FILE, DataStoreType.S3)));
    storageConfig.setHybridConfig(hybridConfig);
    insightConfig.setStorage(storageConfig);

    assertThat(insightConfig.isValidStorageConfig()).isTrue();
    assertThat(logOutput).atErrorLevel().isEmpty();
  }

  @Test
  public void testStorageConfig_hybridType_invalid_nullHybridStorage() {
    InsightConfig insightConfig = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.HYBRID);
    insightConfig.setStorage(storageConfig);

    assertThat(insightConfig.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: hybridConfig is required when the "
            + "data store type is hybrid.");
  }

  @Test
  public void testStorageConfig_hybridType_invalid_nullTypes() {
    InsightConfig insightConfig = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.HYBRID);
    HybridDataStoreConfig hybridConfig = new HybridDataStoreConfig();
    storageConfig.setHybridConfig(hybridConfig);
    insightConfig.setStorage(storageConfig);

    assertThat(insightConfig.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: Property 'types' must be provided "
            + "and at least have 2 elements.");
  }

  @Test
  public void testStorageConfig_hybridType_invalid_emptyTypes() {
    InsightConfig insightConfig = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.HYBRID);
    HybridDataStoreConfig hybridConfig = new HybridDataStoreConfig();
    hybridConfig.setTypes(new LinkedHashSet<>());
    storageConfig.setHybridConfig(hybridConfig);
    insightConfig.setStorage(storageConfig);

    assertThat(insightConfig.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: Property 'types' must be provided "
            + "and at least have 2 elements.");
  }

  @Test
  public void testStorageConfig_hybridType_invalid_onlyOneType() {
    InsightConfig insightConfig = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.HYBRID);
    HybridDataStoreConfig hybridConfig = new HybridDataStoreConfig();
    hybridConfig.setTypes(new LinkedHashSet<>(List.of(DataStoreType.FILE)));
    storageConfig.setHybridConfig(hybridConfig);
    insightConfig.setStorage(storageConfig);

    assertThat(insightConfig.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: Property 'types' must be provided "
            + "and at least have 2 elements.");
  }

  @Test
  public void testStorageConfig_hybridType_invalid_containsHybridType() {
    InsightConfig insightConfig = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.HYBRID);
    HybridDataStoreConfig hybridConfig = new HybridDataStoreConfig();
    hybridConfig.setTypes(new LinkedHashSet<>(List.of(DataStoreType.FILE, DataStoreType.HYBRID)));
    storageConfig.setHybridConfig(hybridConfig);
    insightConfig.setStorage(storageConfig);

    assertThat(insightConfig.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: Property 'types' cannot contain "
            + "'HYBRID'.");
  }

  @Test
  public void testStorageConfig_hybridType_invalid_s3SetButNotConfigured() {
    InsightConfig insightConfig = new InsightConfig();
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.HYBRID);

    HybridDataStoreConfig hybridConfig = new HybridDataStoreConfig();
    hybridConfig.setTypes(new LinkedHashSet<>(List.of(DataStoreType.FILE, DataStoreType.S3)));
    storageConfig.setHybridConfig(hybridConfig);
    insightConfig.setStorage(storageConfig);

    assertThat(insightConfig.isValidStorageConfig()).isFalse();
    assertThat(logOutput).atErrorLevel()
        .contains("Invalid storage configuration: s3Config is required when the data store type is S3.");
  }

  @Test
  public void testInsightConfigIsFrozen() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    objectMapper.registerModule(new Jdk8Module());

    InsightConfig insightConfig = new InsightConfig();
    JsonNode jsonNode = objectMapper.valueToTree(insightConfig);
    Set<String> fieldNames = new TreeSet<>();
    getAllFieldNames("", jsonNode, fieldNames);

    assertThat(fieldNames)
        .as(
            "InsightConfig should not be changed except for migrating values from config.yml to the database."
                + " Any new configuration should be added to the database,"
                + " see https://github.com/sonatype/insight-brain#adding-configuration for more information.")
        .containsExactly(
            "additionalDBParams",
            "admin",
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
            "health",
            "importReferencePoliciesFromHDS",
            "jira",
            "licenseFile",
            "licenseLegalHdsRequestLimit",
            "logging",
            "mail",
            "matcherConfiguration",
            "maxAdvancedSearchClauseCount",
            "maxApplicationsToQueryOnDashboard",
            "metrics",
            "needsAcknowledgementOfInitialDashboardFilter",
            "policyMonitoringHour",
            "proxy",
            "pullRequestMonitoringIntervalInSeconds",
            "releaseGraphCacheSize",
            "reportTimeoutInSeconds",
            "reverseProxyAuthentication",
            "search",
            "server",
            "socketTimeoutInSeconds",
            "sonatypeWork",
            "sourceControl",
            "storage",
            "storage.hybridConfig",
            "storage.s3Config",
            "storage.type",
            "support",
            "systemAllowlist",
            "userAgentSuffix",
            "web",
            "webhookSecretPassphrase",
            "webhookSecretPassphraseFips");
  }

  private void getAllFieldNames(String name, JsonNode jsonNode, Set<String> fieldNames) {
    if (jsonNode.isObject()) {
      Set<Entry<String, JsonNode>> fields = jsonNode.properties();
      fields.forEach(field -> {
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

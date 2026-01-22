/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.FeatureAlreadyDisabledException;
import com.sonatype.insight.brain.api.v2.FeatureAlreadyEnabledException;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.*;

/**
 * @since 1.142
 */
@Named
public class SimpleConfigurationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(SimpleConfigurationMigrator.class);

  // Visible for testing
  static final String MIGRATION_ID = "simple-config";

  // Visible for testing
  static final Map<String, Function<InsightConfig, Object>> NAME_TO_GETTER;

  static {
    Map<String, Function<InsightConfig, Object>> nameToGetter = new LinkedHashMap<>();
    nameToGetter.put(HDS_URL, InsightConfig::getHdsUrl);
    nameToGetter.put(CDN_URL, InsightConfig::getCdnUrl);
    nameToGetter.put(SUPPORT_READ_LIMIT_BYTES,
        config -> config.getSupportConfig() == null ? null : config.getSupportConfig().getReadLimitBytes());
    nameToGetter.put(EVENT_BUS_MAX_THREAD_POOL_SIZE,
        config -> config.getEventBusConfig() == null ? null : config.getEventBusConfig().getMaxPoolSize());
    nameToGetter.put(CSRF_PROTECTION, InsightConfig::isCsrfProtection);
    nameToGetter.put(USER_AGENT_SUFFIX, InsightConfig::getUserAgentSuffix);
    nameToGetter.put(CSP_ENABLED, InsightConfig::isCspEnabled);
    nameToGetter.put(BLOCK_SEMICOLON_IN_PATH, InsightConfig::isBlockSemicolonInPath);
    nameToGetter.put(BLOCK_BACKSLASH_IN_PATH, InsightConfig::isBlockBackslashInPath);
    nameToGetter.put(BLOCK_NON_ASCII_IN_PATH, InsightConfig::isBlockNonAsciiInPath);
    nameToGetter.put(RELEASE_GRAPH_CACHE_SIZE, InsightConfig::getReleaseGraphCacheSize);
    nameToGetter.put(LICENSE_LEGAL_HDS_REQUEST_LIMIT, InsightConfig::getLicenseLegalHdsRequestLimit);
    nameToGetter.put(MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD, InsightConfig::getMaxApplicationsToQueryOnDashboard);
    nameToGetter.put(MAX_ADVANCED_SEARCH_CLAUSE_COUNT, InsightConfig::getMaxAdvancedSearchClauseCount);
    nameToGetter.put(ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, InsightConfig::getAdvancedSearchCSVExportDelimiter);
    nameToGetter.put(CONNECT_TIMEOUT_IN_SECONDS, InsightConfig::getConnectTimeoutInSeconds);
    nameToGetter.put(SOCKET_TIMEOUT_IN_SECONDS, InsightConfig::getSocketTimeoutInSeconds);
    nameToGetter.put(REPORT_TIMEOUT_IN_SECONDS, InsightConfig::getReportTimeoutInSeconds);
    nameToGetter.put(NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
        InsightConfig::isNeedsAcknowledgementOfInitialDashboardFilter);
    nameToGetter.put(ENABLE_DEFAULT_PASSWORD_WARNING, InsightConfig::isEnableDefaultPasswordWarning);
    nameToGetter.put(POLICY_MONITORING_HOUR, InsightConfig::getPolicyMonitoringHour);
    nameToGetter.put(DB_BACKUP_DIR, InsightConfig::getDbBackupDir);
    nameToGetter.put(WEBHOOK_SECRET_PASSPHRASE, InsightConfig::getWebhookSecretPassphrase);
    nameToGetter.put(WEBHOOK_SECRET_PASSPHRASE_FIPS, InsightConfig::getWebhookSecretPassphraseFips);
    nameToGetter.put(EXTERNAL_HYPERLINKS_ALLOWED, InsightConfig::isExternalHyperlinksAllowed);
    nameToGetter.put(MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, config ->
        config.getMatcherConfiguration() == null ? null : ConfigurationUtils.parseBooleanWithDefault(
            config.getMatcherConfiguration().get("disableConanNamespaceMatching"), null));
    for (Feature feature : Feature.values()) {
      nameToGetter.put(feature.getFlag(), config -> getFeature(config, feature));
    }
    NAME_TO_GETTER = Collections.unmodifiableMap(nameToGetter);
  }

  private static Boolean getFeature(InsightConfig config, Feature feature) {
    return config.getFeatures() == null ? null : config.getFeatures().get(feature.getFlag());
  }

  // Visible for testing
  static final Set<String> FEATURE_FLAGS = new HashSet<>();

  static {
    FEATURE_FLAGS.addAll(Arrays.stream(Feature.values()).map(Feature::getFlag).collect(Collectors.toSet()));
  }

  private static final String ALL_TO_MIGRATE = String.join(", ", NAME_TO_GETTER.keySet());

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ApiConfigurationService configurationService;

  private final ApiConfigFeaturesService configFeaturesService;

  private final InsightConfig insightConfig;

  @Inject
  public SimpleConfigurationMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      ApiConfigurationService configurationService,
      ApiConfigFeaturesService configFeaturesService,
      InsightConfig insightConfig)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.configurationService = configurationService;
    this.configFeaturesService = configFeaturesService;
    this.insightConfig = insightConfig;
  }

  void migrate() {
    Map<String, Object> nonNullToMigrate = NAME_TO_GETTER.entrySet()
        .stream()
        .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().apply(insightConfig)))
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v2, LinkedHashMap::new));

    if (!nonNullToMigrate.isEmpty()) {
      log.warn("{} is now configured using the REST API. " +
              "The configuration in the config.yml or via system properties is obsolete.",
          String.join(", ", nonNullToMigrate.keySet()));
    }

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("{} configuration already migrated.", ALL_TO_MIGRATE);
      return;
    }

    log.debug("Migrating {} configuration to the database...", ALL_TO_MIGRATE);

    Set<String> migratedProperties = new HashSet<>();
    nonNullToMigrate.forEach((key, value) -> {
      if (FEATURE_FLAGS.contains(key)) {
        boolean booleanValue = (boolean) value;
        try {
          if (booleanValue) {
            configFeaturesService.enableFeatureNoAuthz(key);
          }
          else {
            configFeaturesService.disableFeatureNoAuthz(key);
          }
        }
        catch (FeatureAlreadyEnabledException | FeatureAlreadyDisabledException e) {
          // noop (already enabled/disabled)
        }
      }
      else {
        try {
          configurationService.setConfigurationInDatabaseNoAuthz(key, value);
          migratedProperties.add(key);
        }
        catch (BadRequestException e) {
          log.warn("The current {} configuration is invalid and cannot be migrated.", key, e);
        }
      }
    });
    if (!migratedProperties.isEmpty()) {
      configurationService.applyConfigurationToClients(migratedProperties);
    }

    migrationTrackerDAO.insertTracker(MIGRATION_ID);

    log.info("Migrated {} configurations to the database.", ALL_TO_MIGRATE);
  }
}

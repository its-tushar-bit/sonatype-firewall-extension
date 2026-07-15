/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FeaturePropertiesInfoTest
    extends AbstractComponentTest
{
  @Inject
  FeaturePropertiesInfo featurePropertiesInfo;

  @Test
  public void testGetSystemConfigPropertiesJson_defaultSysConfig() throws IOException {
    JsonNode sysConfigNode = JsonUtils.parse(featurePropertiesInfo.getSystemConfigPropertiesJson());

    assertThat(sysConfigNode.size()).isEqualTo(32);
    assertThat(sysConfigNode.get(SystemConfigurationProperty.AUTO_WAIVERS).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.FORCE_BASE_URL).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT)
        .asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.CSP_ENABLED).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)
        .asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED)
        .asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SBOM_POLICIES).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER)
        .asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED)
        .asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ALP_FOR_SBOM_MANAGER).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.CSRF_PROTECTION).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SBOM_BINARY_SCANNING).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_API).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.CONSUMPTION_REPORTING_ENABLED).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ZSCALER).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.USER_MANAGEMENT_PAGES).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.EPSS_DATA).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.EXIT_ON_FATAL_ERROR).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.MALICIOUS_URLS_PARTNER_ACCESS).asBoolean()).isFalse();
  }

  @Test
  public void testGetSystemConfigPropertiesJson_changedSysConfig() throws IOException {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.AUTO_WAIVERS, "false");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION, "true");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.FORCE_BASE_URL, "true");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SBOM_POLICIES, "false");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.ALP_FOR_SBOM_MANAGER, "true");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, "false");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS, "true");

    JsonNode sysConfigNode = JsonUtils.parse(featurePropertiesInfo.getSystemConfigPropertiesJson());

    assertThat(sysConfigNode.size()).isEqualTo(32);
    assertThat(sysConfigNode.get(SystemConfigurationProperty.AUTO_WAIVERS).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.FORCE_BASE_URL).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT)
        .asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.CSP_ENABLED).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)
        .asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED)
        .asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SBOM_POLICIES).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER)
        .asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED)
        .asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ALP_FOR_SBOM_MANAGER).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.CSRF_PROTECTION).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.SBOM_BINARY_SCANNING).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_API).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.CONSUMPTION_REPORTING_ENABLED).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.ZSCALER).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.USER_MANAGEMENT_PAGES).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.EPSS_DATA).asBoolean()).isFalse();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.EXIT_ON_FATAL_ERROR).asBoolean()).isTrue();
    assertThat(sysConfigNode.get(SystemConfigurationProperty.MALICIOUS_URLS_PARTNER_ACCESS).asBoolean()).isFalse();
  }

  @Test
  public void testGetFeatureConfigPropertiesJson_defaultFeatureConfig() throws IOException {
    JsonNode featureConfigNode = JsonUtils.parse(featurePropertiesInfo.getFeatureConfigPropertiesJson());
    assertThat(featureConfigNode.size()).isEqualTo(77);
    assertThat(featureConfigNode).isEqualTo(JsonUtils.parse(
        """
            {
              "ADVANCED_SEARCH_CONFIGURATION": true,
              "ADVANCED_SEARCH_ENABLED": false,
              "GLOBAL_SEARCH": false,
              "alpForSbomManager": false,
              "API_PAGE": true,
              "AUTOMATIC_APPLICATION_CONFIGURATION": true,
              "AUTOMATIC_SCM_CONFIGURATION": true,
              "autoWaivers": true,
              "BUILT_FROM_SOURCE": false,
              "PREVIEW_NEXUS_ONE_UI": false,
              "PREVIEW_NEXUS_ONE_UI_ANONYMOUS_ENABLED": false,
              "PREVIEW_NEXUS_ONE_UI_DEFAULT_TO_PREVIEW": false,
              "PREVIEW_NEXUS_ONE_UI_DISABLE_SWITCH_FEEDBACK": false,
              "PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED": false,
              "cleanUpSbomContinuousMonitoringReport": true,
              "codeInsights": true,
              "componentChangeDetectionApi": false,
              "componentSearchApiWithInnerSource": true,
              "CROWD_INTEGRATION": true,
              "dashboard": true,
              "defaultBranchMonitoring": true,
              "dependencyDataInApi": true,
              "developerBulkRecommendations": false,
              "developerSuggestNonBreakingVersion": true,
              "developerSummaryTable": true,
              "developmentDashboardMetricCollection": true,
              "EMAIL_CONFIGURATION": true,
              "enableFedRAMPAudit": false,
              "enableSsoOnly": false,
              "enableUnauthenticatedPages": true,
              "exitOnFatalError": true,
              "expireWaiverWhenRemediationAvailable": false,
              "firewallEnterpriseReporting": true,

              "hostedRepositoryEvaluation": false,

              "guideUiEnabled": false,

              "innerSourceRepositoryIntegration": true,
              "innerSourceTransitiveWaiver": true,
              "internalFirewallOnboardingEnabled": false,
              "internalSourceControlPolicyEvaluations": true,
              "LDAP_CONFIGURATION": true,
              "logoutAuth0OnLogout": false,
              "maliciousUrlsPartnerAccess":false,
              "nonBreakingVersionSuggestionTelemetry": true,
              "OAUTH2_ENABLED": false,
              "prioritizedFindingsReport": true,
              "PRODUCT_LICENSE_CONFIGURATION": true,
              "PROXY_CONFIGURATION": true,
              "prCommenting": true,
              "prLineCommenting": true,
              "prLineCommentingBitbucketOnNoChange": false,
              "scmRelayIntegration": false,
              "reportsList": true,
              "saasLifecycleScmPrsEnabled": true,
              "sbomBinaryScanning": true,
              "sbomContinuousMonitoringUi": true,
              "sbomManager": false,
              "sbomPolicies": true,
              "scanNpmDevAndOptDependencies": false,
              "scanPomFilesInMetaInfDirectory": false,
              "scmUxImprovements": false,
              "skipSbomImportValidation": false,
              "SSO_IDP_MANAGED_BY_SONATYPE": false,
              "SUCCESS_METRICS_CONFIGURATION": true,
              "SYSTEM_NOTICE_CONFIGURATION": true,
              "vulnerabilitySource": false,
              "WEBHOOK_CONFIGURATION": true,
              "containerImagesEvalEnabled": true,
              "zScaler": true,
              "thirdPartyKevLookup": true,
              "SAML_ENABLED": true,
              "userManagementPages": true,
              "userActivityTracking": false,
              "epssDataEnabled": false,
              "waiverRequestWorkflowEnabled": true,
              "consumptionReportingEnabled": false,
              "iqProxyEnabled": false,
              "sloViolationFeedEnabled": false
            }"""));
  }

  @Test
  public void testGetFeatureConfigPropertiesJson_changedFeatureConfig() throws IOException {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.DASHBOARD_DISABLED, "true");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.DEFAULT_BRANCH_MONITORING, "false");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.EMAIL_CONFIGURATION, "false");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.PR_LINE_COMMENTING, "false");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SYSTEM_NOTICE_CONFIGURATION, "false");

    JsonNode featureConfigNode = JsonUtils.parse(featurePropertiesInfo.getFeatureConfigPropertiesJson());

    assertThat(featureConfigNode.size()).isEqualTo(77);
    assertThat(featureConfigNode).isEqualTo(JsonUtils.parse(
        """
            {
              "ADVANCED_SEARCH_CONFIGURATION": true,
              "ADVANCED_SEARCH_ENABLED": true,
              "GLOBAL_SEARCH": false,
              "alpForSbomManager": false,
              "API_PAGE": true,
              "AUTOMATIC_APPLICATION_CONFIGURATION": true,
              "AUTOMATIC_SCM_CONFIGURATION": true,
              "autoWaivers": true,
              "BUILT_FROM_SOURCE": false,
              "PREVIEW_NEXUS_ONE_UI": false,
              "PREVIEW_NEXUS_ONE_UI_ANONYMOUS_ENABLED": false,
              "PREVIEW_NEXUS_ONE_UI_DEFAULT_TO_PREVIEW": false,
              "PREVIEW_NEXUS_ONE_UI_DISABLE_SWITCH_FEEDBACK": false,
              "PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED": false,
              "cleanUpSbomContinuousMonitoringReport": true,
              "codeInsights": true,
              "componentChangeDetectionApi": false,
              "componentSearchApiWithInnerSource": true,
              "CROWD_INTEGRATION": true,
              "dashboard": false,
              "defaultBranchMonitoring": false,
              "dependencyDataInApi": true,
              "developerBulkRecommendations": false,
              "developerSuggestNonBreakingVersion": true,
              "developerSummaryTable": true,
              "developmentDashboardMetricCollection": true,
              "EMAIL_CONFIGURATION": false,
              "enableFedRAMPAudit": false,
              "enableSsoOnly": false,
              "enableUnauthenticatedPages": true,
              "exitOnFatalError": true,
              "expireWaiverWhenRemediationAvailable": false,
              "firewallEnterpriseReporting": true,

              "hostedRepositoryEvaluation": false,

              "guideUiEnabled": false,

              "innerSourceRepositoryIntegration": true,
              "innerSourceTransitiveWaiver": true,
              "internalFirewallOnboardingEnabled": false,
              "internalSourceControlPolicyEvaluations": true,
              "LDAP_CONFIGURATION": true,
              "logoutAuth0OnLogout": false,
              "maliciousUrlsPartnerAccess":false,
              "nonBreakingVersionSuggestionTelemetry": true,
              "OAUTH2_ENABLED": false,
              "prioritizedFindingsReport": true,
              "PRODUCT_LICENSE_CONFIGURATION": true,
              "PROXY_CONFIGURATION": true,
              "prCommenting": true,
              "prLineCommenting": false,
              "prLineCommentingBitbucketOnNoChange": false,
              "scmRelayIntegration": false,
              "reportsList": true,
              "saasLifecycleScmPrsEnabled": true,
              "sbomBinaryScanning": true,
              "sbomContinuousMonitoringUi": true,
              "sbomManager": false,
              "sbomPolicies": true,
              "scanNpmDevAndOptDependencies": false,
              "scanPomFilesInMetaInfDirectory": false,
              "scmUxImprovements": false,
              "skipSbomImportValidation": false,
              "SSO_IDP_MANAGED_BY_SONATYPE": false,
              "SUCCESS_METRICS_CONFIGURATION": true,
              "SYSTEM_NOTICE_CONFIGURATION": false,
              "vulnerabilitySource": false,
              "WEBHOOK_CONFIGURATION": true,
              "containerImagesEvalEnabled": true,
              "zScaler": true,
              "thirdPartyKevLookup": true,
              "SAML_ENABLED": true,
              "userManagementPages": true,
              "userActivityTracking": false,
              "epssDataEnabled": false,
              "waiverRequestWorkflowEnabled": true,
              "consumptionReportingEnabled": false,
              "iqProxyEnabled": false,
              "sloViolationFeedEnabled": false
            }"""));
  }

  @Test
  public void testGetFeatureConfigProperties_filteredFeatures() {
    List<SystemConfigurationPropertyFeature> filteredFeatures = List.of(
        SystemConfigurationPropertyFeature.SUCCESS_METRICS_CONFIGURATION,
        SystemConfigurationPropertyFeature.PRODUCT_LICENSE_CONFIGURATION,
        SystemConfigurationPropertyFeature.SYSTEM_NOTICE_CONFIGURATION,
        SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES,
        SystemConfigurationPropertyFeature.PROXY_CONFIGURATION,
        SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API,
        SystemConfigurationPropertyFeature.CODE_INSIGHTS,
        SystemConfigurationPropertyFeature.LDAP_CONFIGURATION,
        SystemConfigurationPropertyFeature.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES,
        SystemConfigurationPropertyFeature.SCAN_POM_FILES_IN_META_INF_DIRECTORY);

    Map<String, Boolean> featureConfigMap = featurePropertiesInfo.getFeatureConfigProperties(filteredFeatures);

    assertThat(featureConfigMap)
        .hasSize(67)
        .doesNotContainKeys(
            "SUCCESS_METRICS_CONFIGURATION",
            "PRODUCT_LICENSE_CONFIGURATION",
            "SYSTEM_NOTICE_CONFIGURATION",
            "enableUnauthenticatedPages",
            "PROXY_CONFIGURATION",
            "dependencyDataInApi",
            "codeInsights",
            "LDAP_CONFIGURATION",
            "scanNpmDevAndOptDependencies",
            "scanPomFilesInMetaInfDirectory");
  }
}

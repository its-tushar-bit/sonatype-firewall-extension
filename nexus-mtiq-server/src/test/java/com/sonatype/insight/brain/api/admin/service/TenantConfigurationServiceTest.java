/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConfig;
import com.sonatype.insight.brain.service.CopyStorageConfig;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantConfigurationServiceTest
    extends AbstractMultiTenantTest
{
  private static final Map<String, Object> EXPECTED_TENANT_CONFIGURABLE_PROPERTIES = new HashMap<>();

  static {
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, ",");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, 1);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(BASE_URL, "http://127.0.0.1:8070");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(EVENT_BUS_MAX_THREAD_POOL_SIZE, 1);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(FRAME_ANCESTORS_ALLOWLIST, "'self'");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(MAX_ADVANCED_SEARCH_CLAUSE_COUNT, 1);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(POLICY_MONITORING_HOUR, 1);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(PURGE_SCAN_FILES, "withReports");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS, 1);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(SAAS_LIFECYCLE_SCM_ENABLED, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(SOURCE_CONTROL_CLONE_DIRECTORY_ON_CLUSTER_STORAGE, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(WEBHOOK_SECRET_PASSPHRASE, "pass");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(WEBHOOK_SECRET_PASSPHRASE_FIPS, "pass");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(SKIP_SBOM_IMPORT_VALIDATION, false);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(SBOM_BINARY_SCANNING, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(SBOM_CONTINUOUS_MONITORING_UI, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(SBOM_POLICIES, false);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(AUTO_WAIVERS, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(QUARANTINED_ITEM_CUSTOM_MESSAGE, "a custom quarantine message");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(MALWARE_DEFENSE_API_MAX_COMPONENTS, "100");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(COMPONENT_CHANGE_DETECTION_MAX_COMPONENTS, "1500000");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(COMPONENT_CHANGE_DETECTION_BATCH_SIZE, "100");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(COMPONENT_CHANGE_DETECTION_TASK_PERIOD, 24);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(COMPONENT_CHANGE_DETECTION_MAX_EVENTS, "1500000");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(ALP_OBSERVED_LICENSE_DETECTION_ENABLED, "true");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(COMPONENT_CHANGE_DETECTION_API, false);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(CONSUMPTION_REPORTING_ENABLED, false);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(CONTAINER_IMAGES_EVAL_ENABLED, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(ZSCALER_UPDATE_TASK_PERIOD, 24);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(ZSCALER, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(THIRD_PARTY_KEV_LOOKUP, true);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(EPSS_DATA, false);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(SESSION_TIMEOUT_MINUTES, 1);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(USER_ACTIVITY_TRACKING, false);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 30);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(MALICIOUS_URLS_PARTNER_ACCESS, false);
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(EVALUATION_QUEUE_CONFIG,
        JsonUtils.convertValue(EvaluationQueueConfig.builder().build(), Map.class));
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(LIFECYCLE_TIER, "Pro");
    EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.put(HOSTED_REPOSITORY_EVALUATION, false);
  }

  private static final Map<String, Object> EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES = new HashMap<>();

  static {
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(HDS_URL, "https://clm-staging.sonatype.com/");
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(RELAY_URL, "https://clm-staging.sonatype.com/");
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(SAAS_POLICY_MONITOR_POOL_SIZE, 1);
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(SOURCE_CONTROL_IMPORT_POOL_SIZE, 1);
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE, 1);
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(USER_AGENT_SUFFIX, "userAgentSuffix");
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR, 12);
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(WARN_ON_NON_PRIMARY_STORAGE_ACCESS, "warnOnNonPrimaryStorageAccess");
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(COPY_STORAGE_CONFIG,
        JsonUtils.convertValue(new CopyStorageConfig(1, 1), Map.class));
    EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.put(MAX_CONCURRENT_TENANT_INDEX_CREATION, 5);
  }

  @Mock
  private TenantUtil tenantUtil;

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private ApiConfigurationService apiConfigurationService;

  TenantConfigurationService underTest;

  @BeforeEach
  public void setup() {
    underTest = new TenantConfigurationService(apiConfigurationService, tenantUtil, tenantValidator);
  }

  @Test
  public void shouldGetSinglePropertyConfiguration() {
    testAsNewTenant(tenant -> {
      String expectedProperty = "baseUrl";
      Set<String> query = new HashSet<>(Arrays.asList(expectedProperty));
      Map<String, Object> expected = Collections.singletonMap(expectedProperty, "http://127.0.0.1:8070");

      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(apiConfigurationService.getConfigurationNoAuthz(query)).thenReturn(expected);

      assertThat(underTest.getPropertiesConfiguration(tenant.tenantSlug, query)).isEqualTo(expected);
    });
  }

  @Test
  public void shouldNotThrowRuntimeException_getPropertiesConfiguration_whenUsingGlobalTenant() {
    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);

      String expectedProperty = "baseUrl";
      Set<String> query = new HashSet<>(Arrays.asList(expectedProperty));
      Map<String, Object> expected = Collections.singletonMap(expectedProperty, "http://127.0.0.1:8070");

      when(apiConfigurationService.getConfigurationNoAuthz(query)).thenReturn(expected);

      assertThat(underTest.getPropertiesConfiguration(global.tenantSlug, query)).isEqualTo(expected);
    });
  }

  @Test
  public void shouldThrowRuntimeException_getPropertiesConfiguration_whenTenantDoesntExist() {
    final String errorMessage = "Tenant does not exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      Set<String> query = new HashSet<>(Arrays.asList("baseUrl"));

      assertThatThrownBy(() -> underTest.getPropertiesConfiguration(tenant.tenantSlug, query))
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_getPropertiesConfiguration_whenPropertyIsNotConfigurable() {
    final String errorMessage = "Property forceBaseUrl is not configurable.";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      Set<String> query = new HashSet<>(Arrays.asList("forceBaseUrl"));

      assertThatThrownBy(() -> underTest.getPropertiesConfiguration(tenant.tenantSlug, query))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void testSetPropertiesConfiguration_TenantConfigurable_AsTenant() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      underTest.setPropertiesConfiguration(tenant.tenantSlug, EXPECTED_TENANT_CONFIGURABLE_PROPERTIES);

      verify(apiConfigurationService).setConfigurationNoAuthz(EXPECTED_TENANT_CONFIGURABLE_PROPERTIES);
    });
  }

  @Test
  public void testSetPropertiesConfiguration_TenantConfigurable_AsGlobal() {
    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);

      underTest.setPropertiesConfiguration(global.tenantSlug, EXPECTED_TENANT_CONFIGURABLE_PROPERTIES);

      verify(apiConfigurationService).setConfigurationNoAuthz(EXPECTED_TENANT_CONFIGURABLE_PROPERTIES);
    });
  }

  @Test
  public void shouldThrowRuntimeException_setPropertiesConfiguration_whenTenantDoesntExist() {
    final String errorMessage = "Tenant does not exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      String expectedProperty = "baseUrl";
      String expectedValue = "http://127.0.0.1:8070";
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(tenant.tenantSlug, propertyConfiguration))
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void testSetPropertiesConfiguration_GlobalConfigurable_AsTenant() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      for (Entry<String, Object> entry : EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.entrySet()) {
        Map<String, Object> propertyConfiguration = Collections.singletonMap(entry.getKey(), entry.getValue());
        String errorMessage = String.format("Property %s is only configurable globally.", entry.getKey());

        assertThatThrownBy(() -> underTest.setPropertiesConfiguration(tenant.tenantSlug, propertyConfiguration))
            .withFailMessage(errorMessage)
            .isInstanceOf(BadRequestException.class);
      }
    });
  }

  @Test
  public void testSetPropertiesConfiguration_GlobalConfigurable_AsGlobal() {
    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);

      underTest.setPropertiesConfiguration(global.tenantSlug, EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES);

      verify(apiConfigurationService).setConfigurationNoAuthz(EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES);
    });
  }

  @Test
  public void shouldThrowRuntimeException_setPropertiesConfiguration_whenNoConfigurationProvided() {
    final String errorMessage = "No configuration was specified.";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      Map<String, Object> propertyConfiguration = Collections.emptyMap();

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(tenant.tenantSlug, propertyConfiguration))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_setPropertiesConfiguration_whenPropertyIsNotConfigurable() {
    final String errorMessage = "Property forceBaseUrl is not configurable.";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      String expectedProperty = "forceBaseUrl";
      boolean expectedValue = true;
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(tenant.tenantSlug, propertyConfiguration))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldDeleteSinglePropertyConfiguration() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      Set<String> query = new HashSet<>(Arrays.asList("baseUrl"));
      underTest.deletePropertiesConfiguration(tenant.tenantSlug, query);

      verify(apiConfigurationService).deleteConfigurationNoAuthz(query);
    });
  }

  @Test
  public void shouldNotThrowRuntimeException_deletePropertiesConfiguration_whenUsingGlobalTenant() {
    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);

      Set<String> query = new HashSet<>(Arrays.asList("baseUrl"));
      underTest.deletePropertiesConfiguration(global.tenantSlug, query);

      verify(apiConfigurationService).deleteConfigurationNoAuthz(query);
    });
  }

  @Test
  public void shouldThrowRuntimeException_deletePropertiesConfiguration_whenTenantDoesntExist() {
    final String errorMessage = "Tenant does not exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      Set<String> query = new HashSet<>(Arrays.asList("baseUrl"));

      assertThatThrownBy(() -> underTest.deletePropertiesConfiguration(tenant.tenantSlug, query))
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_deletePropertiesConfiguration_whenPropertyIsNotConfigurable() {
    final String errorMessage = "Property forceBaseUrl is not configurable.";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      Set<String> query = new HashSet<>(Arrays.asList("forceBaseUrl"));

      assertThatThrownBy(() -> underTest.deletePropertiesConfiguration(tenant.tenantSlug, query))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void testConfigurableProperties() {
    assertThat(TenantConfigurationService.CONFIGURABLE_PROPERTIES).containsExactlyInAnyOrderElementsOf(
        EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.keySet());
  }

  @Test
  public void testGlobalConfigurableProperties() {
    assertThat(TenantConfigurationService.GLOBAL_CONFIGURABLE_PROPERTIES).containsExactlyInAnyOrderElementsOf(
        EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.keySet());
  }

  @Test
  public void testConfigurableProperties_GlobalConfigurableProperties_MutuallyExclusive() {
    assertThat(EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.keySet()).doesNotContainAnyElementsOf(
        EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.keySet());
    assertThat(EXPECTED_TENANT_CONFIGURABLE_PROPERTIES.keySet()).doesNotContainAnyElementsOf(
        EXPECTED_GLOBAL_CONFIGURABLE_PROPERTIES.keySet());
  }
}

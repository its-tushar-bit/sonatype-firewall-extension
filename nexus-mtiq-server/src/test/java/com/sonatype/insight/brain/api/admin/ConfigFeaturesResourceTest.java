/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_CONFIG_FEATURES_PATH;
import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.FEATURE_SAAS_LIFECYCLE_SCM_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ConfigFeaturesResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private TemporaryEntity privateGlobalTemporaryEntity;

  /**
   * SystemConfigurationPropertyFeature that are enabled by default for regular tenants in MTIQ should be added here
   * Before adding new SystemConfigurationPropertyFeature into these lists check that the feature should be allowed in
   * MTIQ, if the feature should not be allowed for MTIQ please add to be MTIQ_BANNED_FEATURES
   */
  private final String[] defaultTenantEnabledFeatures = new String[]{
    SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
    SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
    SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.AUTOMATIC_APPLICATION_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
    SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
    SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
    SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
    SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId(),
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId(),
    SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.getId(),
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.getId(),
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.getId(),
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.getId(),
    SystemConfigurationPropertyFeature.DEVELOPMENT_DASHBOARD_METRIC_COLLECTION.getId(),
    SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT.getId(),
    SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE.getId(),
    SystemConfigurationPropertyFeature.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT.getId(),
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.getId(),
    SystemConfigurationPropertyFeature.SBOM_CONTINUOUS_MONITORING_UI.getId(),
    SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.getId(),
    SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.getId(),
    SystemConfigurationPropertyFeature.NON_BREAKING_VERSION_SUGGESTION_TELEMETRY.getId(),
    SystemConfigurationPropertyFeature.SBOM_POLICIES.getId(),
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.getId(),
    SystemConfigurationPropertyFeature.API_PAGE.getId(),
    SystemConfigurationPropertyFeature.ZSCALER.getId(),
    SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.getId(),
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.getId(),
    SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.getId(),
    SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.getId(),
    SystemConfigurationPropertyFeature.CODE_INSIGHTS.getId(),
    SystemConfigurationPropertyFeature.FIREWALL_ENTERPRISE_REPORTING.getId(),
  };

  /**
   * SystemConfigurationPropertyFeature that are enabled by default for the global tenant in MTIQ should be added here
   * Before adding new SystemConfigurationPropertyFeature into these lists check that the feature should be allowed in
   * MTIQ, if the feature should not be allowed for MTIQ please add to be MTIQ_BANNED_FEATURES
   */
  private final String[] defaultGlobalTenantEnabledFeatures = new String[]{
    SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
    SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
    SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.AUTOMATIC_APPLICATION_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
    SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
    SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
    SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
    SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId(),
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId(),
    SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.getId(),
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.getId(),
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.getId(),
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.getId(),
    SystemConfigurationPropertyFeature.DEVELOPMENT_DASHBOARD_METRIC_COLLECTION.getId(),
    SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT.getId(),
    SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE.getId(),
    SystemConfigurationPropertyFeature.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT.getId(),
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.getId(),
    SystemConfigurationPropertyFeature.SBOM_CONTINUOUS_MONITORING_UI.getId(),
    SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.getId(),
    SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.getId(),
    SystemConfigurationPropertyFeature.NON_BREAKING_VERSION_SUGGESTION_TELEMETRY.getId(),
    SystemConfigurationPropertyFeature.SBOM_POLICIES.getId(),
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.getId(),
    SystemConfigurationPropertyFeature.API_PAGE.getId(),
    SystemConfigurationPropertyFeature.ZSCALER.getId(),
    SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.getId(),
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.getId(),
    SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.getId(),
    SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.getId(),
    SystemConfigurationPropertyFeature.CODE_INSIGHTS.getId(),
    SystemConfigurationPropertyFeature.FIREWALL_ENTERPRISE_REPORTING.getId(),
  };

  /**
   * SystemConfigurationPropertyFeature that are allowed to be used in MTIQ should be added to this list
   */
  private final String[] allFeatures = Stream.concat(Arrays.stream(defaultGlobalTenantEnabledFeatures), Arrays.stream(
      new String[]{
        SystemConfigurationPropertyFeature.SSO_IDP_MANAGED_BY_SONATYPE.getId(),
        SystemConfigurationPropertyFeature.SCM_UX_IMPROVEMENTS.getId(),
        SystemConfigurationPropertyFeature.SBOM_MANAGER.getId(),
        SystemConfigurationPropertyFeature.OAUTH2_ENABLED.getId(),
        SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.getId(),
        SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS.getId(),
        SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.getId(),
        SystemConfigurationPropertyFeature.EXPIRE_WAIVER_WHEN_REMEDIATION_AVAILABLE.getId(),
        SystemConfigurationPropertyFeature.ALP_FOR_SBOM_MANAGER.getId(),
        SystemConfigurationPropertyFeature.COMPONENT_CHANGE_DETECTION_API.getId(),
        SystemConfigurationPropertyFeature.EPSS_DATA.getId(),
        SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES.getId(),
        SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.getId(),
        SystemConfigurationPropertyFeature.SAML_ENABLED.getId(),
        SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE.getId(),
        SystemConfigurationPropertyFeature.MALICIOUS_URLS_PARTNER_ACCESS.getId(),
        SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.getId(),
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getId(),
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_ANONYMOUS_ENABLED.getId(),
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED.getId(),
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_DEFAULT_TO_PREVIEW.getId(),
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_DISABLE_SWITCH_FEEDBACK.getId(),
        SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.getId(),
        SystemConfigurationPropertyFeature.SCM_RELAY_INTEGRATION.getId(),
        SystemConfigurationPropertyFeature.IQ_PROXY_ENABLED.getId(),
        SystemConfigurationPropertyFeature.IQ_FIREWALL_ENTERPRISE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.IQ_FIREWALL_ENTERPRISE_REDIRECT_UI_ENABLED.getId(),
        SystemConfigurationPropertyFeature.SLO_VIOLATION_FEED.getId(),
        SystemConfigurationPropertyFeature.GLOBAL_SEARCH.getId(),
        SystemConfigurationPropertyFeature.CATALOG_FEDERATION.getId(),
      })).toArray(String[]::new);

  @Before
  public void before() {
    systemConfigurationPropertyDAO = lookup(SystemConfigurationPropertyDAO.class);

    // This test mucks with the system properties in the global schema. Use TemporaryEntity to clean it up (see #after)
    testAsGlobal(g -> {
      privateGlobalTemporaryEntity = new TemporaryEntity(databaseContainerRule);
      privateGlobalTemporaryEntity.before();
    });
  }

  @After
  public void after() {
    testAsGlobal(g -> {
      privateGlobalTemporaryEntity.after();
    });
  }

  @Test
  public void testFeatures_asGlobal() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(defaultGlobalTenantEnabledFeatures);
  }

  @Test
  public void testFeatures_asTenant() throws Exception {
    Tenant testTenant = getTestTenant();
    HttpResponse response = callConfigFeaturesEndpoint(testTenant.tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(defaultTenantEnabledFeatures);
  }

  @Test
  public void testFeatures_all_asGlobal() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug)
        .path("all")
        .get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(allFeatures);
  }

  @Test
  public void testFeatures_all_asTenant() throws Exception {
    Tenant testTenant = getTestTenant();
    HttpResponse response = callConfigFeaturesEndpoint(testTenant.tenantSlug)
        .path("all")
        .get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(allFeatures);
  }

  @Test
  public void testDeleteFeature_asGlobal_alsoDisablesTenantFeatures() throws Exception {
    testAsGlobal(global -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });
    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });

    HttpResponse response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug)
        .path("dashboard")
        .delete();
    assertResponseStatus(204, response);

    testAsGlobal(global -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });
    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });

    response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).doesNotContain(SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId());

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    features = response.getBody(String[].class);
    assertThat(features).doesNotContain(SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId());
  }

  @Test
  public void testDeleteFeature_asTenant() throws Exception {
    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });

    HttpResponse response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug)
        .path("dashboard")
        .delete();
    assertResponseStatus(204, response);

    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    List<String> expected = new LinkedList<>(Arrays.asList(defaultTenantEnabledFeatures));
    expected.remove(SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId());

    assertThat(features).containsExactlyInAnyOrder(expected.toArray(new String[0]));
  }

  @Test
  public void testDeleteFeature_asInvalidTenant() throws Exception {
    String notTenantSlug = "not-a-tenant";
    HttpResponse response = callConfigFeaturesEndpoint(notTenantSlug)
        .path("dashboard")
        .delete();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(String.format("Tenant %s does not exist", notTenantSlug));
  }

  @Test
  public void testEnableFeature_asGlobal_alsoEnablesTenantFeatures() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug)
        .path("dashboard")
        .delete();
    assertResponseStatus(204, response);

    testAsGlobal(global -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });
    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });

    response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug)
        .path("dashboard")
        .post();
    assertResponseStatus(204, response);

    testAsGlobal(global -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });
    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });

    response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).contains(SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId());

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    features = response.getBody(String[].class);
    assertThat(features).contains(SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId());
  }

  @Test
  public void testEnableFeature_asTenant() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug)
        .path("dashboard")
        .delete();
    assertResponseStatus(204, response);

    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug)
        .path("dashboard")
        .post();
    assertResponseStatus(204, response);

    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(defaultTenantEnabledFeatures);
  }

  @Test
  public void testEnableFeature_asInvalidTenant() throws Exception {
    String notTenantSlug = "not-a-tenant";
    HttpResponse response = callConfigFeaturesEndpoint(notTenantSlug)
        .path("dashboard")
        .post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(String.format("Tenant %s does not exist", notTenantSlug));
  }

  @Test
  public void testEnableFeature_saasLifecycleScmEnabled_asTenant() throws Exception {
    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(SAAS_LIFECYCLE_SCM_ENABLED)).isNull();
    });

    HttpResponse response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).contains(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.getId());
  }

  @Test
  public void testEnableFeature_saasLifecycleScmDisabled_asTenant() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug)
        .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
        .delete();
    assertResponseStatus(204, response);

    testAsTestTenant(tenant -> {
      assertThat(systemConfigurationPropertyDAO.getByName(SAAS_LIFECYCLE_SCM_ENABLED)).isNotNull();
    });

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).doesNotContain(SAAS_LIFECYCLE_SCM_ENABLED);
  }

  private HttpRequest callConfigFeaturesEndpoint(String tenantSlug) {
    return adminRestRequest(ADMIN_TENANT_CONFIG_FEATURES_PATH)
        .parameter(tenantSlug);
  }
}

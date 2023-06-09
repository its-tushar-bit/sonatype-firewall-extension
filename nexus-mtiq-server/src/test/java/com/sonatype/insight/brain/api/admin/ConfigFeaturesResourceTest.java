/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import javax.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_CONFIG_FEATURES_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFeaturesResourceTest
    extends AbstractMultiTenantResourceTest
{
  private final SystemConfigurationPropertyDAO configurationPropertyDAO = new SystemConfigurationPropertyDAO();

  protected HttpRequest restRequest(String path) {
    return super.adminRequest().path("api/").path(path);
  }

  @Test
  public void testFeatures_asGlobal() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId()
    );
  }

  @Test
  public void testFeatures_asTenant() throws Exception {
    Tenant testTenant = getTestTenant();
    HttpResponse response = callConfigFeaturesEndpoint(testTenant.tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId()
    );
  }

  @Test
  public void testFeatures_all_asGlobal() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug)
        .path("all").get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_MANAGED_IDP_SSO.getId()
    );
  }

  @Test
  public void testFeatures_all_asTenant() throws Exception {
    Tenant testTenant = getTestTenant();
    HttpResponse response = callConfigFeaturesEndpoint(testTenant.tenantSlug)
        .path("all").get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_MANAGED_IDP_SSO.getId()
    );
  }

  @Test
  public void testDeleteFeature_asGlobal_alsoDisablesTenantFeatures() throws Exception {
    testAsGlobal(global -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });
    testAsTestTenant(tenant -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });

    HttpResponse response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug)
        .path("dashboard")
        .delete();
    assertResponseStatus(204, response);

    testAsGlobal(global -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });
    testAsTestTenant(tenant -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });

    String[] expectedFeatures = {
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId()
    };

    response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).containsExactlyInAnyOrder(expectedFeatures);

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    features = response.getBody(String[].class);
    assertThat(features).containsExactlyInAnyOrder(expectedFeatures);
  }

  @Test
  public void testDeleteFeature_asTenant() throws Exception {
    testAsTestTenant(tenant -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });

    HttpResponse response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug)
        .path("dashboard").delete();
    assertResponseStatus(204, response);

    testAsTestTenant(tenant -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId()
    );
  }

  @Test
  public void testDeleteFeature_asInvalidTenant() throws Exception {
    String notTenantSlug = "not-a-tenant";
    HttpResponse response = callConfigFeaturesEndpoint(notTenantSlug)
        .path("dashboard").delete();

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
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });
    testAsTestTenant(tenant -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });

    response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug)
        .path("dashboard")
        .post();
    assertResponseStatus(204, response);

    testAsGlobal(global -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });
    testAsTestTenant(tenant -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });

    response = callConfigFeaturesEndpoint(Tenant.GLOBAL_TENANT.tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId()
    );

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    features = response.getBody(String[].class);
    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId()
    );
  }

  @Test
  public void testEnableFeature_asTenant() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug)
        .path("dashboard")
        .delete();
    assertResponseStatus(204, response);

    testAsTestTenant(tenant -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
    });

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug)
        .path("dashboard")
        .post();
    assertResponseStatus(204, response);

    testAsTestTenant(tenant -> {
      assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
    });

    response = callConfigFeaturesEndpoint(getTestTenant().tenantSlug).get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId()
    );
  }

  @Test
  public void testEnableFeature_asInvalidTenant() throws Exception {
    String notTenantSlug = "not-a-tenant";
    HttpResponse response = callConfigFeaturesEndpoint(notTenantSlug)
        .path("dashboard").post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(String.format("Tenant %s does not exist", notTenantSlug));
  }

  private HttpRequest callConfigFeaturesEndpoint(String tenantSlug) throws Exception {
    return restRequest(ADMIN_TENANT_CONFIG_FEATURES_PATH)
        .parameter(tenantSlug)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt());
  }
}

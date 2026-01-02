/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationResourceTest;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

/**
 * Integration test to verify that IQ-only resources marked with @IqOnlyEndpoint are not exposed in MTIQ and return 404
 * responses.
 */
@Category(SlowTest.class)
public class IqOnlyResourcesNotExposedTest
    extends AbstractMultiTenantBaseIntegrationResourceTest
{
  @Test
  public void testApiConfigFeaturesResource_NotAccessible() throws Exception {
    // Test GET request as regular tenant user
    HttpResponse response = restRequest()
        .path("/api/v2/config/features")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testApiConfigFeaturesResource_NotAccessible_AllMethods() throws Exception {
    // Test multiple HTTP methods to ensure complete blocking

    // GET
    HttpResponse getResponse = restRequest()
        .path("/api/v2/config/features")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, getResponse);

    // PUT
    HttpResponse putResponse = restRequest()
        .path("/api/v2/config/features")
        .auth(getUser())
        .body("{}")
        .put();
    assertResponseStatus(SC_NOT_FOUND, putResponse);
  }

  @Test
  public void testApiCrowdConfigurationResource_NotAccessible() throws Exception {
    HttpResponse response = restRequest()
        .path("/api/v2/config/crowd")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testApiCrowdConfigurationResource_NotAccessible_AllMethods() throws Exception {
    // GET
    HttpResponse getResponse = restRequest()
        .path("/api/v2/config/crowd")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, getResponse);

    // POST
    HttpResponse postResponse = restRequest()
        .path("/api/v2/config/crowd")
        .auth(getUser())
        .body("{}")
        .post();
    assertResponseStatus(SC_NOT_FOUND, postResponse);

    // DELETE
    HttpResponse deleteResponse = restRequest()
        .path("/api/v2/config/crowd")
        .auth(getUser())
        .delete();
    assertResponseStatus(SC_NOT_FOUND, deleteResponse);
  }

  @Test
  public void testApiDataRetentionPolicyResource_NotAccessible() throws Exception {
    HttpResponse response = restRequest()
        .path("/api/v2/dataRetentionPolicies")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testApiDataRetentionPolicyResource_NotAccessible_AllMethods() throws Exception {
    // GET
    HttpResponse getResponse = restRequest()
        .path("/api/v2/dataRetentionPolicies")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, getResponse);

    // PUT
    HttpResponse putResponse = restRequest()
        .path("/api/v2/dataRetentionPolicies")
        .auth(getUser())
        .body("{}")
        .put();
    assertResponseStatus(SC_NOT_FOUND, putResponse);
  }

  @Test
  public void testSupportResource_NotAccessible() throws Exception {
    HttpResponse response = restRequest()
        .path("/rest/support")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testSupportResource_NotAccessible_AllMethods() throws Exception {
    // GET
    HttpResponse getResponse = restRequest()
        .path("/rest/support")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, getResponse);

    // GET with query param
    HttpResponse getWithParamResponse = restRequest()
        .path("/rest/support?includeDb=true")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, getWithParamResponse);
  }

  @Test
  public void testApiSourceControlConfigurationResource_NotAccessible() throws Exception {
    HttpResponse response = restRequest()
        .path("/api/v2/config/sourceControl")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testApiSourceControlConfigurationResource_NotAccessible_AllMethods() throws Exception {
    // GET all configurations
    HttpResponse getResponse = restRequest()
        .path("/api/v2/config/sourceControl")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, getResponse);

    // POST new configuration
    HttpResponse postResponse = restRequest()
        .path("/api/v2/config/sourceControl")
        .auth(getUser())
        .body("{}")
        .post();
    assertResponseStatus(SC_NOT_FOUND, postResponse);

    // PUT update configuration
    HttpResponse putResponse = restRequest()
        .path("/api/v2/config/sourceControl/test-id")
        .auth(getUser())
        .body("{}")
        .put();
    assertResponseStatus(SC_NOT_FOUND, putResponse);

    // DELETE configuration
    HttpResponse deleteResponse = restRequest()
        .path("/api/v2/config/sourceControl/test-id")
        .auth(getUser())
        .delete();
    assertResponseStatus(SC_NOT_FOUND, deleteResponse);
  }

  @Test
  public void testApiOidcConfigurationResource_NotAccessible() throws Exception {
    HttpResponse response = restRequest()
        .path("/api/v2/config/oidc")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testApiOidcConfigurationResource_NotAccessible_AllMethods() throws Exception {
    // GET configuration
    HttpResponse getResponse = restRequest()
        .path("/api/v2/config/oidc")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, getResponse);

    // POST configuration
    HttpResponse postResponse = restRequest()
        .path("/api/v2/config/oidc")
        .auth(getUser())
        .body("{}")
        .post();
    assertResponseStatus(SC_NOT_FOUND, postResponse);

    // DELETE configuration
    HttpResponse deleteResponse = restRequest()
        .path("/api/v2/config/oidc")
        .auth(getUser())
        .delete();
    assertResponseStatus(SC_NOT_FOUND, deleteResponse);
  }

  @Test
  public void testApiProxyServerConfigurationResource_NotAccessible() throws Exception {
    // ApiProxyServerConfigurationResource is also marked as @IqOnlyEndpoint
    HttpResponse response = restRequest()
        .path("/api/v2/config/httpProxyServer")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testApiJiraConfigurationResource_NotAccessible() throws Exception {
    // ApiJiraConfigurationResource is also marked as @IqOnlyEndpoint
    HttpResponse response = restRequest()
        .path("/api/v2/config/jira")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testLdapResource_NotAccessible() throws Exception {
    // LdapResource is also marked as @IqOnlyEndpoint
    HttpResponse response = restRequest()
        .path("/rest/config/ldap")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testLabsResource_NotAccessible() throws Exception {
    // LabsResource is also marked as @IqOnlyEndpoint
    HttpResponse response = restRequest()
        .path("/rest/labs")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testSuccessMetricsResource_NotAccessible() throws Exception {
    // SuccessMetricsResource is also marked as @IqOnlyEndpoint
    HttpResponse response = restRequest()
        .path("/rest/successMetrics")
        .auth(getUser())
        .get();

    assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  public void testIqOnlyResources_NotAccessible_AsAdmin() throws Exception {
    // Create an admin user with all permissions to ensure it's not a permission issue
    User admin = tenantTemporaryEntity.newUser();
    Role adminRole = tenantTemporaryEntity.newRole(false /* global */, Permission.values());
    tenantTemporaryEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, adminRole.getId(),
        admin.getUsername());

    // Test a few critical endpoints with admin user

    // Config features should still be 404 even for admin
    HttpResponse featuresResponse = restRequest()
        .path("/api/v2/config/features")
        .auth(admin)
        .get();
    assertResponseStatus(SC_NOT_FOUND, featuresResponse);

    // Crowd configuration should still be 404 even for admin
    HttpResponse crowdResponse = restRequest()
        .path("/api/v2/config/crowd")
        .auth(admin)
        .get();
    assertResponseStatus(SC_NOT_FOUND, crowdResponse);

    // Support endpoint should still be 404 even for admin
    HttpResponse supportResponse = restRequest()
        .path("/rest/support")
        .auth(admin)
        .get();
    assertResponseStatus(SC_NOT_FOUND, supportResponse);

    // OIDC configuration should still be 404 even for admin
    HttpResponse oidcResponse = restRequest()
        .path("/api/v2/config/oidc")
        .auth(admin)
        .get();
    assertResponseStatus(SC_NOT_FOUND, oidcResponse);
  }

  @Test
  public void testIqOnlyResources_SubPaths_AlsoNotAccessible() throws Exception {
    // Test that sub-paths under IQ-only resources are also not accessible

    // Features sub-path
    HttpResponse featuresSubPath = restRequest()
        .path("/api/v2/config/features/someFeature")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, featuresSubPath);

    // Source control sub-path
    HttpResponse sourceControlSubPath = restRequest()
        .path("/api/v2/config/sourceControl/github/test")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, sourceControlSubPath);

    // OIDC sub-path
    HttpResponse oidcSubPath = restRequest()
        .path("/api/v2/config/oidc/test")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, oidcSubPath);

    // Support sub-path
    HttpResponse supportSubPath = restRequest()
        .path("/rest/support/logs")
        .auth(getUser())
        .get();
    assertResponseStatus(SC_NOT_FOUND, supportSubPath);
  }

  /**
   * Helper method to create a test user with basic permissions
   */
  private User getUser() {
    User user = tenantTemporaryEntity.newUser();
    Role role = tenantTemporaryEntity.newRole(false /* global */, Permission.READ);
    tenantTemporaryEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
    return user;
  }
}

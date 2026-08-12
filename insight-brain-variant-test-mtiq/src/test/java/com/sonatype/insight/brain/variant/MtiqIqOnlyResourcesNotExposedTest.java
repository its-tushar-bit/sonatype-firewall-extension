/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

/**
 * MTIQ variant conversion of {@code IqOnlyResourcesNotExposedTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationResourceTest}). Integration test to verify that IQ-only resources marked
 * with @IqOnlyEndpoint are not exposed in MTIQ and return 404 responses.
 */
@MtiqTest
class MtiqIqOnlyResourcesNotExposedTest
{
  private MtiqTestContext ctx;

  @Test
  void testApiConfigFeaturesResource_NotAccessible() throws Exception {
    // Test GET request as regular tenant user
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/config/features")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testApiConfigFeaturesResource_NotAccessible_AllMethods() throws Exception {
    // Test multiple HTTP methods to ensure complete blocking

    // GET
    HttpResponse getResponse = ctx.restRequest()
        .path("/api/v2/config/features")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, getResponse);

    // PUT
    HttpResponse putResponse = ctx.restRequest()
        .path("/api/v2/config/features")
        .auth(getUser())
        .body("{}")
        .put();
    ctx.assertResponseStatus(SC_NOT_FOUND, putResponse);
  }

  @Test
  void testApiCrowdConfigurationResource_NotAccessible() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/config/crowd")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testApiCrowdConfigurationResource_NotAccessible_AllMethods() throws Exception {
    // GET
    HttpResponse getResponse = ctx.restRequest()
        .path("/api/v2/config/crowd")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, getResponse);

    // POST
    HttpResponse postResponse = ctx.restRequest()
        .path("/api/v2/config/crowd")
        .auth(getUser())
        .body("{}")
        .post();
    ctx.assertResponseStatus(SC_NOT_FOUND, postResponse);

    // DELETE
    HttpResponse deleteResponse = ctx.restRequest()
        .path("/api/v2/config/crowd")
        .auth(getUser())
        .delete();
    ctx.assertResponseStatus(SC_NOT_FOUND, deleteResponse);
  }

  @Test
  void testApiDataRetentionPolicyResource_NotAccessible() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/dataRetentionPolicies")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testApiDataRetentionPolicyResource_NotAccessible_AllMethods() throws Exception {
    // GET
    HttpResponse getResponse = ctx.restRequest()
        .path("/api/v2/dataRetentionPolicies")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, getResponse);

    // PUT
    HttpResponse putResponse = ctx.restRequest()
        .path("/api/v2/dataRetentionPolicies")
        .auth(getUser())
        .body("{}")
        .put();
    ctx.assertResponseStatus(SC_NOT_FOUND, putResponse);
  }

  @Test
  void testSupportResource_NotAccessible() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path("/rest/support")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testSupportResource_NotAccessible_AllMethods() throws Exception {
    // GET
    HttpResponse getResponse = ctx.restRequest()
        .path("/rest/support")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, getResponse);

    // GET with query param
    HttpResponse getWithParamResponse = ctx.restRequest()
        .path("/rest/support?includeDb=true")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, getWithParamResponse);
  }

  @Test
  void testApiSourceControlConfigurationResource_NotAccessible() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/config/sourceControl")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testApiSourceControlConfigurationResource_NotAccessible_AllMethods() throws Exception {
    // GET all configurations
    HttpResponse getResponse = ctx.restRequest()
        .path("/api/v2/config/sourceControl")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, getResponse);

    // POST new configuration
    HttpResponse postResponse = ctx.restRequest()
        .path("/api/v2/config/sourceControl")
        .auth(getUser())
        .body("{}")
        .post();
    ctx.assertResponseStatus(SC_NOT_FOUND, postResponse);

    // PUT update configuration
    HttpResponse putResponse = ctx.restRequest()
        .path("/api/v2/config/sourceControl/test-id")
        .auth(getUser())
        .body("{}")
        .put();
    ctx.assertResponseStatus(SC_NOT_FOUND, putResponse);

    // DELETE configuration
    HttpResponse deleteResponse = ctx.restRequest()
        .path("/api/v2/config/sourceControl/test-id")
        .auth(getUser())
        .delete();
    ctx.assertResponseStatus(SC_NOT_FOUND, deleteResponse);
  }

  @Test
  void testApiOidcConfigurationResource_NotAccessible() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/config/oidc")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testApiOidcConfigurationResource_NotAccessible_AllMethods() throws Exception {
    // GET configuration
    HttpResponse getResponse = ctx.restRequest()
        .path("/api/v2/config/oidc")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, getResponse);

    // POST configuration
    HttpResponse postResponse = ctx.restRequest()
        .path("/api/v2/config/oidc")
        .auth(getUser())
        .body("{}")
        .post();
    ctx.assertResponseStatus(SC_NOT_FOUND, postResponse);

    // DELETE configuration
    HttpResponse deleteResponse = ctx.restRequest()
        .path("/api/v2/config/oidc")
        .auth(getUser())
        .delete();
    ctx.assertResponseStatus(SC_NOT_FOUND, deleteResponse);
  }

  @Test
  void testApiProxyServerConfigurationResource_NotAccessible() throws Exception {
    // ApiProxyServerConfigurationResource is also marked as @IqOnlyEndpoint
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/config/httpProxyServer")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testApiJiraConfigurationResource_NotAccessible() throws Exception {
    // ApiJiraConfigurationResource is also marked as @IqOnlyEndpoint
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/config/jira")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testLdapResource_NotAccessible() throws Exception {
    // LdapResource is also marked as @IqOnlyEndpoint
    HttpResponse response = ctx.restRequest()
        .path("/rest/config/ldap")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testLabsResource_NotAccessible() throws Exception {
    // LabsResource is also marked as @IqOnlyEndpoint
    HttpResponse response = ctx.restRequest()
        .path("/rest/labs")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testSuccessMetricsResource_NotAccessible() throws Exception {
    // SuccessMetricsResource is also marked as @IqOnlyEndpoint
    HttpResponse response = ctx.restRequest()
        .path("/rest/successMetrics")
        .auth(getUser())
        .get();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testIqOnlyResources_NotAccessible_AsAdmin() throws Exception {
    // Create an admin user with all permissions to ensure it's not a permission issue
    User[] adminHolder = new User[1];
    ctx.testAsTestTenant(t -> {
      User admin = ctx.tempEntity().newUser();
      Role adminRole = ctx.tempEntity().newRole(false /* global */, Permission.values());
      ctx.tempEntity()
          .newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, adminRole.getId(),
              admin.getUsername());
      adminHolder[0] = admin;
    });
    User admin = adminHolder[0];

    // Test a few critical endpoints with admin user

    // Config features should still be 404 even for admin
    HttpResponse featuresResponse = ctx.restRequest()
        .path("/api/v2/config/features")
        .auth(admin)
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, featuresResponse);

    // Crowd configuration should still be 404 even for admin
    HttpResponse crowdResponse = ctx.restRequest()
        .path("/api/v2/config/crowd")
        .auth(admin)
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, crowdResponse);

    // Support endpoint should still be 404 even for admin
    HttpResponse supportResponse = ctx.restRequest()
        .path("/rest/support")
        .auth(admin)
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, supportResponse);

    // OIDC configuration should still be 404 even for admin
    HttpResponse oidcResponse = ctx.restRequest()
        .path("/api/v2/config/oidc")
        .auth(admin)
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, oidcResponse);
  }

  @Test
  void testIqOnlyResources_SubPaths_AlsoNotAccessible() throws Exception {
    // Test that sub-paths under IQ-only resources are also not accessible

    // Features sub-path
    HttpResponse featuresSubPath = ctx.restRequest()
        .path("/api/v2/config/features/someFeature")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, featuresSubPath);

    // Source control sub-path
    HttpResponse sourceControlSubPath = ctx.restRequest()
        .path("/api/v2/config/sourceControl/github/test")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, sourceControlSubPath);

    // OIDC sub-path
    HttpResponse oidcSubPath = ctx.restRequest()
        .path("/api/v2/config/oidc/test")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, oidcSubPath);

    // Support sub-path
    HttpResponse supportSubPath = ctx.restRequest()
        .path("/rest/support/logs")
        .auth(getUser())
        .get();
    ctx.assertResponseStatus(SC_NOT_FOUND, supportSubPath);
  }

  /**
   * Helper method to create a test user with basic permissions
   */
  private User getUser() {
    User[] userHolder = new User[1];
    ctx.testAsTestTenant(t -> {
      User user = ctx.tempEntity().newUser();
      Role role = ctx.tempEntity().newRole(false /* global */, Permission.READ);
      ctx.tempEntity().newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
      userHolder[0] = user;
    });
    return userHolder[0];
  }
}

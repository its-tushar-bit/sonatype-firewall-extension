/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiDataRetentionPolicyResource;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.license.model.LicensedFeature;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wide-and-shallow regression coverage for RBAC ({@code /api/v2/users},
 * {@code /api/v2/roles}, {@code /api/v2/roleMemberships}, {@code /api/v2/userTokens}),
 * user activity, global config ({@code /api/v2/config}), and data-retention policies.
 * Pins the HTTP contract on the highest-signal endpoint per family; full CRUD lives at
 * the resource tier ({@code ApiUserResourceTest}, {@code ApiRoleResourceTest},
 * {@code ApiConfigurationResourceTest}, {@code ApiDataRetentionPolicyResourceTest}).
 *
 * <p>
 * <b>License setup.</b> Only {@link LicensedFeature#DATA_RETENTION} needs enabling
 * ({@link ApiDataRetentionPolicyResource} is the only license-enforced family here).
 * RBAC families are gated by Shiro permissions, not license features;
 * {@link ApiConfigurationResource} is {@code @UnlicensedPath}.
 *
 * <p>
 * <b>AC deviations.</b>
 * <ul>
 * <li>{@code GET /api/v2/product/license} and {@code GET /api/v2/telemetry} do not
 * exist — the ticket wording was inaccurate.</li>
 * <li>{@code POST /api/v2/roles} has no reliable 400 branch; happy path is covered at
 * the resource tier.</li>
 * <li>Data retention path is {@code organizations/{organizationId}} (plural), not
 * {@code {ownerType}/{ownerId}}.</li>
 * <li>SCM/SAML/OIDC/Crowd/Jira/mail/proxy/zscaler/artifactory sub-config families
 * require external services and stay at the resource tier.</li>
 * <li>User activity endpoint is feature-gated by {@code USER_ACTIVITY_TRACKING}; only
 * the disabled-side 404 is pinned here (the enabled-side happy path needs DB seeding
 * of the system property, which is out of scope).</li>
 * </ul>
 */
public class RbacAndSystemConfigApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String USERS_BASE = PublicApiPaths.USER_RESOURCE_PATH_V2;

  private static final String ROLES_BASE = PublicApiPaths.ROLE_RESOURCE_PATH_V2;

  private static final String ROLE_MEMBERSHIPS_BASE = PublicApiPaths.ROLE_MEMBERSHIP_PATH_V2;

  private static final String USER_TOKENS_BASE = PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2;

  private static final String CONFIG_BASE = PublicApiPaths.CONFIG_RESOURCE_PATH_V2;

  private static final String DATA_RETENTION_BASE = PublicApiPaths.DATA_RETENTION_POLICY_RESOURCE_PATH;

  @BeforeEach
  public void enableLicenseFeatures() throws Exception {
    setFeatures(LicensedFeature.DATA_RETENTION);
  }

  private static String userByUsernamePath(final String username) {
    return USERS_BASE + "/" + username;
  }

  private static String roleByIdPath(final String roleId) {
    return ROLES_BASE + "/" + roleId;
  }

  private static String roleMembershipsOrgPath(final String orgId) {
    return ROLE_MEMBERSHIPS_BASE + "/organization/" + orgId;
  }

  private static String userTokenByUsernamePath(final String username) {
    return USER_TOKENS_BASE + "/" + username;
  }

  private static String dataRetentionOrgPath(final String orgId) {
    return DATA_RETENTION_BASE + "/organizations/" + orgId;
  }

  private static ApiUserDTO newUserBody(final String username) {
    ApiUserDTO body = new ApiUserDTO();
    body.username = username;
    body.password = "RegressionP@ss1";
    body.firstName = "Api";
    body.lastName = "Regression";
    body.email = username + "@example.com";
    return body;
  }

  /**
   * {@code POST /api/v2/users} returns 204 with an empty body — the DTO is validated and
   * inserted via {@code UserService.addUser} but the resource method is {@code void}.
   * API-created users survive {@code TemporaryEntity.after()}; the uniquely-suffixed
   * username here prevents cross-test collisions inside the fork.
   */
  @Test
  public void testCreateUser_happyPath_returns204() throws Exception {
    String username = uniqueId("api-user");
    boolean created = false;
    try {
      HttpResponse response = apiPostJson(USERS_BASE, newUserBody(username));

      assertResponseStatus(204, response);
      created = true;
    }
    finally {
      if (created) {
        apiDelete(userByUsernamePath(username));
      }
    }
  }

  @Test
  public void testCreateUser_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(USERS_BASE, newUserBody(uniqueId("anon-user")));

    assertResponseStatus(401, response);
  }

  /**
   * Happy-path GET single-user lookup — seeds via {@code tempEntity.newUser(...)} so the
   * row lands on the internal realm, then asserts 200 + the {@code username} echo.
   * Mirrors the {@code testGetUserToken_byUsername_happyPath_returns200} pattern used
   * elsewhere in this class for realm-backed single-entity GETs.
   */
  @Test
  public void testGetUser_byUsername_happyPath_returns200() throws Exception {
    String username = uniqueId("api-user-get");
    tempEntity.newUser(username);

    HttpResponse response = apiGet(userByUsernamePath(username));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("username").isEqualTo(username);
  }

  @Test
  public void testGetUser_unknownUsername_returns404() throws Exception {
    HttpResponse response = apiGet(userByUsernamePath(uniqueId("no-such-user")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("cannot find a user")
        .containsIgnoringCase("username");
  }

  /**
   * 401 anon pin for GET — {@code ApiUserResource} is not {@code @UnlicensedPath} and
   * carries no Shiro anon exception, so an anonymous GET is rejected upstream of the
   * DAO lookup. A synthetic username is enough to reach the auth branch.
   */
  @Test
  public void testGetUser_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(userByUsernamePath(uniqueId("any-user")));

    assertResponseStatus(401, response);
  }

  /**
   * {@code GET /api/v2/roles} returns the built-in role catalog plus any custom roles.
   * Contract pin: 200 + JSON body wrapping a non-empty {@code roles} array. Specific role
   * ids are not asserted because they may vary across releases when the built-in set
   * changes.
   */
  @Test
  public void testGetRoles_happyPath_returns200() throws Exception {
    HttpResponse response = apiGet(ROLES_BASE);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("roles").isArray().isNotEmpty();
  }

  @Test
  public void testGetRole_unknownRoleId_returns404() throws Exception {
    HttpResponse response = apiGet(roleByIdPath(uniqueId("no-such-role")));

    assertResponseStatus(404, response);
    // The role resource surfaces the DAO-level 404 wording rather than the service's
    // "No role found with id …" — pin the DAO fragment which is stable across releases.
    assertThat(response.getBodyText())
        .containsIgnoringCase("role with id")
        .containsIgnoringCase("does not exist");
  }

  /**
   * Uses {@link Organization#ROOT_ORGANIZATION_ID} because the root org always exists
   * across test forks and always carries at least the seeded admin mapping — a stable
   * happy-path target that avoids the fixture cost of seeding a real membership.
   */
  @Test
  public void testGetRoleMemberships_organization_happyPath_returns200() throws Exception {
    HttpResponse response = apiGet(roleMembershipsOrgPath(Organization.ROOT_ORGANIZATION_ID));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("memberMappings").isArray();
  }

  /**
   * 401 anon pin for the GET verb — symmetric with the grant (PUT) and revoke (DELETE)
   * pins below. Shiro rejects upstream of the DAO lookup, so a synthetic org id
   * suffices.
   */
  @Test
  public void testGetRoleMemberships_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(roleMembershipsOrgPath(uniqueId("any-org")));

    assertResponseStatus(401, response);
  }

  /**
   * Seeds via {@code tempEntity.newUser(...)} + {@code tempEntity.newUserToken(...)} so
   * the row exists on the internal realm before the GET. The user-token GET returns
   * metadata only (no {@code passCode}) — the assertion pins the {@code userCode}
   * presence and the correct {@code realm} echo.
   */
  @Test
  public void testGetUserToken_byUsername_happyPath_returns200() throws Exception {
    String username = uniqueId("api-tok-user");
    tempEntity.newUser(username);
    tempEntity.newUserToken(username, InternalRealm.ID);

    HttpResponse response = apiGet(userTokenByUsernamePath(username));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("userCode").isString().isNotEmpty();
    assertThatJson(response.getBodyText()).node("realm").isEqualTo(User.INTERNAL_REALM_ID);
  }

  @Test
  public void testGetUserToken_unknownUser_returns404() throws Exception {
    HttpResponse response = apiGet(userTokenByUsernamePath(uniqueId("no-token-user")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("no user token found");
  }

  /**
   * Query-param GET — the endpoint accepts one or more {@code property=} values and
   * returns the requested subset. {@code baseUrl} is guaranteed to exist as a known
   * property (even if unset) so the response is a well-formed JSON object.
   */
  @Test
  public void testGetConfiguration_baseUrl_happyPath_returns200() throws Exception {
    HttpResponse response = apiGet(CONFIG_BASE, "property", "baseUrl");

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isObject();
  }

  /**
   * Reliable 400 pin — {@code ApiConfigurationService.setConfiguration} throws
   * {@code BadRequestException("No properties were specified.")} when the request body is
   * empty. Runs before any DAO write so it pins independently of state.
   */
  @Test
  public void testPutConfiguration_emptyBody_returns400() throws Exception {
    HttpResponse response = apiPutJson(CONFIG_BASE, Map.of());

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("no properties")
        .containsIgnoringCase("specified");
  }

  @Test
  public void testGetDataRetentionPolicies_organization_happyPath_returns200() throws Exception {
    Organization org = tempEntity.newOrganization();

    HttpResponse response = apiGet(dataRetentionOrgPath(org.getId()));

    assertResponseStatus(200, response);
    // Response shape is ApiDataRetentionPoliciesDTO — assert JSON object contract.
    assertThatJson(response.getBodyText()).isObject();
  }

  /**
   * Reliable 400 pin — {@code ApiDataRetentionPolicyService.setDataRetentionPolicies}
   * rejects an empty policies map with the pinned fragment before any DAO write, matching
   * the resource-tier test at {@code ApiDataRetentionPolicyServiceTest:299}.
   */
  @Test
  public void testPutDataRetentionPolicies_emptyPolicies_returns400() throws Exception {
    Organization org = tempEntity.newOrganization();

    HttpResponse response = apiPutJson(dataRetentionOrgPath(org.getId()), Map.of());

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("does not specify")
        .containsIgnoringCase("retention policies");
  }

  /**
   * Happy-path PUT — {@code tempEntity.newUser} seeds an internal-realm user; the PUT
   * updates {@code firstName}/{@code lastName}/{@code email} (username/realm/password
   * are non-updatable per resource Javadoc). Response echoes the merged DTO.
   */
  @Test
  public void testUpdateUser_happyPath_returns200() throws Exception {
    String username = uniqueId("api-user-update");
    tempEntity.newUser(username);
    ApiUserDTO body = new ApiUserDTO();
    body.firstName = "Updated";
    body.lastName = "Regression";
    body.email = username + "@example.com";

    HttpResponse response = apiPutJson(userByUsernamePath(username), body);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("firstName").isEqualTo("Updated");
  }

  @Test
  public void testUpdateUser_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiPutJson(userByUsernamePath(uniqueId("no-user")), newUserBody(uniqueId("anon")));

    assertResponseStatus(401, response);
  }

  /**
   * DELETE happy path — creates a user via API POST (so uniqueness is guaranteed inside
   * the fork) then deletes it. The internal-realm default applies on both verbs.
   */
  @Test
  public void testDeleteUser_happyPath_returns204() throws Exception {
    String username = uniqueId("api-user-delete");
    HttpResponse seed = apiPostJson(USERS_BASE, newUserBody(username));
    assertResponseStatus(204, seed);

    HttpResponse response = apiDelete(userByUsernamePath(username));

    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteUser_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(userByUsernamePath(uniqueId("no-user")));

    assertResponseStatus(401, response);
  }

  private static ApiRoleDTO newRoleBody(final String name) {
    ApiRoleDTO body = new ApiRoleDTO();
    body.name = name;
    body.description = "Regression role fixture";
    body.permissionCategories = List.of();
    return body;
  }

  /**
   * Happy-path POST — creates a custom role with empty {@code permissionCategories}
   * (allowed for creation; permissions can be layered in later). The response echoes
   * the role details including the assigned {@code id}, which is captured for the
   * follow-up DELETE inside {@code try/finally} — {@code role} rows are not tracked by
   * {@code TemporaryEntity.after()}.
   */
  @Test
  public void testAddRole_happyPath_returns200() throws Exception {
    String roleName = uniqueName("regression-role");
    String createdRoleId = null;
    try {
      HttpResponse response = apiPostJson(ROLES_BASE, newRoleBody(roleName));
      assertResponseStatus(200, response);
      assertThatJson(response.getBodyText()).node("id").isString().isNotEmpty();
      assertThatJson(response.getBodyText()).node("name").isEqualTo(roleName);
      createdRoleId = response.getBody(ApiRoleDTO.class).id;
    }
    finally {
      if (createdRoleId != null) {
        apiDelete(roleByIdPath(createdRoleId));
      }
    }
  }

  @Test
  public void testAddRole_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(ROLES_BASE, newRoleBody(uniqueName("anon-role")));

    assertResponseStatus(401, response);
  }

  @Test
  public void testUpdateRole_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiPutJson(roleByIdPath(uniqueId("no-role")), newRoleBody(uniqueName("anon-role")));

    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteRole_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(roleByIdPath(uniqueId("no-role")));

    assertResponseStatus(401, response);
  }

  /**
   * 401-only pins — happy-path grant/revoke require both a real user and a real role id;
   * with role ids being release-sensitive (custom roles are stable but built-ins may
   * shift) the resource-tier tests {@code ApiRoleMembershipResourceTest} cover the
   * bookkeeping. Here the auth ordering is what we pin.
   */
  @Test
  public void testGrantRoleMembership_unauthenticated_returns401() throws Exception {
    String path = ROLE_MEMBERSHIPS_BASE
        + "/organization/" + Organization.ROOT_ORGANIZATION_ID
        + "/role/" + uniqueId("role")
        + "/user/" + uniqueId("user");

    HttpResponse response = anonApiPut(path);

    assertResponseStatus(401, response);
  }

  @Test
  public void testRevokeRoleMembership_unauthenticated_returns401() throws Exception {
    String path = ROLE_MEMBERSHIPS_BASE
        + "/organization/" + Organization.ROOT_ORGANIZATION_ID
        + "/role/" + uniqueId("role")
        + "/user/" + uniqueId("user");

    HttpResponse response = anonApiDelete(path);

    assertResponseStatus(401, response);
  }

  private static final String USER_TOKEN_CURRENT_USER_PATH = USER_TOKENS_BASE + "/currentUser";

  /**
   * Idempotent POST — creates a user token for the current admin. Admin may already have
   * a token from a prior test's POST; either way the endpoint returns 200 with the
   * canonical DTO shape ({@code userCode}, {@code passCode}, {@code username},
   * {@code realm}). The DELETE afterwards cleans up so the ordering doesn't leak into
   * later tests.
   */
  @Test
  public void testCreateUserToken_currentUser_happyPath_returns200() throws Exception {
    // Reset admin's token slot so the POST is deterministic even if a prior test left
    // one behind — DELETE is a no-op when no token exists.
    apiDelete(USER_TOKEN_CURRENT_USER_PATH);
    try {
      HttpResponse response = apiPostJson(USER_TOKEN_CURRENT_USER_PATH, Map.of());

      assertResponseStatus(200, response);
      assertThatJson(response.getBodyText()).node("userCode").isString().isNotEmpty();
      assertThatJson(response.getBodyText()).node("passCode").isString().isNotEmpty();
    }
    finally {
      apiDelete(USER_TOKEN_CURRENT_USER_PATH);
    }
  }

  @Test
  public void testCreateUserToken_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(USER_TOKEN_CURRENT_USER_PATH, Map.of());

    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteUserToken_currentUser_happyPath_returns204() throws Exception {
    HttpResponse seed = apiPostJson(USER_TOKEN_CURRENT_USER_PATH, Map.of());
    assertResponseStatus(200, seed);

    HttpResponse response = apiDelete(USER_TOKEN_CURRENT_USER_PATH);

    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteUserToken_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(USER_TOKEN_CURRENT_USER_PATH);

    assertResponseStatus(401, response);
  }

  private static final String USER_TOKEN_CONFIG_BASE = PublicApiPaths.USER_TOKEN_CONFIG_RESOURCE_PATH_V2;

  @Test
  public void testGetUserTokenConfig_happyPath_returns200() throws Exception {
    HttpResponse response = apiGet(USER_TOKEN_CONFIG_BASE);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isObject();
  }

  @Test
  public void testGetUserTokenConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(USER_TOKEN_CONFIG_BASE);

    assertResponseStatus(401, response);
  }

  /**
   * Round-trip PUT — sends a numeric expiration in days and asserts the response echoes
   * it. Follow-up DELETE resets to system defaults so this test is idempotent.
   */
  @Test
  public void testUpdateUserTokenConfig_happyPath_returns200() throws Exception {
    try {
      HttpResponse putResponse =
          apiPutJson(USER_TOKEN_CONFIG_BASE, Map.of("userTokenDefaultExpirationDays", 30));

      assertResponseStatus(200, putResponse);
      assertThatJson(putResponse.getBodyText())
          .node("userTokenDefaultExpirationDays")
          .isEqualTo(30);
    }
    finally {
      apiDelete(USER_TOKEN_CONFIG_BASE);
    }
  }

  @Test
  public void testUpdateUserTokenConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPutJson(USER_TOKEN_CONFIG_BASE, Map.of());

    assertResponseStatus(401, response);
  }

  private static final String USER_ACTIVITY_BASE = PublicApiPaths.USER_ACTIVITY_RESOURCE_PATH;

  /**
   * User activity endpoints are gated on the {@code USER_ACTIVITY_TRACKING} system
   * feature flag which is off by default on a fresh IQ. JAX-RS
   * {@code @HasFeature}-flag-off returns 404 "Feature not supported" (emitted by
   * {@code HasFeatureMethodInterceptor#invoke}) — same pattern pinned in Class 4 for
   * container-image quarantined. Body fragment ties the 404 to the feature gate
   * specifically; a routing 404 would return the JAX-RS "Resource not found" default
   * and fail this assertion. Enabling the flag is out of scope for the regression
   * module (no DAO helper for {@code SystemConfigurationPropertyFeature}); resource-tier
   * {@code UserActivityResourceTest} covers the flag-on happy path.
   */
  @Test
  public void testGetUserActivitySummary_featureDisabled_returns404() throws Exception {
    HttpResponse response = apiGet(USER_ACTIVITY_BASE, "startUtcDate", "2026-01-01");

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("not supported");
  }

  @Test
  public void testGetUserActivitySummary_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(USER_ACTIVITY_BASE, "startUtcDate", "2026-01-01");

    assertResponseStatus(401, response);
  }
}

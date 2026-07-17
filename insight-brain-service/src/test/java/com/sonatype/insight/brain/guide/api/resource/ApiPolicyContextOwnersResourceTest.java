/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApiPolicyContextOwnersResource}. Covers authentication,
 * permission filtering, limit capping, and error conditions.
 *
 * <p>
 * Every test that needs permission filtering must call {@code .auth(user)} explicitly —
 * the default {@code restRequest()} authenticates as ADMIN (see
 * {@link com.sonatype.insight.brain.HttpRequest#auth()}), which bypasses the
 * {@code @AuthzFilter}-based scoping used by the owner-picker endpoints.
 */
public class ApiPolicyContextOwnersResourceTest
    extends AbstractResourceTest
{
  private static final String TOP_ORGS_PATH = "api/v2/policy-context/owners/top-orgs";

  private static final String SEARCH_PATH = "api/v2/policy-context/owners/search";

  /**
   * The owner-picker resource is gated by {@link LicensedFeature#GUIDE_SEARCH}, which is
   * HDS-controlled and not present in the integration-test license mock by default. Without
   * this override every request would 402 before reaching the resource. Sibling Guide tests
   * (e.g. {@code GuideComponentsResourceTest}) follow the same pattern.
   */
  @Before
  public void enableGuideFeatures() throws Exception {
    setFeatures(LicensedFeature.GUIDE, LicensedFeature.GUIDE_MCP, LicensedFeature.GUIDE_SEARCH);
  }

  // --- Authentication ---

  @Test
  public void getTopOrgs_unauthenticated_returns401() throws Exception {
    HttpResponse response = restRequest()
        .path(TOP_ORGS_PATH)
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void search_unauthenticated_returns401() throws Exception {
    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .query("query", "zeta")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  // --- Permission filtering ---

  @Test
  public void getTopOrgs_withEvaluateApplicationPermission_returnsOrgs() throws Exception {
    tempEntity.newOrganization("Zeta-permission-org");
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-permission-org\"");
  }

  @Test
  public void getTopOrgs_withNoPermission_returnsEmptyList() throws Exception {
    tempEntity.newOrganization("Zeta-nopermission-org");
    User user = createUserWithPermissions(); // no permissions granted

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"orgs\":[]");
    assertThat(response.getBodyText()).contains("\"totalOrgCount\":0");
    assertThat(response.getBodyText()).doesNotContain("Zeta-nopermission-org");
  }

  // --- Limit capping ---

  @Test
  public void getTopOrgs_withLimit_capsResults() throws Exception {
    tempEntity.newOrganization("Zeta-limit-A");
    tempEntity.newOrganization("Zeta-limit-B");
    tempEntity.newOrganization("Zeta-limit-C");
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .query("limit", "2")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    // totalOrgCount reflects total permitted (3) even though only 2 returned
    assertThat(response.getBodyText()).contains("\"totalOrgCount\":3");
    assertThat(response.getBodyText()).contains("Zeta-limit-A");
    assertThat(response.getBodyText()).contains("Zeta-limit-B");
    assertThat(response.getBodyText()).doesNotContain("Zeta-limit-C");
  }

  @Test
  public void getTopOrgs_limitOverMax_returns400() throws Exception {
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .query("limit", "101")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("limit must not exceed 100");
  }

  @Test
  public void getTopOrgs_limitUnderOne_returns400() throws Exception {
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .query("limit", "0")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("limit must be at least 1");
  }

  // --- Search ---

  @Test
  public void search_withQuery_returnsMatching() throws Exception {
    Organization org = tempEntity.newOrganization("Zeta-search-Corp");
    tempEntity.newApplication("Zeta-search-Portal", "zeta-search-portal", org.getId());
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "zeta-search")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("Zeta-search-Corp");
    assertThat(response.getBodyText()).contains("Zeta-search-Portal");
  }

  @Test
  public void search_typeOrgOnly_returnsOnlyOrgs() throws Exception {
    Organization org = tempEntity.newOrganization("Zeta-typeorg-Corp");
    tempEntity.newApplication("Zeta-typeorg-Portal", "zeta-typeorg-portal", org.getId());
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "zeta-typeorg")
        .query("type", "org")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("Zeta-typeorg-Corp");
    assertThat(response.getBodyText()).doesNotContain("Zeta-typeorg-Portal");
    // Apps array should be present but empty
    assertThat(response.getBodyText()).contains("\"apps\":[]");
  }

  @Test
  public void search_typeAppOnly_returnsOnlyApps() throws Exception {
    // Uses distinct org/app names so we can assert on names independently: the parent org
    // "Zeta-typeapponly-Corp" is expected in the app's ancestorPath breadcrumb (it just
    // shouldn't appear as a top-level orgs result), while the app name "Zeta-typeapponly-App"
    // must appear in the apps array.
    Organization org = tempEntity.newOrganization("Zeta-typeapponly-Corp");
    tempEntity.newApplication("Zeta-typeapponly-App", "zeta-typeapponly-app", org.getId());
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "zeta-typeapponly-app") // matches the app only, not the org
        .query("type", "app")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("Zeta-typeapponly-App");
    assertThat(response.getBodyText()).contains("\"orgs\":[]");
  }

  @Test
  public void search_missingQuery_returns400() throws Exception {
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("query parameter is required");
  }

  @Test
  public void search_blankQuery_returns400() throws Exception {
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "   ")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("query parameter is required");
  }

  @Test
  public void search_invalidType_returns400() throws Exception {
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "zeta")
        .query("type", "bogus")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("type must be one of");
  }

  @Test
  public void search_limitOverMax_returns400() throws Exception {
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "zeta")
        .query("limit", "51")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("limit must not exceed 50");
  }

  // --- Organization apps endpoint ---

  @Test
  public void getOrgApps_withEvaluateComponentPermission_returnsApps() throws Exception {
    Organization org = tempEntity.newOrganization("Zeta-orgapps-Corp");
    tempEntity.newApplication("Zeta-orgapps-A", "zeta-orgapps-a", org.getId());
    tempEntity.newApplication("Zeta-orgapps-B", "zeta-orgapps-b", org.getId());
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/orgs/" + org.getId() + "/apps")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-orgapps-A\"");
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-orgapps-B\"");
    assertThat(response.getBodyText()).contains("\"truncated\":false");
  }

  @Test
  public void getOrgApps_nonExistentOrg_returns404() throws Exception {
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/orgs/nonexistent-id/apps")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void getOrgApps_orgExistsButNoEvaluateAppPermission_returns404() throws Exception {
    // Spec: 404 for no permission on the org must be indistinguishable from 404 for
    // "org not found". Even if the caller has EVALUATE_COMPONENT on apps in the org, they
    // must not be able to enumerate the org's existence via this endpoint.
    Organization org = tempEntity.newOrganization("Zeta-hidden-Corp");
    tempEntity.newApplication("Zeta-hidden-App", "zeta-hidden-app", org.getId());
    User user = createUserWithPermissions(Permission.EVALUATE_COMPONENT); // no EVALUATE_APPLICATION

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/orgs/" + org.getId() + "/apps")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  // --- Resolve owner endpoint ---

  @Test
  public void resolveOwner_existingOrg_withPermission_returnsOrgSummary() throws Exception {
    Organization org = tempEntity.newOrganization("Zeta-resolve-Org");
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/" + org.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-resolve-Org\"");
    assertThat(response.getBodyText()).contains("\"type\":\"organization\"");
    assertThat(response.getBodyText()).contains("\"appCount\":0");
  }

  @Test
  public void resolveOwner_existingOrg_withoutEvaluateAppPermission_returns404() throws Exception {
    // Verifies the resolveOwner permission gate: IdUtils.getOwnerNotNull is not authz-checked,
    // so the service must explicitly intersect against the summary service.
    Organization org = tempEntity.newOrganization("Zeta-resolvenone-Org");
    User user = createUserWithPermissions(); // no EVALUATE_APPLICATION

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/" + org.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void resolveOwner_existingApp_byInternalId_withPermission_returnsAppSummary() throws Exception {
    Organization org = tempEntity.newOrganization("Zeta-resolveapp-Parent");
    Application app = tempEntity.newApplication("Zeta-resolveapp-App", "zeta-resolveapp-app", org.getId());
    User user = createUserWithPermissions(Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/" + app.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-resolveapp-App\"");
    assertThat(response.getBodyText()).contains("\"type\":\"application\"");
    assertThat(response.getBodyText()).contains("\"publicId\":\"" + app.getPublicId() + "\"");
  }

  @Test
  public void resolveOwner_existingApp_byPublicId_returnsAppSummary() throws Exception {
    Organization org = tempEntity.newOrganization("Zeta-resolveapppublic-Parent");
    Application app =
        tempEntity.newApplication("Zeta-resolveapppublic-App", "zeta-resolveapppublic", org.getId());
    User user = createUserWithPermissions(Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/" + app.getPublicId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-resolveapppublic-App\"");
  }

  @Test
  public void resolveOwner_existingApp_withoutEvaluateComponentPermission_returns404() throws Exception {
    Organization org = tempEntity.newOrganization("Zeta-resolveappnone-Parent");
    Application app = tempEntity.newApplication("Zeta-resolveappnone-App", "zeta-resolveappnone", org.getId());
    User user = createUserWithPermissions(); // no EVALUATE_COMPONENT

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/" + app.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void resolveOwner_nonExistent_returns404() throws Exception {
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/nonexistent-id")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  // --- App count in /top-orgs ---

  @Test
  public void getTopOrgs_includesAppCount() throws Exception {
    Organization org = tempEntity.newOrganization("Zeta-appcount-Org");
    tempEntity.newApplication("Zeta-appcount-1", "zeta-appcount-1", org.getId());
    tempEntity.newApplication("Zeta-appcount-2", "zeta-appcount-2", org.getId());
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-appcount-Org\"");
    assertThat(response.getBodyText()).contains("\"appCount\":2");
  }

  @Test
  public void getTopOrgs_orgWithNoApps_appCountIsZero() throws Exception {
    tempEntity.newOrganization("Zeta-empty-Org");
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-empty-Org\"");
    assertThat(response.getBodyText()).contains("\"appCount\":0");
  }

  // --- Ancestor path ---

  @Test
  public void resolveOwner_ancestorPath_excludesRootOrganizationAndTarget() throws Exception {
    // OwnerDAO.walkHierarchy yields [target, parent, ..., ROOT_ORGANIZATION]. flattenHierarchy
    // must exclude both the target and ROOT_ORGANIZATION from ancestorPath (spec: "chain of
    // parent orgs from root down to but not including the org itself"). Verify by resolving
    // a nested org and checking the breadcrumb contains only the parent.
    Organization parent = tempEntity.newOrganization("Zeta-ancestor-Parent");
    Organization child = tempEntity.newOrganization("Zeta-ancestor-Child", parent);
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path("api/v2/policy-context/owners/" + child.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    // Neither ROOT_ORGANIZATION nor the target itself may appear in the ancestorPath.
    assertThat(response.getBodyText()).doesNotContain("ROOT_ORGANIZATION_ID");
    assertThat(response.getBodyText()).doesNotContain("Root Organization");
    // Parent should appear (breadcrumb up to but not including the target).
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-ancestor-Parent\"");
    // Ancestor path is a non-empty array (there IS a parent).
    assertThat(response.getBodyText()).doesNotContain("\"ancestorPath\":[]");
  }

  // --- Case-insensitive sorting ---

  @Test
  public void getTopOrgs_sortedCaseInsensitive() throws Exception {
    tempEntity.newOrganization("Zeta-sort-beta");
    tempEntity.newOrganization("Zeta-sort-Alpha");
    tempEntity.newOrganization("Zeta-sort-Gamma");
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    String body = response.getBodyText();
    // Alphabetical order regardless of case.
    assertThat(body.indexOf("Zeta-sort-Alpha")).isLessThan(body.indexOf("Zeta-sort-beta"));
    assertThat(body.indexOf("Zeta-sort-beta")).isLessThan(body.indexOf("Zeta-sort-Gamma"));
  }

  // --- App count respects EVALUATE_COMPONENT permission ---

  @Test
  public void getTopOrgs_appCountRespectsEvaluateComponentPermission() throws Exception {
    // Spec: appCount is "the number of applications directly under that org that the caller
    // has EVALUATE_COMPONENT on" — not the raw count. User has EVALUATE_APPLICATION at root
    // (so the org appears) and EVALUATE_COMPONENT scoped to only one of the two apps.
    // appCount must be 1.
    Organization org = tempEntity.newOrganization("Zeta-partial-Org");
    Application permittedApp =
        tempEntity.newApplication("Zeta-partial-A", "zeta-partial-a", org.getId());
    tempEntity.newApplication("Zeta-partial-B", "zeta-partial-b", org.getId());

    User user = tempEntity.newUser();
    Role appEvalRole = tempEntity.newRole(false, Permission.EVALUATE_APPLICATION);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, appEvalRole.getId(), user.getUsername());
    // EVALUATE_COMPONENT scoped only to permittedApp — user cannot evaluate against the other app.
    tempEntity.newMembershipMapping(permittedApp.getId(), Role.COMPONENT_EVALUATOR_ROLE_ID, user.getUsername());

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"Zeta-partial-Org\"");
    assertThat(response.getBodyText()).contains("\"appCount\":1");
    assertThat(response.getBodyText()).doesNotContain("\"appCount\":2");
  }

  // --- Search truncation ---

  @Test
  public void search_orgsExceedingLimit_setsOrgsTruncatedTrue() throws Exception {
    for (int i = 1; i <= 12; i++) {
      tempEntity.newOrganization("Zeta-trunc-Org-" + i);
    }
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "zeta-trunc")
        .query("type", "org")
        .query("limit", "10")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"orgsTruncated\":true");
  }

  @Test
  public void search_appsExceedingLimit_setsAppsTruncatedTrue() throws Exception {
    Organization org = tempEntity.newOrganization("Zeta-trunc-App-Corp");
    for (int i = 1; i <= 12; i++) {
      tempEntity.newApplication("Zeta-trunc-App-" + i, "zeta-trunc-app-" + i, org.getId());
    }
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "zeta-trunc-app")
        .query("type", "app")
        .query("limit", "10")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"appsTruncated\":true");
  }

  // --- Query length cap ---

  @Test
  public void search_queryExceedsMaxLength_returns400() throws Exception {
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);
    String longQuery = "a".repeat(201);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", longQuery)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
  }

  @Test
  public void search_queryAtMaxLength_returns200() throws Exception {
    // Boundary test: exactly 200 chars must be accepted (limit is inclusive).
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);
    String maxLengthQuery = "a".repeat(200);

    HttpResponse response = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", maxLengthQuery)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  // --- Unicode name handling ---

  @Test
  public void search_orgWithUnicodeName_matchesSubstring() throws Exception {
    // German umlaut and other Unicode characters must round-trip through
    // NameHelper.normalize and the pg_trgm/H2 index without corruption.
    tempEntity.newOrganization("Zeta-Uber-Cafe");
    tempEntity.newOrganization("Zeta-Muenchen-Org");
    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION);

    HttpResponse umlautResponse = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "Uber")
        .query("type", "org")
        .get();
    assertThat(umlautResponse.getStatusCode()).isEqualTo(200);
    assertThat(umlautResponse.getBodyText()).contains("Zeta-Uber-Cafe");

    HttpResponse munichResponse = restRequest()
        .auth(user)
        .path(SEARCH_PATH)
        .query("query", "Muenchen")
        .query("type", "org")
        .get();
    assertThat(munichResponse.getStatusCode()).isEqualTo(200);
    assertThat(munichResponse.getBodyText()).contains("Zeta-Muenchen-Org");
  }

  // --- Batch app-count regression (guard against N+1 re-introduction) ---

  @Test
  public void getTopOrgs_multiOrg_batchAppCountReturnsCorrectPerOrg() throws Exception {
    // Regression test for the N+1 fix: three orgs with different app counts under the same
    // caller. If the batch grouping is wrong, every org will get the same total (or zero).
    Organization orgA = tempEntity.newOrganization("Zeta-batch-Alpha");
    Organization orgB = tempEntity.newOrganization("Zeta-batch-Bravo");
    Organization orgC = tempEntity.newOrganization("Zeta-batch-Charlie");
    tempEntity.newApplication("Zeta-batch-A1", "zeta-batch-a1", orgA.getId());
    tempEntity.newApplication("Zeta-batch-A2", "zeta-batch-a2", orgA.getId());
    tempEntity.newApplication("Zeta-batch-A3", "zeta-batch-a3", orgA.getId());
    tempEntity.newApplication("Zeta-batch-B1", "zeta-batch-b1", orgB.getId());
    // orgC has zero apps — must appear with appCount:0, not be dropped.

    User user = createUserWithPermissions(Permission.EVALUATE_APPLICATION, Permission.EVALUATE_COMPONENT);

    HttpResponse response = restRequest()
        .auth(user)
        .path(TOP_ORGS_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    String body = response.getBodyText();
    // Each org must appear once with the right count. The Zeta- prefix makes findings unique
    // to this test even when other orgs exist in the shared DB.
    assertThat(body).contains("\"name\":\"Zeta-batch-Alpha\"");
    assertThat(body).contains("\"name\":\"Zeta-batch-Bravo\"");
    assertThat(body).contains("\"name\":\"Zeta-batch-Charlie\"");
    // Verify each org's appCount by locating its name and the next appCount value.
    assertAppCountFor(body, "Zeta-batch-Alpha", 3);
    assertAppCountFor(body, "Zeta-batch-Bravo", 1);
    assertAppCountFor(body, "Zeta-batch-Charlie", 0);
  }

  private static void assertAppCountFor(String responseBody, String orgName, int expectedCount) {
    int nameIdx = responseBody.indexOf("\"name\":\"" + orgName + "\"");
    assertThat(nameIdx).as("org %s present in response", orgName).isGreaterThanOrEqualTo(0);
    int appCountIdx = responseBody.indexOf("\"appCount\":", nameIdx);
    assertThat(appCountIdx).as("appCount field present after %s", orgName).isGreaterThan(nameIdx);
    String tail = responseBody.substring(appCountIdx + "\"appCount\":".length());
    // The value ends at the next comma or closing brace.
    int end = tail.length();
    for (int i = 0; i < tail.length(); i++) {
      char c = tail.charAt(i);
      if (c == ',' || c == '}') {
        end = i;
        break;
      }
    }
    int actual = Integer.parseInt(tail.substring(0, end).trim());
    assertThat(actual).as("appCount for org %s", orgName).isEqualTo(expectedCount);
  }
}

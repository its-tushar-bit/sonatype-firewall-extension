/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.license.model.LicensedFeature;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression coverage for policy-violation listing
 * ({@code /api/v2/policyViolations}), waiver-request lifecycle
 * ({@code /api/v2/policyWaiverRequests/...}), and the waiver-reason catalog
 * ({@code /api/v2/policyWaiverReasons}).
 *
 * <p>
 * License features enabled by {@code @Before}: {@code POLICY_WAIVERS},
 * {@code POLICY_VIOLATIONS}, plus the {@code WAIVER_REQUEST_WORKFLOW} entitlement
 * required by the waiver-request mutating methods.
 *
 * <p>
 * <b>Scope exclusion.</b> The deprecated
 * {@code POST /api/v2/policyWaiver/{violationId}/{ownerType}} is intentionally not
 * covered — it's marked {@code @Deprecated + @Operation(hidden = true)}, and the same
 * service delegate is already exercised end-to-end by the non-hidden
 * {@code POST /api/v2/policyWaivers/...} in {@code PolicyWaiversApiRegressionTest}.
 *
 * <p>
 * <b>AC deviations.</b> {@code GET /api/v2/policyViolations} and
 * {@code GET /api/v2/policyWaiverReasons} have no owner-scoped path parameter, so
 * neither has a reachable 404 branch at the HTTP layer — no synthetic 404 tests
 * asserted.
 */
@Category(ApiRegressionTest.class)
public class PolicyViolationsAndWaiverRequestsApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String VIOLATIONS_PATH = PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2;

  private static final String WAIVER_REQUESTS_BASE = PublicApiPaths.POLICY_WAIVER_REQUEST_PATH;

  private static final String WAIVER_REASONS_PATH = PublicApiPaths.POLICY_WAIVER_REASONS_PATH;

  @Before
  public void enableLicenseFeatures() throws Exception {
    setFeatures(
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.WAIVER_REQUEST_WORKFLOW);
  }

  private static String waiverRequestAppBase(final Application app) {
    return WAIVER_REQUESTS_BASE + "/application/" + app.getId();
  }

  private static String waiverRequestCreatePath(final Application app, final String violationId) {
    return waiverRequestAppBase(app) + "/policyViolation/" + violationId;
  }

  private static String waiverRequestReviewPath(final Application app, final String requestId) {
    return waiverRequestAppBase(app) + "/review/" + requestId;
  }

  private static String waiverRequestByIdPath(final Application app, final String requestId) {
    return waiverRequestAppBase(app) + "/" + requestId;
  }

  /**
   * Seeds the full policy-violation triple against the given app. Mirrors the pattern used
   * by {@code PolicyWaiversApiRegressionTest#seedPolicyViolation} so both suites break in
   * the same place if the underlying DAO or evaluation schema drifts.
   *
   * <p>
   * The returned {@link PolicyViolation}'s parent {@link Policy} is auto-cleaned by
   * {@code TemporaryEntity.after()}, which cascades to {@code policy_violation},
   * {@code policy_waiver}, and {@code policy_waiver_request} via schema FKs.
   */
  private PolicyViolation seedPolicyViolation(final Application app) {
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-part7-scan"));
    return tempEntity.newPolicyViolation(evaluation, policy, "g1", "a1", "v1", "hash1", "reason1");
  }

  /**
   * Fabricates a seeded application <em>and</em> its parent policy/violation, then submits
   * a waiver request against the violation via API POST and returns the
   * {@code policyWaiverRequestId}. Used as the setup step for review / withdraw tests so
   * their assertions only need to focus on their own verb's contract.
   */
  private String seedWaiverRequestId(final Application app) throws Exception {
    PolicyViolation violation = seedPolicyViolation(app);

    ApiPolicyWaiverRequestOptionsDTO body = new ApiPolicyWaiverRequestOptionsDTO();
    body.comment = "seed request";

    HttpResponse response = apiPostJson(waiverRequestCreatePath(app, violation.getId()), body);
    assertResponseStatus(200, response);
    String requestId = readWaiverRequestId(response);
    assertThat(requestId).as("seed waiver request must return a non-blank id").isNotBlank();
    return requestId;
  }

  /**
   * Extracts {@code policyWaiverRequestId} via the harness's server-matched
   * deserialization — matches the module's "no {@code ObjectMapper} in tests" convention
   * (see the skill guide for details). A missing field surfaces as an NPE at the call
   * site rather than a silent empty-string, which is the loud-failure mode the module
   * prefers.
   */
  private static String readWaiverRequestId(final HttpResponse response) {
    return response.getBody(ApiPolicyWaiverRequestDTO.class).policyWaiverRequestId;
  }

  /**
   * Seed a violation on a fresh app + policy and verify the wrapper contains it when
   * filtered by policyId. The wrapper shape ({@code {"applicationViolations":[…]}}) is a
   * regression pin: if a refactor of {@code ApiApplicationViolationListDTOV2} inlined
   * violations into a flat array, downstream consumers would break silently until the
   * DTO name-space was restored.
   */
  @Test
  public void testGetPolicyViolations_withSeededViolation_wrapperContainsEntry() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-viol-list"), Organization.ROOT_ORGANIZATION_ID);
    PolicyViolation violation = seedPolicyViolation(app);

    HttpResponse response = apiGet(VIOLATIONS_PATH, "p", violation.getPolicyId());

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("applicationViolations").isArray().isNotEmpty();
    assertThatJson(response.getBodyText())
        .inPath("$.applicationViolations[*].policyViolations[*].policyViolationId")
        .isArray()
        .contains(violation.getId());
  }

  @Test
  public void testGetPolicyViolations_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(VIOLATIONS_PATH, "p", uniqueId("any-policy"));

    assertResponseStatus(401, response);
  }

  /**
   * Only reliably-pinnable 400 branch on this endpoint. Missing/empty {@code p} returns
   * {@code 200 + empty wrapper} (not 400), and invalid {@code type} enum values are
   * bounced by Jersey with a framework-layer 400 whose exact fragment drifts across
   * Jersey upgrades. {@code openTimeAfter} / {@code openTimeBefore} on the other hand
   * are parsed inside the resource method and produce a stable resource-body 400 with
   * fragment {@code "not a valid date"}.
   */
  @Test
  public void testGetPolicyViolations_invalidOpenTimeAfter_returns400() throws Exception {
    HttpResponse response = apiGet(VIOLATIONS_PATH, "openTimeAfter", "not-a-date");

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("not a valid date");
  }

  @Test
  public void testCreatePolicyWaiverRequest_application_happyPath_returns200() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-req-create"), Organization.ROOT_ORGANIZATION_ID);
    PolicyViolation violation = seedPolicyViolation(app);

    ApiPolicyWaiverRequestOptionsDTO body = new ApiPolicyWaiverRequestOptionsDTO();
    body.comment = "please waive";
    body.noteToReviewer = "urgent";

    HttpResponse response = apiPostJson(waiverRequestCreatePath(app, violation.getId()), body);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("policyViolationId").isEqualTo(violation.getId());
    assertThatJson(response.getBodyText()).node("comment").isEqualTo("please waive");
    assertThatJson(response.getBodyText()).node("status").isEqualTo("REQUESTED");
    assertThat(readWaiverRequestId(response)).as("create must return a non-blank waiver-request id").isNotBlank();
  }

  @Test
  public void testCreatePolicyWaiverRequest_unauthenticated_returns401() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-req-anon"), Organization.ROOT_ORGANIZATION_ID);

    ApiPolicyWaiverRequestOptionsDTO body = new ApiPolicyWaiverRequestOptionsDTO();
    body.comment = "n/a";

    HttpResponse response =
        anonApiPostJson(waiverRequestCreatePath(app, uniqueId("any-violation")), body);

    assertResponseStatus(401, response);
  }

  /**
   * Reliable 400 pin for the create verb — expiry must be in the future. Alternative 400
   * candidates ({@code expireWhenRemediationAvailable} + non-EXACT strategy;
   * unknown-{@code waiverReasonId}) work but require more setup; the past-expiry branch is
   * a self-contained validator throw in
   * {@code ApiPolicyWaiverRequestService.validatePolicyWaiverRequestOptions}.
   */
  @Test
  public void testCreatePolicyWaiverRequest_pastExpiry_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-req-past"), Organization.ROOT_ORGANIZATION_ID);
    PolicyViolation violation = seedPolicyViolation(app);

    ApiPolicyWaiverRequestOptionsDTO body = new ApiPolicyWaiverRequestOptionsDTO();
    body.comment = "expired";
    body.expiryTime = Date.from(Instant.now().minus(30, ChronoUnit.DAYS));

    HttpResponse response = apiPostJson(waiverRequestCreatePath(app, violation.getId()), body);

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("expiration date must be in the future");
  }

  @Test
  public void testCreatePolicyWaiverRequest_unknownViolation_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-req-badviol"), Organization.ROOT_ORGANIZATION_ID);

    ApiPolicyWaiverRequestOptionsDTO body = new ApiPolicyWaiverRequestOptionsDTO();
    body.comment = "n/a";

    HttpResponse response =
        apiPostJson(waiverRequestCreatePath(app, uniqueId("no-violation")), body);

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("could not find policy violation");
  }

  @Test
  public void testReviewPolicyWaiverRequest_approve_happyPath_returns200() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-req-review"), Organization.ROOT_ORGANIZATION_ID);
    String requestId = seedWaiverRequestId(app);

    ApiPolicyWaiverRequestReviewDTO body = new ApiPolicyWaiverRequestReviewDTO();
    body.status = "APPROVED";
    body.comment = "looks fine";

    HttpResponse response = apiPostJson(waiverRequestReviewPath(app, requestId), body);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("status").isEqualTo("APPROVED");
    assertThatJson(response.getBodyText()).node("policyWaiverRequestId").isEqualTo(requestId);
  }

  /**
   * Reliable 400 pin on the review verb — {@code status} is the only field with an
   * explicit up-front presence check. Null-status body reaches the validator, which throws
   * with fragment {@code "status is required"}.
   */
  @Test
  public void testReviewPolicyWaiverRequest_missingStatus_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-req-review-400"), Organization.ROOT_ORGANIZATION_ID);
    String requestId = seedWaiverRequestId(app);

    ApiPolicyWaiverRequestReviewDTO body = new ApiPolicyWaiverRequestReviewDTO();
    body.comment = "no status";

    HttpResponse response = apiPostJson(waiverRequestReviewPath(app, requestId), body);

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("status is required");
  }

  @Test
  public void testReviewPolicyWaiverRequest_unauthenticated_returns401() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-req-review-anon"), Organization.ROOT_ORGANIZATION_ID);

    ApiPolicyWaiverRequestReviewDTO body = new ApiPolicyWaiverRequestReviewDTO();
    body.status = "APPROVED";

    HttpResponse response = anonApiPostJson(waiverRequestReviewPath(app, uniqueId("any-req")), body);

    assertResponseStatus(401, response);
  }

  /**
   * <b>Fixture note — list endpoint returns repo/repo-manager/container-scoped requests
   * only.</b> The {@code getPolicyWaiverRequests} service method uses the path's
   * {@code {ownerType}/{ownerId}} <em>only</em> for the {@code checkReadPermission} entry
   * check; the returned list is always assembled from
   * {@code repositoryService.getRepositoriesWithReadPermission()} plus their parent repo
   * managers plus container-image applications the caller can access. Application-scoped
   * waiver requests are never surfaced by this endpoint — they are read via
   * {@code /api/v2/policyViolations/{violationId}/applicableWaiverRequests} instead.
   * <p>
   * To pin the list contract, this test mirrors the sibling
   * {@code ApiPolicyWaiverRequestResourceTest#testGetPolicyWaiverRequests_httpEndpoint_issuesBoundedSelectCount}
   * pattern: seed a repository, submit a repo-scoped waiver request, then query with the
   * canonical {@code (repository_container, REPOSITORY_CONTAINER_ID)} scope which admin
   * callers use to see every repo/rm/container waiver request across the instance.
   */
  @Test
  public void testGetPolicyWaiverRequests_list_afterCreate_containsRepoScopedRequest() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, uniqueName("api-part7-list-policy"));
    Repository repo = tempEntity.newRepository();
    ProxyRepositoryPolicyViolation violation =
        tempEntity.newRepositoryPolicyViolation(repo.getId(), policy.getId(), policy.getThreatLevel());

    ApiPolicyWaiverRequestOptionsDTO body = new ApiPolicyWaiverRequestOptionsDTO();
    body.comment = "for list";
    HttpResponse created = apiPostJson(
        WAIVER_REQUESTS_BASE + "/repository/" + repo.getId() + "/policyViolation/" + violation.getId(),
        body);
    assertResponseStatus(200, created);
    String requestId = readWaiverRequestId(created);
    assertThat(requestId).as("seed repo-scoped request must return a non-blank id").isNotBlank();

    HttpResponse response = apiGet(
        WAIVER_REQUESTS_BASE + "/repository_container/" + RepositoryContainer.REPOSITORY_CONTAINER_ID);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText())
        .inPath("$[*].policyWaiverRequestId")
        .isArray()
        .contains(requestId);
  }

  @Test
  public void testGetPolicyWaiverRequests_list_unknownOwner_returns404() throws Exception {
    HttpResponse response = apiGet(WAIVER_REQUESTS_BASE + "/application/" + uniqueId("no-app"));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("application")
        .containsIgnoringCase("does not exist");
  }

  @Test
  public void testGetPolicyWaiverRequests_list_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(WAIVER_REQUESTS_BASE + "/application/" + uniqueId("any-app"));

    assertResponseStatus(401, response);
  }

  @Test
  public void testWithdrawPolicyWaiverRequest_happyPath_returns204() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-req-withdraw"), Organization.ROOT_ORGANIZATION_ID);
    String requestId = seedWaiverRequestId(app);

    HttpResponse response = apiDelete(waiverRequestByIdPath(app, requestId));

    assertResponseStatus(204, response);
  }

  @Test
  public void testWithdrawPolicyWaiverRequest_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(
        WAIVER_REQUESTS_BASE + "/application/" + uniqueId("any-app") + "/" + uniqueId("any-req"));

    assertResponseStatus(401, response);
  }

  /**
   * Verifies the seed reason catalog is served as a non-empty JSON array. IQ ships with
   * eight system reasons (see {@code ApiPolicyWaiverReasonResourceTest} for the canonical
   * list); the assertion here uses {@code isNotEmpty} rather than a size-8 pin so a future
   * PR that legitimately adds a system reason doesn't false-positive.
   */
  @Test
  public void testGetPolicyWaiverReasons_returnsSeedCatalog_returns200() throws Exception {
    HttpResponse response = apiGet(WAIVER_REASONS_PATH);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isArray().isNotEmpty();
    assertThatJson(response.getBodyText())
        .inPath("$[*].reasonText")
        .isArray()
        .isNotEmpty();
  }

  /**
   * Swagger documents this endpoint as "Permissions required: None" but Shiro still
   * requires authentication at the HTTP layer — the same two-layer pattern as
   * {@code PolicyManagementApiRegressionTest#testGetPolicies_unauthenticated_returns401}.
   * If a future PR intentionally opens this to anonymous access (e.g. to satisfy a UI
   * bootstrap flow), this test will trip and the change needs an explicit decision on
   * whether the reason catalog should be public.
   */
  @Test
  public void testGetPolicyWaiverReasons_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(WAIVER_REASONS_PATH);

    assertResponseStatus(401, response);
  }
}

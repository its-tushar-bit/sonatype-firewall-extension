/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiStaleWaiversReportingResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code GET /api/v2/reports/waivers/stale}
 * ({@link ApiStaleWaiversReportingResource}).
 *
 * <p>
 * Regression guards:
 * <ul>
 * <li>Legacy repository policy violations (with null waiverId/comment/date) trigger 409 with a
 * "re-evaluate the repository" message — pre-v76 waiver data must not silently pass through
 * as valid stale waivers.
 * <li>Endpoint is server-wide (no owner filter). Assertions must not assume the response only
 * contains fixtures from a single test — reuseForks=true means state may carry between
 * methods until {@code TemporaryEntity} teardown. Every positive-path assertion is scoped by
 * seeded waiver id, not by array size.
 * </ul>
 */
@Category(ApiRegressionTest.class)
public class StaleWaiversReportApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String STALE_WAIVERS_PATH =
      PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiStaleWaiversReportingResource.PATH;

  /**
   * Positive-path + shape guard for the stale-waivers endpoint.
   *
   * <p>
   * A waiver created against an application whose evaluation predates the waiver is stale
   * (the app hasn't been re-evaluated since the waiver was written). This single test
   * verifies both that the seeded waiver appears AND that its DTO exposes every field
   * downstream consumers depend on ({@code waiverId}, {@code policyId}, {@code policyName},
   * {@code scopeOwnerId}, {@code scopeOwnerType}). The JSONPath filter
   * {@code [?(@.waiverId == '...')]} implicitly proves the waiver is present — if it
   * weren't, every subsequent {@code contains(...)} would fail.
   */
  @Test
  public void testGetStaleWaivers_returnsExpectedWaiverFields() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-stale-shape"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(app);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-stale-shape-scan"));
    PolicyWaiver waiver = tempEntity.newWaiver(policy.getId(), app.getId());

    HttpResponse response = apiGet(STALE_WAIVERS_PATH);
    assertResponseStatus(200, response);

    String bodyText = response.getBodyText();
    String waiverFilter = "$.staleWaivers[?(@.waiverId == '" + waiver.getId() + "')]";
    assertThatJson(bodyText).inPath(waiverFilter + ".policyId").isArray().contains(policy.getId());
    assertThatJson(bodyText).inPath(waiverFilter + ".policyName").isArray().isNotEmpty();
    assertThatJson(bodyText).inPath(waiverFilter + ".scopeOwnerId").isArray().contains(app.getId());
    assertThatJson(bodyText).inPath(waiverFilter + ".scopeOwnerType").isArray().contains("application");
  }

  /** Multiple stale waivers seeded in one test — response contains all of them. */
  @Test
  public void testGetStaleWaivers_multipleWaivers_returnsAll() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-stale-multi"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy1 = tempEntity.newPolicy(app);
    Policy policy2 = tempEntity.newPolicy(app);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-stale-multi-scan"));
    PolicyWaiver waiver1 = tempEntity.newWaiver(policy1.getId(), app.getId());
    PolicyWaiver waiver2 = tempEntity.newWaiver(policy2.getId(), app.getId());

    HttpResponse response = apiGet(STALE_WAIVERS_PATH);
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText())
        .inPath("$.staleWaivers[*].waiverId")
        .isArray()
        .contains(waiver1.getId(), waiver2.getId());
  }

  /**
   * Regression guard: legacy repository policy violations with null waiverId / comment / date
   * (pre-v76 data) trigger a 409 with a "re-evaluate the repository" message. If this ever
   * becomes 200 or 500 silently, the endpoint would either hide the legacy data or crash for
   * downstream users. Copied from {@code ApiStaleWaiversReportingResourceTest}.
   */
  @Test
  public void testGetStaleWaivers_legacyRepositoryWaiver_returns409() throws Exception {
    Date now = new Date();
    Application app = tempEntity.newApplication(uniqueId("api-stale-legacy"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(app);
    Repository repo = tempEntity.newRepository(uniqueId("api-stale-legacy-repo"));

    ConstraintFact constraintFact = new ConstraintFact("cf-legacy", "aa c", "OR");
    constraintFact.addConditionFact(
        new ConditionFact("MatchState", 0, "Match State is exact", "Match State was exact"));
    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact);

    tempEntity.newWaiver("h-legacy", policy.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, "repo waiver");
    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 6, "path-legacy", "hash-legacy", constraintFacts, true,
        "actionId-legacy", policy.getId(), policy.getName(), null, now,
        null, null, null);

    HttpResponse response = apiGet(STALE_WAIVERS_PATH);
    assertResponseStatus(409, response);
    // Pin the specific 409 reason so an unrelated 409 (e.g. a future conflict on a different
    // legacy code path) does not silently pass this test.
    assertThat(response.getBodyText()).containsIgnoringCase("re-evaluate");
  }

  /** Auth contract: unauthenticated callers get 401. */
  @Test
  public void testGetStaleWaivers_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(STALE_WAIVERS_PATH);
    assertResponseStatus(401, response);
  }
}

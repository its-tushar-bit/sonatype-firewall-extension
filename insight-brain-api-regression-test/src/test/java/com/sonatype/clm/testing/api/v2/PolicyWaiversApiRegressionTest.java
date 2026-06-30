/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiBulkWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService.MAX_BULK_WAIVER_VIOLATIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/policyWaivers} — covers create, bulk-create, list,
 * get, update, and delete for both {@code application} and {@code organization} owners.
 */
@Category(ApiRegressionTest.class)
public class PolicyWaiversApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String WAIVER_APP_BASE = "api/v2/policyWaivers/application/";

  private static final String WAIVER_ORG_BASE = "api/v2/policyWaivers/organization/";

  @Test
  public void testCreateWaiverByViolationId_success() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-create-waiver"), Organization.ROOT_ORGANIZATION_ID);
    PolicyViolation violation = seedPolicyViolation(app);

    ApiWaiverOptionsDTO body = new ApiWaiverOptionsDTO();
    body.comment = "approved";

    HttpResponse response = apiPostJson(waiverPath(app, violation.getId()), body);
    assertResponseStatus(204, response);

    HttpResponse listResponse = apiGet(waiverPath(app));
    assertResponseStatus(200, listResponse);
    assertThatJson(listResponse.getBodyText())
        .isArray()
        .hasSize(1);
    assertThatJson(listResponse.getBodyText())
        .node("[0].comment")
        .isEqualTo("approved");
  }

  @Test
  public void testCreateWaiverByViolationId_alreadyWaived() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-dup-waiver"), Organization.ROOT_ORGANIZATION_ID);
    PolicyViolation violation = seedPolicyViolation(app);

    ApiWaiverOptionsDTO body = new ApiWaiverOptionsDTO();
    body.comment = "first waiver";

    assertResponseStatus(204, apiPostJson(waiverPath(app, violation.getId()), body));

    HttpResponse duplicate = apiPostJson(waiverPath(app, violation.getId()), body);
    assertResponseStatus(400, duplicate);
    assertThat(duplicate.getBodyText()).containsIgnoringCase("already exists");
  }

  @Test
  public void testCreateWaiverByViolationId_invalidViolation_notFound() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-waiver-bad-violation"), Organization.ROOT_ORGANIZATION_ID);

    ApiWaiverOptionsDTO body = new ApiWaiverOptionsDTO();
    body.comment = "approved";

    HttpResponse response = apiPostJson(waiverPath(app, "nonexistent-violation-id"), body);
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("policy violation");
  }

  @Test
  public void testCreateWaiverByViolationId_withExpiry() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-waiver-expiry"), Organization.ROOT_ORGANIZATION_ID);
    PolicyViolation violation = seedPolicyViolation(app);
    Date expiry = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));

    ApiWaiverOptionsDTO body = new ApiWaiverOptionsDTO();
    body.comment = "temp";
    body.expiryTime = expiry;

    assertResponseStatus(204, apiPostJson(waiverPath(app, violation.getId()), body));

    HttpResponse getResponse = apiGet(waiverPath(app));
    assertResponseStatus(200, getResponse);
    assertThatJson(getResponse.getBodyText())
        .isArray()
        .hasSize(1);
    assertThatJson(getResponse.getBodyText())
        .node("[0].expiryTime")
        .isString()
        .isNotEmpty();
  }

  /** Bulk create within the {@code MAX_BULK_WAIVER_VIOLATIONS} limit. */
  @Test
  public void testBulkCreateWaivers_success() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-bulk-waiver"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-bulk-waiver-scan"));
    PolicyViolation violation1 =
        tempEntity.newPolicyViolation(evaluation, policy, "g1", "a1", "v1", "hash1", "reason");
    PolicyViolation violation2 =
        tempEntity.newPolicyViolation(evaluation, policy, "g2", "a2", "v2", "hash2", "reason");

    ApiWaiverOptionsDTO options = new ApiWaiverOptionsDTO();
    options.comment = "bulk waiver";
    options.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulk = new ApiBulkWaiversDTO(
        Arrays.asList(violation1.getId(), violation2.getId()),
        options);

    assertResponseStatus(204, apiPostJson(waiverPath(app), bulk));

    HttpResponse listResponse = apiGet(waiverPath(app));
    assertResponseStatus(200, listResponse);
    assertThatJson(listResponse.getBodyText())
        .isArray()
        .hasSize(2);
    assertThatJson(listResponse.getBodyText())
        .inPath("$[*].comment")
        .isArray()
        .containsOnly("bulk waiver");
  }

  /** Bulk create rejects requests over {@link MAX_BULK_WAIVER_VIOLATIONS}. */
  @Test
  public void testBulkCreateWaivers_tooManyViolations() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-bulk-limit"), Organization.ROOT_ORGANIZATION_ID);

    List<String> tooManyIds = new ArrayList<>();
    for (int i = 0; i < MAX_BULK_WAIVER_VIOLATIONS + 1; i++) {
      tooManyIds.add("violation-" + i);
    }

    ApiWaiverOptionsDTO options = new ApiWaiverOptionsDTO();
    options.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulk = new ApiBulkWaiversDTO(tooManyIds, options);

    HttpResponse response = apiPostJson(waiverPath(app), bulk);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(String.valueOf(MAX_BULK_WAIVER_VIOLATIONS));
  }

  @Test
  public void testGetPolicyWaiversForApp_returnsSeededWaiver() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-waiver-app"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(app);
    PolicyWaiver waiver = tempEntity.newWaiver(policy.getId(), app.getId());

    HttpResponse response = apiGet(waiverPath(app));
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText())
        .isArray();
    assertThatJson(response.getBodyText())
        .inPath("$[*].policyWaiverId")
        .isArray()
        .contains(waiver.getId());
  }

  @Test
  public void testGetPolicyWaiverById_returnsWaiver() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-waiver-by-id"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(app);
    PolicyWaiver waiver = tempEntity.newWaiver(policy.getId(), app.getId());

    HttpResponse response = apiGet(waiverPath(app, waiver.getId()));
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText())
        .node("policyWaiverId")
        .isEqualTo(waiver.getId());
  }

  @Test
  public void testGetPolicyWaiverById_notFound() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-waiver-404"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiGet(waiverPath(app, "nonexistent-waiver-id"));
    assertResponseStatus(404, response);
  }

  @Test
  public void testGetPolicyWaiversForOrg_returnsSeededWaiver() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("api-waiver-org"));
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver = tempEntity.newWaiver(policy.getId(), org.getId());

    HttpResponse response = apiGet(orgWaiverPath(org));
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText())
        .isArray();
    assertThatJson(response.getBodyText())
        .inPath("$[*].policyWaiverId")
        .isArray()
        .contains(waiver.getId());
  }

  @Test
  public void testUpdateWaiverComment_success() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-waiver-update"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(app);
    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), app.getId(), "original comment");

    ApiWaiverOptionsDTO body = new ApiWaiverOptionsDTO();
    body.comment = "updated comment";
    body.matcherStrategy = EXACT_COMPONENT;

    assertResponseStatus(204, apiPutJson(waiverPath(app, waiver.getId()), body));

    HttpResponse getResponse = apiGet(waiverPath(app, waiver.getId()));
    assertResponseStatus(200, getResponse);
    assertThatJson(getResponse.getBodyText())
        .node("comment")
        .isEqualTo("updated comment");
  }

  @Test
  public void testUpdateWaiver_notFound() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-waiver-update-404"), Organization.ROOT_ORGANIZATION_ID);

    ApiWaiverOptionsDTO body = new ApiWaiverOptionsDTO();
    body.comment = "updated comment";
    body.matcherStrategy = EXACT_COMPONENT;

    HttpResponse response = apiPutJson(waiverPath(app, "nonexistent-waiver-id"), body);
    assertResponseStatus(404, response);
  }

  @Test
  public void testDeleteWaiver_success() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-waiver-delete"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(app);
    PolicyWaiver waiver = tempEntity.newWaiver(policy.getId(), app.getId());

    assertResponseStatus(204, apiDelete(waiverPath(app, waiver.getId())));
    assertResponseStatus(404, apiGet(waiverPath(app, waiver.getId())));
  }

  @Test
  public void testDeleteWaiver_notFound() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-waiver-delete-404"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiDelete(waiverPath(app, "nonexistent-waiver-id"));
    assertResponseStatus(404, response);
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testGetPolicyWaivers_unauthenticated_returns401() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-waiver-anon"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = anonApiGet(waiverPath(app));
    assertResponseStatus(401, response);
  }

  private PolicyViolation seedPolicyViolation(final Application app) {
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-waiver-scan"));
    return tempEntity.newPolicyViolation(evaluation, policy, "g1", "a1", "v1", "h1", "r1");
  }

  private static String waiverPath(final Application app) {
    return WAIVER_APP_BASE + app.getId();
  }

  private static String waiverPath(final Application app, final String suffixId) {
    return WAIVER_APP_BASE + app.getId() + "/" + suffixId;
  }

  private static String orgWaiverPath(final Organization org) {
    return WAIVER_ORG_BASE + org.getId();
  }
}

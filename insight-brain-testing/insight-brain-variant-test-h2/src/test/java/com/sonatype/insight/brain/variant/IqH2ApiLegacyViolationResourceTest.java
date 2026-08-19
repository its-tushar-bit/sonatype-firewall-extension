/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiLegacyViolationResource;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationChangeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end HTTP-to-DB integration tests for {@code ApiLegacyViolationResource}.
 * Verifies request flow through the resource into the service layer and database,
 * focusing on purl parsing, query-string filters, and DB state changes from grant/revoke.
 * Authorization is covered separately by {@code ApiLegacyViolationResourceAuthzTest}.
 */
@IqH2Test
class IqH2ApiLegacyViolationResourceTest
{
  private IqTestContext ctx;

  private PolicyViolationDAO policyViolationDAO;

  private Application app;

  @BeforeEach
  void setUp() {
    policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);
    app = ctx.tempEntity().newApplicationWithParent();
  }

  @Test
  void listLegacyViolations_returnsRowsFromDb() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pv = ctx.tempEntity().newLegacyPolicyViolation(eval, policy);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    List<ApiPolicyViolationDTOV2> result = response.getBodyList(ApiPolicyViolationDTOV2.class);
    assertThat(result).extracting(dto -> dto.policyViolationId).containsExactly(pv.getId());
  }

  @Test
  void listLegacyViolations_filtersByPolicyIdQuery() throws Exception {
    Policy policyA = ctx.tempEntity().newPolicy();
    Policy policyB = ctx.tempEntity().newPolicy();
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pvA = ctx.tempEntity().newLegacyPolicyViolation(eval, policyA);
    ctx.tempEntity().newLegacyPolicyViolation(eval, policyB);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .query("policyId", policyA.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    List<ApiPolicyViolationDTOV2> result = response.getBodyList(ApiPolicyViolationDTOV2.class);
    assertThat(result).extracting(dto -> dto.policyViolationId).containsExactly(pvA.getId());
  }

  @Test
  void listLegacyViolations_parsesComponentIdentifierFromPurl() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    ComponentIdentifier wantedCid = ComponentIdentifier.createMavenCoordinates("org.apache", "commons", "1.0");
    ComponentIdentifier otherCid = ComponentIdentifier.createMavenCoordinates("org.other", "lib", "2.0");
    PolicyViolation matching =
        ctx.tempEntity().newLegacyPolicyViolation(eval, policy, wantedCid, ctx.tempEntity().newRandomHash());
    ctx.tempEntity().newLegacyPolicyViolation(eval, policy, otherCid, ctx.tempEntity().newRandomHash());

    String purl = "pkg:maven/org.apache/commons@1.0";
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .query("componentIdentifier", purl)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    List<ApiPolicyViolationDTOV2> result = response.getBodyList(ApiPolicyViolationDTOV2.class);
    assertThat(result).extracting(dto -> dto.policyViolationId).containsExactly(matching.getId());
  }

  @Test
  void listLegacyViolations_emptyComponentIdentifierTreatedAsAbsent() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pv = ctx.tempEntity().newLegacyPolicyViolation(eval, policy);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .query("componentIdentifier", "")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    List<ApiPolicyViolationDTOV2> result = response.getBodyList(ApiPolicyViolationDTOV2.class);
    assertThat(result).extracting(dto -> dto.policyViolationId).containsExactly(pv.getId());
  }

  @Test
  void listLegacyViolations_invalidComponentIdentifierReturnsBadRequest() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .query("componentIdentifier", "not-a-valid-purl")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
  }

  @Test
  void listLegacyViolations_sortsByLegacyTimeDesc() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation older = ctx.tempEntity().newLegacyPolicyViolation(eval, policy);
    PolicyViolation newer = ctx.tempEntity().newLegacyPolicyViolation(eval, policy);
    older.setLegacyViolationTime(new Date(1_000L));
    newer.setLegacyViolationTime(new Date(2_000L));
    policyViolationDAO.update(older);
    policyViolationDAO.update(newer);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    List<ApiPolicyViolationDTOV2> result = response.getBodyList(ApiPolicyViolationDTOV2.class);
    assertThat(result).extracting(dto -> dto.policyViolationId)
        .containsExactly(newer.getId(), older.getId());
  }

  @Test
  void revoke_clearsLegacyTimeInDb() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pv = ctx.tempEntity().newLegacyPolicyViolation(eval, policy);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(app.getPublicId())
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiLegacyViolationChangeResponseDTO body = response.getBody(ApiLegacyViolationChangeResponseDTO.class);
    assertThat(body.changedPolicyViolationCount).isEqualTo(1);
    assertThat(policyViolationDAO.getById(pv.getId()).isLegacyViolation()).isFalse();
  }

  @Test
  void revoke_returnsZeroWhenNothingToRevoke() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(app.getPublicId())
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiLegacyViolationChangeResponseDTO body = response.getBody(ApiLegacyViolationChangeResponseDTO.class);
    assertThat(body.changedPolicyViolationCount).isEqualTo(0);
  }
}

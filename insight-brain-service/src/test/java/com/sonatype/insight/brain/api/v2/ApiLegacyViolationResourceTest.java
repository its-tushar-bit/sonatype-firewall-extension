/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationChangeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end HTTP→DB integration tests for {@link ApiLegacyViolationResource}.
 * Verifies request flow through the resource into the service layer and database,
 * focusing on behavior bhavat asked us to keep: purl parsing, query-string filters,
 * and DB state changes from grant/revoke. Authorization is covered separately by
 * {@link ApiLegacyViolationResourceAuthzTest}.
 */
public class ApiLegacyViolationResourceTest
    extends AbstractResourceTest
{
  private PolicyViolationDAO policyViolationDAO;

  private Application app;

  @Before
  public void setUp() {
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void listLegacyViolations_returnsRowsFromDb() throws Exception {
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pv = tempEntity.newLegacyPolicyViolation(eval, policy);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    List<ApiPolicyViolationDTOV2> result = response.getBodyList(ApiPolicyViolationDTOV2.class);
    assertThat(result).extracting(dto -> dto.policyViolationId).containsExactly(pv.getId());
  }

  @Test
  public void listLegacyViolations_filtersByPolicyIdQuery() throws Exception {
    Policy policyA = tempEntity.newPolicy();
    Policy policyB = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pvA = tempEntity.newLegacyPolicyViolation(eval, policyA);
    tempEntity.newLegacyPolicyViolation(eval, policyB);

    HttpResponse response = restRequest()
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
  public void listLegacyViolations_parsesComponentIdentifierFromPurl() throws Exception {
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    ComponentIdentifier wantedCid = ComponentIdentifier.createMavenCoordinates("org.apache", "commons", "1.0");
    ComponentIdentifier otherCid = ComponentIdentifier.createMavenCoordinates("org.other", "lib", "2.0");
    PolicyViolation matching =
        tempEntity.newLegacyPolicyViolation(eval, policy, wantedCid, tempEntity.newRandomHash());
    tempEntity.newLegacyPolicyViolation(eval, policy, otherCid, tempEntity.newRandomHash());

    String purl = "pkg:maven/org.apache/commons@1.0";
    HttpResponse response = restRequest()
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
  public void listLegacyViolations_emptyComponentIdentifierTreatedAsAbsent() throws Exception {
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pv = tempEntity.newLegacyPolicyViolation(eval, policy);

    HttpResponse response = restRequest()
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
  public void listLegacyViolations_invalidComponentIdentifierReturnsBadRequest() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .query("componentIdentifier", "not-a-valid-purl")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
  }

  @Test
  public void listLegacyViolations_sortsByLegacyTimeDesc() throws Exception {
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation older = tempEntity.newLegacyPolicyViolation(eval, policy);
    PolicyViolation newer = tempEntity.newLegacyPolicyViolation(eval, policy);
    older.setLegacyViolationTime(new Date(1_000L));
    newer.setLegacyViolationTime(new Date(2_000L));
    policyViolationDAO.update(older);
    policyViolationDAO.update(newer);

    HttpResponse response = restRequest()
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
  public void revoke_clearsLegacyTimeInDb() throws Exception {
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pv = tempEntity.newLegacyPolicyViolation(eval, policy);

    HttpResponse response = restRequest()
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
  public void revoke_returnsZeroWhenNothingToRevoke() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(app.getPublicId())
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiLegacyViolationChangeResponseDTO body = response.getBody(ApiLegacyViolationChangeResponseDTO.class);
    assertThat(body.changedPolicyViolationCount).isEqualTo(0);
  }
}

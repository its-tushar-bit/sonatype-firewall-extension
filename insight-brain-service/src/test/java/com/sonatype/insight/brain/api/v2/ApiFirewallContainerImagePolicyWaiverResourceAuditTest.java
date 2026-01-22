/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.v2.dto.containerimagewaiver.ApiContainerImageWaiverDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH;
import static com.sonatype.insight.brain.api.v2.ApiFirewallContainerImagePolicyWaiverResource.CONTAINER_IMAGE_ID;
import static com.sonatype.insight.brain.api.v2.ApiFirewallContainerImagePolicyWaiverResource.POLICY_WAIVER;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallContainerImagePolicyWaiverResourceAuditTest
    extends AbstractAuditTest
{
  private PolicyWaiverDAO policyWaiverDAO;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private Organization org;

  private Application app;

  private Policy policy;

  private PolicyEvaluation policyEvaluation;

  @Before
  public void setUpPolicyViolation() {
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);
    repositoryDAO = lookup(RepositoryDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);

    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy();
    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scan1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, 5, PolicyThreatCategory.SECURITY,
        "g", "a", "v", "hash", FailActionType.ID);
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    org.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(org);

    licenseManager.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
  }

  @Override
  public HttpRequest restRequest() {
    return super.restRequest().path(FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH + CONTAINER_IMAGE_ID + POLICY_WAIVER);
  }

  @Test
  public void testAddWaiver_Unauthorized() throws Exception {
    restRequest()
        .parameter(app.getId())
        .body(new ApiContainerImageWaiverDTO(), MediaType.APPLICATION_JSON)
        .with(unauthorizedUser())
        .post();

    assertAuditLog(AuditEvent.CREATE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER, "unauthorized");
  }

  @Test
  public void testAddWaiver() throws Exception {
    ApiContainerImageWaiverDTO waiverDTO = new ApiContainerImageWaiverDTO();
    waiverDTO.expiryTime = DateUtils.addDays(new Date(), 1);
    waiverDTO.comment = "Container image waiver comment";

    restRequest()
        .parameter(app.getId())
        .body(waiverDTO)
        .post();

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(2);

    assertAuditLog(AuditEvent.CREATE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER, null);
    List<AuditDTO> waiverAuditDTOs = assertAuditLogs(AuditEvent.CREATE_WAIVER, 2, null);

    policyWaivers = new ArrayList<>(policyWaivers);
    policyWaivers.sort(Comparator.comparing(PolicyWaiver::getHash, Comparator.nullsLast(Comparator.naturalOrder())));

    assertWaiverAuditDTO(waiverAuditDTOs.get(0), policyWaivers.get(0));
    assertWaiverAuditDTO(waiverAuditDTOs.get(1), policyWaivers.get(1));
  }

  @Test
  public void testDeleteWaiversToContainerImage_Unauthorized() throws Exception {
    restRequest()
        .parameter(app.getId())
        .with(unauthorizedUser())
        .delete();

    assertAuditLog(AuditEvent.DELETE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER, "unauthorized");
  }

  @Test
  public void testDeleteWaiversToContainerImage_AuditEvent() throws Exception {
    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scan1");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    ApiContainerImageWaiverDTO containerWaiversDTO = new ApiContainerImageWaiverDTO();
    containerWaiversDTO.comment = "Container image waiver comment";
    containerWaiversDTO.waiverReasonId = tempEntity.newWaiverReason("type", "reasons").getId();
    containerWaiversDTO.expiryTime = DateUtils.addDays(new Date(), 1);

    restRequest()
        .parameter(app.getId())
        .body(containerWaiversDTO)
        .post();

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(2);

    restRequest()
        .parameter(app.getId())
        .delete();

    AuditDTO resourceAudit = assertAuditLog(AuditEvent.DELETE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER, null);
    assertThat(resourceAudit.domain).isEqualTo("governance.waiver.container");

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.DELETE_WAIVER, 2, null);

    auditDTOs = auditDTOs.stream().filter(audit -> audit.data != null).toList();
    assertThat(
        auditDTOs.stream().filter(auditDTO -> auditDTO.data.get("isForContainerImageComponent").equals(Boolean.TRUE))
            .toList()).hasSize(1);
    assertThat(auditDTOs.stream().filter(auditDTO -> auditDTO.data.get("isForContainerImage").equals(Boolean.TRUE))
        .toList()).hasSize(1);
  }

  private void assertWaiverAuditDTO(AuditDTO waiverAuditDTO, PolicyWaiver policyWaiver) {
    assertCustomData(waiverAuditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(waiverAuditDTO, "policyId", policy.getId());
    assertCustomData(waiverAuditDTO, "policyName", policy.getName());
    assertCustomData(waiverAuditDTO, "componentHash", policyWaiver.getHash());
    assertCustomData(waiverAuditDTO, "expiryTime", policyWaiver.getExpiryTime().getTime());
    assertCustomData(waiverAuditDTO, "comment", policyWaiver.getComment());
    assertCustomData(waiverAuditDTO, "isForContainerImageComponent", policyWaiver.isForContainerImageComponent());
    assertCustomData(waiverAuditDTO, "isForContainerImage", policyWaiver.isForContainerImage());
    assertCustomObject(waiverAuditDTO, "policyConstraints",
        policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
  }
}

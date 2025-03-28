/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.google.inject.Binder;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ApiPolicyWaiverRequestServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  @Inject
  private PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  private ApiPolicyWaiverRequestService apiPolicyWaiverRequestService;

  private Policy policy;

  private PolicyEvaluation policyEvaluation;

  private PolicyViolation policyViolation;

  private PackageUrlIdentifier componentPurl;

  private Application app;

  private Organization org;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(PolicyWaiverTelemetryCreator.class).toInstance(policyWaiverTelemetryCreator);
  }

  @Before
  public void setUpPolicyViolation() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy(org.getId());

    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "java");
    policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, componentIdentifier, "h1", "r1");
    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_WithoutWaiverReason() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null, false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_WithWaiverReason() {
    String waiverReasonId = policyWaiverReasonDAO.getAll().get(0).getId();
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, waiverReasonId, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), waiverReasonId, false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_WithNoteToReviewer() {
    String waiverReasonId = policyWaiverReasonDAO.getAll().get(0).getId();
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO =
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, waiverReasonId, false);
    policyWaiverRequestOptionsDTO.noteToReviewer = "note to reviewer";
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
            policyViolation.getId(), policyWaiverRequestOptionsDTO);

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), waiverReasonId, false);
    assertThat(policyWaiverRequest.getNoteToReviewer()).isEqualTo("note to reviewer");
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_WithWrongWaiverReason() {
    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
          policyViolation.getId(),
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, "WrongId", false));
    }).isInstanceOf(BadRequestException.class).hasMessage("Policy waiver reason ID WrongId not found.");
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_WithExpireWhenRemediationAvailableAndAllComponents() {
    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
          policyViolation.getId(),
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_COMPONENTS, null, null, true));
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("Expire When Remediation Available Waivers can only be applied to Exact Components.");
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_ApplicationPublicId() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null, false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_NonParentApplicationId() {
    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, otherApp.getId(),
          policyViolation.getId(),
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID " + policyViolation.getId() + ".");
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_NonParentApplicationPublicId() {
    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION,
          otherApp.getPublicId(), policyViolation.getId(),
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID " + policyViolation.getId() + ".");
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Organization() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, org.getId(), "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null, false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getByOwnerId(org.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_NonParentOrganization() {
    Organization otherOrg = tempEntity.newOrganization();

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.ORGANIZATION, otherOrg.getId(),
          policyViolation.getId(),
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID " + policyViolation.getId() + ".");
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_NullComment() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO(null /* comment */, EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), null /* comment */, "testuser", "Test User",
        policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null, false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_ApplyToAllComponents() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO(null /* comment */, ALL_COMPONENTS, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), null /* comment */, "testuser", "Test User",
        null /* hash */, policyViolation.getConstraintFactsJson(), null, ALL_COMPONENTS,
        null /* packageUrl */, null, false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_InvalidPolicyViolationId() {
    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
          "invalid-policyViolationId", new ApiPolicyWaiverRequestOptionsDTO(null, EXACT_COMPONENT, null, null, false));
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID invalid-policyViolationId.");
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_WithExpiry() {
    Date expiryTime = DateUtils.addDays(new Date(), 1);

    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, expiryTime, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), policyViolation.getConstraintFactsJson(), expiryTime, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null, false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_DuplicatedAllVersions() {
    apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(),
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_VERSIONS, null, null, false));

    PolicyViolation policyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v2", "c1", "java", "h1", "r1");
    PolicyViolation policyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation, policy, "g2", "a2", "v1", "c1", "java", "h1", "r1");

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
          policyViolation1.getId(),
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_VERSIONS, null, null, false));
    }).isInstanceOf(BadRequestException.class).hasMessage("This policy waiver request already exists.");

    apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation2.getId(),
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_VERSIONS, null, null, false));

    List<PolicyWaiverRequest> policyWaiversRequest = policyWaiverRequestDAO.getByOwnerId(app.getId());
    assertThat(policyWaiversRequest).isNotEmpty().hasSize(2);
    assertThat(policyWaiversRequest.get(0).getAssociatedPackageUrl())
        .isNotEqualTo(policyWaiversRequest.get(1).getAssociatedPackageUrl());
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_ExpirationInThePast() {
    Date yesterday = DateUtils.addDays(new Date(), -1);

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
          policyViolation.getId(),
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_VERSIONS, yesterday, null, false));
    }).isInstanceOf(BadRequestException.class).hasMessage("Expiration date must be in the future.");
  }

  private void assertPolicyWaiverRequest(
      PolicyWaiverRequest policyWaiverRequest,
      String ownerId,
      String comment,
      String creatorId,
      String creatorName,
      String hash,
      String constraintFactsJson,
      Date expiryTime,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      String packageUrl,
      String waiverReasonId,
      boolean expireWhenRemediationAvailable)
  {
    assertThat(policyWaiverRequest.getId()).isNotNull();
    assertThat(policyWaiverRequest.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiverRequest.getHash()).isEqualTo(hash);
    assertThat(policyWaiverRequest.getComment()).isEqualTo(comment);
    assertThat(policyWaiverRequest.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiverRequest.getRequestTime()).isNotNull();
    assertThat(policyWaiverRequest.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiverRequest.getRequesterId()).isEqualTo(creatorId);
    assertThat(policyWaiverRequest.getRequesterName()).isEqualTo(creatorName);
    assertThat(policyWaiverRequest.getConstraintFactsJson()).isEqualTo(constraintFactsJson);
    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isEqualTo(matcherStrategy);
    assertThat(policyWaiverRequest.getAssociatedPackageUrl()).isEqualTo(packageUrl);
    assertThat(policyWaiverRequest.getWaiverReasonId()).isEqualTo(waiverReasonId);
    assertThat(policyWaiverRequest.isExpireWhenRemediationAvailable()).isEqualTo(expireWhenRemediationAvailable);
  }

  private void assertPolicyWaiverRequestDTO(
      ApiPolicyWaiverRequestDTO policyWaiverRequestDTO,
      PolicyWaiverRequest policyWaiverRequest)
  {
    assertThat(policyWaiverRequestDTO.policyWaiverRequestId).isEqualTo(policyWaiverRequest.getId());
    assertThat(policyWaiverRequestDTO.scopeOwnerId).isEqualTo(policyWaiverRequest.getOwnerId());
    assertThat(policyWaiverRequestDTO.hash).isEqualTo(policyWaiverRequest.getHash());
    assertThat(policyWaiverRequestDTO.comment).isEqualTo(policyWaiverRequest.getComment());
    assertThat(policyWaiverRequestDTO.policyId).isEqualTo(policyWaiverRequest.getPolicyId());
    assertThat(policyWaiverRequestDTO.requestTime).isEqualTo(policyWaiverRequest.getRequestTime());
    assertThat(policyWaiverRequestDTO.expiryTime).isEqualTo(policyWaiverRequest.getExpiryTime());
    assertThat(policyWaiverRequestDTO.requesterId).isEqualTo(policyWaiverRequest.getRequesterId());
    assertThat(policyWaiverRequestDTO.requesterName).isEqualTo(policyWaiverRequest.getRequesterName());
    assertThat(policyWaiverRequestDTO.constraintFactsJson).isEqualTo(policyWaiverRequest.getConstraintFactsJson());
    assertThat(policyWaiverRequestDTO.matcherStrategy).isEqualTo(policyWaiverRequest.getComponentMatchStrategy());
    assertThat(policyWaiverRequestDTO.associatedPackageUrl).isEqualTo(policyWaiverRequest.getAssociatedPackageUrl());
    assertThat(policyWaiverRequestDTO.policyWaiverReasonId).isEqualTo(policyWaiverRequest.getWaiverReasonId());
    assertThat(policyWaiverRequestDTO.expireWhenRemediationAvailable)
        .isEqualTo(policyWaiverRequest.isExpireWhenRemediationAvailable());
  }

  private void assertPolicyWaiverRequestTelemetry(
      PolicyViolation policyViolation,
      PolicyWaiverRequest policyWaiverRequest)
  {
    ArgumentCaptor<TelemetryData> telemetryDataCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(1)).send(telemetryDataCaptor.capture());
    TelemetryData actualTelemetryData = telemetryDataCaptor.getValue();
    PolicyWaiverReason policyWaiverReason = policyWaiverReasonDAO.getById(policyWaiverRequest.getWaiverReasonId());
    String reasonText = policyWaiverReason != null ? policyWaiverReason.getReasonText() : null;
    Map<String, Object> attributes = actualTelemetryData.getAttributes();
    assertThat(attributes).containsKey("application_id")
        .doesNotContainEntry("application_id", policyViolation.getApplicationId())
        .containsEntry("real_application_id", policyViolation.getApplicationId()).containsEntry("count", 1)
        .containsEntry("open_time", policyViolation.getOpenTime().getTime())
        .containsEntry("policy_name", policyViolation.getPolicyName())
        .containsEntry("policy_violation_id", policyViolation.getId())
        .containsEntry("stage_id", policyViolation.getStageTypeId())
        .containsEntry("threat_category", policyViolation.getThreatCategory().getName())
        .containsEntry("threat_level", policyViolation.getThreatLevel())
        .containsEntry("waiver_reason", reasonText);
  }

  private void assertPolicyWaiverRequestTelemetry(
      OwnerType ownerType,
      AbstractPolicyViolation abstractPolicyViolation,
      PolicyWaiverRequest policyWaiverRequest,
      int index,
      int invocations)
  {
    final ArgumentCaptor<PolicyWaiver> policyWaiverArgumentCaptor = ArgumentCaptor.forClass(PolicyWaiver.class);
    final ArgumentCaptor<OwnerType> ownerTypeArgumentCaptor = ArgumentCaptor.forClass(OwnerType.class);
    final ArgumentCaptor<AbstractPolicyViolation> policyViolationArgumentCaptor =
        ArgumentCaptor.forClass(AbstractPolicyViolation.class);
    verify(policyWaiverTelemetryCreator, times(invocations))
        .sendWaiverTelemetryForOwnerType(policyWaiverArgumentCaptor.capture(), ownerTypeArgumentCaptor.capture(),
            policyViolationArgumentCaptor.capture());

    final PolicyWaiver policyWaiverValue = policyWaiverArgumentCaptor.getAllValues().get(index);
    final OwnerType ownerTypeValue = ownerTypeArgumentCaptor.getAllValues().get(index);
    final AbstractPolicyViolation policyViolationValue = policyViolationArgumentCaptor.getAllValues().get(index);
    assertThat(ownerTypeValue).isNotNull().isEqualTo(ownerType);
    assertThat(policyViolationValue).isNotNull();
    assertThat(policyViolationValue.getId()).isEqualTo(abstractPolicyViolation.getId());
    assertThat(policyWaiverValue).isNotNull();
    assertThat(policyWaiverValue.getId()).isEqualTo(policyWaiverRequest.getId());
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Repository() {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.REPOSITORY,
            repository.getId(), repositoryPolicyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(repositoryPolicyViolation.getComponentIdentifier());
    assertPolicyWaiverRequest(policyWaiverRequest, repository.getId(), "waiver comment", "testuser", "Test User",
        repositoryPolicyViolation.getHash(), repositoryPolicyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null, false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryManager() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId(), repositoryPolicyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(repositoryPolicyViolation.getComponentIdentifier());
    assertPolicyWaiverRequest(policyWaiverRequest, repositoryManager.getId(), "waiver comment", "testuser", "Test User",
        repositoryPolicyViolation.getHash(), repositoryPolicyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null, false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryContainer() {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, repositoryPolicyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(repositoryPolicyViolation.getComponentIdentifier());
    assertPolicyWaiverRequest(policyWaiverRequest, RepositoryContainer.REPOSITORY_CONTAINER_ID, "waiver comment",
        "testuser", "Test User", repositoryPolicyViolation.getHash(),
        repositoryPolicyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        false);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
  }
}

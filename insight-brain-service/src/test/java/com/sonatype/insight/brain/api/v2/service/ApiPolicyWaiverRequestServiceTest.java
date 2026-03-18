/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestsApplicableToViolationDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
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
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.brain.webhook.WaiverRequestEvent;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.commons.lang.time.DateUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverRequestDTOTestUtils.assertPolicyWaiverRequestDTO;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus.APPROVED;
import static com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus.REJECTED;
import static com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus.REQUESTED;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class ApiPolicyWaiverRequestServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

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

  @Inject
  private AsyncEventBus asyncEventBus;

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
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, componentIdentifier, "h1", "r1");
    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);

    // Posting a RequestWaiverReviewEvent requires a base URL to be set
    // For this test there is no running IQ instance so we need to set a dummy URL
    setBaseUrl("http://localhost:1234");
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_WithoutWaiverReason() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getById(policyWaiverRequest.getId()));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_WithWaiverReason() {
    String waiverReasonId = policyWaiverReasonDAO.getAll().get(0).getId();
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, waiverReasonId, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), waiverReasonId, false, REQUESTED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getById(policyWaiverRequest.getId()));
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
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), waiverReasonId, false, REQUESTED);
    assertThat(policyWaiverRequest.getNoteToReviewer()).isEqualTo("note to reviewer");
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getById(policyWaiverRequest.getId()));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_WithInvalidWaiverReason() {
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
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getById(policyWaiverRequest.getId()));
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
    assertPolicyWaiverRequest(policyWaiverRequest, org.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getById(policyWaiverRequest.getId()));
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
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), null /* comment */, "testuser",
        "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getById(policyWaiverRequest.getId()));
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_ApplyToAllComponents() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO(null /* comment */, ALL_COMPONENTS, null, null, false));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), null /* comment */, "testuser",
        "Test User", null, null, null /* hash */, policyViolation.getConstraintFactsJson(), null, ALL_COMPONENTS,
        null /* packageUrl */, null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getById(policyWaiverRequest.getId()));
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
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), expiryTime,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getById(policyWaiverRequest.getId()));
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

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_PolicyViolationShouldTriggerWebhookEvent() throws InterruptedException {
    // Set up event handler to capture events
    TestEventHandler<WaiverRequestEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), WaiverRequestEvent.class);
    asyncEventBus.register(handler);

    try {
      ApiPolicyWaiverRequestOptionsDTO optionsDTO = new ApiPolicyWaiverRequestOptionsDTO(
          "waiver comment", EXACT_COMPONENT, null, null, false);

      apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(
          OwnerType.APPLICATION, app.getId(), policyViolation.getId(), optionsDTO);

      // Verify event was sent and check its content
      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
      WaiverRequestEvent event = handler.getEvent();
      assertThat(event.initiator).isEqualTo(USERNAME);
      assertThat(event.timestamp).isNotNull();
      assertThat(event.comment).isEqualTo("waiver comment");
      assertThat(event.policyViolationId).isEqualTo(policyViolation.getId());
      assertThat(event.ownerId).isEqualTo(app.getId());
    }
    finally {
      asyncEventBus.unregister(handler);
    }
  }

  private void assertPolicyWaiverRequest(
      PolicyWaiverRequest policyWaiverRequest,
      String ownerId,
      String policyViolationId,
      String comment,
      String requesterId,
      String requesterName,
      String reviewerId,
      String reviewerName,
      String hash,
      String constraintFactsJson,
      Date expiryTime,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      String packageUrl,
      String waiverReasonId,
      boolean expireWhenRemediationAvailable,
      PolicyWaiverRequestStatus status)
  {
    assertThat(policyWaiverRequest.getId()).isNotNull();
    assertThat(policyWaiverRequest.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiverRequest.getPolicyViolationId()).isEqualTo(policyViolationId);
    assertThat(policyWaiverRequest.getHash()).isEqualTo(hash);
    assertThat(policyWaiverRequest.getComment()).isEqualTo(comment);
    assertThat(policyWaiverRequest.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiverRequest.getRequestTime()).isNotNull();
    assertThat(policyWaiverRequest.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiverRequest.getRequesterId()).isEqualTo(requesterId);
    assertThat(policyWaiverRequest.getRequesterName()).isEqualTo(requesterName);
    assertThat(policyWaiverRequest.getReviewerId()).isEqualTo(reviewerId);
    assertThat(policyWaiverRequest.getReviewerName()).isEqualTo(reviewerName);
    Date now = new Date();
    assertThat(policyWaiverRequest.getRequestTime()).isBetween(DateUtils.addSeconds(now, -5), now, true, true);
    if (APPROVED.equals(status) || REJECTED.equals(status)) {
      assertThat(policyWaiverRequest.getReviewTime()).isBetween(DateUtils.addSeconds(now, -5), now, true, true);
    }
    else {
      assertThat(policyWaiverRequest.getReviewTime()).isNull();
    }
    assertThat(policyWaiverRequest.getConstraintFactsJson()).isEqualTo(constraintFactsJson);
    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isEqualTo(matcherStrategy);
    assertThat(policyWaiverRequest.getAssociatedPackageUrl()).isEqualTo(packageUrl);
    assertThat(policyWaiverRequest.getWaiverReasonId()).isEqualTo(waiverReasonId);
    assertThat(policyWaiverRequest.isExpireWhenRemediationAvailable()).isEqualTo(expireWhenRemediationAvailable);
    assertThat(policyWaiverRequest.getStatus()).isEqualTo(status);
  }

  private void assertPolicyWaiverTelemetry(
      OwnerType ownerType,
      String ownerId,
      String policyWaiverId,
      String policyViolationId)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(1)).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("owner_type", ownerType.toString());

    expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));
    expectedAttributes.put("real_owner_id", ownerId);

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.POLICY_WAIVER_API);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
    final ArgumentCaptor<PolicyWaiver> policyWaiverArgumentCaptor = ArgumentCaptor.forClass(PolicyWaiver.class);
    final ArgumentCaptor<OwnerType> ownerTypeArgumentCaptor = ArgumentCaptor.forClass(OwnerType.class);
    final ArgumentCaptor<AbstractPolicyViolation> policyViolationArgumentCaptor =
        ArgumentCaptor.forClass(AbstractPolicyViolation.class);

    verify(policyWaiverTelemetryCreator, times(1)).sendWaiverTelemetryForOwnerType(policyWaiverArgumentCaptor.capture(),
        ownerTypeArgumentCaptor.capture(), policyViolationArgumentCaptor.capture());

    PolicyWaiver policyWaiverValue = policyWaiverArgumentCaptor.getAllValues().get(0);
    OwnerType ownerTypeValue = ownerTypeArgumentCaptor.getAllValues().get(0);
    AbstractPolicyViolation policyViolationValue = policyViolationArgumentCaptor.getAllValues().get(0);
    assertThat(ownerTypeValue).isNotNull().isEqualTo(ownerType);
    assertThat(policyViolationValue).isNotNull();
    assertThat(policyViolationValue.getId()).isEqualTo(policyViolationId);
    assertThat(policyWaiverValue).isNotNull();
    assertThat(policyWaiverValue.getId()).isEqualTo(policyWaiverId);
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
        .containsEntry("real_application_id", policyViolation.getApplicationId())
        .containsEntry("count", 1)
        .containsEntry("open_time", policyViolation.getOpenTime().getTime())
        .containsEntry("policy_name", policyViolation.getPolicyName())
        .containsEntry("policy_violation_id", policyViolation.getId())
        .containsEntry("stage_id", policyViolation.getStageTypeId())
        .containsEntry("threat_category", policyViolation.getThreatCategory().getName())
        .containsEntry("threat_level", policyViolation.getThreatLevel())
        .containsEntry("waiver_reason", reasonText);
  }

  private void assertPolicyWaiver(
      String ownerId,
      Policy policy,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      String reviewerId,
      String reviewerName,
      String hash,
      Date expiryTime,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      String associatedPackageUrl,
      String policyWaiverReasonId,
      String policyWaiverId)
  {
    PolicyWaiver policyWaiver = policyWaiverDAO.getById(policyWaiverId);
    assertThat(policyWaiver.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiver.getHash()).isEqualTo(hash);
    assertThat(policyWaiver.getAssociatedPackageUrl()).isEqualTo(associatedPackageUrl);
    assertThat(policyWaiver.getComment()).isEqualTo(comment);
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiver.getCreatorId()).isEqualTo(reviewerId);
    assertThat(policyWaiver.getCreatorName()).isEqualTo(reviewerName);
    assertThat(policyWaiver.getCreateTime()).isNotNull();
    assertThat(policyWaiver.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiver.getComponentMatchStrategy()).isEqualTo(matcherStrategy);
    assertThat(policyWaiver.getConstraintFactsJson())
        .isEqualTo(abstractPolicyViolation.getConstraintFactsJson());
    assertThat(policyWaiver.getWaiverReasonId()).isEqualTo(policyWaiverReasonId);
    Date now = new Date();
    assertThat(policyWaiver.getCreateTime()).isBetween(DateUtils.addSeconds(now, -5), now, true, true);
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
    assertPolicyWaiverRequest(policyWaiverRequest, repository.getId(), repositoryPolicyViolation.getId(),
        "waiver comment", "testuser", "Test User", null, null, repositoryPolicyViolation.getHash(),
        repositoryPolicyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        false, REQUESTED);
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
    assertPolicyWaiverRequest(policyWaiverRequest, repositoryManager.getId(), repositoryPolicyViolation.getId(),
        "waiver comment", "testuser", "Test User", null, null, repositoryPolicyViolation.getHash(),
        repositoryPolicyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        false, REQUESTED);
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
    assertPolicyWaiverRequest(policyWaiverRequest, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        repositoryPolicyViolation.getId(), "waiver comment", "testuser", "Test User", null, null,
        repositoryPolicyViolation.getHash(), repositoryPolicyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
  }

  @Test
  public void testReviewPolicyWaiverRequest_ApplicationPublicId() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getPublicId(),
            policyWaiverRequestDTO.policyWaiverRequestId, new ApiPolicyWaiverRequestReviewDTO("waiver comment",
                EXACT_COMPONENT, null, null, false, PolicyWaiverRequestStatus.REJECTED.name()));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REJECTED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation, policyWaiverRequestDAO.getById(policyWaiverRequest.getId()));
  }

  @Test
  public void testReviewPolicyWaiverRequest_InvalidPolicyWaiverRequestId() {
    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          "invalid-policyWaiverRequestId",
          new ApiPolicyWaiverRequestReviewDTO(null, EXACT_COMPONENT, null, null, false, APPROVED.name()));
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy waiver request with ID invalid-policyWaiverRequestId.");
  }

  @Test
  public void testReviewPolicyWaiverRequest_NonParentApplicationId() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, otherApp.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestReviewDTO(null, EXACT_COMPONENT, null, null, false, APPROVED.name()));
    }).isInstanceOf(NotFoundException.class)
        .hasMessage(
            "Could not find policy violation with ID " + policyViolation.getId() + ".");
  }

  @Test
  public void testReviewPolicyWaiverRequest_NonParentOrganizationId() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    Organization otherOrg = tempEntity.newOrganization();

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.ORGANIZATION, otherOrg.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestReviewDTO(null, EXACT_COMPONENT, null, null, false, APPROVED.name()));
    }).isInstanceOf(NotFoundException.class)
        .hasMessage(
            "Could not find policy violation with ID " + policyViolation.getId() + ".");
  }

  @Test
  public void testReviewPolicyWaiverRequest_NoApiPolicyWaiverRequestReviewDTO() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId, null /* apiPolicyWaiverRequestReviewDTO */);
    }).isInstanceOf(BadRequestException.class).hasMessage("ApiPolicyWaiverRequestReviewDTO is required.");
  }

  @Test
  public void testReviewPolicyWaiverRequest_NoStatus() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestReviewDTO(null, EXACT_COMPONENT, null, null, false, null /* status */));
    }).isInstanceOf(BadRequestException.class).hasMessage("status is required.");
  }

  @Test
  public void testReviewPolicyWaiverRequest_InvalidStatus() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestReviewDTO(null, EXACT_COMPONENT, null, null, false, REQUESTED.name()));
    }).isInstanceOf(BadRequestException.class).hasMessage("status must be APPROVED or REJECTED.");
  }

  @Test
  public void testReviewPolicyWaiverRequest_Rejected_StatusApproved() {
    PolicyWaiverRequest policyWaiverRequest =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setOwnerId(app.getId())
            .setPolicyId(policy.getId())
            .setPolicyViolationId(policyViolation.getId())
            .setStatus(APPROVED));

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequest.getId(),
          new ApiPolicyWaiverRequestReviewDTO(null, EXACT_COMPONENT, null, null, false, REJECTED.name()));
    }).isInstanceOf(BadRequestException.class).hasMessage("Cannot reject an approved policy waiver request.");
  }

  @Test
  public void testReviewPolicyWaiverRequest_Rejected() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO =
        new ApiPolicyWaiverRequestReviewDTO(null, EXACT_COMPONENT, null, null, false, REJECTED.name());
    apiPolicyWaiverRequestReviewDTO.rejectionReason = "rejection reason";
    policyWaiverRequestDTO = apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REJECTED);
    assertThat(policyWaiverRequest.getRejectionReason()).isEqualTo("rejection reason");
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithoutWaiverReason() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO =
        new ApiPolicyWaiverRequestReviewDTO("waiver comment", EXACT_COMPONENT, null, null, false, APPROVED.name());
    policyWaiverRequestDTO = apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, APPROVED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        policyWaiverRequest.getPolicyWaiverId());
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithWaiverReason() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    String waiverReasonId = policyWaiverReasonDAO.getAll().get(0).getId();
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO = new ApiPolicyWaiverRequestReviewDTO(
        "waiver comment", EXACT_COMPONENT, null, waiverReasonId, false, APPROVED.name());
    policyWaiverRequestDTO = apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, APPROVED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), waiverReasonId,
        policyWaiverRequest.getPolicyWaiverId());
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithInvalidWaiverReason() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO = new ApiPolicyWaiverRequestReviewDTO(
        "waiver comment", EXACT_COMPONENT, null, "invalidWaiverReasonId", false, APPROVED.name());
    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);
    }).isInstanceOf(BadRequestException.class).hasMessage("Policy waiver reason ID invalidWaiverReasonId not found.");
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithExpiry() {
    Date expiryTime = DateUtils.addDays(new Date(), 1);

    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, expiryTime, null, false));

    // Approve the waiver request with a different expiry date
    Date updatedExpiryTime = DateUtils.addDays(new Date(), 2);
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO = new ApiPolicyWaiverRequestReviewDTO(
        "waiver comment", EXACT_COMPONENT, updatedExpiryTime, null, false, APPROVED.name());
    policyWaiverRequestDTO = apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", policyViolation.getHash(), policyViolation.getConstraintFactsJson(),
        expiryTime, EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, APPROVED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), updatedExpiryTime, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        policyWaiverRequest.getPolicyWaiverId());
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_ExpirationInThePast() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    // Approve the waiver request with expiry date in the past
    Date yesterday = DateUtils.addDays(new Date(), -1);
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO =
        new ApiPolicyWaiverRequestReviewDTO("waiver comment", EXACT_COMPONENT, yesterday, null, false, APPROVED.name());

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);
    }).isInstanceOf(BadRequestException.class).hasMessage("Expiration date must be in the future.");
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithExpireWhenRemediationAvailableAndAllComponents() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO =
        new ApiPolicyWaiverRequestReviewDTO("waiver comment", ALL_COMPONENTS, null, null, true, APPROVED.name());
    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("Expire When Remediation Available Waivers can only be applied to Exact Components.");
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithMatcherStrategy_ExactComponent() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_COMPONENTS, null, null, false));
    clearInvocations(telemetrySenderMock);

    policyWaiverRequestDTO = apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId, new ApiPolicyWaiverRequestReviewDTO("waiver comment",
            EXACT_COMPONENT, null, null, false, PolicyWaiverRequestStatus.APPROVED.name()));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", null, policyViolation.getConstraintFactsJson(), null, ALL_COMPONENTS,
        null, null, false, APPROVED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        policyWaiverRequest.getPolicyWaiverId());
    assertPolicyWaiverTelemetry(OwnerType.APPLICATION, app.getId(), policyWaiverRequest.getPolicyWaiverId(),
        policyViolation.getId());
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithMatcherStrategy_AllVersions() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    clearInvocations(telemetrySenderMock);

    policyWaiverRequestDTO = apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId, new ApiPolicyWaiverRequestReviewDTO("waiver comment",
            ALL_VERSIONS, null, null, false, PolicyWaiverRequestStatus.APPROVED.name()));

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, APPROVED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", "testuser", "Test User",
        null /* hash */, null, ALL_VERSIONS, componentPurl.getPackageUrl(), null,
        policyWaiverRequest.getPolicyWaiverId());
    assertPolicyWaiverTelemetry(OwnerType.APPLICATION, app.getId(), policyWaiverRequest.getPolicyWaiverId(),
        policyViolation.getId());
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithMatcherStrategy_AllComponents() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    // Approve the waiver request with a different matcher strategy
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO =
        new ApiPolicyWaiverRequestReviewDTO("waiver comment", ALL_COMPONENTS, null, null, false, APPROVED.name());
    policyWaiverRequestDTO = apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, APPROVED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", "testuser", "Test User",
        null /* hash */, null, ALL_COMPONENTS, null /* associatedPackageUrl */, null,
        policyWaiverRequest.getPolicyWaiverId());
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithScope() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_COMPONENTS, null, null, false));

    // Approve the waiver request with a different owner (aka scope)
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO = new ApiPolicyWaiverRequestReviewDTO(
        "waiver comment", EXACT_COMPONENT, null, null, false, APPROVED.name());
    policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.ORGANIZATION, org.getId(),
            policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", null, policyViolation.getConstraintFactsJson(),
        null, ALL_COMPONENTS, null, null, false, APPROVED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiver(org.getId(), policy, policyViolation, "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        policyWaiverRequest.getPolicyWaiverId());
  }

  @Test
  public void testReviewPolicyWaiverRequest_Approved_WithScopeOrgToApp() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_COMPONENTS, null, null, false));

    // Approve the waiver request with a different owner (aka scope)
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO =
        new ApiPolicyWaiverRequestReviewDTO("waiver comment", EXACT_COMPONENT, null, null, false, APPROVED.name());
    policyWaiverRequestDTO = apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId, apiPolicyWaiverRequestReviewDTO);

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(policyWaiverRequest, org.getId(), policyViolation.getId(), "waiver comment", "testuser",
        "Test User", "testuser", "Test User", null, policyViolation.getConstraintFactsJson(), null, ALL_COMPONENTS,
        null, null, false, APPROVED);
    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
    assertPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", "testuser", "Test User",
        policyViolation.getHash(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        policyWaiverRequest.getPolicyWaiverId());
  }

  @Test
  public void testGetPolicyWaiverRequest_ApplicationPublicId() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);

    policyWaiverRequestDTO = apiPolicyWaiverRequestService.getPolicyWaiverRequest(OwnerType.APPLICATION,
        app.getPublicId(), policyWaiverRequestDTO.policyWaiverRequestId);

    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
  }

  @Test
  public void testGetPolicyWaiverRequest() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);

    policyWaiverRequestDTO = apiPolicyWaiverRequestService.getPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId);

    assertPolicyWaiverRequestDTO(policyWaiverRequestDTO, policyWaiverRequest);
  }

  @Test
  public void testGetPolicyWaiverRequest_IncorrectParent() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);

    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.getPolicyWaiverRequest(OwnerType.APPLICATION, otherApp.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId);
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a policy waiver request with ID " + policyWaiverRequest.getId() + " for owner "
            + otherApp.getId() + ".");
  }

  @Test
  public void testGetApplicableWaiverRequests_NullId() {
    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.getApplicableWaiverRequests(null);
    }).isInstanceOf(NotFoundException.class).hasMessage("Could not find policy violation with ID null.");
  }

  @Test
  public void testGetApplicableWaiverRequests_InvalidId() {
    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.getApplicableWaiverRequests("InvalidPolicyViolationId");
    })
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID InvalidPolicyViolationId.");
  }

  @Test
  public void testGetApplicableWaiverRequests_NoWaivers() {
    ApiPolicyWaiverRequestsApplicableToViolationDTO results =
        apiPolicyWaiverRequestService.getApplicableWaiverRequests(policyViolation.getId());
    assertThat(results.activeWaiverRequests).isEmpty();
    assertThat(results.expiredWaiverRequests).isEmpty();
  }

  @Test
  public void testGetApplicableWaiverRequests_Application() {
    Date now = new Date();
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    Policy policyOrg = tempEntity.newPolicy(org);
    Policy policyApp = tempEntity.newPolicy(app);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    ComponentIdentifier identifierAllVersions1 =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "2.0", "c1", "jar");
    ComponentIdentifier identifierAllVersions2 =
        ComponentIdentifier.createMavenCoordinates("group", "otherArtifact", "3.0", "c1", "jar");
    String packageUrlAllVersions1 = PackageUrlIdentifier.toPackageUrl(identifierAllVersions1);
    String packageUrlAllVersions2 = PackageUrlIdentifier.toPackageUrl(identifierAllVersions2);
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policyOrg, identifier, "hash");
    policyViolation.setConstraintFacts(constraintFacts);
    policyViolationDAO.update(policyViolation);

    String policyIdOrg = policyOrg.getId();
    String policyIdApp = policyApp.getId();
    String orgId = org.getId();
    String appId = app.getId();

    Date expiredExpiryTime = DateUtils.addMilliseconds(now, -1);
    Date expiringInFutureExpiryTime = DateUtils.addMinutes(now, 1);

    PolicyWaiverRequest policyWaiverRequest1 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hashX")
            .setPolicyId(policyIdOrg)
            .setOwnerId(orgId)
            .setConstraintFacts(constraintFacts)
            .setAssociatedPackageUrl(packageUrlAllVersions1)
            .setComponentMatchStrategy(ALL_VERSIONS)
            .setRequestTime(DateUtils.addDays(now, -10)));
    PolicyWaiverRequest policyWaiverRequest2 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash(null)
        .setPolicyId(policyIdOrg)
        .setOwnerId(orgId)
        .setConstraintFacts(constraintFacts)
        .setComponentMatchStrategy(ALL_COMPONENTS)
        .setRequestTime(DateUtils.addDays(now, -9)));
    PolicyWaiverRequest policyWaiverRequest3 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash")
        .setPolicyId(policyIdOrg)
        .setOwnerId(appId)
        .setConstraintFacts(constraintFacts)
        .setAssociatedPackageUrl(packageUrlAllVersions1)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setExpiryTime(expiredExpiryTime)
        .setRequestTime(DateUtils.addDays(now, -8)));
    PolicyWaiverRequest policyWaiverRequest4 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash(null)
            .setPolicyId(policyIdOrg)
            .setOwnerId(appId)
            .setConstraintFacts(constraintFacts)
            .setComponentMatchStrategy(ALL_COMPONENTS)
            .setExpiryTime(expiringInFutureExpiryTime)
            .setRequestTime(DateUtils.addDays(now, -7)));
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash2")
        .setPolicyId(policyIdApp)
        .setOwnerId(appId)
        .setConstraintFacts(null)
        .setAssociatedPackageUrl(packageUrlAllVersions2)
        .setComponentMatchStrategy(ALL_VERSIONS)
        .setRequestTime(DateUtils.addDays(now, -2)));
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash(null)
        .setPolicyId(policyIdApp)
        .setOwnerId(appId)
        .setConstraintFacts(null)
        .setComponentMatchStrategy(ALL_COMPONENTS)
        .setExpiryTime(expiringInFutureExpiryTime)
        .setRequestTime(DateUtils.addDays(now, -1)));
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash")
        .setPolicyId(policyIdApp)
        .setOwnerId(appId)
        .setConstraintFacts(constraintFacts)
        .setAssociatedPackageUrl(packageUrlAllVersions1)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setExpiryTime(expiredExpiryTime)
        .setRequestTime(now));

    ApiPolicyWaiverRequestsApplicableToViolationDTO dto =
        apiPolicyWaiverRequestService.getApplicableWaiverRequests(policyViolation.getId());

    // activeWaiverRequests - results sorted to have deterministic ordering in the test
    List<ApiPolicyWaiverRequestDTO> activeApplicableWaiverRequests = dto.activeWaiverRequests.stream()
        .sorted(Comparator.comparing(apiPolicyWaiverRequestDTO -> apiPolicyWaiverRequestDTO.requestTime))
        .toList();

    assertThat(activeApplicableWaiverRequests).hasSize(3);
    assertPolicyWaiverRequestDTO(activeApplicableWaiverRequests.get(0), policyWaiverRequest1);
    assertPolicyWaiverRequestDTO(activeApplicableWaiverRequests.get(1), policyWaiverRequest2);
    assertPolicyWaiverRequestDTO(activeApplicableWaiverRequests.get(2), policyWaiverRequest4);

    // expiredWaiverRequests
    List<ApiPolicyWaiverRequestDTO> expiredApplicableWaiverRequests = dto.expiredWaiverRequests;

    assertThat(expiredApplicableWaiverRequests).hasSize(1);
    assertPolicyWaiverRequestDTO(expiredApplicableWaiverRequests.get(0), policyWaiverRequest3);
  }

  @Test
  public void testGetApplicableWaiverRequests_Repository() {
    Date now = new Date();
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Repository repository = tempEntity.newRepository();
    Policy policyRootOrg = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    Policy policyRepoContainer = tempEntity.newPolicy(REPOSITORY_CONTAINER_ID);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    ComponentIdentifier identifierAllVersions1 =
        ComponentIdentifier.createMavenCoordinates("group", "a" + "rtifact", "*", "c1", "jar");
    ComponentIdentifier identifierAllVersions2 =
        ComponentIdentifier.createMavenCoordinates("group", "otherArtifact", "*", "c1", "jar");
    String packageUrlAllVersions1 = PackageUrlIdentifier.toPackageUrl(identifierAllVersions1);
    String packageUrlAllVersions2 = PackageUrlIdentifier.toPackageUrl(identifierAllVersions2);
    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository, policyRootOrg, "testPathname", identifier, "hash");
    policyViolation.setConstraintFacts(constraintFacts);
    repositoryPolicyViolationDAO.update(policyViolation);

    String policyIdRootOrg = policyRootOrg.getId();
    String policyIdRepoContainer = policyRepoContainer.getId();
    String orgId = Organization.ROOT_ORGANIZATION_ID;
    String repoId = repository.getId();

    Date expiredExpiryTime = DateUtils.addMilliseconds(now, -1);
    Date expiringInFutureExpiryTime = DateUtils.addMinutes(now, 1);

    PolicyWaiverRequest policyWaiverRequest1 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hashX")
            .setPolicyId(policyIdRootOrg)
            .setOwnerId(orgId)
            .setConstraintFacts(constraintFacts)
            .setAssociatedPackageUrl(packageUrlAllVersions1)
            .setComponentMatchStrategy(ALL_VERSIONS)
            .setRequestTime(DateUtils.addDays(now, -10)));
    PolicyWaiverRequest policyWaiverRequest2 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash(null)
        .setPolicyId(policyIdRootOrg)
        .setOwnerId(orgId)
        .setConstraintFacts(constraintFacts)
        .setComponentMatchStrategy(ALL_COMPONENTS)
        .setRequestTime(DateUtils.addDays(now, -9)));
    PolicyWaiverRequest policyWaiverRequest3 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash")
        .setPolicyId(policyIdRootOrg)
        .setOwnerId(repoId)
        .setConstraintFacts(constraintFacts)
        .setAssociatedPackageUrl(packageUrlAllVersions1)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setExpiryTime(expiredExpiryTime)
        .setRequestTime(DateUtils.addDays(now, -8)));
    PolicyWaiverRequest policyWaiverRequest4 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash(null)
            .setPolicyId(policyIdRootOrg)
            .setOwnerId(repoId)
            .setConstraintFacts(constraintFacts)
            .setComponentMatchStrategy(ALL_COMPONENTS)
            .setExpiryTime(expiringInFutureExpiryTime)
            .setRequestTime(DateUtils.addDays(now, -7)));
    tempEntity
        .newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash2")
            .setPolicyId(policyIdRepoContainer)
            .setOwnerId(repoId)
            .setConstraintFacts(null)
            .setAssociatedPackageUrl(packageUrlAllVersions2)
            .setComponentMatchStrategy(ALL_VERSIONS)
            .setRequestTime(DateUtils.addDays(now, -2)));
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash(null)
        .setPolicyId(policyIdRepoContainer)
        .setOwnerId(repoId)
        .setConstraintFacts(null)
        .setComponentMatchStrategy(ALL_COMPONENTS)
        .setExpiryTime(expiringInFutureExpiryTime)
        .setRequestTime(DateUtils.addDays(now, -1)));
    tempEntity
        .newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash")
            .setPolicyId(policyIdRepoContainer)
            .setOwnerId(repoId)
            .setConstraintFacts(constraintFacts)
            .setAssociatedPackageUrl(packageUrlAllVersions1)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setExpiryTime(expiredExpiryTime)
            .setRequestTime(now));

    ApiPolicyWaiverRequestsApplicableToViolationDTO dto =
        apiPolicyWaiverRequestService.getApplicableWaiverRequests(policyViolation.getId());

    // activeWaiverRequests - results sorted to have deterministic ordering in the test
    List<ApiPolicyWaiverRequestDTO> activeApplicableWaiverRequests = dto.activeWaiverRequests.stream()
        .sorted(Comparator.comparing(apiPolicyWaiverRequestDTO -> apiPolicyWaiverRequestDTO.requestTime))
        .toList();

    assertThat(activeApplicableWaiverRequests).hasSize(3);
    assertPolicyWaiverRequestDTO(activeApplicableWaiverRequests.get(0), policyWaiverRequest1);
    assertPolicyWaiverRequestDTO(activeApplicableWaiverRequests.get(1), policyWaiverRequest2);
    assertPolicyWaiverRequestDTO(activeApplicableWaiverRequests.get(2), policyWaiverRequest4);

    // expiredWaiverRequests
    List<ApiPolicyWaiverRequestDTO> expiredApplicableWaiverRequests = dto.expiredWaiverRequests;

    assertThat(expiredApplicableWaiverRequests).hasSize(1);
    assertPolicyWaiverRequestDTO(expiredApplicableWaiverRequests.get(0), policyWaiverRequest3);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_ApplicationPublicId() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO = apiPolicyWaiverRequestService.updatePolicyWaiverRequest(
        OwnerType.APPLICATION, app.getPublicId(), policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment updated", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(),
        "waiver comment updated", "testuser", "Test User", null, null, policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false,
        REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Application() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO = apiPolicyWaiverRequestService.updatePolicyWaiverRequest(
        OwnerType.APPLICATION, app.getId(), policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment updated", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(),
        "waiver comment updated", "testuser", "Test User", null, null, policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false,
        REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_ApplyToAllComponents() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO = apiPolicyWaiverRequestService.updatePolicyWaiverRequest(
        OwnerType.APPLICATION, app.getId(), policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment updated", ALL_COMPONENTS, null, null, false));

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(),
        "waiver comment updated", "testuser", "Test User", null, null, null /* hash */,
        policyViolation.getConstraintFactsJson(), null, ALL_COMPONENTS, null /* purl */, null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_DuplicatedAllVersions() {
    apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(),
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_VERSIONS, null, null, false));
    PolicyViolation policyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v2", "c1", "java", "h1", "r1");
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation1.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_VERSIONS, null, null, false));
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("A policy waiver request for the same policy violation already exists.");
  }

  @Test
  public void testUpdatePolicyWaiverRequest_ExpirationInThePast() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    Date yesterday = DateUtils.addDays(new Date(), -1);

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, yesterday, null, false));
    }).isInstanceOf(BadRequestException.class).hasMessage("Expiration date must be in the future.");
  }

  @Test
  public void testUpdatePolicyWaiverRequest_NonParentApplication() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    Application otherApp = tempEntity.newApplicationWithParent();

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, otherApp.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID " + policyViolation.getId() + ".");
  }

  @Test
  public void testUpdatePolicyWaiverRequest_NonParentOrganization() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    Organization otherOrg = tempEntity.newOrganization();

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.ORGANIZATION, otherOrg.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID " + policyViolation.getId() + ".");
  }

  @Test
  public void testUpdatePolicyWaiverRequest_WithComment() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO(null /* comment */, EXACT_COMPONENT, null, null, false));

    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO = apiPolicyWaiverRequestService.updatePolicyWaiverRequest(
        OwnerType.APPLICATION, app.getId(), policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment updated", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(),
        "waiver comment updated", "testuser", "Test User", null, null, policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false,
        REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Organization() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO = apiPolicyWaiverRequestService.updatePolicyWaiverRequest(
        OwnerType.ORGANIZATION, org.getId(), policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, org.getId(), policyViolation.getId(), "waiver comment",
        "testuser", "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Repository() {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.REPOSITORY_MANAGER,
            repository.getRepositoryManagerId(), repositoryPolicyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO = apiPolicyWaiverRequestService.updatePolicyWaiverRequest(
        OwnerType.REPOSITORY, repository.getId(), policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(repositoryPolicyViolation.getComponentIdentifier());
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, repository.getId(), repositoryPolicyViolation.getId(),
        "waiver comment", "testuser", "Test User", null, null, repositoryPolicyViolation.getHash(),
        repositoryPolicyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_RepositoryContainer() {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.REPOSITORY,
            repository.getId(), repositoryPolicyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO = apiPolicyWaiverRequestService.updatePolicyWaiverRequest(
        OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(repositoryPolicyViolation.getComponentIdentifier());
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, REPOSITORY_CONTAINER_ID, repositoryPolicyViolation.getId(),
        "waiver comment", "testuser", "Test User", null, null, repositoryPolicyViolation.getHash(),
        repositoryPolicyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_RepositoryManager() {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.REPOSITORY,
            repository.getId(), repositoryPolicyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO = apiPolicyWaiverRequestService.updatePolicyWaiverRequest(
        OwnerType.REPOSITORY_MANAGER, repository.getRepositoryManagerId(), policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(repositoryPolicyViolation.getComponentIdentifier());
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, repository.getRepositoryManagerId(),
        repositoryPolicyViolation.getId(), "waiver comment", "testuser", "Test User", null, null,
        repositoryPolicyViolation.getHash(), repositoryPolicyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_WithExpireWhenRemediationAvailableAndAllComponents() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", ALL_COMPONENTS, null, null, true));
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("Expire When Remediation Available Waivers can only be applied to Exact Components.");
  }

  @Test
  public void testUpdatePolicyWaiverRequest_WithExpiry() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    reset(telemetrySenderMock);
    Date expiryTime = DateUtils.addDays(new Date(), 1);

    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO = apiPolicyWaiverRequestService.updatePolicyWaiverRequest(
        OwnerType.APPLICATION, app.getId(), policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment updated", EXACT_COMPONENT, expiryTime, null, false));

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(),
        "waiver comment updated", "testuser", "Test User", null, null, policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), expiryTime, EXACT_COMPONENT, componentPurl.getPackageUrl(), null,
        false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_WithInvalidWaiverReason() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, "WrongId", false));
    }).isInstanceOf(BadRequestException.class).hasMessage("Policy waiver reason ID WrongId not found.");
  }

  @Test
  public void testUpdatePolicyWaiverRequest_WithNoteToReviewer() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO =
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false);
    policyWaiverRequestOptionsDTO.noteToReviewer = "note to reviewer";
    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO =
        apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
            policyWaiverRequestDTO.policyWaiverRequestId, policyWaiverRequestOptionsDTO);

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(),
        "waiver comment", "testuser", "Test User", null, null, policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), null, EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false,
        REQUESTED);
    assertThat(updatedPolicyWaiverRequest.getNoteToReviewer()).isEqualTo("note to reviewer");
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_WithoutWaiverReason() {
    String waiverReasonId = policyWaiverReasonDAO.getAll().get(0).getId();
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, waiverReasonId, false));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO =
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null /* waiverReasonId */, false);
    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO =
        apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
            policyWaiverRequestDTO.policyWaiverRequestId, policyWaiverRequestOptionsDTO);

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment",
        "testuser", "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_WithWaiverReason() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null /* waiverReasonId */,
                false));
    reset(telemetrySenderMock);

    String waiverReasonId = policyWaiverReasonDAO.getAll().get(0).getId();
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO =
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, waiverReasonId, false);
    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO =
        apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
            policyWaiverRequestDTO.policyWaiverRequestId, policyWaiverRequestOptionsDTO);

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment",
        "testuser", "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), waiverReasonId, false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Rejected() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestReviewDTO(null, null, null, null, false, REJECTED.toString()));
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO =
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false);
    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO =
        apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
            policyWaiverRequestDTO.policyWaiverRequestId, policyWaiverRequestOptionsDTO);

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment",
        "testuser", "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Approved() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO =
        apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
            policyViolation.getId(), new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null,
                null, false));
    apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
        policyWaiverRequestDTO.policyWaiverRequestId,
        new ApiPolicyWaiverRequestReviewDTO("waiver comment", EXACT_COMPONENT, null, null, false, APPROVED.toString()));

    assertThatThrownBy(() -> {
      apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
          policyWaiverRequestDTO.policyWaiverRequestId,
          new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    }).isInstanceOf(BadRequestException.class).hasMessage("Cannot update an approved policy waiver request.");
  }

  @Test
  public void testUpdatePolicyWaiverRequest_DifferentRequester() {
    ApiPolicyWaiverRequestDTO policyWaiverRequestDTO = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(), policyViolation.getId(),
            new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getById(policyWaiverRequestDTO.policyWaiverRequestId);
    policyWaiverRequest.setRequesterId("otherUser");
    policyWaiverRequest.setRequesterName("Other User");
    policyWaiverRequestDAO.update(policyWaiverRequest);
    reset(telemetrySenderMock);

    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO =
        new ApiPolicyWaiverRequestOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false);
    ApiPolicyWaiverRequestDTO updatedPolicyWaiverRequestDTO =
        apiPolicyWaiverRequestService.updatePolicyWaiverRequest(OwnerType.APPLICATION, app.getId(),
            policyWaiverRequestDTO.policyWaiverRequestId, policyWaiverRequestOptionsDTO);

    PolicyWaiverRequest updatedPolicyWaiverRequest =
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequestDTO.policyWaiverRequestId);
    assertPolicyWaiverRequest(updatedPolicyWaiverRequest, app.getId(), policyViolation.getId(), "waiver comment",
        "testuser", "Test User", null, null, policyViolation.getHash(), policyViolation.getConstraintFactsJson(), null,
        EXACT_COMPONENT, componentPurl.getPackageUrl(), null, false, REQUESTED);
    assertPolicyWaiverRequestDTO(updatedPolicyWaiverRequestDTO, updatedPolicyWaiverRequest);
    assertPolicyWaiverRequestTelemetry(policyViolation,
        policyWaiverRequestDAO.getById(updatedPolicyWaiverRequest.getId()));
  }

  @Test
  public void testAddPolicyWaiverRequest_DisableWorkflowFeatureDisabled() {
    // When the config is disabled (default behavior), waiver requests should work with READ permission
    ApiPolicyWaiverRequestOptionsDTO requestOptions =
        new ApiPolicyWaiverRequestOptionsDTO("test comment", EXACT_COMPONENT, null, null, false);

    // This should succeed as it uses READ permission when feature is disabled
    ApiPolicyWaiverRequestDTO result = apiPolicyWaiverRequestService
        .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
            policyViolation.getId(), requestOptions);

    assertThat(result).isNotNull();
    assertThat(result.comment).isEqualTo("test comment");
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiverRequest_DisableWorkflowFeatureEnabled() {
    try {
      SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.setEnabled(false);

      ApiPolicyWaiverRequestOptionsDTO requestOptions =
          new ApiPolicyWaiverRequestOptionsDTO("test comment", EXACT_COMPONENT, null, null, false);

      apiPolicyWaiverRequestService
          .addPolicyWaiverRequestByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
              policyViolation.getId(), requestOptions);
    }
    finally {
      // Clean up - disable the feature flag
      SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.setEnabled(true);
    }
  }
}

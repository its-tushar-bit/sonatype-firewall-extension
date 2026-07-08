/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.containerimagewaiver.ApiContainerImageWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestsApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyWaiverRequestMatcherWrapper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetryBuilder;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;
import com.sonatype.insight.brain.webhook.RequestPolicyWaiverEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.purl.PackageUrlIdentifier.toPackageUrl;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.POLICY_WAIVER_REQUEST;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

@Named
public class ApiPolicyWaiverRequestService
{
  private static final Logger log = LoggerFactory.getLogger(ApiPolicyWaiverRequestService.class);

  private static final String WAIVER_REASON = "waiver_reason";

  private final ApiPolicyWaiverService apiPolicyWaiverService;

  private final TelemetrySender telemetrySender;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private final PolicyDAO policyDAO;

  private final OwnerDAO ownerDAO;

  private final CurrentUser currentUser;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  private final IdUtils idUtils;

  private final TelemetryUtils telemetryUtils;

  private final RequestPolicyWaiverEventService requestPolicyWaiverEventService;

  private final LicenseNameProvider licenseNameProvider;

  private final RepositoryService repositoryService;

  private final RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  public ApiPolicyWaiverRequestService(
      ApiPolicyWaiverService apiPolicyWaiverService,
      TelemetrySender telemetrySender,
      PolicyWaiverDAO policyWaiverDAO,
      PolicyWaiverRequestDAO policyWaiverRequestDAO,
      PolicyDAO policyDAO,
      OwnerDAO ownerDAO,
      CurrentUser currentUser,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      PolicyViolationDAO policyViolationDAO,
      PolicyWaiverReasonDAO policyWaiverReasonDAO,
      IdUtils idUtils,
      TelemetryUtils telemetryUtils,
      RequestPolicyWaiverEventService requestPolicyWaiverEventService,
      LicenseNameProvider licenseNameProvider,
      RepositoryService repositoryService,
      RepositoryManagerDAO repositoryManagerDAO)
  {
    this.apiPolicyWaiverService = apiPolicyWaiverService;
    this.telemetrySender = telemetrySender;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyWaiverRequestDAO = policyWaiverRequestDAO;
    this.policyDAO = policyDAO;
    this.ownerDAO = ownerDAO;
    this.currentUser = currentUser;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
    this.idUtils = idUtils;
    this.telemetryUtils = telemetryUtils;
    this.requestPolicyWaiverEventService = requestPolicyWaiverEventService;
    this.licenseNameProvider = licenseNameProvider;
    this.repositoryService = repositoryService;
    this.repositoryManagerDAO = repositoryManagerDAO;
  }

  /**
   * @param policyViolationId The id of an application or repository policy violation
   */
  public ApiPolicyWaiverRequestDTO addPolicyWaiverRequestByPolicyViolationId(
      OwnerType ownerType,
      String ownerId,
      String policyViolationId,
      ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO)
  {
    log.debug("Received request to add policy waiver request for ownerType {}, ownerId {}, policy violation ID {}",
        ownerType, ownerId, policyViolationId);

    if (!SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.isEnabled()) {
      throw new UnauthorizedException("Waiver requests are disabled by system property "
          + SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.getPropertyName());
    }

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    // Check permission before any other validation, to avoid giving away extra information to an unauthorized user.
    checkReadPermission(ownerType, internalOwnerId);

    // Throws NotFoundException if the policy violation does not exist.
    AbstractPolicyViolation abstractPolicyViolation = getAbstractPolicyViolation(policyViolationId);

    if (!isViolationOwnerId(abstractPolicyViolation, internalOwnerId)) {
      throw new NotFoundException("Could not find policy violation with ID " + policyViolationId + ".");
    }

    ComponentMatcherStrategyForWaiver matcherStrategy;
    if (policyWaiverRequestOptionsDTO != null) {
      validatePolicyWaiverReasonId(policyWaiverRequestOptionsDTO.waiverReasonId);

      if (policyWaiverRequestOptionsDTO.matcherStrategy != null) {
        matcherStrategy = policyWaiverRequestOptionsDTO.matcherStrategy;
      }
      else {
        matcherStrategy = EXACT_COMPONENT;
      }
    }
    else {
      matcherStrategy = EXACT_COMPONENT;
    }

    String comment = policyWaiverRequestOptionsDTO == null ? null : policyWaiverRequestOptionsDTO.comment;
    String noteToReviewer = policyWaiverRequestOptionsDTO == null ? null : policyWaiverRequestOptionsDTO.noteToReviewer;
    Date expiryTime = policyWaiverRequestOptionsDTO == null ? null : policyWaiverRequestOptionsDTO.expiryTime;
    String waiverReasonId = policyWaiverRequestOptionsDTO == null ? null : policyWaiverRequestOptionsDTO.waiverReasonId;
    boolean expireWhenRemediationAvailable =
        policyWaiverRequestOptionsDTO != null && policyWaiverRequestOptionsDTO.expireWhenRemediationAvailable;

    validateExpiryTime(expiryTime);

    validateExpireWhenRemediationAvailable(expireWhenRemediationAvailable, matcherStrategy);

    PolicyWaiverRequest policyWaiverRequest = createPolicyWaiverRequest(internalOwnerId, abstractPolicyViolation,
        comment, noteToReviewer, matcherStrategy, expiryTime, waiverReasonId, expireWhenRemediationAvailable);

    if (abstractPolicyViolation instanceof PolicyViolation) {
      requestPolicyWaiverEventService.postPolicyWaiverRequestEvent(policyViolationId, comment,
          waiverReasonId, ownerType.toString(), ownerId, policyWaiverRequest.getId());
    }
    else {
      requestPolicyWaiverEventService.postRepositoryWaiverRequestEvent(policyViolationId, comment,
          waiverReasonId, ownerType.toString(), ownerId, policyWaiverRequest.getId());
    }

    auditPolicyWaiverRequest(policyWaiverRequest);

    PolicyWaiverReason policyWaiverReason =
        waiverReasonId != null ? policyWaiverReasonDAO.getById(waiverReasonId) : null;
    sendTelemetryForPolicyWaiverRequest(abstractPolicyViolation, policyWaiverReason);

    return toDto(policyWaiverRequest, policyWaiverReason);
  }

  public void addContainerImagePolicyWaiverRequest(
      String containerImageId,
      ApiContainerImageWaiverRequestDTO requestDTO)
  {
    if (!SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.isEnabled()) {
      throw new UnauthorizedException("Waiver requests are disabled by system property "
          + SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.getPropertyName());
    }

    String internalApplicationOwnerId = idUtils.getInternalOwnerId(OwnerType.APPLICATION, containerImageId);

    List<PolicyViolation> policyViolations =
        policyViolationDAO.getActiveByApplicationIdAndStageIdAndActionId(internalApplicationOwnerId, Stage.ID_PROXY,
            Action.ID_FAIL);

    if (policyViolations.isEmpty()) {
      throw new NotFoundException(
          "No applicable policy violations found to request a waiver for container image with the given ID");
    }

    policyViolationDAO.loadConstraintFacts(policyViolations);
    PolicyViolation anchorViolation = policyViolations.get(0);

    Date expiryTime = requestDTO != null ? requestDTO.expiryTime : null;
    String comment = requestDTO != null ? requestDTO.comment : null;
    String noteToReviewer = requestDTO != null ? requestDTO.noteToReviewer : null;
    String waiverReasonId = requestDTO != null ? requestDTO.waiverReasonId : null;

    validateExpiryTime(expiryTime);
    validatePolicyWaiverReasonId(waiverReasonId);

    // Scope the waiver request to REPOSITORY_CONTAINER_ID so it appears in the
    // Firewall waiver requests tab, which queries by repository/repository_manager/REPOSITORY_CONTAINER_ID.
    createPolicyWaiverRequest(RepositoryContainer.REPOSITORY_CONTAINER_ID, anchorViolation, comment, noteToReviewer,
        ALL_COMPONENTS, expiryTime, waiverReasonId, false);
  }

  private ApiPolicyWaiverRequestDTO toDto(
      PolicyWaiverRequest policyWaiverRequest,
      PolicyWaiverReason policyWaiverReason)
  {
    Owner owner = ownerDAO.getById(policyWaiverRequest.getOwnerId());

    ApiPolicyWaiverRequestDTO dto = new ApiPolicyWaiverRequestDTO();

    dto.policyWaiverRequestId = policyWaiverRequest.getId();
    dto.comment = policyWaiverRequest.getComment();
    dto.noteToReviewer = policyWaiverRequest.getNoteToReviewer();
    dto.requestTime = policyWaiverRequest.getRequestTime();
    dto.expiryTime = policyWaiverRequest.getExpiryTime();
    dto.hash = policyWaiverRequest.getHash();
    dto.policyId = policyWaiverRequest.getPolicyId();
    Policy policy = policyDAO.getById(policyWaiverRequest.getPolicyId());
    if (policy != null) {
      dto.policyName = policy.getName();
      dto.threatLevel = policy.getThreatLevel();
    }
    dto.requesterId = policyWaiverRequest.getRequesterId();
    dto.requesterName = policyWaiverRequest.getRequesterName();
    dto.reviewerName = policyWaiverRequest.getReviewerName();
    dto.reviewerId = policyWaiverRequest.getReviewerId();
    dto.rejectionReason = policyWaiverRequest.getRejectionReason();
    dto.componentUpgradeAvailable = policyWaiverRequest.isComponentUpgradeAvailable();
    dto.expireWhenRemediationAvailable = policyWaiverRequest.isExpireWhenRemediationAvailable();
    dto.policyViolationId = policyWaiverRequest.getPolicyViolationId();

    if (policyWaiverRequest.getComponentIdentifier() != null) {
      dto.componentIdentifier =
          ApiComponentIdentifierDTOV2.fromComponentIdentifier(policyWaiverRequest.getComponentIdentifier());
    }
    else if (policyWaiverRequest.getPolicyViolationId() != null) {
      // Fallback: try to get component identifier from the violation itself
      try {
        AbstractPolicyViolation violation = getAbstractPolicyViolation(policyWaiverRequest.getPolicyViolationId());
        if (violation.getComponentIdentifier() != null) {
          dto.componentIdentifier =
              ApiComponentIdentifierDTOV2.fromComponentIdentifier(violation.getComponentIdentifier());
        }
      }
      catch (NotFoundException e) {
        log.debug("Policy violation {} no longer exists for waiver request {}; componentIdentifier will be null",
            policyWaiverRequest.getPolicyViolationId(), policyWaiverRequest.getId());
      }
    }

    if (owner != null) {
      dto.scopeOwnerId = owner.getId();
      dto.scopeOwnerType = ScopeOwnerUtils.getScopeOwnerType(owner.getType(), owner.getId());
      dto.scopeOwnerName = owner.getName();
    }
    else {
      dto.scopeOwnerId = policyWaiverRequest.getOwnerId();
    }

    if (policyWaiverRequest.getComponentMatchStrategy() != null) {
      dto.matcherStrategy = policyWaiverRequest.getComponentMatchStrategy();
      if (policyWaiverRequest.getComponentMatchStrategy() != ALL_COMPONENTS) {
        dto.associatedPackageUrl = policyWaiverRequest.getAssociatedPackageUrl();
      }
    }

    if (policyWaiverRequest.getConstraintFacts() != null) {
      policyWaiverRequest.getConstraintFacts()
          .stream()
          .flatMap(constraintFact -> constraintFact.getConditionFacts().stream().map(ConditionFact::getReference))
          .filter(Objects::nonNull)
          .filter(triggerReference -> triggerReference.getType().equals(SECURITY_VULNERABILITY_REFID))
          .map(TriggerReference::getValue)
          .findFirst()
          .ifPresent(vulnerabilityId -> dto.vulnerabilityId = vulnerabilityId);
    }
    dto.constraintFactsJson = policyWaiverRequest.getConstraintFactsJson();
    dto.constraintFacts = policyWaiverRequest.getConstraintFacts();

    if (policyWaiverReason != null) {
      dto.reasonText = policyWaiverReason.getReasonText();
      dto.policyWaiverReasonId = policyWaiverReason.getId();
    }

    dto.status = policyWaiverRequest.getStatus().name();

    return dto;
  }

  @NotNull
  private AbstractPolicyViolation getAbstractPolicyViolation(String policyViolationId) {
    AbstractPolicyViolation policyViolation = policyViolationDAO.getByIdWithConstraintFacts(policyViolationId);
    if (policyViolation == null) {
      policyViolation = repositoryPolicyViolationDAO.getByIdWithConstraintFacts(policyViolationId);
      if (policyViolation == null) {
        throw new NotFoundException("Could not find policy violation with ID " + policyViolationId + ".");
      }
    }
    return policyViolation;
  }

  private void validateExpiryTime(Date expiryTime) {
    if (expiryTime != null
        && !expiryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(LocalDate.now()))
    {
      throw new BadRequestException("Expiration date must be in the future.");
    }
  }

  private void validateExpireWhenRemediationAvailable(
      boolean expireWhenRemediationAvailable,
      ComponentMatcherStrategyForWaiver matcherStrategy)
  {
    if (expireWhenRemediationAvailable && matcherStrategy != EXACT_COMPONENT) {
      throw new BadRequestException(
          "Expire When Remediation Available Waivers can only be applied to Exact Components.");
    }
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @AuthzContext(Key.TYPE) @SuppressWarnings("unused") OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) @SuppressWarnings("unused") String ownerId)
  {
    // permission checked by annotations, no method body needed
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  void checkWaivePolicyViolationsPermission(
      @AuthzContext(Key.TYPE) @SuppressWarnings("unused") OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) @SuppressWarnings("unused") String ownerId)
  {
    // permission checked by annotations, no method body needed
  }

  private void auditPolicyWaiverRequest(PolicyWaiverRequest policyWaiverRequest) {
    AuditData.get()
        .setData("policyWaiverRequestId", policyWaiverRequest.getId())
        .setPolicy(policyDAO.getById(policyWaiverRequest.getPolicyId()))
        .setComment(policyWaiverRequest.getComment())
        .setComponentHash(policyWaiverRequest.getHash());
    if (policyWaiverRequest.getConstraintFacts() != null) {
      AuditData.get()
          .setData("policyConstraints",
              policyWaiverRequest.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(toList()));
    }
    AuditData.get().setData("status", policyWaiverRequest.getStatus());
  }

  private boolean isViolationOwnerId(AbstractPolicyViolation policyViolation, String ownerId) {
    for (Owner owner : ownerDAO.walkHierarchy(policyViolation.getOwnerId())) {
      if (owner.getId().equals(ownerId)) {
        return true;
      }
    }
    return false;
  }

  private void validatePolicyWaiverReasonId(String policyWaiverReasonId) {
    if (policyWaiverReasonId != null && isNull(policyWaiverReasonDAO.getById(policyWaiverReasonId))) {
      throw new BadRequestException("Policy waiver reason ID " + policyWaiverReasonId + " not found.");
    }
  }

  private PolicyWaiverRequest createPolicyWaiverRequest(
      String ownerId,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      String noteToReviewer,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime,
      String policyWaiverReasonId,
      boolean expireWhenRemediationAvailable)
  {
    String hash =
        matcherStrategy == ALL_COMPONENTS || matcherStrategy == ALL_VERSIONS ? null : abstractPolicyViolation.getHash();
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest(hash, abstractPolicyViolation.getPolicyId(), ownerId, comment);
    policyWaiverRequest.setNoteToReviewer(noteToReviewer);
    policyWaiverRequest.setConstraintFactsJson(abstractPolicyViolation.getConstraintFactsJson());
    policyWaiverRequest.setPolicyViolationId(abstractPolicyViolation.getId());
    policyWaiverRequest.setExpiryTime(expiryTime);
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    policyWaiverRequest.setRequesterId(userPrincipal.getUsername());
    policyWaiverRequest.setRequesterName(userPrincipal.getDisplayName());
    policyWaiverRequest.setComponentMatchStrategy(matcherStrategy);
    policyWaiverRequest.setExpireWhenRemediationAvailable(expireWhenRemediationAvailable);
    if (matcherStrategy != ALL_COMPONENTS && abstractPolicyViolation.getComponentIdentifier() != null) {
      policyWaiverRequest.setAssociatedPackageUrl(toPackageUrl(abstractPolicyViolation.getComponentIdentifier()));
    }
    policyWaiverRequest.setWaiverReasonId(policyWaiverReasonId);

    policyWaiverRequestDAO.insert(policyWaiverRequest);
    return policyWaiverRequest;
  }

  private void sendTelemetryForPolicyWaiverRequest(
      AbstractPolicyViolation policyViolation,
      PolicyWaiverReason policyWaiverReason)
  {
    if (!(policyViolation instanceof PolicyViolation)) {
      return;
    }

    String reasonText = policyWaiverReason != null ? policyWaiverReason.getReasonText() : null;
    TelemetryData telemetryData = new PolicyViolationTelemetryBuilder(
        (PolicyViolation) policyViolation, POLICY_WAIVER_REQUEST, telemetryUtils, licenseNameProvider)
            .build()
            .put(WAIVER_REASON, reasonText);

    telemetrySender.send(telemetryData);
  }

  public ApiPolicyWaiverRequestDTO reviewPolicyWaiverRequest(
      OwnerType ownerType,
      String ownerId,
      String policyWaiverRequestId,
      ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO)
  {
    log.debug("Received request to review policy waiver request for ownerType {}, ownerId {}, "
        + "policy waiver request ID {}", ownerType, ownerId, policyWaiverRequestId);

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    // Check permission before any other validation, to avoid giving away extra information to an unauthorized user.
    checkWaivePolicyViolationsPermission(ownerType, internalOwnerId);

    PolicyWaiverRequest policyWaiverRequest = policyWaiverRequestDAO.getById(policyWaiverRequestId);
    if (policyWaiverRequest == null) {
      throw new NotFoundException("Could not find policy waiver request with ID " + policyWaiverRequestId + ".");
    }

    // Throws NotFoundException if the policy violation does not exist.
    AbstractPolicyViolation abstractPolicyViolation =
        getAbstractPolicyViolation(policyWaiverRequest.getPolicyViolationId());
    // REPOSITORY_CONTAINER_ID is a virtual scope that covers all container image violations.
    // Container image applications live in the org hierarchy, not the repository hierarchy,
    // so isViolationOwnerId would always return false for that scope — skip the check.
    if (!RepositoryContainer.REPOSITORY_CONTAINER_ID.equals(internalOwnerId)
        && !isViolationOwnerId(abstractPolicyViolation, internalOwnerId))
    {
      throw new NotFoundException("Could not find policy violation with ID " + abstractPolicyViolation.getId() + ".");
    }

    if (apiPolicyWaiverRequestReviewDTO == null) {
      throw new BadRequestException("ApiPolicyWaiverRequestReviewDTO is required.");
    }
    if (StringUtils.isEmpty(apiPolicyWaiverRequestReviewDTO.status)) {
      throw new BadRequestException("status is required.");
    }

    PolicyWaiverRequestStatus status = PolicyWaiverRequestStatus.fromString(apiPolicyWaiverRequestReviewDTO.status);
    switch (status) {
      case APPROVED:
        Owner owner = ownerDAO.getById(internalOwnerId);
        // For container image waiver requests scoped to REPOSITORY_CONTAINER_ID, save the waiver
        // against REPOSITORY_CONTAINER_ID so it appears in the Existing Waivers table, which
        // queries policyWaivers by REPOSITORY_CONTAINER_ID as the owner.
        Owner effectiveOwner = owner;
        approvePolicyWaiverRequest(effectiveOwner, policyWaiverRequest, abstractPolicyViolation,
            apiPolicyWaiverRequestReviewDTO);
        break;
      case REJECTED:
        rejectPolicyWaiverRequest(policyWaiverRequest, apiPolicyWaiverRequestReviewDTO);
        break;
      default:
        throw new BadRequestException("status must be APPROVED or REJECTED.");
    }

    auditPolicyWaiverRequest(policyWaiverRequest);

    String waiverReasonId = policyWaiverRequest.getWaiverReasonId();
    PolicyWaiverReason policyWaiverReason =
        waiverReasonId != null ? policyWaiverReasonDAO.getById(waiverReasonId) : null;

    return toDto(policyWaiverRequest, policyWaiverReason);
  }

  private void approvePolicyWaiverRequest(
      Owner owner,
      PolicyWaiverRequest policyWaiverRequest,
      AbstractPolicyViolation abstractPolicyViolation,
      ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO)
  {
    validatePolicyWaiverReasonId(apiPolicyWaiverRequestReviewDTO.waiverReasonId);
    validateExpiryTime(apiPolicyWaiverRequestReviewDTO.expiryTime);

    // ownerId is set to REPOSITORY_CONTAINER_ID by addContainerImagePolicyWaiverRequest. Only
    // PolicyViolation (regular) anchors require the canonical container-image waiver set; a
    // RepositoryPolicyViolation under this scope uses the single-waiver flow.
    if (RepositoryContainer.REPOSITORY_CONTAINER_ID.equals(policyWaiverRequest.getOwnerId())
        && abstractPolicyViolation instanceof PolicyViolation)
    {
      approveContainerImagePolicyWaiverRequest(policyWaiverRequest, abstractPolicyViolation,
          apiPolicyWaiverRequestReviewDTO);
    }
    else {
      approveSinglePolicyWaiverRequest(owner, policyWaiverRequest, abstractPolicyViolation,
          apiPolicyWaiverRequestReviewDTO);
    }
  }

  private void approveContainerImagePolicyWaiverRequest(
      PolicyWaiverRequest policyWaiverRequest,
      AbstractPolicyViolation abstractPolicyViolation,
      ApiPolicyWaiverRequestReviewDTO reviewDTO)
  {
    // The anchor violation lives under the container-image application (set by
    // addContainerImagePolicyWaiverRequest), so its ownerId identifies the image to waive.
    String containerImageApplicationId = abstractPolicyViolation.getOwnerId();

    ApiWaiverOptionsDTO options = new ApiWaiverOptionsDTO(
        reviewDTO.comment,
        EXACT_COMPONENT, // helper overrides per pass
        reviewDTO.expiryTime,
        reviewDTO.waiverReasonId,
        false);

    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      PolicyWaiver containerLevelWaiver =
          apiPolicyWaiverService.applyContainerImageWaivers(containerImageApplicationId, options, tx);

      policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.APPROVED);
      UserPrincipal userPrincipal = currentUser.getUserPrincipal();
      policyWaiverRequest.setReviewerId(userPrincipal.getUsername());
      policyWaiverRequest.setReviewerName(userPrincipal.getDisplayName());
      policyWaiverRequest.setReviewTime(new Date());
      policyWaiverRequest.setPolicyWaiverId(containerLevelWaiver.getId());
      policyWaiverRequestDAO.update(tx, policyWaiverRequest);
      tx.commit();
    }
    // Per-waiver audit + telemetry already emitted from inside createPolicyWaiversInternal.
    // The request-level audit (auditPolicyWaiverRequest) runs after the approve/reject switch
    // in reviewPolicyWaiverRequest — covers both approval paths uniformly.
  }

  private void approveSinglePolicyWaiverRequest(
      Owner owner,
      PolicyWaiverRequest policyWaiverRequest,
      AbstractPolicyViolation abstractPolicyViolation,
      ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO)
  {
    ComponentMatcherStrategyForWaiver matcherStrategy =
        apiPolicyWaiverRequestReviewDTO.matcherStrategy != null
            ? apiPolicyWaiverRequestReviewDTO.matcherStrategy
            : EXACT_COMPONENT;

    validateExpireWhenRemediationAvailable(apiPolicyWaiverRequestReviewDTO.expireWhenRemediationAvailable,
        matcherStrategy);

    PolicyWaiver policyWaiver;
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      policyWaiver = apiPolicyWaiverService.savePolicyWaiver(tx, owner.getId(), abstractPolicyViolation,
          apiPolicyWaiverRequestReviewDTO.comment, matcherStrategy, apiPolicyWaiverRequestReviewDTO.expiryTime,
          apiPolicyWaiverRequestReviewDTO.waiverReasonId,
          apiPolicyWaiverRequestReviewDTO.expireWhenRemediationAvailable);

      policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.APPROVED);
      UserPrincipal userPrincipal = currentUser.getUserPrincipal();
      policyWaiverRequest.setReviewerId(userPrincipal.getUsername());
      policyWaiverRequest.setReviewerName(userPrincipal.getDisplayName());
      policyWaiverRequest.setReviewTime(new Date());
      policyWaiverRequest.setPolicyWaiverId(policyWaiver.getId());
      policyWaiverRequestDAO.update(tx, policyWaiverRequest);
      tx.commit();
    }

    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CREATE_WAIVER, true)) {
      AuditData.get().setOwner(owner);
      apiPolicyWaiverService.auditAndSendTelemetry(owner.getType(), owner.getId(), policyWaiver,
          abstractPolicyViolation);
    }
  }

  private void rejectPolicyWaiverRequest(
      PolicyWaiverRequest policyWaiverRequest,
      ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO)
  {
    if (PolicyWaiverRequestStatus.APPROVED.equals(policyWaiverRequest.getStatus())) {
      throw new BadRequestException("Cannot reject an approved policy waiver request.");
    }

    policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.REJECTED);
    policyWaiverRequest.setRejectionReason(apiPolicyWaiverRequestReviewDTO.rejectionReason);
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    policyWaiverRequest.setReviewerId(userPrincipal.getUsername());
    policyWaiverRequest.setReviewerName(userPrincipal.getDisplayName());
    policyWaiverRequest.setReviewTime(new Date());
    policyWaiverRequestDAO.update(policyWaiverRequest);

    auditPolicyWaiverRequest(policyWaiverRequest);
  }

  /**
   * Withdraw a pending policy waiver request as the original requester.
   *
   * The request row is hard-deleted: this endpoint is intended for cleaning up requests
   * that were created by mistake (wrong scope, wrong rationale, etc.), so we don't keep
   * a soft-state record around. The audit log (see {@link AuditEvent#WITHDRAW_WAIVER_REQUEST})
   * still captures who withdrew which request and when, so the action is fully traceable.
   *
   * Authorization is ownership-based: only the user whose username matches the request's
   * {@code requesterId} may withdraw. Any other authenticated caller — whether they lack
   * read access to the owner, lack the {@link Permission#WAIVE_POLICY_VIOLATIONS} permission,
   * or simply aren't the requester — receives a {@link NotFoundException} so as not to leak
   * existence of requests they don't own. That permission's flow is the existing
   * {@link #reviewPolicyWaiverRequest review} endpoint, which handles already-decided
   * requests (APPROVED / REJECTED).
   *
   * Only requests in the {@link PolicyWaiverRequestStatus#REQUESTED} state may be withdrawn;
   * already-acted-on requests produce a {@link BadRequestException}.
   */
  public void withdrawPolicyWaiverRequest(
      OwnerType ownerType,
      String ownerId,
      String policyWaiverRequestId)
  {
    log.debug(
        "Received request to withdraw policy waiver request for ownerType {}, ownerId {}, "
            + "policy waiver request ID {}",
        ownerType, ownerId, policyWaiverRequestId);

    if (!SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.isEnabled()) {
      throw new UnauthorizedException("Waiver requests are disabled by system property "
          + SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED.getPropertyName());
    }

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    // The withdraw flow is ownership-based: a caller who can't read the owner is — by
    // definition — not the requester, so collapse that 403 into a 404 to avoid leaking
    // the existence of requests under owners the caller can't see. The 401 path
    // (UnauthenticatedException from the AuditFilter chain before we get here) still
    // surfaces normally.
    try {
      checkReadPermission(ownerType, internalOwnerId);
    }
    catch (UnauthorizedException e) {
      throw new NotFoundException(
          "Cannot find a policy waiver request with ID " + policyWaiverRequestId + ".");
    }

    // Throws NotFoundException if not found under this owner — the same code path
    // is used to hide non-owned requests from the caller.
    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getByIdAndOwnerIdNotNull(policyWaiverRequestId, internalOwnerId);

    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    if (!userPrincipal.getUsername().equals(policyWaiverRequest.getRequesterId())) {
      // 404 (not 403) so callers can't probe other users' requests by ID.
      throw new NotFoundException(
          "Cannot find a policy waiver request with ID " + policyWaiverRequestId + ".");
    }

    if (!PolicyWaiverRequestStatus.REQUESTED.equals(policyWaiverRequest.getStatus())) {
      throw new BadRequestException(
          "Cannot withdraw a policy waiver request that is already "
              + policyWaiverRequest.getStatus() + ".");
    }

    // Resolve telemetry inputs from the in-memory request before delete so the post-delete
    // telemetry call has them in hand. The lookups tolerate a missing underlying violation
    // or reason (e.g., the violation has been re-evaluated away) — null falls through
    // sendTelemetryForPolicyWaiverRequest's early return.
    AbstractPolicyViolation abstractPolicyViolation =
        policyViolationDAO.getByIdWithConstraintFacts(policyWaiverRequest.getPolicyViolationId());
    if (abstractPolicyViolation == null) {
      abstractPolicyViolation =
          repositoryPolicyViolationDAO.getByIdWithConstraintFacts(policyWaiverRequest.getPolicyViolationId());
    }
    PolicyWaiverReason policyWaiverReason = policyWaiverRequest.getWaiverReasonId() != null
        ? policyWaiverReasonDAO.getById(policyWaiverRequest.getWaiverReasonId())
        : null;

    // Atomic conditional delete closes the TOCTOU window between the status check above
    // and the delete: if a concurrent reviewer transitions REQUESTED -> APPROVED in this
    // window, deleteIfStatusEquals returns false rather than deleting an approved row
    // (which would leave the just-created PolicyWaiver pointing at nothing).
    if (!policyWaiverRequestDAO.deleteIfStatusEquals(policyWaiverRequestId,
        PolicyWaiverRequestStatus.REQUESTED))
    {
      throw new BadRequestException(
          "Cannot withdraw a policy waiver request that was concurrently modified. "
              + "Please refresh and try again.");
    }

    // Audit after the delete succeeds. AuditData captures the entity's in-memory fields
    // (policyId, comment, hash, constraintFacts, status); the @Audited interceptor on the
    // resource method writes the audit log entry after the method returns normally.
    auditPolicyWaiverRequest(policyWaiverRequest);

    // Mirror the create/update telemetry path so dashboards counting waiver request
    // activity see withdrawals too. The helper no-ops on non-PolicyViolation (e.g., null
    // or RepositoryPolicyViolation).
    sendTelemetryForPolicyWaiverRequest(abstractPolicyViolation, policyWaiverReason);
  }

  public List<ApiPolicyWaiverRequestDTO> getPolicyWaiverRequests(
      OwnerType ownerType,
      String ownerId,
      String repositoryFormat)
  {
    log.debug("Received request to list policy waiver requests for ownerType {}, ownerId {}, repositoryFormat {}",
        ownerType, ownerId, repositoryFormat);

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    checkReadPermission(ownerType, internalOwnerId);

    // Collect owner IDs that waiver requests could be stored against.
    // Requests can be scoped to a repository, repository_manager, or repository_container.
    List<Repository> matchingRepos = repositoryService.getRepositoriesWithReadPermission()
        .stream()
        .filter(repo -> matchesRepositoryFormat(repo, repositoryFormat))
        .collect(Collectors.toList());

    Set<String> allowedOwnerIds = matchingRepos.stream()
        .map(Repository::getId)
        .collect(Collectors.toSet());

    // Also include parent repository managers and the repository container,
    // since waiver requests can be scoped at those levels too.
    Set<String> repoManagerIds = matchingRepos.stream()
        .map(Repository::getRepositoryManagerId)
        .collect(Collectors.toSet());
    allowedOwnerIds.addAll(repoManagerIds);
    allowedOwnerIds.add(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    List<PolicyWaiverRequest> policyWaiverRequests = policyWaiverRequestDAO.getByOwnerIds(allowedOwnerIds);

    // Prefetch to keep toDto from re-introducing per-record lookups.
    Map<String, PolicyWaiverReason> reasonsById =
        policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();
    Map<String, Policy> policiesById = prefetchPoliciesForWaiverRequests(policyWaiverRequests);
    Map<String, Owner> ownersById = prefetchOwnersForWaiverRequests(policyWaiverRequests);
    Map<String, AbstractPolicyViolation> violationsByIdForFallback =
        prefetchViolationsForComponentIdentifierFallback(policyWaiverRequests);

    return policyWaiverRequests.stream()
        .map(r -> toDtoWithPrefetched(r, reasonsById.get(r.getWaiverReasonId()),
            policiesById.get(r.getPolicyId()), ownersById.get(r.getOwnerId()),
            violationsByIdForFallback.get(r.getPolicyViolationId())))
        .collect(Collectors.toList());
  }

  private Map<String, Policy> prefetchPoliciesForWaiverRequests(List<PolicyWaiverRequest> policyWaiverRequests) {
    Set<String> policyIds = policyWaiverRequests.stream()
        .map(PolicyWaiverRequest::getPolicyId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (policyIds.isEmpty()) {
      // emptyMap() (not Map.of()) so a null-key get() returns null instead of throwing.
      return Collections.emptyMap();
    }
    return policyDAO.getByIds(policyIds).stream().collect(Collectors.toMap(Policy::getId, p -> p));
  }

  // WARN when the list endpoint is about to fan out to more owners than expected.
  private static final int OWNER_PREFETCH_WARN_THRESHOLD = 50;

  // OwnerDAO composites org/app/repo/manager and rejects batch lookups, so fall back to per-id.
  private Map<String, Owner> prefetchOwnersForWaiverRequests(List<PolicyWaiverRequest> policyWaiverRequests) {
    Set<String> distinctOwnerIds = policyWaiverRequests.stream()
        .map(PolicyWaiverRequest::getOwnerId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (distinctOwnerIds.size() > OWNER_PREFETCH_WARN_THRESHOLD) {
      log.warn("getPolicyWaiverRequests fanning out to {} owners (threshold {}); consider batching ownerDAO.",
          distinctOwnerIds.size(), OWNER_PREFETCH_WARN_THRESHOLD);
    }
    Map<String, Owner> ownersById = new HashMap<>(distinctOwnerIds.size());
    for (String id : distinctOwnerIds) {
      Owner owner = ownerDAO.getById(id);
      if (owner != null) {
        ownersById.put(id, owner);
      }
    }
    return ownersById;
  }

  // Batch-load violations only for requests missing their own componentIdentifier
  // (e.g. ALL_COMPONENTS matcher). Constraint facts aren't needed here.
  private Map<String, AbstractPolicyViolation> prefetchViolationsForComponentIdentifierFallback(
      List<PolicyWaiverRequest> policyWaiverRequests)
  {
    Set<String> violationIds = policyWaiverRequests.stream()
        .filter(r -> r.getComponentIdentifier() == null && r.getPolicyViolationId() != null)
        .map(PolicyWaiverRequest::getPolicyViolationId)
        .collect(Collectors.toSet());
    if (violationIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, AbstractPolicyViolation> byId = new HashMap<>(violationIds.size());
    for (PolicyViolation v : policyViolationDAO.getByIds(violationIds)) {
      byId.put(v.getId(), v);
    }
    // Ids not in policy_violation may live in the repository sibling table.
    Set<String> unresolvedIds = new LinkedHashSet<>(violationIds);
    unresolvedIds.removeAll(byId.keySet());
    if (!unresolvedIds.isEmpty()) {
      for (var v : repositoryPolicyViolationDAO.getByIds(unresolvedIds)) {
        byId.put(v.getId(), v);
      }
      unresolvedIds.removeAll(byId.keySet());
    }
    // Batched equivalent of the per-record "violation no longer exists" debug log.
    if (!unresolvedIds.isEmpty() && log.isDebugEnabled()) {
      log.debug("Policy violation(s) {} no longer exist for the referenced waiver requests; "
          + "componentIdentifier fallback will be null for those DTOs.", unresolvedIds);
    }
    return byId;
  }

  private ApiPolicyWaiverRequestDTO toDtoWithPrefetched(
      PolicyWaiverRequest policyWaiverRequest,
      PolicyWaiverReason policyWaiverReason,
      Policy prefetchedPolicy,
      Owner prefetchedOwner,
      AbstractPolicyViolation prefetchedViolationForFallback)
  {
    ApiPolicyWaiverRequestDTO dto = new ApiPolicyWaiverRequestDTO();

    dto.policyWaiverRequestId = policyWaiverRequest.getId();
    dto.comment = policyWaiverRequest.getComment();
    dto.noteToReviewer = policyWaiverRequest.getNoteToReviewer();
    dto.requestTime = policyWaiverRequest.getRequestTime();
    dto.expiryTime = policyWaiverRequest.getExpiryTime();
    dto.hash = policyWaiverRequest.getHash();
    dto.policyId = policyWaiverRequest.getPolicyId();
    if (prefetchedPolicy != null) {
      dto.policyName = prefetchedPolicy.getName();
      dto.threatLevel = prefetchedPolicy.getThreatLevel();
    }
    dto.requesterId = policyWaiverRequest.getRequesterId();
    dto.requesterName = policyWaiverRequest.getRequesterName();
    dto.reviewerName = policyWaiverRequest.getReviewerName();
    dto.reviewerId = policyWaiverRequest.getReviewerId();
    dto.rejectionReason = policyWaiverRequest.getRejectionReason();
    dto.componentUpgradeAvailable = policyWaiverRequest.isComponentUpgradeAvailable();
    dto.expireWhenRemediationAvailable = policyWaiverRequest.isExpireWhenRemediationAvailable();
    dto.policyViolationId = policyWaiverRequest.getPolicyViolationId();

    if (policyWaiverRequest.getComponentIdentifier() != null) {
      dto.componentIdentifier =
          ApiComponentIdentifierDTOV2.fromComponentIdentifier(policyWaiverRequest.getComponentIdentifier());
    }
    else if (prefetchedViolationForFallback != null
        && prefetchedViolationForFallback.getComponentIdentifier() != null)
    {
      dto.componentIdentifier =
          ApiComponentIdentifierDTOV2.fromComponentIdentifier(prefetchedViolationForFallback.getComponentIdentifier());
    }

    if (prefetchedOwner != null) {
      dto.scopeOwnerId = prefetchedOwner.getId();
      dto.scopeOwnerType = ScopeOwnerUtils.getScopeOwnerType(prefetchedOwner.getType(), prefetchedOwner.getId());
      dto.scopeOwnerName = prefetchedOwner.getName();
    }
    else {
      dto.scopeOwnerId = policyWaiverRequest.getOwnerId();
    }

    if (policyWaiverRequest.getComponentMatchStrategy() != null) {
      dto.matcherStrategy = policyWaiverRequest.getComponentMatchStrategy();
      if (policyWaiverRequest.getComponentMatchStrategy() != ALL_COMPONENTS) {
        dto.associatedPackageUrl = policyWaiverRequest.getAssociatedPackageUrl();
      }
    }

    if (policyWaiverRequest.getConstraintFacts() != null) {
      policyWaiverRequest.getConstraintFacts()
          .stream()
          .flatMap(constraintFact -> constraintFact.getConditionFacts().stream().map(ConditionFact::getReference))
          .filter(Objects::nonNull)
          .filter(triggerReference -> triggerReference.getType().equals(SECURITY_VULNERABILITY_REFID))
          .map(TriggerReference::getValue)
          .findFirst()
          .ifPresent(vulnerabilityId -> dto.vulnerabilityId = vulnerabilityId);
    }
    dto.constraintFactsJson = policyWaiverRequest.getConstraintFactsJson();
    dto.constraintFacts = policyWaiverRequest.getConstraintFacts();

    if (policyWaiverReason != null) {
      dto.reasonText = policyWaiverReason.getReasonText();
      dto.policyWaiverReasonId = policyWaiverReason.getId();
    }

    dto.status = policyWaiverRequest.getStatus().name();

    return dto;
  }

  private static boolean matchesRepositoryFormat(Repository repo, String repositoryFormat) {
    if (repositoryFormat == null || repositoryFormat.isEmpty()) {
      return true;
    }
    if ("docker".equalsIgnoreCase(repositoryFormat)) {
      return "docker".equalsIgnoreCase(repo.getFormat());
    }
    if ("component".equalsIgnoreCase(repositoryFormat)) {
      return !"docker".equalsIgnoreCase(repo.getFormat());
    }
    return true;
  }

  public ApiPolicyWaiverRequestDTO getPolicyWaiverRequest(
      OwnerType ownerType,
      String ownerId,
      String policyWaiverRequestId)
  {
    log.debug("Received request to get policy waiver request for ownerType {}, ownerId {}, policy waiver request ID {}",
        ownerType, ownerId, policyWaiverRequestId);

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    // Check permission before anything else, to avoid giving away extra information to an unauthorized user.
    checkReadPermission(ownerType, internalOwnerId);

    PolicyWaiverRequest policyWaiverRequest =
        policyWaiverRequestDAO.getByIdAndOwnerIdNotNull(policyWaiverRequestId, internalOwnerId);
    PolicyWaiverReason policyWaiverReason = policyWaiverReasonDAO.getById(policyWaiverRequest.getWaiverReasonId());
    return toDto(policyWaiverRequest, policyWaiverReason);
  }

  public ApiPolicyWaiverRequestsApplicableToViolationDTO getApplicableWaiverRequests(String policyViolationId) {
    // The violationId may reference an application policy violation or a repository policy violation
    AbstractPolicyViolation policyViolation = getAbstractPolicyViolation(policyViolationId);

    String policyId = policyViolation.getPolicyId();
    String constraintFactsJson = policyViolation.getConstraintFactsJson();
    List<ConstraintFact> constraintFacts = policyViolation.getConstraintFacts();
    String hash = policyViolation.getHash();
    String ownerId = policyViolation.getOwnerId();
    ComponentIdentifier componentIdentifier = policyViolation.getComponentIdentifier();

    Owner owner = ownerDAO.getById(ownerId);
    Map<String, PolicyWaiverReason> policyWaiversReasons =
        policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    Map<Boolean, List<ApiPolicyWaiverRequestDTO>> applicableWaiverRequests =
        getByOwnerHierarchyAndPolicyIdWithReadPermission(owner, policyId)
            .stream()
            .filter(policyWaiverRequest -> filterPolicyWaiverRequestByCriteria(constraintFactsJson,
                constraintFacts, componentIdentifier, hash, policyWaiverRequest))
            .map(policyWaiverRequest -> toDto(policyWaiverRequest,
                policyWaiversReasons.get(policyWaiverRequest.getWaiverReasonId())))
            .collect(partitioningBy(dto -> isExpired(dto.expiryTime), toList()));

    ApiPolicyWaiverRequestsApplicableToViolationDTO result = new ApiPolicyWaiverRequestsApplicableToViolationDTO();
    result.activeWaiverRequests = applicableWaiverRequests.get(Boolean.FALSE);
    result.expiredWaiverRequests = applicableWaiverRequests.get(Boolean.TRUE);

    return result;
  }

  @Authorize(permission = Permission.READ)
  List<PolicyWaiverRequest> getByOwnerHierarchyAndPolicyIdWithReadPermission(
      @AuthzContext(Key.OWNER) Owner owner,
      String policyId)
  {
    return policyWaiverRequestDAO.getByOwnerHierarchyAndPolicyId(owner, policyId);
  }

  private boolean filterPolicyWaiverRequestByCriteria(
      String constraintFactsJson,
      List<ConstraintFact> constraintFacts,
      ComponentIdentifier componentIdentifier,
      String hash,
      PolicyWaiverRequest policyWaiverRequest)
  {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    ComponentFact componentFact = new ComponentFact(componentIdentifier, hash);

    return policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)
        && (policyWaiverRequestMatcherWrapper.matchesConstraintFactsJson(constraintFactsJson)
            || policyWaiverRequestMatcherWrapper.matchesConstraintFacts(constraintFacts));
  }

  private static boolean isExpired(Date expiryTime) {
    return expiryTime != null && expiryTime.before(new Date());
  }

  public ApiPolicyWaiverRequestDTO updatePolicyWaiverRequest(
      OwnerType ownerType,
      String ownerId,
      String policyWaiverRequestId,
      ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO)
  {
    log.debug(
        "Received request to update policy waiver request for ownerType {}, ownerId {}, policy waiver request ID {}",
        ownerType, ownerId, policyWaiverRequestId);

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    // Check permission before any other validation, to avoid giving away extra information to an unauthorized user.
    checkReadPermission(ownerType, internalOwnerId);

    PolicyWaiverRequest policyWaiverRequest = policyWaiverRequestDAO.getByIdNotNull(policyWaiverRequestId);

    // Throws NotFoundException if the policy violation does not exist.
    AbstractPolicyViolation abstractPolicyViolation =
        getAbstractPolicyViolation(policyWaiverRequest.getPolicyViolationId());
    if (!isViolationOwnerId(abstractPolicyViolation, internalOwnerId)) {
      throw new NotFoundException("Could not find policy violation with ID " + abstractPolicyViolation.getId() + ".");
    }

    if (PolicyWaiverRequestStatus.APPROVED.equals(policyWaiverRequest.getStatus())) {
      throw new BadRequestException("Cannot update an approved policy waiver request.");
    }

    ComponentMatcherStrategyForWaiver matcherStrategy;
    if (policyWaiverRequestOptionsDTO != null) {
      validatePolicyWaiverReasonId(policyWaiverRequestOptionsDTO.waiverReasonId);

      if (policyWaiverRequestOptionsDTO.matcherStrategy != null) {
        matcherStrategy = policyWaiverRequestOptionsDTO.matcherStrategy;
      }
      else {
        matcherStrategy = EXACT_COMPONENT;
      }
    }
    else {
      matcherStrategy = EXACT_COMPONENT;
    }

    String comment = policyWaiverRequestOptionsDTO == null ? null : policyWaiverRequestOptionsDTO.comment;
    String noteToReviewer = policyWaiverRequestOptionsDTO == null ? null : policyWaiverRequestOptionsDTO.noteToReviewer;
    Date expiryTime = policyWaiverRequestOptionsDTO == null ? null : policyWaiverRequestOptionsDTO.expiryTime;
    String waiverReasonId = policyWaiverRequestOptionsDTO == null ? null : policyWaiverRequestOptionsDTO.waiverReasonId;
    boolean expireWhenRemediationAvailable =
        policyWaiverRequestOptionsDTO != null && policyWaiverRequestOptionsDTO.expireWhenRemediationAvailable;

    validateExpiryTime(expiryTime);

    validateExpireWhenRemediationAvailable(expireWhenRemediationAvailable, matcherStrategy);

    updatePolicyWaiverRequest(policyWaiverRequest, internalOwnerId, abstractPolicyViolation,
        comment, noteToReviewer, matcherStrategy, expiryTime, waiverReasonId, expireWhenRemediationAvailable);

    auditPolicyWaiverRequest(policyWaiverRequest);

    PolicyWaiverReason policyWaiverReason =
        waiverReasonId != null ? policyWaiverReasonDAO.getById(waiverReasonId) : null;
    sendTelemetryForPolicyWaiverRequest(abstractPolicyViolation, policyWaiverReason);

    return toDto(policyWaiverRequest, policyWaiverReason);
  }

  private void updatePolicyWaiverRequest(
      PolicyWaiverRequest policyWaiverRequest,
      String ownerId,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      String noteToReviewer,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime,
      String policyWaiverReasonId,
      boolean expireWhenRemediationAvailable)
  {
    String hash =
        matcherStrategy == ALL_COMPONENTS || matcherStrategy == ALL_VERSIONS ? null : abstractPolicyViolation.getHash();
    policyWaiverRequest.setHash(hash);
    policyWaiverRequest.setOwnerId(ownerId);
    policyWaiverRequest.setComment(comment);
    policyWaiverRequest.setNoteToReviewer(noteToReviewer);
    policyWaiverRequest.setExpiryTime(expiryTime);
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    policyWaiverRequest.setRequesterId(userPrincipal.getUsername());
    policyWaiverRequest.setRequesterName(userPrincipal.getDisplayName());
    policyWaiverRequest.setComponentMatchStrategy(matcherStrategy);
    policyWaiverRequest.setExpireWhenRemediationAvailable(expireWhenRemediationAvailable);
    if (matcherStrategy != ALL_COMPONENTS && abstractPolicyViolation.getComponentIdentifier() != null) {
      policyWaiverRequest.setAssociatedPackageUrl(toPackageUrl(abstractPolicyViolation.getComponentIdentifier()));
    }
    else {
      policyWaiverRequest.setAssociatedPackageUrl(null);
    }
    policyWaiverRequest.setWaiverReasonId(policyWaiverReasonId);
    policyWaiverRequest.setReviewerId(null);
    policyWaiverRequest.setReviewerName(null);
    policyWaiverRequest.setRejectionReason(null);
    policyWaiverRequest.setReviewTime(null);
    policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.REQUESTED);

    policyWaiverRequestDAO.update(policyWaiverRequest);
  }
}

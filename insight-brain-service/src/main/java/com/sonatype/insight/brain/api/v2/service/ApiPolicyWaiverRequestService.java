/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
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
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetryBuilder;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.purl.PackageUrlIdentifier.toPackageUrl;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.POLICY_WAIVER_REQUEST;
import static java.util.Objects.isNull;
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
      TelemetryUtils telemetryUtils)
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

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    // Check permission before any other validation, to avoid giving away extra information to an unauthorized user.
    checkReadPermission(ownerType, internalOwnerId);

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

    auditPolicyWaiverRequest(policyWaiverRequest);

    PolicyWaiverReason policyWaiverReason =
        waiverReasonId != null ? policyWaiverReasonDAO.getById(waiverReasonId) : null;
    sendTelemetryForPolicyWaiverRequest(abstractPolicyViolation, policyWaiverReason);

    return toDto(policyWaiverRequest, policyWaiverReason);
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
    dto.policyName = policyDAO.getById(policyWaiverRequest.getPolicyId()).getName();
    dto.requesterId = policyWaiverRequest.getRequesterId();
    dto.requesterName = policyWaiverRequest.getRequesterName();
    dto.componentUpgradeAvailable = policyWaiverRequest.isComponentUpgradeAvailable();
    dto.expireWhenRemediationAvailable = policyWaiverRequest.isExpireWhenRemediationAvailable();

    if (policyWaiverRequest.getComponentIdentifier() != null) {
      dto.componentIdentifier =
          ApiComponentIdentifierDTOV2.fromComponentIdentifier(policyWaiverRequest.getComponentIdentifier());
    }

    dto.scopeOwnerId = owner.getId();
    dto.scopeOwnerType = ScopeOwnerUtils.getScopeOwnerType(owner.getType(), owner.getId());
    dto.scopeOwnerName = owner.getName();

    if (policyWaiverRequest.getComponentMatchStrategy() != null) {
      dto.matcherStrategy = policyWaiverRequest.getComponentMatchStrategy();
      if (policyWaiverRequest.getComponentMatchStrategy() != ALL_COMPONENTS) {
        dto.associatedPackageUrl = policyWaiverRequest.getAssociatedPackageUrl();
      }
    }

    if (policyWaiverRequest.getConstraintFacts() != null) {
      policyWaiverRequest.getConstraintFacts().stream()
          .flatMap(constraintFact -> constraintFact.getConditionFacts().stream().map(ConditionFact::getReference))
          .filter(Objects::nonNull)
          .filter(triggerReference -> triggerReference.getType().equals(SECURITY_VULNERABILITY_REFID))
          .map(TriggerReference::getValue).findFirst()
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
        && !expiryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(LocalDate.now())) {
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
    AuditData.get().setData("policyWaiverRequestId", policyWaiverRequest.getId())
        .setPolicy(policyDAO.getById(policyWaiverRequest.getPolicyId())).setComment(policyWaiverRequest.getComment())
        .setComponentHash(policyWaiverRequest.getHash());
    if (policyWaiverRequest.getConstraintFacts() != null) {
      AuditData.get().setData("policyConstraints",
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
    TelemetryData telemetryData =
        new PolicyViolationTelemetryBuilder((PolicyViolation) policyViolation, POLICY_WAIVER_REQUEST, telemetryUtils)
            .build().put(WAIVER_REASON, reasonText);

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

    if (!isPolicyWaiverRequestOwnerId(policyWaiverRequest, internalOwnerId)) {
      throw new NotFoundException("Could not find policy waiver request with ID " + policyWaiverRequestId + ".");
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
        approvePolicyWaiverRequest(owner, policyWaiverRequest, apiPolicyWaiverRequestReviewDTO);
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
      ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO)
  {
    validatePolicyWaiverReasonId(apiPolicyWaiverRequestReviewDTO.waiverReasonId);

    ComponentMatcherStrategyForWaiver matcherStrategy =
        apiPolicyWaiverRequestReviewDTO.matcherStrategy != null ? apiPolicyWaiverRequestReviewDTO.matcherStrategy
            : EXACT_COMPONENT;

    String hash = null;
    String associatedPackageUrl = null;
    if (matcherStrategy != ALL_COMPONENTS && matcherStrategy != ALL_VERSIONS) {
      AbstractPolicyViolation abstractPolicyViolation =
          getAbstractPolicyViolation(policyWaiverRequest.getPolicyViolationId());
      hash = abstractPolicyViolation.getHash();
      associatedPackageUrl = toPackageUrl(abstractPolicyViolation.getComponentIdentifier());
    }

    validateExpiryTime(apiPolicyWaiverRequestReviewDTO.expiryTime);

    validateExpireWhenRemediationAvailable(apiPolicyWaiverRequestReviewDTO.expireWhenRemediationAvailable,
        matcherStrategy);

    // Create the policy waiver
    PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setOwnerId(owner.getId());
    policyWaiver.setPolicyId(policyWaiverRequest.getPolicyId());
    policyWaiver.setHash(hash);
    policyWaiver.setAssociatedPackageUrl(associatedPackageUrl);
    policyWaiver.setConstraintFacts(policyWaiverRequest.getConstraintFacts());
    policyWaiver.setConstraintFactsJson(policyWaiverRequest.getConstraintFactsJson());
    policyWaiver.setComponentMatchStrategy(matcherStrategy);
    policyWaiver.setComponentUpgradeAvailable(policyWaiverRequest.isComponentUpgradeAvailable());
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    policyWaiver.setCreatorId(userPrincipal.getUsername());
    policyWaiver.setCreatorName(userPrincipal.getDisplayName());
    policyWaiver.setExpiryTime(apiPolicyWaiverRequestReviewDTO.expiryTime);
    policyWaiver.setWaiverReasonId(apiPolicyWaiverRequestReviewDTO.waiverReasonId);
    policyWaiver.setExpireWhenRemediationAvailable(apiPolicyWaiverRequestReviewDTO.expireWhenRemediationAvailable);
    policyWaiver.setComment(apiPolicyWaiverRequestReviewDTO.comment);

    // Update the policy waiver request
    policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.APPROVED);
    policyWaiverRequest.setReviewerId(userPrincipal.getUsername());
    policyWaiverRequest.setReviewerName(userPrincipal.getDisplayName());
    policyWaiverRequest.setReviewTime(new Date());

    // Persist the policy waiver and the policy waiver request
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      policyWaiverDAO.insert(tx, policyWaiver);
      policyWaiverRequest.setPolicyWaiverId(policyWaiver.getId());
      policyWaiverRequestDAO.update(tx, policyWaiverRequest);
      tx.commit();
    }

    // Audit and send telemetry for the policy waiver.
    AbstractPolicyViolation abstractPolicyViolation =
        getAbstractPolicyViolation(policyWaiverRequest.getPolicyViolationId());
    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CREATE_WAIVER, true)) {
      AuditData.get().setOwner(owner);
      apiPolicyWaiverService.auditAndSendTelemetry(owner.getType(), owner.getId(), policyWaiver,
          abstractPolicyViolation);
    }

    auditPolicyWaiverRequest(policyWaiverRequest);
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

  private boolean isPolicyWaiverRequestOwnerId(PolicyWaiverRequest policyWaiverRequest, String ownerId) {
    for (Owner owner : ownerDAO.walkHierarchy(policyWaiverRequest.getOwnerId())) {
      if (owner.getId().equals(ownerId)) {
        return true;
      }
    }
    return false;
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
}

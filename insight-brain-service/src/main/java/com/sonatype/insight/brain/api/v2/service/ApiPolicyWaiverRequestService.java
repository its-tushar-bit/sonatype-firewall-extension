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
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
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
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;

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

  private final TelemetrySender telemetrySender;

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
      TelemetrySender telemetrySender,
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
    this.telemetrySender = telemetrySender;
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

    return toDto(policyWaiverRequest, policyWaiverReason, ownerDAO.getById(internalOwnerId));
  }

  private ApiPolicyWaiverRequestDTO toDto(
      PolicyWaiverRequest policyWaiverRequest,
      PolicyWaiverReason policyWaiverReason,
      Owner owner)
  {
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

  private void auditPolicyWaiverRequest(PolicyWaiverRequest policyWaiverRequest) {
    AuditData.get().setData("policyWaiverRequestId", policyWaiverRequest.getId())
        .setPolicy(policyDAO.getById(policyWaiverRequest.getPolicyId())).setComment(policyWaiverRequest.getComment())
        .setComponentHash(policyWaiverRequest.getHash());
    if (policyWaiverRequest.getConstraintFacts() != null) {
      AuditData.get().setData("policyConstraints",
          policyWaiverRequest.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(toList()));
    }
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
}

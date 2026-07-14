/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.FirewallPermissionGate;
import com.sonatype.insight.brain.api.v2.dto.ApiBulkWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.containerimagewaiver.ApiContainerImageWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiComponentPolicyWaiversDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO.PolicyContainerWaiverData;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.repository.hosted.ApplicationForHostedRepositoryComponentService;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.owner.OwnerService;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyWaiverMatcherWrapper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.purl.PackageUrlIdentifier.toPackageUrl;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

/**
 * @since 1.70
 */
@Named
public class ApiPolicyWaiverService
{
  private static final Logger log = LoggerFactory.getLogger(ApiPolicyWaiverService.class);

  private static final String OWNER_TYPE_ATTR = "owner_type";

  private static final String OWNER_ID_ATTR = "owner_id";

  public static final int MAX_BULK_WAIVER_VIOLATIONS = 1000;

  private final TelemetrySender telemetrySender;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyDAO policyDAO;

  private final ApplicationDAO applicationDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiPolicyViolationServiceV2 apiPolicyViolationServiceV2;

  private final PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  private final CurrentUser currentUser;

  private final OwnerService ownerService;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private final OrganizationDAO organizationDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  private final RepositoryDAO repositoryDAO;

  private final IdUtils idUtils;

  private final TelemetryUtils telemetryUtils;

  private final FirewallPermissionGate firewallPermissionGate;

  @Inject
  public ApiPolicyWaiverService(
      TelemetrySender telemetrySender,
      PolicyWaiverDAO policyWaiverDAO,
      PolicyDAO policyDAO,
      ApplicationDAO applicationDAO,
      OwnerDAO ownerDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      ApiPolicyViolationServiceV2 apiPolicyViolationServiceV2,
      PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator,
      CurrentUser currentUser,
      OwnerService ownerService,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      PolicyViolationDAO policyViolationDAO,
      PolicyWaiverRequestDAO policyWaiverRequestDAO,
      OrganizationDAO organizationDAO,
      PolicyWaiverReasonDAO policyWaiverReasonDAO,
      RepositoryDAO repositoryDAO,
      IdUtils idUtils,
      TelemetryUtils telemetryUtils,
      FirewallPermissionGate firewallPermissionGate)
  {
    this.telemetrySender = telemetrySender;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyDAO = policyDAO;
    this.applicationDAO = applicationDAO;
    this.ownerDAO = ownerDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.apiPolicyViolationServiceV2 = apiPolicyViolationServiceV2;
    this.policyWaiverTelemetryCreator = policyWaiverTelemetryCreator;
    this.currentUser = currentUser;
    this.ownerService = ownerService;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyWaiverRequestDAO = policyWaiverRequestDAO;
    this.organizationDAO = organizationDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
    this.repositoryDAO = repositoryDAO;
    this.idUtils = idUtils;
    this.telemetryUtils = telemetryUtils;
    this.firewallPermissionGate = firewallPermissionGate;
  }

  private static Optional<String> findFirstTriggerReference(Stream<ConstraintFact> streamOfConstraintFacts) {
    return streamOfConstraintFacts
        .flatMap(constraintFact -> constraintFact.getConditionFacts().stream().map(ConditionFact::getReference))
        .filter(Objects::nonNull)
        .filter(triggerReference -> triggerReference.getType().equals(SECURITY_VULNERABILITY_REFID))
        .map(TriggerReference::getValue)
        .findFirst();
  }

  /**
   * This is currently used in "request waiver"
   *
   * @deprecated Use {@link #addPolicyWaiverByPolicyViolationId(OwnerType, String, String, ApiWaiverOptionsDTO)}
   */
  @Deprecated
  public void addPolicyWaiver(
      final String policyViolationId,
      final OwnerType ownerType,
      final String comment)
  {
    PolicyViolation policyViolation = policyViolationDAO.getByIdWithConstraintFacts(policyViolationId);

    if (policyViolation == null) {
      throw new NotFoundException("Could not find policy violation with ID " + policyViolationId + ".");
    }

    final String ownerId;
    switch (ownerType) {
      case APPLICATION:
        ownerId = policyViolation.getApplicationId();
        AuditData.get().setData("applicationId", ownerId).setApplication(applicationDAO.getById(ownerId));
        break;
      case ORGANIZATION:
        ownerId = applicationDAO.getByIdNotNull(policyViolation.getApplicationId()).getOrganizationId();
        AuditData.get().setData("organizationId", ownerId).setOrganization(organizationDAO.getById(ownerId));
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }

    addPolicyWaiver(ownerType, ownerId, policyViolation, comment, EXACT_COMPONENT, null, null, false);
  }

  /**
   * @param policyViolationId The id of an application or repository policy violation
   */
  public void addPolicyWaiverByPolicyViolationId(
      final OwnerType ownerType,
      final String ownerId,
      final String policyViolationId,
      final ApiWaiverOptionsDTO waiverOptionsDTO)
  {
    AbstractPolicyViolation abstractPolicyViolation = policyViolationDAO.getByIdWithConstraintFacts(policyViolationId);
    if (abstractPolicyViolation == null) {
      abstractPolicyViolation = repositoryPolicyViolationDAO.getByIdWithConstraintFacts(policyViolationId);
    }

    if (abstractPolicyViolation == null) {
      throw new NotFoundException("Could not find policy violation with ID " + policyViolationId + ".");
    }

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    if (!isViolationOwnerId(abstractPolicyViolation, internalOwnerId)) {
      throw new BadRequestException("Invalid owner id: " + ownerId);
    }

    ComponentMatcherStrategyForWaiver matcherStrategy;
    if (waiverOptionsDTO != null) {
      validateExistingPolicyWaiverReason(waiverOptionsDTO.waiverReasonId);

      if (waiverOptionsDTO.matcherStrategy != null) {
        matcherStrategy = waiverOptionsDTO.matcherStrategy;
      }
      else {
        matcherStrategy = waiverOptionsDTO.applyToAllComponents ? ALL_COMPONENTS : EXACT_COMPONENT;
      }
    }
    else {
      matcherStrategy = EXACT_COMPONENT;
    }

    String comment = waiverOptionsDTO == null ? null : waiverOptionsDTO.comment;
    Date expiryTime = waiverOptionsDTO == null ? null : waiverOptionsDTO.expiryTime;
    String waiverReasonId = waiverOptionsDTO == null ? null : waiverOptionsDTO.waiverReasonId;
    boolean expireWhenRemediationAvailable =
        waiverOptionsDTO != null && waiverOptionsDTO.expireWhenRemediationAvailable;

    validateExpiryTime(expiryTime);

    validateExpireWhenRemediationAvailable(expireWhenRemediationAvailable, matcherStrategy);

    addPolicyWaiver(ownerType, internalOwnerId, abstractPolicyViolation, comment, matcherStrategy,
        expiryTime, waiverReasonId, expireWhenRemediationAvailable);
  }

  /**
   * Creates policy waivers for multiple policy violations using the same waiver options
   *
   * @param ownerType The owner type
   * @param ownerId The owner ID
   * @param bulkWaiversDTO The waiver request containing violation IDs and waiver options
   */
  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public void addBulkPolicyWaivers(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.ID) final String ownerId,
      final ApiBulkWaiversDTO bulkWaiversDTO)
  {
    validateRequestData(bulkWaiversDTO);

    // Deduplicate violation IDs early for accurate validation and processing
    Set<String> uniqueViolationIds = new HashSet<>(bulkWaiversDTO.violationIds());
    validateUniqueViolationIds(uniqueViolationIds);

    validateExpiryTime(bulkWaiversDTO.apiWaiverOptionsDTO().expiryTime);
    validateExpireWhenRemediationAvailable(
        bulkWaiversDTO.apiWaiverOptionsDTO().expireWhenRemediationAvailable,
        bulkWaiversDTO.apiWaiverOptionsDTO().matcherStrategy);

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    Owner owner = ownerDAO.getById(internalOwnerId);

    List<AbstractPolicyViolation> allViolations = new ArrayList<>();

    for (String violationId : uniqueViolationIds) {
      try {
        AbstractPolicyViolation abstractPolicyViolation = policyViolationDAO.getByIdWithConstraintFacts(violationId);
        if (abstractPolicyViolation == null) {
          abstractPolicyViolation = repositoryPolicyViolationDAO.getByIdWithConstraintFacts(violationId);
        }

        if (abstractPolicyViolation == null) {
          throw new BadRequestException("Could not find policy violation with ID: " + violationId);
        }

        allViolations.add(abstractPolicyViolation);
      }
      catch (Exception e) {
        throw new BadRequestException("Error processing policy violation with ID: " + violationId);
      }
    }

    if (allViolations.isEmpty()) {
      throw new BadRequestException("No valid policy violations found for the provided violation IDs");
    }

    createBulkPolicyWaivers(owner, bulkWaiversDTO.apiWaiverOptionsDTO(), allViolations);
  }

  private void validateExpiryTime(final Date expiryTime) {
    if (Objects.nonNull(expiryTime) &&
        !expiryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(LocalDate.now()))
    {
      throw new BadRequestException("Expiration date must be in the future.");
    }
  }

  private void validateExpireWhenRemediationAvailable(
      final boolean expireWhenRemediationAvailable,
      final ComponentMatcherStrategyForWaiver matcherStrategy)
  {
    if (expireWhenRemediationAvailable && matcherStrategy != EXACT_COMPONENT) {
      throw new BadRequestException(
          "Expire When Remediation Available Waivers can only be applied to Exact Components.");
    }
  }

  private void validateRequestData(ApiBulkWaiversDTO bulkWaiversDTO) {
    if (bulkWaiversDTO == null) {
      throw new BadRequestException("Waivers request cannot be null");
    }

    if (bulkWaiversDTO.violationIds() == null || bulkWaiversDTO.violationIds().isEmpty()) {
      throw new BadRequestException("Violation IDs list cannot be null or empty");
    }

    if (bulkWaiversDTO.apiWaiverOptionsDTO() == null) {
      throw new BadRequestException("Waiver options cannot be null");
    }

    ComponentMatcherStrategyForWaiver matcherStrategy = bulkWaiversDTO.apiWaiverOptionsDTO().matcherStrategy;
    if (matcherStrategy == null) {
      throw new BadRequestException("Matcher strategy is required");
    }

    if (matcherStrategy != EXACT_COMPONENT && matcherStrategy != ALL_VERSIONS) {
      throw new BadRequestException("Only EXACT_COMPONENT and ALL_VERSIONS matcher " +
          "strategies are supported for bulk waivers");
    }
  }

  private void validateUniqueViolationIds(Set<String> uniqueViolationIds) {
    if (uniqueViolationIds.isEmpty()) {
      throw new BadRequestException("No unique violation IDs found");
    }

    if (uniqueViolationIds.size() > MAX_BULK_WAIVER_VIOLATIONS) {
      throw new BadRequestException("Maximum " + MAX_BULK_WAIVER_VIOLATIONS + " violations allowed per waiver request");
    }
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  void addPolicyWaiver(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final AbstractPolicyViolation abstractPolicyViolation,
      final String comment,
      final ComponentMatcherStrategyForWaiver matcherStrategy,
      final Date expiryTime,
      final String waiverReasonId,
      final boolean expireWhenRemediationAvailable)
  {
    // For repository policy violations, save the waiver under the repository (violation's owner), not the
    // synthetic application. The policy evaluator loads waivers by repository ID — saving under the synthetic
    // app ID would make the waiver invisible during re-evaluation.
    String effectiveOwnerId = (abstractPolicyViolation instanceof RepositoryPolicyViolation
        && OwnerType.APPLICATION.equals(ownerType))
            ? abstractPolicyViolation.getOwnerId()
            : ownerId;
    PolicyWaiver policyWaiver =
        savePolicyWaiver(effectiveOwnerId, abstractPolicyViolation, comment, matcherStrategy, expiryTime,
            waiverReasonId,
            expireWhenRemediationAvailable);
    auditAndSendTelemetry(ownerType, effectiveOwnerId, policyWaiver, abstractPolicyViolation);
  }

  void auditAndSendTelemetry(
      OwnerType ownerType,
      String ownerId,
      PolicyWaiver policyWaiver,
      AbstractPolicyViolation abstractPolicyViolation)
  {
    auditPolicyWaiver(policyWaiver);
    policyWaiverTelemetryCreator.sendWaiverTelemetryForOwnerType(policyWaiver, ownerType, abstractPolicyViolation);
    sendTelemetry(ownerType, ownerId);
  }

  public List<ApiPolicyWaiverDTO> getPolicyWaivers(OwnerType ownerType, String ownerId) {
    return getPolicyWaivers(ownerType, ownerId, null);
  }

  public List<ApiPolicyWaiverDTO> getPolicyWaivers(OwnerType ownerType, String ownerId, Integer expiringWithin) {
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    // Container-image waivers are stored under their container-image application (per approval
    // flow, matching direct-add). Calls scoped to the virtual REPOSITORY_CONTAINER_ID are the
    // Firewall Containers → Existing Waivers view — return all container-image waivers the
    // caller can see (filtered by their accessible container-image apps), regardless of the
    // waiver's raw owner_id. Firewall-scoped users lack READ at the container-container level;
    // the gate throws only when they have zero Firewall access, preserving 403 in that case.
    if (RepositoryContainer.REPOSITORY_CONTAINER_ID.equals(owner.getId())) {
      Set<String> accessibleContainerImageOwnerIds = resolveAccessibleContainerImageOwnerIds();
      return getContainerImageWaiversForFirewallView(owner, expiringWithin, accessibleContainerImageOwnerIds);
    }
    return getPolicyWaiversWithAuthzCheck(owner, expiringWithin);
  }

  private List<ApiPolicyWaiverDTO> getContainerImageWaiversForFirewallView(
      Owner owner,
      Integer expiringWithin,
      Set<String> accessibleContainerImageOwnerIds)
  {
    // accessibleContainerImageOwnerIds:
    // null → caller has container-level READ (admin / unscoped), no filter.
    // empty → scoped user with no matching container-image apps → empty result.
    // non-empty set → filter to those app IDs.
    List<PolicyWaiver> policyWaivers = policyWaiverDAO
        .getActiveContainerImageWaiversFilteredByApplicationIds(accessibleContainerImageOwnerIds);

    // Container-image waivers are stored with owner_id = container-image application id, so each row
    // must render its scope as the underlying application name (e.g. the image path), not the
    // virtual "Repository Managers" owner the caller addressed the list through.
    Set<String> ownerIds = policyWaivers.stream()
        .map(PolicyWaiver::getOwnerId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<String, Owner> ownersById = ownerIds.isEmpty()
        ? Collections.emptyMap()
        : applicationDAO.getByIds(ownerIds)
            .stream()
            .collect(Collectors.toMap(Application::getId, app -> app));
    List<ApiPolicyWaiverDTO> dtos = buildPolicyWaiverDTOsPerOwner(owner, ownersById, policyWaivers, expiringWithin);
    // Preserve audit behavior of the legacy per-owner query path: emit a VIEW_WAIVER sub-event
    // per waiver so audit logs still carry policyId/policyWaiverId/etc. for the Firewall
    // Containers → Existing Waivers view.
    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.VIEW_WAIVER, true)) {
      policyWaivers.forEach(this::auditPolicyWaiver);
    }
    return dtos;
  }

  private List<ApiPolicyWaiverDTO> buildPolicyWaiverDTOsPerOwner(
      Owner fallbackOwner,
      Map<String, Owner> ownersById,
      List<PolicyWaiver> policyWaivers,
      Integer expiringWithin)
  {
    if (expiringWithin != null) {
      if (expiringWithin <= 0) {
        throw new BadRequestException("expiringWithin must be a positive integer");
      }
      final Instant cutoff = Instant.now().plus(expiringWithin, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      policyWaivers = policyWaivers.stream()
          .filter(w -> w.getExpiryTime() != null
              && !w.getExpiryTime().toInstant().truncatedTo(ChronoUnit.DAYS).isAfter(cutoff))
          .collect(Collectors.toList());
    }

    List<String> waiverReasonIds = policyWaivers.stream().map(PolicyWaiver::getWaiverReasonId).collect(toList());
    List<PolicyWaiverReason> policyWaiverReasons =
        waiverReasonIds.isEmpty() ? new ArrayList<>() : policyWaiverReasonDAO.getAllByIds(waiverReasonIds);
    Map<String, PolicyWaiverReason> reasonsById = policyWaiverReasons.stream()
        .collect(Collectors.toMap(PolicyWaiverReason::getId, r -> r));

    Set<String> policyIds = policyWaivers.stream()
        .map(PolicyWaiver::getPolicyId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<String, Policy> policiesById = policyDAO.getByIds(policyIds)
        .stream()
        .collect(Collectors.toMap(Policy::getId, p -> p));

    List<ApiPolicyWaiverDTO> dtos = new ArrayList<>();
    for (PolicyWaiver policyWaiver : policyWaivers) {
      PolicyWaiverReason policyWaiverReason = reasonsById.get(policyWaiver.getWaiverReasonId());
      Owner perWaiverOwner = ownersById.getOrDefault(policyWaiver.getOwnerId(), fallbackOwner);
      ApiPolicyWaiverDTO dto = ApiPolicyWaiverDTO.toDto(policyWaiver, policyWaiverReason, perWaiverOwner);
      Policy policy = policiesById.get(policyWaiver.getPolicyId());
      if (policy != null) {
        dto.policyName = policy.getName();
        dto.threatLevel = policy.getThreatLevel();
      }
      dtos.add(dto);
    }
    return dtos;
  }

  @Authorize(permission = Permission.READ)
  List<ApiPolicyWaiverDTO> getPolicyWaiversWithAuthzCheck(
      @AuthzContext(Key.OWNER) Owner owner,
      Integer expiringWithin)
  {
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(owner.getId());

    if (expiringWithin != null) {
      if (expiringWithin <= 0) {
        throw new BadRequestException("expiringWithin must be a positive integer");
      }
      final Instant cutoff = Instant.now().plus(expiringWithin, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      policyWaivers = policyWaivers.stream()
          .filter(w -> w.getExpiryTime() != null &&
              !w.getExpiryTime().toInstant().truncatedTo(ChronoUnit.DAYS).isAfter(cutoff))
          .collect(Collectors.toList());
    }

    List<String> waiverReasonIds = policyWaivers.stream().map(PolicyWaiver::getWaiverReasonId).collect(toList());
    List<PolicyWaiverReason> policyWaiverReasons = new ArrayList<>();
    if (!waiverReasonIds.isEmpty()) {
      policyWaiverReasons = policyWaiverReasonDAO.getAllByIds(waiverReasonIds);
    }

    Set<String> policyIds = policyWaivers.stream()
        .map(PolicyWaiver::getPolicyId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<String, Policy> policiesById = policyDAO.getByIds(policyIds)
        .stream()
        .collect(Collectors.toMap(Policy::getId, p -> p));

    List<ApiPolicyWaiverDTO> apiPolicyWaiverDTOS = new ArrayList<>();
    for (PolicyWaiver policyWaiver : policyWaivers) {
      PolicyWaiverReason policyWaiverReason = policyWaiverReasons.stream()
          .filter(waiverReason -> waiverReason.getId().equals(policyWaiver.getWaiverReasonId()))
          .findFirst()
          .orElse(null);
      ApiPolicyWaiverDTO dto = ApiPolicyWaiverDTO.toDto(policyWaiver, policyWaiverReason, owner);
      Policy policy = policiesById.get(policyWaiver.getPolicyId());
      if (policy != null) {
        dto.policyName = policy.getName();
        dto.threatLevel = policy.getThreatLevel();
      }
      apiPolicyWaiverDTOS.add(dto);
    }

    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.VIEW_WAIVER, true)) {
      policyWaivers.forEach(this::auditPolicyWaiver);
    }
    return apiPolicyWaiverDTOS;
  }

  public ApiComponentPolicyWaiversDTO getTransitivePolicyWaiversByAppScanComponent(
      OwnerType ownerType,
      String ownerId,
      String scanId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash)
  {
    if (!OwnerType.APPLICATION.equals(ownerType)) {
      throw new BadRequestException("scanId can only be specified for an application.");
    }
    if (componentIdentifier == null && packageUrl == null && hash == null) {
      throw new BadRequestException("componentIdentifier or packageUrl or hash must be specified.");
    }
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    checkOwnerReadAuthz(owner);
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(owner.getId(), scanId);
    if (policyEvaluation == null) {
      throw new NotFoundException("scanId " + scanId + " not found for application " + owner.getPublicId() + ".");
    }
    List<Component> transitiveComponents = apiPolicyViolationServiceV2.getTransitiveComponentsByAppScanComponent(
        owner.getId(), scanId, componentIdentifier, packageUrl, hash);
    return getPolicyWaivers(owner, transitiveComponents.stream());
  }

  @Authorize(permission = Permission.READ)
  void checkOwnerReadAuthz(@SuppressWarnings("unused") @AuthzContext(Key.OWNER) Owner owner) {
    // permission checked by annotations, no method body needed
  }

  // Visible for testing
  ApiComponentPolicyWaiversDTO getPolicyWaivers(Owner owner, Stream<Component> components) {
    // Add a component with a null hash to get policy waivers that apply to any component
    components = Stream.concat(Stream.of(new Component()), components);
    ApiComponentPolicyWaiversDTO result = new ApiComponentPolicyWaiversDTO();
    Map<String, String> policyNameById = new HashMap<>();
    Map<String, Owner> ownerById = new HashMap<>();
    ownerDAO.walkHierarchy(owner)
        .forEach(ownerInHierarchy -> ownerById.put(ownerInHierarchy.getId(), ownerInHierarchy));
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      components.forEach(component -> {
        PackageUrlIdentifier purl = null;
        if (component.getComponentIdentifier() != null) {
          purl = PackageUrlIdentifier.fromComponentIdentifier(component.getComponentIdentifier());
        }
        for (Owner ownerInHierarchy : ownerById.values()) {
          // For the given owner, add policy waivers that apply to the component (i.e. they match its hash)
          // If the hash is null, then the policy waiver applies to any component
          List<PolicyWaiver> waivers;
          if (component.getHash() == null) {
            waivers = policyWaiverDAO.getActiveByOwnerIdAndHash(tx, ownerInHierarchy.getId(), component.getHash(),
                ALL_COMPONENTS);
            if (purl != null) {
              waivers.addAll(
                  policyWaiverDAO.getApplicableToComponentOnlyAllVersions(tx, ownerInHierarchy.getId(), purl));
            }
          }
          else {
            waivers = policyWaiverDAO.getActiveByOwnerIdAndHash(tx, ownerInHierarchy.getId(), component.getHash(),
                EXACT_COMPONENT);
          }
          waivers.forEach(policyWaiver -> addApiPolicyWaiverDTO(tx, result, policyNameById, ownerById, policyWaiver,
              component.getDisplayName()));
        }
      });
    }
    return result;
  }

  private void addApiPolicyWaiverDTO(
      TransactionContext tx,
      ApiComponentPolicyWaiversDTO apiComponentPolicyWaiversDTO,
      Map<String, String> policyNameById,
      Map<String, Owner> ownerById,
      PolicyWaiver policyWaiver,
      String componentName)
  {
    policyNameById.putIfAbsent(policyWaiver.getPolicyId(), policyDAO.getById(tx, policyWaiver.getPolicyId()).getName());
    PolicyWaiverReason policyWaiverReason = null;
    if (policyWaiver.getWaiverReasonId() != null) {
      policyWaiverReason = policyWaiverReasonDAO.getById(policyWaiver.getWaiverReasonId());
    }
    apiComponentPolicyWaiversDTO.componentPolicyWaivers.add(
        convert(policyWaiver, policyNameById, ownerById, componentName, policyWaiverReason));
  }

  private ApiPolicyWaiverDTO convert(
      PolicyWaiver policyWaiver,
      Map<String, String> policyNameById,
      Map<String, Owner> ownerById,
      String componentName,
      PolicyWaiverReason policyWaiverReason)
  {
    Owner owner = ownerById.get(policyWaiver.getOwnerId());
    ApiPolicyWaiverDTO result = ApiPolicyWaiverDTO.toDto(policyWaiver, policyWaiverReason, owner);
    result.policyName = policyNameById.get(policyWaiver.getPolicyId());
    result.constraintFacts = policyWaiver.getConstraintFacts();
    result.constraintFactsJson = policyWaiver.getConstraintFactsJson();
    result.componentName = componentName;
    return result;
  }

  public void deletePolicyWaiver(OwnerType ownerType, String ownerId, String policyWaiverId) {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    // Callers that address a container-image waiver via the virtual REPOSITORY_CONTAINER_ID scope
    // will fail the ownerId equality check inside deletePolicyWaiverWithAuthzCheck (waivers now
    // live under their container-image application). Look the waiver up and re-authorize against
    // its actual application owner in that case.
    if (RepositoryContainer.REPOSITORY_CONTAINER_ID.equals(internalOwnerId)) {
      PolicyWaiver policyWaiver = policyWaiverDAO.getById(policyWaiverId);
      if (policyWaiver != null && policyWaiver.isForContainerImage()) {
        deletePolicyWaiverWithAuthzCheck(OwnerType.APPLICATION, policyWaiver.getOwnerId(), policyWaiverId);
        return;
      }
    }
    deletePolicyWaiverWithAuthzCheck(ownerType, internalOwnerId, policyWaiverId);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  void deletePolicyWaiverWithAuthzCheck(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String policyWaiverId)
  {
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull(policyWaiverId);
    if (!internalOwnerId.equals(policyWaiver.getOwnerId())) {
      throw new NotFoundException("Cannot find a policy waiver with ID " + policyWaiverId + " for " + ownerType
          + " with ID " + internalOwnerId);
    }
    auditPolicyWaiver(policyWaiver);
    policyWaiverDAO.delete(policyWaiver);
  }

  private void auditPolicyWaiver(PolicyWaiver policyWaiver) {
    try (TransactionContext tx = policyDAO.createTransactionContext()) {
      auditPolicyWaiver(policyWaiver, tx);
    }
  }

  private void auditPolicyWaiver(PolicyWaiver policyWaiver, TransactionContext tx) {
    AuditData.get()
        .setData("policyWaiverId", policyWaiver.getId())
        .setPolicy(policyDAO.getById(tx, policyWaiver.getPolicyId()))
        .setComponentHash(policyWaiver.getHash())
        .setData("expiryTime", policyWaiver.getExpiryTime())
        .setComment(policyWaiver.getComment())
        .setData("isForContainerImageComponent", policyWaiver.isForContainerImageComponent())
        .setData("isForContainerImage", policyWaiver.isForContainerImage());

    if (policyWaiver.getConstraintFacts() != null) {
      AuditData.get()
          .setData("policyConstraints",
              policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(toList()));
    }
  }

  private void sendTelemetry(OwnerType ownerType, String ownerId) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.POLICY_WAIVER_API);
    telemetryData.getAttributes().put(OWNER_TYPE_ATTR, ownerType.toString());
    telemetryData.getAttributes().put(OWNER_ID_ATTR, telemetryUtils.obfuscate(ownerId));
    telemetryUtils.includeRealOwnerId(telemetryData.getAttributes(), ownerId);
    telemetrySender.send(telemetryData);
  }

  private boolean isViolationOwnerId(AbstractPolicyViolation policyViolation, String ownerId) {
    for (Owner owner : ownerDAO.walkHierarchy(policyViolation.getOwnerId())) {
      if (owner.getId().equals(ownerId)) {
        return true;
      }
    }
    // For repository policy violations, also check the synthetic application hierarchy.
    // The synthetic app is created per-component by ApplicationForHostedRepositoryComponentService
    // and is not in the repository owner hierarchy, but is a valid waiver target.
    if (policyViolation instanceof RepositoryPolicyViolation) {
      Repository repository = repositoryDAO.getById(policyViolation.getOwnerId());
      if (repository != null) {
        String pathname = ((RepositoryPolicyViolation) policyViolation).getPathname();
        String appPublicId = ApplicationForHostedRepositoryComponentService
            .generatePublicId(repository.getPublicId(), pathname);
        Application syntheticApp = applicationDAO.getByPublicId(appPublicId);
        if (syntheticApp != null) {
          // Walk the synthetic app's full hierarchy (app → org → parent orgs), stopping before
          // ROOT_ORGANIZATION_ID to prevent a cross-tenant owner ID that happens to be in a
          // shared root from matching a different tenant's synthetic app.
          for (Owner owner : ownerDAO.walkHierarchy(syntheticApp.getId())) {
            if (Organization.ROOT_ORGANIZATION_ID.equals(owner.getId())) {
              break;
            }
            if (owner.getId().equals(ownerId)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * @since 1.98
   */
  public ApiPolicyWaiversApplicableToViolationDTO getApplicableWaivers(final String violationId) {
    // The violationId may reference an application policy violation or a repository policy violation
    final AbstractPolicyViolation policyViolation = getAbstractPolicyViolation(violationId);

    String policyId = policyViolation.getPolicyId();
    String constraintFactsJson = policyViolation.getConstraintFactsJson();
    List<ConstraintFact> constraintFacts = policyViolation.getConstraintFacts();
    String hash = policyViolation.getHash();
    String ownerId = policyViolation.getOwnerId();
    ComponentIdentifier componentIdentifier = policyViolation.getComponentIdentifier();

    Owner owner = ownerDAO.getById(ownerId);
    final var policyWaiversReasons =
        policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    Map<Boolean, List<ApiPolicyWaiverDTO>> applicableWaivers =
        getByOwnerHierarchyAndPolicyIdWithReadPermission(owner, policyId)
            .stream()
            .filter(policyWaiver -> filterWaiverByCriteria(constraintFactsJson, constraintFacts,
                componentIdentifier, hash, policyWaiver))
            .map(policyWaiver -> ApiPolicyWaiverDTO.toDto(
                policyWaiver,
                policyWaiversReasons.get(policyWaiver.getWaiverReasonId()),
                ownerDAO.getById(policyWaiver.getOwnerId()),
                violationId))
            .collect(partitioningBy(dto -> hasWaiverExpired(dto.expiryTime), toList()));

    ApiPolicyWaiversApplicableToViolationDTO apiPolicyWaivers = new ApiPolicyWaiversApplicableToViolationDTO();
    apiPolicyWaivers.activeWaivers = applicableWaivers.get(Boolean.FALSE);
    apiPolicyWaivers.expiredWaivers = applicableWaivers.get(Boolean.TRUE);

    return apiPolicyWaivers;
  }

  public List<ApiPolicyWaiverDTO> getSimilarWaivers(final String violationId) {
    // The violationId may reference an application policy violation or a repository policy violation
    final AbstractPolicyViolation policyViolation = getAbstractPolicyViolation(violationId);

    // Waiver is created for the same policy ID
    // Should include expired waivers
    // Waivers are not limited to current scope - query across all (orgs and apps)
    String policyId = policyViolation.getPolicyId();
    List<PolicyWaiver> waiversForPolicy = policyWaiverDAO.getByPolicyId(policyId);

    // User has view permission for the waiver
    Map<String, Owner> availableOwners = ownerService.getOwnersWithReadPermissionsById();
    Predicate<PolicyWaiver> userHasViewPermissionOnWaiverOwner =
        policyWaiver -> availableOwners.containsKey(policyWaiver.getOwnerId());

    // Waivers that are applicable to the current component (any version)
    // Exact waivers for the same component (hash)
    // Waivers for any version of the same component
    // “All component” waivers
    final ComponentFact componentFact =
        new ComponentFact(policyViolation.getComponentIdentifier(), policyViolation.getHash());
    Predicate<PolicyWaiver> waiverMatchesComponentOrAnyVersionOfIt =
        policyWaiver -> new PolicyWaiverMatcherWrapper(policyWaiver).matchesComponentOrAnyVersionOfComponent(
            componentFact);

    // For security violations, we also need to limit waivers to the same Vulnerability ID
    Predicate<PolicyWaiver> securityWaiverAppliesToSameVulnerabilityId = policyWaiver -> true;
    if (PolicyThreatCategory.SECURITY.equals(policyViolation.getThreatCategory())) {
      final Optional<String> policyViolationSecurityVulnerabilityId =
          findFirstTriggerReference(policyViolation.getConstraintFacts().stream());

      if (policyViolationSecurityVulnerabilityId.isPresent()) {
        securityWaiverAppliesToSameVulnerabilityId = policyWaiver -> {
          final List<ConstraintFact> constraintFacts = policyWaiver.getConstraintFacts();

          final Optional<String> firstTriggerReference = Objects.nonNull(constraintFacts)
              ? findFirstTriggerReference(policyWaiver.getConstraintFacts().stream())
              : Optional.empty();

          return policyViolationSecurityVulnerabilityId.equals(firstTriggerReference);
        };
      }
    }

    // Should exclude Applicable waivers (waivers shown in Applicable Waivers table)
    ApiPolicyWaiversApplicableToViolationDTO applicableWaiversDTO = getApplicableWaivers(violationId);
    List<String> applicableWaiversIds =
        Stream.of(applicableWaiversDTO.activeWaivers, applicableWaiversDTO.expiredWaivers)
            .flatMap(Collection::stream)
            .map(apiPolicyWaiverDTO -> apiPolicyWaiverDTO.policyWaiverId)
            .collect(toList());
    Predicate<PolicyWaiver> isNotAnApplicableWaiver =
        policyWaiver -> !applicableWaiversIds.contains(policyWaiver.getId());

    final var policyWaiversReasons =
        policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    return waiversForPolicy.stream()
        .filter(userHasViewPermissionOnWaiverOwner)
        .filter(isNotAnApplicableWaiver) // higher in filter hierarchy since it's a lighter filter to process
        .filter(waiverMatchesComponentOrAnyVersionOfIt)
        .filter(securityWaiverAppliesToSameVulnerabilityId) // saved for the end as it requires more processing
        .map(policyWaiver -> ApiPolicyWaiverDTO.toDtoWithConstraints(
            policyWaiver,
            policyWaiversReasons.get(policyWaiver.getWaiverReasonId()),
            availableOwners.get(policyWaiver.getOwnerId()),
            violationId))
        .collect(toList());
  }

  @NotNull
  private AbstractPolicyViolation getAbstractPolicyViolation(final String violationId) {
    AbstractPolicyViolation policyViolation = policyViolationDAO.getByIdWithConstraintFacts(violationId);
    if (policyViolation == null) {
      policyViolation = repositoryPolicyViolationDAO.getByIdWithConstraintFacts(violationId);
      if (policyViolation == null) {
        throw new NotFoundException("Could not find policy violation with ID " + violationId + ".");
      }
    }
    return policyViolation;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public void addWaiverToTransitivePolicyViolationsByAppScanComponent(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.ID) String ownerId,
      String scanId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      ApiWaiverOptionsDTO apiWaiverOptionsDTO)
  {
    ApiWaiverOptionsDTO waiverDTO = apiWaiverOptionsDTO != null ? apiWaiverOptionsDTO : new ApiWaiverOptionsDTO();
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    Pair<Component, List<Pair<PolicyViolation, Component>>> pair = apiPolicyViolationServiceV2
        .getTransitivePolicyViolationsForLastEvaluation(owner.getId(), scanId, componentIdentifier, packageUrl, hash);

    Component component = pair.getLeft();
    List<PolicyViolation> policyViolations = pair.getRight()
        .stream()
        .map(Pair::getLeft)
        .collect(Collectors.toList());

    AuditData.get()
        .setScanId(scanId)
        .setComponentIdentifier(component.getComponentIdentifier())
        .setComponentHash(component.getHash())
        .setComment(waiverDTO.comment)
        .setData("expiryTime", waiverDTO.expiryTime);

    createPolicyWaivers(owner, waiverDTO, policyViolations);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public void addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      String stageId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      ApiWaiverOptionsDTO apiWaiverOptionsDTO)
  {
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    String stageIdLowercase = stageId.toLowerCase(Locale.ROOT);
    if (!Stage.isValidStageTypeId(stageIdLowercase)) {
      throw new InvalidStageException(stageId);
    }
    List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getLastByApplicationIdsAndStageIds(
        ownerDAO.getDescendantOrSelfApplicationIds(owner), Collections.singleton(stageIdLowercase));
    Pair<Component, List<Pair<PolicyViolation, Component>>> pair = apiPolicyViolationServiceV2
        .getTransitivePolicyViolationsByComponent(stageId, componentIdentifier, packageUrl, hash, policyEvaluations);
    Component component = pair.getLeft();
    List<PolicyViolation> policyViolations = pair.getRight()
        .stream()
        .map(Pair::getLeft)
        .collect(Collectors.toList());
    ApiWaiverOptionsDTO waiverDTO = apiWaiverOptionsDTO != null ? apiWaiverOptionsDTO : new ApiWaiverOptionsDTO();
    AuditData.get()
        .setStageId(stageId)
        .setComponentIdentifier(component.getComponentIdentifier())
        .setComponentHash(component.getHash())
        .setComment(waiverDTO.comment)
        .setData("expiryTime", waiverDTO.expiryTime);
    createPolicyWaivers(owner, waiverDTO, policyViolations);
  }

  private void createPolicyWaivers(
      Owner owner,
      ApiWaiverOptionsDTO waiverDTO,
      List<PolicyViolation> policyViolations)
  {
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      createPolicyWaiversInternal(owner, waiverDTO, policyViolations, false, false, tx);
      tx.commit();
    }
  }

  private void createBulkPolicyWaivers(
      Owner owner,
      ApiWaiverOptionsDTO waiverDTO,
      List<AbstractPolicyViolation> abstractPolicyViolations)
  {
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      createBulkWaiversInternal(owner, waiverDTO, abstractPolicyViolations, tx);
      tx.commit();
    }
  }

  private List<PolicyWaiver> createPolicyWaiversInternal(
      Owner owner,
      ApiWaiverOptionsDTO waiverDTO,
      List<PolicyViolation> policyViolations,
      boolean isForContainerImageComponent,
      boolean isForContainerImage,
      TransactionContext tx)
  {
    waiverDTO.matcherStrategy = waiverDTO.matcherStrategy != null ? waiverDTO.matcherStrategy : EXACT_COMPONENT;
    validateExistingPolicyWaiverReason(waiverDTO.waiverReasonId);

    List<PolicyWaiver> created = new ArrayList<>();
    for (PolicyViolation policyViolation : policyViolations) {
      try {
        PolicyWaiver policyWaiver = savePolicyWaiverInternal(
            tx,
            owner.getId(),
            policyViolation,
            waiverDTO.comment,
            waiverDTO.matcherStrategy,
            waiverDTO.expiryTime,
            waiverDTO.waiverReasonId,
            waiverDTO.expireWhenRemediationAvailable,
            isForContainerImageComponent,
            isForContainerImage);
        created.add(policyWaiver);
        policyWaiverTelemetryCreator.sendWaiverTelemetryForOwnerType(
            policyWaiver, owner.getType(), policyViolation);
        try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CREATE_WAIVER, false)) {
          auditPolicyWaiver(policyWaiver, tx);
        }
      }
      catch (BadRequestException e) {
        // Failure on the image-level pass breaks the container-image contract — the helper will
        // throw InternalServerException to roll back the transaction. Log at ERROR there.
        // Other passes (per-component container-image, transitive-violation callers) treat a
        // duplicate as a benign skip and log at WARN.
        if (isForContainerImage) {
          log.error("Unable to add waiver for PolicyViolation ID {}", policyViolation.getId(), e);
        }
        else {
          log.warn("Unable to add waiver for PolicyViolation ID {}", policyViolation.getId(), e);
        }
      }
    }

    AuditData.get().commitSubEvents();
    sendTelemetry(owner.getType(), owner.getPublicId());
    return created;
  }

  private void createBulkWaiversInternal(
      Owner owner,
      ApiWaiverOptionsDTO waiverDTO,
      List<AbstractPolicyViolation> abstractPolicyViolations,
      TransactionContext tx)
  {
    waiverDTO.matcherStrategy = waiverDTO.matcherStrategy != null ? waiverDTO.matcherStrategy : EXACT_COMPONENT;
    validateExistingPolicyWaiverReason(waiverDTO.waiverReasonId);

    Map<PolicyWaiver, AbstractPolicyViolation> successfulWaivers = new HashMap<>();

    for (AbstractPolicyViolation abstractPolicyViolation : abstractPolicyViolations) {
      try {
        // For repository policy violations coming from a synthetic-app owner, save under the repository
        // ID so the policy evaluator can find the waiver during re-evaluation (it loads by repository ID).
        String effectiveOwnerId = (abstractPolicyViolation instanceof RepositoryPolicyViolation
            && OwnerType.APPLICATION.equals(owner.getType()))
                ? abstractPolicyViolation.getOwnerId()
                : owner.getId();
        PolicyWaiver policyWaiver = savePolicyWaiverInternal(
            tx,
            effectiveOwnerId,
            abstractPolicyViolation,
            waiverDTO.comment,
            waiverDTO.matcherStrategy,
            waiverDTO.expiryTime,
            waiverDTO.waiverReasonId,
            waiverDTO.expireWhenRemediationAvailable,
            false,
            false);
        successfulWaivers.put(policyWaiver, abstractPolicyViolation);
      }
      catch (Exception e) {
        if (e instanceof BadRequestException && e.getMessage() != null &&
            e.getMessage().contains("This policy waiver already exists."))
        {
          // Log duplicate waiver and continue processing other violations
          log.debug("Skipping duplicate waiver for PolicyViolation ID {}: {}",
              abstractPolicyViolation.getId(), e.getMessage());
        }
        else {
          // All other exceptions should fail the entire operation
          successfulWaivers.clear();
          throw new InternalServerException("Unable to add waiver for PolicyViolation: "
              + abstractPolicyViolation.getId(), e);
        }
      }
    }

    if (!successfulWaivers.isEmpty()) {
      for (PolicyWaiver policyWaiver : successfulWaivers.keySet()) {
        policyWaiverTelemetryCreator.sendWaiverTelemetryForOwnerType(
            policyWaiver, owner.getType(), successfulWaivers.get(policyWaiver));
        try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CREATE_WAIVER, false)) {
          auditPolicyWaiver(policyWaiver, tx);
        }
      }
    }

    AuditData.get().commitSubEvents();
    sendTelemetry(owner.getType(), owner.getPublicId());
  }

  private void validateExistingPolicyWaiverReason(String waiverReasonId) {
    if (waiverReasonId != null && isNull(policyWaiverReasonDAO.getById(waiverReasonId))) {
      throw new BadRequestException("Waiver reason not found");
    }
  }

  private boolean filterWaiverByCriteria(
      String constraintFactsJson,
      List<ConstraintFact> constraintFacts,
      ComponentIdentifier componentIdentifier,
      String hash,
      PolicyWaiver policyWaiver)
  {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    ComponentFact componentFact = new ComponentFact(componentIdentifier, hash);

    return policyWaiverMatcherWrapper.matchesComponent(componentFact) &&
        (policyWaiverMatcherWrapper.matchesConstraintFactsJson(constraintFactsJson) ||
            policyWaiverMatcherWrapper.matchesConstraintFacts(constraintFacts));
  }

  private boolean hasWaiverExpired(Date expiryTime) {
    return expiryTime != null && expiryTime.before(new Date());
  }

  @Authorize(permission = Permission.READ)
  List<PolicyWaiver> getByOwnerHierarchyAndPolicyIdWithReadPermission(
      @AuthzContext(Key.OWNER) Owner owner,
      String policyId)
  {
    return policyWaiverDAO.getByOwnerHierarchyAndPolicyId(owner, policyId);
  }

  private PolicyWaiver savePolicyWaiver(
      String ownerId,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime,
      String policyWaiverReasonId,
      boolean expireWhenRemediationAvailable)
  {
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      PolicyWaiver policyWaiver =
          savePolicyWaiver(tx, ownerId, abstractPolicyViolation, comment, matcherStrategy, expiryTime,
              policyWaiverReasonId, expireWhenRemediationAvailable);
      tx.commit();
      return policyWaiver;
    }
  }

  PolicyWaiver savePolicyWaiver(
      TransactionContext tx,
      String ownerId,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime,
      String policyWaiverReasonId,
      boolean expireWhenRemediationAvailable)
  {
    return savePolicyWaiverInternal(
        tx,
        ownerId,
        abstractPolicyViolation,
        comment,
        matcherStrategy,
        expiryTime,
        policyWaiverReasonId,
        expireWhenRemediationAvailable,
        false, // Default value for isForContainerImageComponent
        false // Default value for isForContainerImage
    );
  }

  PolicyWaiver savePolicyWaiver(
      TransactionContext tx,
      String ownerId,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime,
      String policyWaiverReasonId,
      boolean expireWhenRemediationAvailable,
      boolean isForContainerImageComponent,
      boolean isForContainerImage)
  {
    return savePolicyWaiverInternal(
        tx,
        ownerId,
        abstractPolicyViolation,
        comment,
        matcherStrategy,
        expiryTime,
        policyWaiverReasonId,
        expireWhenRemediationAvailable,
        isForContainerImageComponent,
        isForContainerImage);
  }

  private PolicyWaiver savePolicyWaiverInternal(
      TransactionContext tx,
      String ownerId,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime,
      String policyWaiverReasonId,
      boolean expireWhenRemediationAvailable,
      boolean isForContainerImageComponent,
      boolean isForContainerImage)
  {
    String hash = (matcherStrategy == ALL_COMPONENTS || matcherStrategy == ALL_VERSIONS)
        ? null
        : abstractPolicyViolation.getHash();

    PolicyWaiver policyWaiver = new PolicyWaiver(hash, abstractPolicyViolation.getPolicyId(), ownerId, comment);
    policyWaiver.setConstraintFactsJson(abstractPolicyViolation.getConstraintFactsJson());
    policyWaiver.setExpiryTime(expiryTime);
    policyWaiver.setCreatorId(currentUser.getUserPrincipal().getUsername());
    policyWaiver.setCreatorName(currentUser.getUserPrincipal().getDisplayName());
    policyWaiver.setComponentMatchStrategy(matcherStrategy);
    policyWaiver.setExpireWhenRemediationAvailable(expireWhenRemediationAvailable);
    policyWaiver.setWaiverReasonId(policyWaiverReasonId);
    policyWaiver.setForContainerImageComponent(isForContainerImageComponent);
    policyWaiver.setForContainerImage(isForContainerImage);

    if (matcherStrategy != ALL_COMPONENTS && abstractPolicyViolation.getComponentIdentifier() != null) {
      policyWaiver.setAssociatedPackageUrl(toPackageUrl(abstractPolicyViolation.getComponentIdentifier()));
    }

    policyWaiverDAO.insert(tx, policyWaiver);
    return policyWaiver;
  }

  public ApiPolicyWaiverDTO getPolicyWaiver(OwnerType ownerType, String ownerId, String policyWaiverId) {
    return getPolicyWaiverWithAuthzCheck(idUtils.getOwnerNotNull(ownerType, ownerId),
        policyWaiverId);
  }

  /**
   * Retrieve waiver details using Firewall repository-level access instead of owner-level READ.
   *
   * <p>
   * For scoped users ({@code permittedRepositoryIds} non-null), the owner must be reachable
   * from their permitted repositories:
   * <ul>
   * <li>{@code REPOSITORY}: owner ID must be in {@code permittedRepositoryIds}</li>
   * <li>{@code APPLICATION}: the app's shadow organization must link back to a permitted repo
   * via {@code relatedRepositoryId}</li>
   * <li>{@code ORGANIZATION}, {@code REPOSITORY_MANAGER}, {@code REPOSITORY_CONTAINER}:
   * accessible to any user with at least one permitted repository (these are org-level
   * owners whose waivers apply across the entire Firewall scope)</li>
   * </ul>
   *
   * <p>
   * Full-access users ({@code permittedRepositoryIds == null}) bypass scope validation.
   *
   * @throws UnauthorizedException if the owner is outside the user's permitted scope
   */
  public ApiPolicyWaiverDTO getPolicyWaiverForFirewall(
      OwnerType ownerType,
      String ownerId,
      String policyWaiverId,
      Set<String> permittedRepositoryIds)
  {
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    if (permittedRepositoryIds != null) {
      checkOwnerInFirewallScope(owner, permittedRepositoryIds);
    }
    // The Firewall Containers → Existing Waivers list returns container-image waivers under the
    // virtual REPOSITORY_CONTAINER_ID scope for uniformity, but each waiver row is actually
    // stored under its container-image application. Re-resolve the owner from the waiver row so
    // the standard getPolicyWaiverInternal ownerId equality check succeeds.
    if (RepositoryContainer.REPOSITORY_CONTAINER_ID.equals(owner.getId())) {
      // Use the nullable lookup here: if the waiver ID does not exist, fall through with the
      // original REPOSITORY_CONTAINER owner and let getPolicyWaiverInternal below produce the
      // standard NotFoundException. Using getByIdNotNull would work at runtime, but avoiding the
      // extra "not found" throw here also lets tests that don't exercise the container-image
      // branch skip mocking this lookup.
      PolicyWaiver stored = policyWaiverDAO.getById(policyWaiverId);
      if (stored != null && stored.isForContainerImage()) {
        owner = idUtils.getOwnerNotNull(OwnerType.APPLICATION, stored.getOwnerId());
        // Re-check scope on the resolved container-image APPLICATION. The initial check above
        // saw owner=REPOSITORY_CONTAINER_ID (default branch = allow), so without this second
        // check a scoped Firewall user could fetch any container-image waiver by ID via the
        // REPOSITORY_CONTAINER_ID URL, regardless of which Docker repo it belongs to.
        if (permittedRepositoryIds != null) {
          checkOwnerInFirewallScope(owner, permittedRepositoryIds);
        }
      }
    }
    return getPolicyWaiverInternal(owner, policyWaiverId);
  }

  /**
   * Verifies the owner is reachable from the scoped user's permitted repository IDs.
   * Only called for scoped users (permittedRepositoryIds is non-null and non-empty).
   */
  private void checkOwnerInFirewallScope(Owner owner, Set<String> permittedRepositoryIds) {
    switch (owner.getType()) {
      case REPOSITORY:
        if (!permittedRepositoryIds.contains(owner.getId())) {
          throw new UnauthorizedException(
              "Access denied");
        }
        break;
      case APPLICATION:
        // Container image apps live under shadow orgs whose relatedRepositoryId links to the docker proxy repo.
        // owner was already resolved via idUtils.getOwnerNotNull() — cast is safe, no second DB fetch needed.
        Set<String> appOrgIds = organizationDAO.getOrganizationIdsByRelatedRepositoryIds(permittedRepositoryIds);
        Application app = (Application) owner;
        if (!appOrgIds.contains(app.getOrganizationId())) {
          throw new UnauthorizedException(
              "Access denied");
        }
        break;
      default:
        // ORGANIZATION, REPOSITORY_MANAGER, REPOSITORY_CONTAINER: org-level and RM-level waivers
        // are intentionally accessible to any scoped Firewall user. The Firewall Dashboard waiver
        // list already surfaces these waivers to scoped users; denying the detail would be
        // inconsistent. This breadth is a deliberate design decision for the Firewall use case.
        break;
    }
  }

  @Authorize(permission = Permission.READ)
  ApiPolicyWaiverDTO getPolicyWaiverWithAuthzCheck(
      @AuthzContext(Key.OWNER) Owner owner,
      String policyWaiverId)
  {
    return getPolicyWaiverInternal(owner, policyWaiverId);
  }

  private ApiPolicyWaiverDTO getPolicyWaiverInternal(Owner owner, String policyWaiverId) {
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdAndOwnerIdNotNull(policyWaiverId, owner.getId());
    PolicyWaiverReason policyWaiverReason = policyWaiverReasonDAO.getById(policyWaiver.getWaiverReasonId());
    ApiPolicyWaiverDTO apiPolicyWaiverDTO = ApiPolicyWaiverDTO.toDto(policyWaiver, policyWaiverReason, owner);
    if (policyWaiver.getLastRenewalReasonId() != null) {
      PolicyWaiverReason lastRenewalReason = policyWaiverReasonDAO.getById(policyWaiver.getLastRenewalReasonId());
      if (lastRenewalReason != null) {
        apiPolicyWaiverDTO.lastRenewalReasonText = lastRenewalReason.getReasonText();
      }
    }
    augmentPolicyWaiverDtoWithExtraInformation(apiPolicyWaiverDTO, policyWaiver);
    auditPolicyWaiver(policyWaiver);
    return apiPolicyWaiverDTO;
  }

  private void augmentPolicyWaiverDtoWithExtraInformation(
      ApiPolicyWaiverDTO apiPolicyWaiverDTO,
      PolicyWaiver policyWaiver)
  {
    Policy policy = policyDAO.getById(policyWaiver.getPolicyId());

    apiPolicyWaiverDTO.policyName = policy.getName();
    apiPolicyWaiverDTO.threatLevel = policy.getThreatLevel();
    apiPolicyWaiverDTO.constraintFactsJson = policyWaiver.getConstraintFactsJson();
    apiPolicyWaiverDTO.constraintFacts = policyWaiver.getConstraintFacts();
  }

  public void updatePolicyWaiver(
      final OwnerType ownerType,
      final String ownerId,
      final String policyWaiverId,
      final ApiWaiverOptionsDTO dto)
  {
    updatePolicyWaiver(idUtils.getOwnerNotNull(ownerType, ownerId), policyWaiverId, dto);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  void updatePolicyWaiver(
      @AuthzContext(Key.OWNER) final Owner owner,
      final String policyWaiverId,
      final ApiWaiverOptionsDTO dto)
  {
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdAndOwnerIdNotNull(policyWaiverId, owner.getId());
    if (!Objects.equals(dto.matcherStrategy, policyWaiver.getComponentMatchStrategy())) {
      throw new BadRequestException("Matcher strategy cannot be updated.");
    }
    policyWaiver.setComment(dto.comment);
    policyWaiver.setExpiryTime(dto.expiryTime);
    policyWaiver.setExpireWhenRemediationAvailable(dto.expireWhenRemediationAvailable);
    policyWaiver.setWaiverReasonId(dto.waiverReasonId);
    validate(policyWaiver);
    auditPolicyWaiver(policyWaiver);
    policyWaiverDAO.update(policyWaiver);
  }

  private void validate(final PolicyWaiver policyWaiver) {
    validateExpiryTime(policyWaiver.getExpiryTime());
    validateExistingPolicyWaiverReason(policyWaiver.getWaiverReasonId());
    validateExpireWhenRemediationAvailable(policyWaiver.isExpireWhenRemediationAvailable(),
        policyWaiver.getComponentMatchStrategy());
  }

  /**
   * Creates the canonical container-image waiver set: one EXACT_COMPONENT waiver per active
   * proxy/fail violation in the image (with isForContainerImageComponent=true), plus a single
   * ALL_COMPONENTS waiver at the image level (with isForContainerImage=true). Both kinds are
   * required so {@link PolicyWaiverDAO#getAllForContainerImageByOwnerId(String)} returns the
   * set and the matcher can re-apply it during re-evaluation.
   *
   * <p>
   * Audits and telemetry for each individual waiver are emitted inside
   * {@link #createPolicyWaiversInternal}. The caller owns the transaction so additional state
   * changes (e.g. updating the originating {@code PolicyWaiverRequest}) can be bundled atomically.
   *
   * <p>
   * The passed {@code waiverOptions} is mutated during execution: {@code matcherStrategy} ends up
   * as {@code ALL_COMPONENTS} and {@code expireWhenRemediationAvailable} as {@code false}. Pass a
   * fresh DTO; do not reuse it after this call.
   *
   * @return the image-level ({@code ALL_COMPONENTS}, {@code isForContainerImage=true}) waiver, so the
   *         caller can link it from a {@code PolicyWaiverRequest}.
   */
  PolicyWaiver applyContainerImageWaivers(
      String containerImageApplicationId,
      ApiWaiverOptionsDTO waiverOptions,
      TransactionContext tx)
  {
    validateContainerImageId(containerImageApplicationId);

    Application application = applicationDAO.getById(containerImageApplicationId);
    List<PolicyViolation> policyViolations = policyViolationDAO.getActiveByApplicationIdAndStageIdAndActionId(
        application.getId(), Stage.ID_PROXY, Action.ID_FAIL);

    if (policyViolations.isEmpty()) {
      throw new NotFoundException(
          "No applicable policy violations found to waive for container image with the given ID");
    }

    policyViolationDAO.loadConstraintFacts(policyViolations);

    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.expireWhenRemediationAvailable = false;
    createPolicyWaiversInternal(application, waiverOptions, policyViolations, true, false, tx);

    waiverOptions.matcherStrategy = ALL_COMPONENTS;
    List<PolicyWaiver> containerLevel = createPolicyWaiversInternal(application, waiverOptions,
        Collections.singletonList(policyViolations.get(0)), false, true, tx);

    if (containerLevel.isEmpty()) {
      // savePolicyWaiverInternal threw BadRequestException for the ALL_COMPONENTS pass — surfaced as
      // a log warning inside createPolicyWaiversInternal but swallowed there. The container-image
      // contract requires the image-level waiver; fail loud so the caller's transaction rolls back.
      throw new InternalServerException(
          "Failed to create container-image-level waiver for application " + application.getId());
    }
    return containerLevel.get(0);
  }

  public void addContainerImageWaiver(
      String containerImageId,
      ApiContainerImageWaiverDTO waiverDTO)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(OwnerType.APPLICATION, containerImageId);
    addContainerImageWaiverWithAuthzCheck(internalOwnerId, waiverDTO);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  void addContainerImageWaiverWithAuthzCheck(
      @AuthzContext(Key.APPLICATION_ID) String internalOwnerId,
      ApiContainerImageWaiverDTO waiverDTO)
  {
    Date expiryTime = waiverDTO == null ? null : waiverDTO.expiryTime;
    String waiverReasonId = waiverDTO == null ? null : waiverDTO.waiverReasonId;
    String comment = waiverDTO == null ? null : waiverDTO.comment;

    validateExpiryTime(expiryTime);
    validateExistingPolicyWaiverReason(waiverReasonId);

    ApiWaiverOptionsDTO options =
        new ApiWaiverOptionsDTO(comment, EXACT_COMPONENT, expiryTime, waiverReasonId, false);

    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      applyContainerImageWaivers(internalOwnerId, options, tx);
      tx.commit();
    }
  }

  public void deleteContainerImageWaiver(String containerId) {
    String internalOwnerId = idUtils.getInternalOwnerId(OwnerType.APPLICATION, containerId);
    // Defence-in-depth: @Authorize(WAIVE_POLICY_VIOLATIONS) on the container-image app's owner
    // hierarchy is the primary gate, but Firewall repo scope is orthogonal to Shiro's org-level
    // permission model. A caller with WAIVE granted broadly could otherwise reach a
    // container-image application whose shadow org points to a Docker repo outside their
    // Firewall scope. Verify the container image's Docker repo is within the caller's permitted
    // set before delegating to the authz-annotated method.
    Set<String> permittedRepositoryIds = firewallPermissionGate.resolvePermittedRepositoryIds();
    if (permittedRepositoryIds != null) {
      String dockerRepoId = getDockerRepositoryIdForContainerImageApp(internalOwnerId);
      if (dockerRepoId == null || !permittedRepositoryIds.contains(dockerRepoId)) {
        throw new UnauthorizedException("Access denied");
      }
    }
    deletePolicyWaiverContainerImageWithAuthzCheck(internalOwnerId);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  void deletePolicyWaiverContainerImageWithAuthzCheck(
      @AuthzContext(Key.APPLICATION_ID) String internalOwnerId)
  {
    validateContainerImageId(internalOwnerId);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getAllForContainerImageByOwnerId(internalOwnerId);
    log.debug("Found {} container image policy waivers for {}", policyWaivers.size(), internalOwnerId);

    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      policyWaiverDAO.deleteAllForContainerImage(tx, internalOwnerId);
      for (PolicyWaiver waiver : policyWaivers) {
        try (AuditSession auditSession = AuditData.get()
            .recordSubEvent(AuditEvent.DELETE_WAIVER, false))
        {
          auditPolicyWaiver(waiver, tx);
        }
      }
      tx.commit();
    }
  }

  public ApiPageResult<PolicyContainerWaiverData> getAllPolicyContainerWaivers(final int page, final int pageSize) {
    checkAuthenticated();

    // For scoped users, scope to container images linked to their permitted repositories.
    // For full-access users, resolveAccessibleContainerImageOwnerIds returns null (no owner filter).
    Set<String> ownerFilter = resolveAccessibleContainerImageOwnerIds();
    if (ownerFilter != null && ownerFilter.isEmpty()) {
      return new ApiPageResult<>(0L, page, pageSize, Collections.emptyList());
    }

    List<PolicyContainerWaiverData> policyContainerWaivers =
        policyWaiverDAO.getAllContainerPolicyWaivers(page, pageSize, ownerFilter);

    return new ApiPageResult<>(
        policyWaiverDAO.getContainerPolicyWaiversCount(ownerFilter),
        page,
        pageSize,
        policyContainerWaivers);
  }

  /**
   * Resolves the set of container-image application (owner) IDs the current user may access.
   *
   * <p>
   * Returns {@code null} when the caller has container-level READ (i.e. no scoping — see all
   * container-image applications). Returns a (possibly empty) set of application IDs otherwise;
   * an empty set means the caller is scoped but has no matching container-image applications.
   */
  public Set<String> resolveAccessibleContainerImageOwnerIds() {
    Set<String> permittedRepositoryIds = firewallPermissionGate.resolvePermittedRepositoryIds();
    if (permittedRepositoryIds == null) {
      return null;
    }
    return getContainerImageOwnerIdsByRepositoryIds(permittedRepositoryIds);
  }

  /**
   * Returns the Docker proxy repository ID linked to a container-image application via its
   * shadow organization's {@code relatedRepositoryId}. {@code null} if the application does not
   * exist or is not a container-image app (org has no relatedRepositoryId).
   */
  public String getDockerRepositoryIdForContainerImageApp(String applicationId) {
    Application app = applicationDAO.getById(applicationId);
    if (app == null) {
      return null;
    }
    Organization org = organizationDAO.getById(app.getOrganizationId());
    if (org == null) {
      return null;
    }
    return org.getRelatedRepositoryId();
  }

  /**
   * Gets container image application IDs for the given repository IDs.
   * Container images are applications whose organization has a relatedRepositoryId matching one of the given IDs.
   */
  private Set<String> getContainerImageOwnerIdsByRepositoryIds(Set<String> repositoryIds) {
    Set<String> orgIds = organizationDAO.getOrganizationIdsByRelatedRepositoryIds(repositoryIds);
    if (orgIds.isEmpty()) {
      return Collections.emptySet();
    }
    return applicationDAO.getByOrganizationIds(orgIds)
        .stream()
        .map(Application::getId)
        .collect(Collectors.toSet());
  }

  private static void checkAuthenticated() {
    Object principal = SecurityUtils.getSubject().getPrincipal();
    if (principal == null) {
      throw new UnauthenticatedException("Anonymous access forbidden");
    }
  }

  private void validateContainerImageId(String applicationId) {
    Application application = applicationDAO.getById(applicationId);
    if (application == null) {
      throw new NotFoundException("No container image was found with the given ID");
    }
    Organization organization = organizationDAO.getById(application.getOrganizationId());
    if (organization == null) {
      throw new NotFoundException("No container image was found with the given ID");
    }

    if (StringUtils.isAllBlank(organization.getRelatedRepositoryManagerId(), organization.getRelatedRepositoryId())) {
      throw new NotFoundException("No container image was found with the given ID");
    }

    Repository repository = repositoryDAO.getById(organization.getRelatedRepositoryId());
    if (repository == null || repository.getRepositoryType() != RepositoryType.proxy
        || !"docker".equals(repository.getFormat()))
    {
      throw new BadRequestException("The related repository must be of type proxy and format docker");
    }
  }
}

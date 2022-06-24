/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
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
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.brain.policy.evaluator.PolicyWaiverMatcherWrapper;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static com.sonatype.insight.purl.PackageUrlIdentifier.toPackageUrl;

/**
 * @since 1.70
 */
@Named
public class ApiPolicyWaiverService
{
  private static final Logger log = LoggerFactory.getLogger(ApiPolicyWaiverService.class);

  private static final String OWNER_TYPE_ATTR = "owner_type";

  private static final String OWNER_ID_ATTR = "owner_id";

  private final TelemetrySender telemetrySender;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyDAO policyDAO;

  private final ApplicationDAO applicationDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiPolicyViolationServiceV2 apiPolicyViolationServiceV2;

  private final PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  private final CurrentUser currentUser;

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
      CurrentUser currentUser)
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
  }

  /**
   * This is currently used in "request waiver"
   *
   * @deprecated Use {@link #addPolicyWaiverByPolicyViolationId(OwnerType, String, String, String,
   * ComponentMatcherStrategyForWaiver, Date)}
   */
  @Deprecated
  public void addPolicyWaiver(
      final String policyViolationId,
      final OwnerType ownerType,
      final String comment)
  {
    PolicyViolation policyViolation = new PolicyViolationDAO().getById(policyViolationId);

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
        AuditData.get().setData("organizationId", ownerId).setOrganization(new OrganizationDAO().getById(ownerId));
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }

    addPolicyWaiver(ownerType, ownerId, policyViolation, comment, EXACT_COMPONENT, null);
  }

  public void addPolicyWaiverByPolicyViolationId(
      final OwnerType ownerType,
      final String ownerId,
      final String policyViolationId,
      final String comment,
      final ComponentMatcherStrategyForWaiver matcherStrategy,
      final Date expiryTime)
  {
    // disable adding repository waivers (for now)
    switch (ownerType) {
      case APPLICATION:
      case ORGANIZATION:
        break;
      default:
        throw new BadRequestException("Invalid owner type: " + ownerType);
    }

    PolicyViolation policyViolation = new PolicyViolationDAO().getById(policyViolationId);

    if (policyViolation == null) {
      throw new NotFoundException("Could not find policy violation with ID " + policyViolationId + ".");
    }

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    if (!isViolationOwnerId(policyViolation, internalOwnerId)) {
      throw new BadRequestException("Invalid owner id: " + ownerId);
    }

    addPolicyWaiver(ownerType, internalOwnerId, policyViolation, comment, matcherStrategy, expiryTime);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  void addPolicyWaiver(
      /* used to perform authz check even though owner type is unused */
      @AuthzContext(Key.TYPE) @SuppressWarnings("unused") final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final PolicyViolation policyViolation,
      final String comment,
      final ComponentMatcherStrategyForWaiver matcherStrategy,
      final Date expiryTime)
  {
    PolicyWaiver policyWaiver =
        savePolicyWaiver(ownerId, policyViolation, comment, matcherStrategy, expiryTime);
    auditPolicyWaiver(policyWaiver);
    policyWaiverTelemetryCreator.sendWaiverTelemetryForOwnerType(policyWaiver, ownerType, policyViolation);
    sendTelemetry(ownerType, ownerId);
  }

  public List<ApiPolicyWaiverDTO> getPolicyWaivers(OwnerType ownerType, String ownerId) {
    return getPolicyWaiversWithAuthzCheck(IdUtils.getOwnerNotNull(ownerType, ownerId));
  }

  @Authorize(permission = Permission.READ)
  List<ApiPolicyWaiverDTO> getPolicyWaiversWithAuthzCheck(
      @AuthzContext(Key.OWNER) Owner owner)
  {
    List<ApiPolicyWaiverDTO> apiPolicyWaiverDTOS = new ArrayList<>();

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(owner.getId());
    policyWaivers.forEach(policyWaiver -> apiPolicyWaiverDTOS.add(ApiPolicyWaiverDTO.toDto(policyWaiver, owner)));

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
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
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
        String purl = null;
        if (component.getComponentIdentifier() != null) {
          purl = PackageUrlIdentifier.fromComponentIdentifier(component.getComponentIdentifier()).getPackageUrl();
        }
        for (Owner ownerInHierarchy : ownerById.values()) {
          // For the given owner, add policy waivers that apply to the component (i.e. they match its hash)
          // If the hash is null, then the policy waiver applies to any component
          List<PolicyWaiver> waivers;
          if (component.getHash() == null) {
            waivers = policyWaiverDAO.getActiveByOwnerIdAndHash(tx, ownerInHierarchy.getId(), component.getHash(),
                ALL_COMPONENTS);
            if (purl != null) {
              waivers.addAll(policyWaiverDAO.getActiveByOwnerIdAndHash(tx, ownerInHierarchy.getId(),
                  component.getHash(), ALL_VERSIONS, purl));
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
    apiComponentPolicyWaiversDTO.componentPolicyWaivers.add(
        convert(policyWaiver, policyNameById, ownerById, componentName)
    );
  }

  private ApiPolicyWaiverDTO convert(
      PolicyWaiver policyWaiver,
      Map<String, String> policyNameById,
      Map<String, Owner> ownerById,
      String componentName)
  {
    Owner owner = ownerById.get(policyWaiver.getOwnerId());
    ApiPolicyWaiverDTO result = ApiPolicyWaiverDTO.toDto(policyWaiver, owner);
    result.policyName = policyNameById.get(policyWaiver.getPolicyId());
    result.constraintFacts = policyWaiver.getConstraintFacts();
    result.constraintFactsJson = policyWaiver.getConstraintFactsJson();
    result.componentName = componentName;
    return result;
  }

  public void deletePolicyWaiver(OwnerType ownerType, String ownerId, String policyWaiverId) {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
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
        .setComment(policyWaiver.getComment())
        .setComponentHash(policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() != null) {
      AuditData.get().setData("policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(toList()));
    }
  }

  private void sendTelemetry(OwnerType ownerType, String ownerId) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.POLICY_WAIVER_API);
    telemetryData.getAttributes().put(OWNER_TYPE_ATTR, ownerType.toString());
    telemetryData.getAttributes().put(OWNER_ID_ATTR, HdsClientAnalytics.obfuscate(ownerId));
    telemetrySender.send(telemetryData);
  }

  private boolean isViolationOwnerId(PolicyViolation policyViolation, String internalId) {
    for (Owner owner : ownerDAO.walkHierarchy(policyViolation.getApplicationId())) {
      if (owner.getId().equals(internalId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @since 1.98
   */
  public ApiPolicyWaiversApplicableToViolationDTO getApplicableWaivers(final String violationId) {
    ApiPolicyWaiversApplicableToViolationDTO apiPolicyWaivers = new ApiPolicyWaiversApplicableToViolationDTO();
    PolicyViolation policyViolation = new PolicyViolationDAO().getById(violationId);
    if (policyViolation == null) {
      throw new NotFoundException("Could not find policy violation with ID " + violationId + ".");
    }

    String policyId = policyViolation.getPolicyId();
    String constraintFactsJson = policyViolation.getConstraintFactsJson();
    String hash = policyViolation.getHash();
    String applicationId = policyViolation.getApplicationId();
    ComponentIdentifier componentIdentifier = policyViolation.getComponentIdentifier();

    Owner owner = ownerDAO.getById(applicationId);

    Map<Boolean, List<ApiPolicyWaiverDTO>> applicableWaivers = getAllApplicableWaiversWithAuthzCheck(owner).stream()
        .filter(policyWaiver -> filterWaiverByCriteria(policyId, constraintFactsJson, componentIdentifier, hash,
            policyWaiver))
        .map(policyWaiver ->
            ApiPolicyWaiverDTO.toDto(policyWaiver, ownerDAO.getById(policyWaiver.getOwnerId()), violationId))
        .collect(partitioningBy(dto -> hasWaiverExpired(dto.expiryTime), toList()));

    apiPolicyWaivers.activeWaivers = applicableWaivers.get(Boolean.FALSE);

    apiPolicyWaivers.expiredWaivers = applicableWaivers.get(Boolean.TRUE);

    return apiPolicyWaivers;
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
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    Pair<Component, List<Pair<PolicyViolation, Component>>> pair = apiPolicyViolationServiceV2
        .getTransitivePolicyViolationsForLastEvaluation(owner.getId(), scanId, componentIdentifier, packageUrl, hash);

    Component component = pair.getLeft();
    List<PolicyViolation> policyViolations = pair.getRight().stream()
        .map(Pair::getLeft)
        .collect(Collectors.toList());

    AuditData.get()
        .setScanId(scanId)
        .setComponentIdentifier(component.getComponentIdentifier())
        .setComponentHash(component.getHash())
        .setComment(waiverDTO.comment).setData("expiryTime", waiverDTO.expiryTime);

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
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    String stageIdLowercase = stageId.toLowerCase(Locale.ROOT);
    if (!Stage.isValidStageTypeId(stageIdLowercase)) {
      throw new InvalidStageException(stageId);
    }
    List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getLastByApplicationIdsAndStageIds(
        ownerDAO.getDescendantOrSelfApplicationIds(owner), Collections.singleton(stageIdLowercase));
    Pair<Component, List<Pair<PolicyViolation, Component>>> pair = apiPolicyViolationServiceV2
        .getTransitivePolicyViolationsByComponent(stageId, componentIdentifier, packageUrl, hash, policyEvaluations);
    Component component = pair.getLeft();
    List<PolicyViolation> policyViolations = pair.getRight().stream()
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
      for (PolicyViolation policyViolation : policyViolations) {
        try {
          PolicyWaiver policyWaiver =
              savePolicyWaiver(tx, owner.getId(), policyViolation, waiverDTO.comment,
                  EXACT_COMPONENT, waiverDTO.expiryTime);
          policyWaiverTelemetryCreator.sendWaiverTelemetryForOwnerType(policyWaiver, owner.getType(), policyViolation);
          try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CREATE_WAIVER, false)) {
            auditPolicyWaiver(policyWaiver, tx);
          }
        }
        catch (BadRequestException e) {
          log.warn("Unable to add waiver for PolicyViolation ID {}", policyViolation.getId(), e);
        }
      }
      tx.commit();
      AuditData.get().commitSubEvents();
      sendTelemetry(owner.getType(), owner.getPublicId());
    }
  }

  private boolean filterWaiverByCriteria(
      String policyId,
      String constraintFactsJson,
      ComponentIdentifier componentIdentifier,
      String hash,
      PolicyWaiver policyWaiver)
  {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    ComponentFact componentFact = new ComponentFact(componentIdentifier, hash);

    return policyWaiverMatcherWrapper.matchesPolicyId(policyId) &&
        policyWaiverMatcherWrapper.matchesComponent(componentFact) &&
        policyWaiverMatcherWrapper.matchesConstraintFactsJson(constraintFactsJson);
  }

  private boolean hasWaiverExpired(Date expiryTime) {
    return expiryTime != null && expiryTime.before(new Date());
  }

  @Authorize(permission = Permission.READ)
  List<PolicyWaiver> getAllApplicableWaiversWithAuthzCheck(
      @AuthzContext(Key.OWNER) Owner owner)
  {
    return policyWaiverDAO.getApplicableAndExpiredByOwnerId(owner.getId());
  }

  private PolicyWaiver savePolicyWaiver(
      String ownerId,
      PolicyViolation policyViolation,
      String comment,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime)
  {
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      PolicyWaiver policyWaiver =
          savePolicyWaiver(tx, ownerId, policyViolation, comment, matcherStrategy, expiryTime);
      tx.commit();
      return policyWaiver;
    }
  }

  private PolicyWaiver savePolicyWaiver(
      TransactionContext tx,
      String ownerId,
      PolicyViolation policyViolation,
      String comment,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime)
  {
    String hash =
        matcherStrategy == ALL_COMPONENTS || matcherStrategy == ALL_VERSIONS ? null : policyViolation.getHash();
    PolicyWaiver policyWaiver = new PolicyWaiver(hash, policyViolation.getPolicyId(), ownerId, comment);
    policyWaiver.setConstraintFactsJson(policyViolation.getConstraintFactsJson());
    policyWaiver.setExpiryTime(expiryTime);
    policyWaiver.setCreatorId(currentUser.getUserPrincipal().getUsername());
    policyWaiver.setCreatorName(currentUser.getUserPrincipal().getDisplayName());
    policyWaiver.setComponentMatchStrategy(matcherStrategy);
    if (matcherStrategy == ALL_VERSIONS) {
      String packageUrl = toPackageUrl(policyViolation.getComponentIdentifier().createAlternativeVersion("*"));
      policyWaiver.setAssociatedPackageUrl(packageUrl);
    }

    policyWaiverDAO.insert(tx, policyWaiver);
    return policyWaiver;
  }
}

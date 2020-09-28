/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.70
 */
@Named
public class ApiPolicyWaiverService
{
  private static final String OWNER_TYPE_ATTR = "owner_type";

  private static final String OWNER_ID_ATTR = "owner_id";

  private final TelemetrySender telemetrySender;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyDAO policyDAO;

  private final ApplicationDAO applicationDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public ApiPolicyWaiverService(
      TelemetrySender telemetrySender,
      PolicyWaiverDAO policyWaiverDAO,
      PolicyDAO policyDAO,
      ApplicationDAO applicationDAO,
      OwnerDAO ownerDAO)
  {
    this.telemetrySender = telemetrySender;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyDAO = policyDAO;
    this.applicationDAO = applicationDAO;
    this.ownerDAO = ownerDAO;
  }

  /**
   * This is currently used in "request waiver"
   *
   * @deprecated Use {@link #addPolicyWaiverByPolicyViolationId(OwnerType, String, String, String, boolean, Date)}
   */
  @Deprecated
  public void addPolicyWaiver(final String policyViolationId,
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

    addPolicyWaiver(ownerType, ownerId, policyViolation, comment, false, null);
  }

  public void addPolicyWaiverByPolicyViolationId(
      final OwnerType ownerType,
      final String ownerId,
      final String policyViolationId,
      final String comment,
      final boolean applyToAllComponents,
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

    addPolicyWaiver(ownerType, internalOwnerId, policyViolation, comment, applyToAllComponents, expiryTime);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  void addPolicyWaiver(
      /* used to perform authz check even though owner type is unused */
      @AuthzContext(Key.TYPE) @SuppressWarnings("unused") final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final PolicyViolation policyViolation,
      final String comment,
      final boolean applyToAllComponents,
      final Date expiryTime)
  {
    String hash = applyToAllComponents ? null : policyViolation.getHash();
    PolicyWaiver policyWaiver = new PolicyWaiver(hash, policyViolation.getPolicyId(), ownerId, comment);
    policyWaiver.setConstraintFactsJson(policyViolation.getConstraintFactsJson());
    policyWaiver.setExpiryTime(expiryTime);

    policyWaiverDAO.insert(policyWaiver);
    auditPolicyWaiver(policyWaiver);
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
    AuditData.get().setData("policyWaiverId", policyWaiver.getId())
        .setPolicy(policyDAO.getByIdNotNull(policyWaiver.getPolicyId()))
        .setComment(policyWaiver.getComment())
        .setComponentHash(policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() != null) {
      AuditData.get().setData("policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
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

    Owner owner = ownerDAO.getById(applicationId);

    List<ApiPolicyWaiverDTO> applicableWaivers = getApplicableWaiversWithAuthzCheck(owner).stream()
        .filter(policyWaiver -> filterWaiverByCriteria(policyId, constraintFactsJson, hash, policyWaiver))
        .map(policyWaiver -> ApiPolicyWaiverDTO.toDto(policyWaiver, ownerDAO.getById(policyWaiver.getOwnerId())))
        .collect(Collectors.toList());

    apiPolicyWaivers.activeWaivers = applicableWaivers;
    return apiPolicyWaivers;
  }

  private boolean filterWaiverByCriteria(String policyId,
                                         String constraintFactsJson,
                                         String hash,
                                         PolicyWaiver policyWaiver)
  {
    return policyWaiver.getPolicyId().equals(policyId) &&
        (policyWaiver.getHash() == null || policyWaiver.getHash().equals(hash)) &&
        policyWaiver.getConstraintFactsJson() != null &&
        policyWaiver.getConstraintFactsJson().equals(constraintFactsJson);
  }

  @Authorize(permission = Permission.READ)
  List<PolicyWaiver> getApplicableWaiversWithAuthzCheck(
      @AuthzContext(Key.OWNER) Owner owner)
  {
    return policyWaiverDAO.getApplicableByOwnerId(owner.getId());
  }
}

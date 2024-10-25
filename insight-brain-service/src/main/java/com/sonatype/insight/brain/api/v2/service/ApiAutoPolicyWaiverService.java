/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverStatusDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

public class ApiAutoPolicyWaiverService
{
  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final OwnerDAO ownerDAO;

  private final CurrentUser currentUser;

  @Inject
  public ApiAutoPolicyWaiverService(
      AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      PolicyViolationDAO policyViolationDAO,
      OwnerDAO ownerDAO,
      CurrentUser currentUser
  )
  {
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.ownerDAO = ownerDAO;
    this.currentUser = currentUser;
  }

  @Authorize(permission = Permission.READ)
  public ApiAutoPolicyWaiverDTO getAutoPolicyWaiver(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverId)
  {
    checkOwnerType(ownerType, ownerId);
    AutoPolicyWaiver autoPolicyWaiver =
        autoPolicyWaiverDAO.getByIdAndOwnerIdNotNull(autoPolicyWaiverId, ownerId);
    if (!ownerId.equals(autoPolicyWaiver.getOwnerId())) {
      throw new NotFoundException(
          "Cannot find an auto policy waiver with ID " + autoPolicyWaiverId + " for " + ownerType
              + " with ID " + ownerId);
    }
    return ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
  }

  @Authorize(permission = Permission.READ)
  public List<ApiAutoPolicyWaiverDTO> getAutoPolicyWaivers(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId)
  {
    checkOwnerType(ownerType, ownerId);
    List<ApiAutoPolicyWaiverDTO> apiAutoPolicyWaiverDTOs = new ArrayList<>();

    List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(ownerId);
    autoPolicyWaivers.forEach(
        autoPolicyWaiver -> apiAutoPolicyWaiverDTOs.add(ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver)));

    return apiAutoPolicyWaiverDTOs;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public ApiAutoPolicyWaiverDTO addAutoPolicyWaiver(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO)
  {
    checkOwnerType(ownerType, ownerId);
    validateRequestDto(apiAutoPolicyWaiverDTO);

    // Only one auto policy waiver configuration should exist at a time for a given app or org.
    List<AutoPolicyWaiver> existingWaivers = autoPolicyWaiverDAO.getByOwnerId(ownerId);
    if (!existingWaivers.isEmpty()) {
      throw new BadRequestException("An auto policy waiver is already configured for " + ownerId);
    }

    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver();
    autoPolicyWaiver.setOwnerId(ownerId);
    autoPolicyWaiver.setThreatLevel(apiAutoPolicyWaiverDTO.threatLevel);
    if (apiAutoPolicyWaiverDTO.reachable != null) {
      autoPolicyWaiver.setReachable(apiAutoPolicyWaiverDTO.reachable);
    }
    if (apiAutoPolicyWaiverDTO.pathForward != null) {
      autoPolicyWaiver.setPathForward(apiAutoPolicyWaiverDTO.pathForward);
    }
    autoPolicyWaiver.setCreatorId(currentUser.getUserPrincipal().getUsername());
    autoPolicyWaiver.setCreatorName(currentUser.getUserPrincipal().getDisplayName());
    autoPolicyWaiver.setCreateTime(new Date());
    autoPolicyWaiverDAO.insert(autoPolicyWaiver);
    auditAutoPolicyWaiver(autoPolicyWaiver);
    return ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiAutoPolicyWaiverDTO updateAutoPolicyWaiver(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverId,
      ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO)
  {
    if (apiAutoPolicyWaiverDTO.autoPolicyWaiverId == null ||
        !autoPolicyWaiverId.equals(apiAutoPolicyWaiverDTO.autoPolicyWaiverId)) {
      throw new BadRequestException("Auto policy waiver ID in requst path does not match request body");
    }
    checkOwnerType(ownerType, ownerId);
    validateRequestDto(apiAutoPolicyWaiverDTO);

    AutoPolicyWaiver autoPolicyWaiver =
        autoPolicyWaiverDAO.getByIdAndOwnerIdNotNull(apiAutoPolicyWaiverDTO.autoPolicyWaiverId, ownerId);

    validateAutoWaiverUpdateDto(apiAutoPolicyWaiverDTO, autoPolicyWaiver);

    autoPolicyWaiver.setThreatLevel(apiAutoPolicyWaiverDTO.threatLevel);

    autoPolicyWaiver.setReachable(apiAutoPolicyWaiverDTO.reachable);
    autoPolicyWaiver.setPathForward(apiAutoPolicyWaiverDTO.pathForward);
    autoPolicyWaiverDAO.update(autoPolicyWaiver);
    auditAutoPolicyWaiver(autoPolicyWaiver);
    return ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public void deleteAutoPolicyWaiver(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverId)
  {
    checkOwnerType(ownerType, ownerId);
    AutoPolicyWaiver autoPolicyWaiver = autoPolicyWaiverDAO.getByIdNotNull(autoPolicyWaiverId);
    if (!ownerId.equals(autoPolicyWaiver.getOwnerId())) {
      throw new NotFoundException(
          "Cannot find an auto policy waiver with ID " + autoPolicyWaiverId + " for " + ownerType
              + " with ID " + ownerId);
    }
    auditAutoPolicyWaiver(autoPolicyWaiver);
    autoPolicyWaiverDAO.delete(autoPolicyWaiver);
  }

  @Authorize(permission = Permission.READ)
  public ApiAutoPolicyWaiverStatusDTO getAutoPolicyWaiverStatus(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId)
  {
    checkOwnerType(ownerType, ownerId);
    List<String> ownerIds = ownerDAO.getOwnerIds(ownerId);
    List<AutoPolicyWaiver> autoPolicyWaivers = new ArrayList<>();
    for (String id : ownerIds) {
      autoPolicyWaivers.addAll(autoPolicyWaiverDAO.getByOwnerId(id));
      if (!autoPolicyWaivers.isEmpty()) {
        break;
      }
    }

    ApiAutoPolicyWaiverStatusDTO dto = new ApiAutoPolicyWaiverStatusDTO();
    if (autoPolicyWaivers.isEmpty()) {
      dto.isAutoWaiverEnabled = false;
      return dto;
    }

    AutoPolicyWaiver applicableWaiver = autoPolicyWaivers.get(0);
    dto.isAutoWaiverEnabled = true;
    dto.autoPolicyWaiverId = applicableWaiver.getId();
    dto.autoPolicyWaiverOwnerId = applicableWaiver.getOwnerId();
    dto.isInherited = !applicableWaiver.getOwnerId().equals(ownerId);
    if (dto.isInherited || ownerType == OwnerType.ORGANIZATION) {
      Organization owner = organizationDAO.getById(applicableWaiver.getOwnerId());
      dto.autoPolicyWaiverOwnerName = owner.getName();
    }
    else {
      Application owner = applicationDAO.getById(applicableWaiver.getOwnerId());
      dto.autoPolicyWaiverOwnerName = owner.getName();
    }
    return dto;
  }

  public ApiAutoPolicyWaiverDTO getApplicableAutoPolicyWaiver(final String violationId) {
    PolicyViolation policyViolation = policyViolationDAO.getById(violationId);
    if (policyViolation == null) {
      throw new NotFoundException("Could not find policy violation with ID " + violationId + ".");
    }
    String autoPolicyWaiverId = policyViolation.getAutoPolicyWaiverId();
    String applicationId = policyViolation.getApplicationId();
    AutoPolicyWaiver autoPolicyWaiver = getAutoPolicyWaiverByAppOwnerHierarchy(autoPolicyWaiverId, applicationId);

    return ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
  }

  private AutoPolicyWaiver getAutoPolicyWaiverByAppOwnerHierarchy(String autoPolicyWaiverId, String applicationId) {
    List<String> ownerIds = ownerDAO.getOwnerIds(applicationId);
    for (String ownerId : ownerIds) {
      Owner owner = ownerDAO.getById(ownerId);
      AutoPolicyWaiver applicableAutoPolicyWaiver =
          getApplicableAutoPolicyWaiverWithPermissionCheck(autoPolicyWaiverId, owner);
      if (applicableAutoPolicyWaiver != null) {
        return applicableAutoPolicyWaiver;
      }
    }
    return null;
  }

  private void checkOwnerType(OwnerType ownerType, String ownerId) throws IllegalStateException {
    switch (ownerType) {
      case APPLICATION:
        AuditData.get().setData("applicationId", ownerId).setApplication(applicationDAO.getById(ownerId));
        break;
      case ORGANIZATION:
        AuditData.get().setData("organizationId", ownerId).setOrganization(organizationDAO.getById(ownerId));
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
  }

  private void validateRequestDto(ApiAutoPolicyWaiverDTO dto) throws BadRequestException {
    if (dto.threatLevel < 1 || dto.threatLevel > 10) {
      throw new BadRequestException(
          "Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");
    }
  }

  private void validateAutoWaiverUpdateDto(
      ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO,
      AutoPolicyWaiver autoPolicyWaiver) throws BadRequestException
  {
    if (apiAutoPolicyWaiverDTO.threatLevel == autoPolicyWaiver.getThreatLevel() &&
        apiAutoPolicyWaiverDTO.reachable == autoPolicyWaiver.isReachable() &&
        apiAutoPolicyWaiverDTO.pathForward == autoPolicyWaiver.hasPathForward()) {
      throw new BadRequestException("No changes made to auto policy waiver configuration");
    }
  }

  private void auditAutoPolicyWaiver(AutoPolicyWaiver autoPolicyWaiver) {
    AuditData.get().setData("autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Authorize(permission = Permission.READ)
  AutoPolicyWaiver getApplicableAutoPolicyWaiverWithPermissionCheck(
      String autoPolicyWaiverId,
      @AuthzContext(Key.OWNER) Owner owner)
  {
    return autoPolicyWaiverDAO.getByIdAndOwnerIdNullable(autoPolicyWaiverId, owner.getId());
  }
}

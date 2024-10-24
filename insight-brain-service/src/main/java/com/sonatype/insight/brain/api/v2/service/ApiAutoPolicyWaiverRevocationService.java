/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverRevocationDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

public class ApiAutoPolicyWaiverRevocationService
{
  private final AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final CurrentUser currentUser;

  @Inject
  public ApiAutoPolicyWaiverRevocationService(
      AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO,
      AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      CurrentUser currentUser
  )
  {
    this.autoPolicyWaiverRevocationDAO = autoPolicyWaiverRevocationDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.currentUser = currentUser;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public ApiAutoPolicyWaiverRevocationDTO addAutoPolicyWaiverRevocation(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      ApiAutoPolicyWaiverRevocationDTO apiAutoPolicyWaiverRevocationDTO)
  {
    checkOwnerType(ownerType, ownerId);
    validateRequestDto(ownerId, apiAutoPolicyWaiverRevocationDTO);

    AutoPolicyWaiverRevocation autoPolicyWaiverRevocation = new AutoPolicyWaiverRevocation();
    autoPolicyWaiverRevocation.setOwnerId(ownerId);
    autoPolicyWaiverRevocation.setCreatorId(currentUser.getUserPrincipal().getUsername());
    autoPolicyWaiverRevocation.setCreatorName(currentUser.getUserPrincipal().getDisplayName());
    autoPolicyWaiverRevocation.setCreateTime(new Date());
    autoPolicyWaiverRevocation.setAutoPolicyWaiverId(apiAutoPolicyWaiverRevocationDTO.autoPolicyWaiverId);
    autoPolicyWaiverRevocation.setHash(apiAutoPolicyWaiverRevocationDTO.hash);
    autoPolicyWaiverRevocation.setAssociatedPackageUrl(apiAutoPolicyWaiverRevocationDTO.associatedPackageUrl);
    autoPolicyWaiverRevocation.setScanId(apiAutoPolicyWaiverRevocationDTO.scanId);
    autoPolicyWaiverRevocationDAO.insert(autoPolicyWaiverRevocation);
    auditAutoPolicyWaiverRevocation(autoPolicyWaiverRevocation);
    return ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(autoPolicyWaiverRevocation);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public void deleteAutoPolicyWaiverRevocation(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverRevocationId)
  {
    checkOwnerType(ownerType, ownerId);
    AutoPolicyWaiverRevocation autoPolicyWaiverRevocation =
        autoPolicyWaiverRevocationDAO.getByIdNotNull(autoPolicyWaiverRevocationId);
    if (!ownerId.equals(autoPolicyWaiverRevocation.getOwnerId())) {
      throw new NotFoundException("Cannot find an auto policy waiver revocation with ID "
          + autoPolicyWaiverRevocationId + " for " + ownerType
          + " with ID " + ownerId);
    }
    auditAutoPolicyWaiverRevocation(autoPolicyWaiverRevocation);
    autoPolicyWaiverRevocationDAO.delete(autoPolicyWaiverRevocation);
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

  private void validateRequestDto(String ownerId, ApiAutoPolicyWaiverRevocationDTO apiAutoPolicyWaiverRevocationDTO) {
    if (apiAutoPolicyWaiverRevocationDTO == null) {
      throw new BadRequestException("request body is required");
    }
    if (apiAutoPolicyWaiverRevocationDTO.autoPolicyWaiverId == null) {
      throw new BadRequestException("autoPolicyWaiverId is required");
    }
    if (apiAutoPolicyWaiverRevocationDTO.ownerId == null) {
      throw new BadRequestException("ownerId is required");
    }
    try {
      autoPolicyWaiverDAO.getByIdAndOwnerIdNotNull(
          apiAutoPolicyWaiverRevocationDTO.autoPolicyWaiverId,
          apiAutoPolicyWaiverRevocationDTO.ownerId);
    }
    catch (NotFoundException e) {
      throw new BadRequestException("combination of ownerId and autoPolicyWaiverId is invalid");
    }
    if (apiAutoPolicyWaiverRevocationDTO.hash == null) {
      throw new BadRequestException("hash is required");
    }
    if (apiAutoPolicyWaiverRevocationDTO.scanId == null) {
      throw new BadRequestException("scanId is required");
    }
    AutoPolicyWaiverRevocation existing = autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
        ownerId,
        apiAutoPolicyWaiverRevocationDTO.autoPolicyWaiverId,
        apiAutoPolicyWaiverRevocationDTO.hash);
    if (existing != null) {
      throw new BadRequestException("revocation already exists for this component");
    }
  }

  private void auditAutoPolicyWaiverRevocation(AutoPolicyWaiverRevocation autoPolicyWaiverRevocation) {
    AuditData.get().setData("autoPolicyWaiverRevocationId", autoPolicyWaiverRevocation.getId());
  }
}

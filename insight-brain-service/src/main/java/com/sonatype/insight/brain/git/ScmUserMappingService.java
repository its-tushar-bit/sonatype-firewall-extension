/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.UserMapping;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.ScmUserMappingsDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.sourcecontrol.ScmUserMappings;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ScmUserMappingService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSourceControlService.class);

  private final RoleDAO roleDAO;

  private final IqForScmLicenseChecker licenseChecker;

  private final ScmUserMappingsDAO scmUserMappingsDAO;

  private final OwnerDAO ownerDAO;

  private final TelemetrySender telemetrySender;

  @Inject
  public ScmUserMappingService(
      final RoleDAO roleDAO,
      final IqForScmLicenseChecker licenseChecker,
      final ScmUserMappingsDAO scmUserMappingsDAO,
      final OwnerDAO ownerDAO,
      final TelemetrySender telemetrySender)
  {
    this.roleDAO = roleDAO;
    this.licenseChecker = licenseChecker;
    this.scmUserMappingsDAO = scmUserMappingsDAO;
    this.ownerDAO = ownerDAO;
    this.telemetrySender = telemetrySender;
  }

  @Authorize(permission = Permission.WRITE)
  public void addOrUpdateUserMappingByOrg(
      final @AuthzContext(Key.ORGANIZATION_ID) String organizationId,
      SCMUserMappingsDTO scmUserMappingsDTO)
  {
    checkLicense();
    HashSet<UserMapping> uniqueItems = new HashSet<>();
    Optional<UserMapping> duplicatedItem =
        scmUserMappingsDTO.mappings().stream().filter(mapping -> !uniqueItems.add(mapping)).findFirst();
    if (duplicatedItem.isPresent()) {
      UserMapping duplicateItemValue = duplicatedItem.get();
      throw new BadRequestException(String.format(
          "There was a duplicate mapping %s: %s. Mappings should be unique.", duplicateItemValue.from().name(),
          duplicateItemValue.to().name()));
    }
    Role role = scmUserMappingsDTO.role() == null ? null : roleDAO.getByName(scmUserMappingsDTO.role());
    ScmUserMappings scmUserMappings = new ScmUserMappings();
    scmUserMappings.setRoleId(role != null ? role.getId() : Role.DEVELOPER_ROLE_ID);
    scmUserMappings.setOrganizationId(organizationId);
    List<Entry<String, String>> userMappingsAsEntries =
        SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());
    scmUserMappings.setMappingsJson(userMappingsAsEntries);
    scmUserMappingsDAO.addOrUpdate(scmUserMappings);

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTOMATIC_ROLE_ASSIGNMENT_USER_MAPPINGS_ADDED);
    telemetryData.put("mappings_json", scmUserMappings.getMappingsJson());
    telemetrySender.send(telemetryData);
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteUserMappingByOrg(final @AuthzContext(Key.ORGANIZATION_ID) String organizationId) {
    checkLicense();
    scmUserMappingsDAO.deleteByOrganizationId(organizationId);
  }

  @Authorize(permission = Permission.READ)
  public SCMUserMappingsResponseDTO getUserMappingsByOwner(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId)
  {
    checkLicense();
    return getUserMappingsByOwnerNoAuthz(ownerType, ownerId);
  }

  SCMUserMappingsResponseDTO getUserMappingsByOwnerNoAuthz(OwnerType ownerType, String ownerId) {
    if (!OwnerType.ORGANIZATION.equals(ownerType) && !OwnerType.APPLICATION.equals(ownerType)) {
      throw new BadRequestException("OwnerType not supported: " + ownerType);
    }
    Iterator<Owner> ownersHierarchy = ownerDAO.walkHierarchy(ownerId, ownerType).iterator();
    if (OwnerType.APPLICATION.equals(ownerType)) {
      ownersHierarchy.next(); // skip first if ownerType is APPLICATION
    }
    ScmUserMappings scmUserMappings;
    do {
      scmUserMappings = scmUserMappingsDAO.getByOrganizationId(ownersHierarchy.next().getId());
    }
    while (scmUserMappings == null && ownersHierarchy.hasNext());

    if (scmUserMappings == null) {
      return null;
    }

    String role = roleDAO.getById(scmUserMappings.getRoleId()).getNameLowercaseNoWhitespace();
    List<UserMapping> userMappings = scmUserMappings.getMappings()
        .stream()
        .map(UserMapping::new)
        .toList();

    return new SCMUserMappingsResponseDTO(
        scmUserMappings.getOrganizationId(),
        ownerType == OwnerType.APPLICATION || !ownerId.equals(scmUserMappings.getOrganizationId()),
        new SCMUserMappingsDTO(role, userMappings));
  }

  private void checkLicense() {
    if (!licenseChecker.isIqForScmSupported()) {
      log.debug("License does not support source control notification or automation features");
      throw new InvalidLicenseException();
    }
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.webhook.EventAction;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.17.0
 */
@Named
public class LicenseThreatGroupService
{
  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private final PolicyDAO policyDAO;

  private final OwnerDAO ownerDAO;

  private final ManagementEventService managementEventService;

  private final IdUtils idUtils;

  @Inject
  public LicenseThreatGroupService(
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO,
      final PolicyDAO policyDAO,
      final OwnerDAO ownerDAO,
      final ManagementEventService managementEventService,
      final IdUtils idUtils)
  {
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAO;
    this.policyDAO = policyDAO;
    this.ownerDAO = ownerDAO;
    this.managementEventService = managementEventService;
    this.idUtils = idUtils;
  }

  @Authorize(permission = Permission.READ)
  public List<LicenseThreatGroup> getLicenseThreatGroups(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    return licenseThreatGroupDAO.getByOwnerId(ownerId);
  }

  @Authorize(permission = Permission.READ)
  public ApplicableLicenseThreatGroups getApplicableLicenseThreatGroups(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    ApplicableLicenseThreatGroups result = new ApplicableLicenseThreatGroups();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      result.add(owner.getId(), owner.getName(), owner.getType(), loadLicenseThreatGroups(owner.getId()));
    }
    return result;
  }

  private List<LicenseThreatGroupWithLicenses> loadLicenseThreatGroups(final String ownerId) {
    List<LicenseThreatGroupWithLicenses> results = new ArrayList<>();
    for (LicenseThreatGroup ltg : licenseThreatGroupDAO.getByOwnerId(ownerId)) {
      LicenseThreatGroupWithLicenses ltgwl = new LicenseThreatGroupWithLicenses();
      ltgwl.id = ltg.getId();
      ltgwl.name = ltg.getName();
      ltgwl.threatLevel = ltg.getThreatLevel();
      ltgwl.licenses = licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId(ltg.getId());
      results.add(ltgwl);
    }
    return results;
  }

  @Authorize(permission = Permission.WRITE)
  public LicenseThreatGroup addLicenseThreatGroup(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String orgId,
      final LicenseThreatGroup licenseThreatGroup)
  {
    licenseThreatGroup.setId(null);
    licenseThreatGroup.setOwnerId(orgId);
    licenseThreatGroupDAO.insert(licenseThreatGroup);

    auditLicenseThreatGroup(licenseThreatGroup);
    managementEventService.postEvent(EventAction.CREATED, licenseThreatGroup);

    return licenseThreatGroup;
  }

  @Authorize(permission = Permission.WRITE)
  public LicenseThreatGroup updateLicenseThreatGroup(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      final LicenseThreatGroup licenseThreatGroup)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    if (!internalOwnerId.equals(licenseThreatGroupDAO.getByIdNotNull(licenseThreatGroup.getId()).getOwnerId())) {
      throw new NotFoundException(
          "Cannot find a license threat group with id " + licenseThreatGroup.getId() + " for owner id " + ownerId);
    }

    licenseThreatGroup.setOwnerId(internalOwnerId);
    licenseThreatGroupDAO.update(licenseThreatGroup);

    auditLicenseThreatGroup(licenseThreatGroup);
    managementEventService.postEvent(EventAction.UPDATED, licenseThreatGroup);

    return licenseThreatGroup;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteLicenseThreatGroup(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final String licenseThreatGroupId)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByIdNotNull(licenseThreatGroupId);
    if (!internalOwnerId.equals(licenseThreatGroup.getOwnerId())) {
      throw new NotFoundException("Cannot find a license threat group with ID " + licenseThreatGroupId + " for "
          + ownerType + " ID " + ownerId);
    }

    // Verify that the license threat group is not used in a policy condition
    validateLicenseThreatGroupNotUsedInAnyPolicy(ownerDAO.getById(internalOwnerId), licenseThreatGroup);

    licenseThreatGroupDAO.delete(licenseThreatGroup);

    auditLicenseThreatGroup(licenseThreatGroup);
    managementEventService.postEvent(EventAction.DELETED, licenseThreatGroup);
  }

  private void auditLicenseThreatGroup(LicenseThreatGroup licenseThreatGroup) {
    AuditData.get() //
        .setLicenseThreatGroup(licenseThreatGroup)
        .setData("licenseThreatGroupThreatLevel", licenseThreatGroup.getThreatLevel());
  }

  public static class ApplicableLicenseThreatGroups
  {
    public List<LicenseThreatGroupsByOwner> licenseThreatGroupsByOwner = new ArrayList<>();

    public void add(
        String ownerId,
        String ownerName,
        OwnerType ownerType,
        List<LicenseThreatGroupWithLicenses> licenseThreatGroups)
    {
      LicenseThreatGroupsByOwner ltgbo = new LicenseThreatGroupsByOwner();
      ltgbo.ownerId = ownerId;
      ltgbo.ownerName = ownerName;
      ltgbo.ownerType = ownerType;
      ltgbo.licenseThreatGroups = licenseThreatGroups;
      licenseThreatGroupsByOwner.add(ltgbo);
    }
  }

  public static class LicenseThreatGroupsByOwner
  {
    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public List<LicenseThreatGroupWithLicenses> licenseThreatGroups;
  }

  public static class LicenseThreatGroupWithLicenses
  {
    public String id;

    public String name;

    public int threatLevel;

    public List<LicenseThreatGroupLicense> licenses;
  }

  private void validateLicenseThreatGroupNotUsedInAnyPolicy(Owner owner, LicenseThreatGroup licenseThreatGroup) {
    for (Policy policy : policyDAO.getByOwnerId(owner.getId())) {
      if (isLicenseThreatGroupUsedInPolicy(licenseThreatGroup.getId(), policy)) {
        String error = "Cannot delete the license threat group because it is used in a condition for the '"
            + policy.getName() + "' policy";
        if (!licenseThreatGroup.getOwnerId().equals(owner.getId())) {
          error += " in " + owner.getType() + " '" + owner.getName() + "'";
        }
        throw new BadRequestException(error);
      }
    }
    for (Owner childOwner : ownerDAO.getChildOwners(owner)) {
      validateLicenseThreatGroupNotUsedInAnyPolicy(childOwner, licenseThreatGroup);
    }
  }

  /**
   * Returns {@code true} if the given licenseThreatGroupId is used in the given policy; otherwise {@code false}.
   *
   */
  private static boolean isLicenseThreatGroupUsedInPolicy(String licenseThreatGroupId, Policy policy) {
    for (Constraint constraint : policy.getConstraints()) {
      for (Condition condition : constraint.getConditions()) {
        if (LicenseThreatGroupConditionType.ID.equals(condition.getConditionTypeId())
            && licenseThreatGroupId.equals(condition.getValue()))
        {
          return true;
        }
      }
    }
    return false;
  }
}

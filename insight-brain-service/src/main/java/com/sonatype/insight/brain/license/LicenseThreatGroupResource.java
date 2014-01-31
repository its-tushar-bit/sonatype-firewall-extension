/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
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
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Path(LicenseThreatGroupResource.SERVICE_PATH)
public class LicenseThreatGroupResource
{
  public static final String SERVICE_PATH = "rest/licenseThreatGroup/{ownerType: application|organization}/{ownerId}";

  private final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Authorize(permission = Permission.READ)
  public List<LicenseThreatGroup> getLicenseThreatGroups(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return licenseThreatGroupDAO.getByOwnerId(ownerId);
  }

  /**
   * @since 1.6
   */
  @GET
  @Path("applicable")
  @Produces({ MediaType.APPLICATION_JSON })
  @Authorize(permission = Permission.READ)
  public ApplicableLicenseThreatGroups getApplicableLicenseThreatGroups(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    ApplicableLicenseThreatGroups result = new ApplicableLicenseThreatGroups();

    String organizationId;
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application app = new ApplicationDAO().getByIdNotNull(ownerId);
      result.add(app.getId(), app.getName(), IdUtils.TYPE_APPLICATION, loadLicenseThreatGroups(app.getId()));
      organizationId = app.getOrganizationId();
    }
    else {
      organizationId = ownerId;
    }

    Organization org = new OrganizationDAO().getByIdNotNull(organizationId);
    result.add(org.getId(), org.getName(), IdUtils.TYPE_ORGANIZATION, loadLicenseThreatGroups(org.getId()));

    return result;
  }

  private List<LicenseThreatGroupWithLicenses> loadLicenseThreatGroups(String ownerId) {
    List<LicenseThreatGroupWithLicenses> results = new ArrayList<LicenseThreatGroupWithLicenses>();
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

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public LicenseThreatGroup addLicenseThreatGroup(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, LicenseThreatGroup licenseThreatGroup)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    licenseThreatGroup.setId(null);
    licenseThreatGroup.setOwnerId(ownerId);
    licenseThreatGroupDAO.insert(licenseThreatGroup);

    return licenseThreatGroup;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public LicenseThreatGroup updateLicenseThreatGroup(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, LicenseThreatGroup licenseThreatGroup)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    licenseThreatGroup.setOwnerId(ownerId);
    licenseThreatGroupDAO.update(licenseThreatGroup);

    return licenseThreatGroup;
  }

  @DELETE
  @Path("{licenseThreatGroupId}")
  @Authorize(permission = Permission.WRITE)
  public void deleteLicenseThreatGroup(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("licenseThreatGroupId") String licenseThreatGroupId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByIdNotNull(licenseThreatGroupId);
    if (!internalOwnerId.equals(licenseThreatGroup.getOwnerId())) {
      throw new NotFoundException("Cannot find a license threat group with id " + licenseThreatGroupId + " for "
          + ownerType + " id " + ownerId);
    }

    // Verify that the license threat group is not used in a policy condition
    PolicyDAO policyDAO = new PolicyDAO();

    String inUseError = "Cannot delete the license threat group because it is used in a condition for the '%s' policy";

    for (Policy policy : policyDAO.getByOwnerId(internalOwnerId)) {
      if (isLicenseThreatGroupUsedInPolicy(licenseThreatGroupId, policy)) {
        throw new BadRequestException(String.format(inUseError, policy.getName()));
      }
    }

    if (IdUtils.TYPE_ORGANIZATION.equals(ownerType)) {
      inUseError = inUseError + " in application '%s'";

      for (Application app : new ApplicationDAO().getByOrganizationId(internalOwnerId)) {
        for (Policy policy : policyDAO.getByOwnerId(app.getId())) {
          if (isLicenseThreatGroupUsedInPolicy(licenseThreatGroupId, policy)) {
            throw new BadRequestException(String.format(inUseError, policy.getName(), app.getName()));
          }
        }
      }
    }

    licenseThreatGroupDAO.delete(licenseThreatGroup);
  }

  public static class ApplicableLicenseThreatGroups
  {
    public List<LicenseThreatGroupsByOwner> licenseThreatGroupsByOwner = new ArrayList<LicenseThreatGroupsByOwner>();

    public void add(String ownerId, String ownerName, String ownerType,
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

    public String ownerType;

    public List<LicenseThreatGroupWithLicenses> licenseThreatGroups;
  }

  public static class LicenseThreatGroupWithLicenses
  {
    public String id;

    public String name;

    public int threatLevel;

    public List<LicenseThreatGroupLicense> licenses;
  }

  /**
   * Returns {@code true} if the given licenseThreatGroupId is used in the given policy; otherwise {@code false}.
   * 
   * @since 1.6
   */
  private static boolean isLicenseThreatGroupUsedInPolicy(String licenseThreatGroupId, Policy policy) {
    for (Constraint constraint : policy.getConstraints()) {
      for (Condition condition : constraint.getConditions()) {
        if (LicenseThreatGroupConditionType.ID.equals(condition.getConditionTypeId())
            && licenseThreatGroupId.equals(condition.getValue())) {
          return true;
        }
      }
    }
    return false;
  }
}

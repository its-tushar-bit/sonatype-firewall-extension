/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.8
 */
@Named
@Path(PolicyMonitoringResource.SERVICE_PATH)
public class PolicyMonitoringResource
{
  public static final String SERVICE_PATH = "rest/policyMonitoring/{ownerType: application|organization}/{ownerId}";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public PolicyMonitoring get(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    return loadPolicyMonitoring(internalOwnerId);
  }

  /**
   * Returns the Application PolicyMonitoring and its parent Org PolicyMonitoring. Both may be null depending on
   * whether or not these values are stored.
   */
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Path("applicable")
  @Authorize(permission = Permission.READ)
  public ApplicablePolicyMonitors getApplicable(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    if (IdUtils.TYPE_ORGANIZATION.equals(ownerType)) {
      return new ApplicablePolicyMonitors(null, loadPolicyMonitoring(internalOwnerId));
    }
    Application application = new ApplicationDAO().getByIdNotNull(internalOwnerId);
    return new ApplicablePolicyMonitors(loadPolicyMonitoring(application.getId()),
        loadPolicyMonitoring(application.getOrganizationId()));
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public PolicyMonitoring set(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, PolicyMonitoring policyMonitoring)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    if (!Stage.isValidStageTypeId(policyMonitoring.getStageTypeId())) {
      throw new BadRequestException("Invalid stage: " + policyMonitoring.getStageTypeId());
    }

    policyMonitoring.setOwnerId(ownerId);
    new PolicyMonitoringDAO().set(policyMonitoring);

    return policyMonitoring;
  }

  @DELETE
  @Authorize(permission = Permission.WRITE)
  public void delete(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = new PolicyMonitoringDAO().getByOwnerIdNotNull(ownerId);
    dao.delete(policyMonitoring);
  }

  private PolicyMonitoring loadPolicyMonitoring(final String ownerId) {
    return new PolicyMonitoringDAO().getByOwnerId(ownerId);
  }

  public static class ApplicablePolicyMonitors {
    public PolicyMonitoring appPolicyMonitor;
    public PolicyMonitoring orgPolicyMonitor;

    public ApplicablePolicyMonitors()
    {
    }

    public ApplicablePolicyMonitors(final PolicyMonitoring appPolicyMonitor,
                                    final PolicyMonitoring orgPolicyMonitor)
    {
      this.appPolicyMonitor = appPolicyMonitor;
      this.orgPolicyMonitor = orgPolicyMonitor;
    }
  }
}

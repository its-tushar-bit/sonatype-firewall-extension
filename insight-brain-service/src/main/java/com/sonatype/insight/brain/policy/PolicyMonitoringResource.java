/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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

import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;

/**
 * @since 1.7.1
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
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return new PolicyMonitoringDAO().getByOwnerId(ownerId);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public PolicyMonitoring set(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, PolicyMonitoring policyMonitoring)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    policyMonitoring.setOwnerId(ownerId);
    new PolicyMonitoringDAO().set(policyMonitoring);

    return policyMonitoring;
  }

  @DELETE
  @Authorize(permission = Permission.WRITE)
  public void deletePolicyWaiver(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = new PolicyMonitoringDAO().getByOwnerIdNotNull(ownerId);
    dao.delete(policyMonitoring);
  }
}

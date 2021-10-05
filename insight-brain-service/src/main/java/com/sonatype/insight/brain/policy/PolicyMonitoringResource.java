/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.List;

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
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.8
 */
@Named
@Timed
@Path(PolicyMonitoringResource.RESOURCE_PATH)
public class PolicyMonitoringResource
{
  private static final Logger log = LoggerFactory.getLogger(PolicyMonitoringResource.class);

  public static final String RESOURCE_PATH =
      "rest/policyMonitoring/{ownerType: application|organization|repository}/{ownerId}";

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public PolicyMonitoring get(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
                              @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    return policyMonitoringDAO.getByOwnerId(internalOwnerId);
  }

  /**
   * Returns the owner's PolicyMonitor and the PolicyMonitors of all the owner's parents. The PolicyMonitor
   * fields may be null depending on whether these values are stored.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("applicable")
  @Authorize(permission = Permission.READ)
  public ApplicablePolicyMonitors getApplicable(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    ApplicablePolicyMonitors results = new ApplicablePolicyMonitors();
    results.policyMonitoringByOwner = new ArrayList<>();

    OwnerDAO ownerDAO = new OwnerDAO();
    for (Owner owner : ownerDAO.walkHierarchy(internalOwnerId)) {
      PolicyMonitoringByOwner policyMonitoringByOwner = new PolicyMonitoringByOwner();
      policyMonitoringByOwner.ownerName = owner.getName();
      policyMonitoringByOwner.policyMonitoring = policyMonitoringDAO.getByOwnerId(owner.getId());
      results.policyMonitoringByOwner.add(policyMonitoringByOwner);
    }

    return results;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING)
  public PolicyMonitoring set(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
                              @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
                              PolicyMonitoring policyMonitoring)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    if (ProxyStageType.ID.equals(policyMonitoring.getStageTypeId())) {
      if (!Organization.ROOT_ORGANIZATION_ID.equals(ownerId) && !OwnerType.REPOSITORY.equals(ownerType)) {
        throw new InvalidStageException(policyMonitoring.getStageTypeId());
      }
    }
    else if (OwnerType.REPOSITORY.equals(ownerType)) {
      throw new InvalidStageException(policyMonitoring.getStageTypeId());
    }
    else if (!Stage.isValidStageTypeId(policyMonitoring.getStageTypeId())) {
      throw new InvalidStageException(policyMonitoring.getStageTypeId());
    }

    policyMonitoring.setOwnerId(ownerId);
    policyMonitoringDAO.set(policyMonitoring);
    AuditData.get().setStageId(policyMonitoring.getStageTypeId());

    log.debug("Configured policy monitoring for {} with ID {} for stage '{}'.", ownerType, ownerId,
        policyMonitoring.getStageTypeId());

    return policyMonitoring;
  }

  @DELETE
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING)
  public void delete(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
                     @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerIdNotNull(ownerId);
    policyMonitoringDAO.delete(policyMonitoring);
    AuditData.get().setStageId(Organization.ROOT_ORGANIZATION_ID.equals(ownerId) ? "none" : "inherited");

    log.debug("Deleted policy monitoring for {} with ID {} for stage '{}'.", ownerType, ownerId,
        policyMonitoring.getStageTypeId());
  }

  public static class ApplicablePolicyMonitors
  {
    public List<PolicyMonitoringByOwner> policyMonitoringByOwner;
  }

  public static class PolicyMonitoringByOwner
  {
    public String ownerName;

    public PolicyMonitoring policyMonitoring;
  }
}

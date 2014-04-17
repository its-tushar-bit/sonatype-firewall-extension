/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.6
 */
@Named
@Path(PolicyWaiverResource.SERVICE_PATH)
public class PolicyWaiverResource
{
  public static final String SERVICE_BASEPATH = "rest/policyWaiver/";

  public static final String SERVICE_PATH = SERVICE_BASEPATH + "{ownerType: application|organization}/{ownerId}";

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public PolicyWaiver addPolicyWaiver(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, PolicyWaiver policyWaiver)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    policyWaiver.setId(null);
    policyWaiver.setOwnerId(internalOwnerId);
    new PolicyWaiverDAO().insert(policyWaiver);
    return policyWaiver;
  }

  @DELETE
  @Path("{policyWaiverId}")
  @Authorize(permission = Permission.WRITE)
  public void deletePolicyWaiver(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("policyWaiverId") String policyWaiverId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull(policyWaiverId);
    if (!internalOwnerId.equals(policyWaiver.getOwnerId())) {
      throw new NotFoundException("Cannot find a policy waiver with id " + policyWaiverId + " for " + ownerType
          + " id " + ownerId);
    }

    policyWaiverDAO.delete(policyWaiver);
  }

  /**
   * Supports the "View Waivers" functionality of the UI. Most notably, the returned DTO holds the names of relevant
   * entities and public IDs as opposed to internal IDs to facilitate follow-up REST requests like deletion.
   */
  @GET
  @Path("component/{hash}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public AppliedWaivers getPolicyWaiversByHash(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, @PathParam("hash") String hash)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    AppliedWaivers result = new AppliedWaivers();

    String organizationId;
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application app = new ApplicationDAO().getByIdNotNull(ownerId);
      result.add(app.getPublicId(), app.getName(), IdUtils.TYPE_APPLICATION, getAppliedWaivers(app.getId(), hash));
      organizationId = app.getOrganizationId();
    }
    else {
      organizationId = ownerId;
    }

    Organization org = new OrganizationDAO().getByIdNotNull(organizationId);
    result.add(org.getId(), org.getName(), IdUtils.TYPE_ORGANIZATION, getAppliedWaivers(org.getId(), hash));

    return result;
  }

  private List<PolicyWaiverDTO> getAppliedWaivers(String ownerId, String hash) {
    List<PolicyWaiver> waivers = new PolicyWaiverDAO().getByOwnerIdAndHash(ownerId, hash);
    Map<String, String> policyNamesById = new HashMap<String, String>();
    for (Policy policy : new PolicyDAO().getApplicableByOwnerId(ownerId)) {
      policyNamesById.put(policy.getId(), policy.getName());
    }
    List<PolicyWaiverDTO> dtos = new ArrayList<PolicyWaiverDTO>(waivers.size());
    for (PolicyWaiver waiver : waivers) {
      PolicyWaiverDTO dto = new PolicyWaiverDTO();
      dto.setComment(waiver.getComment());
      dto.setConstraintId(waiver.getConstraintId());
      dto.setCreateTime(waiver.getCreateTime());
      dto.setHash(waiver.getHash());
      dto.setId(waiver.getId());
      dto.setOwnerId(waiver.getOwnerId());
      dto.setPolicyId(waiver.getPolicyId());
      dto.policyName = policyNamesById.get(dto.getPolicyId());
      dtos.add(dto);
    }
    return dtos;
  }

  @GET
  @Path("applicable/context/{policyId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public ApplicableContext getApplicableContexts(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("ownerId") String applicationPublicId,
      @PathParam("policyId") String policyId)
  {
    PolicyDAO policyDAO = new PolicyDAO();
    Application application = new ApplicationDAO().getByPublicIdNotNull(applicationPublicId);
    Policy policy = policyDAO.getByIdNotNull(policyId);
    if (application.getId().equals(policy.getOwnerId())) {
      // The policy belongs to the application
      return new ApplicableContext(application.getPublicId(), application.getName(), IdUtils.TYPE_APPLICATION);
    }

    if (!application.getOrganizationId().equals(policy.getOwnerId())) {
      throw new NotFoundException("Cannot find a policy with id " + policyId + " for application public id "
          + applicationPublicId);
    }

    // The policy belongs to an organization
    Organization organization = new OrganizationDAO().getById(application.getOrganizationId());
    ApplicableContext result = new ApplicableContext(organization.getId(), organization.getName(),
        IdUtils.TYPE_ORGANIZATION);
    result.setChildren(new ArrayList<ApplicableContext>());
    // Currently we need only the application specified by the applicationPublicId. In the future we might need to
    // return all the applications for this organization.
    result.getChildren().add(
        new ApplicableContext(application.getPublicId(), application.getName(), IdUtils.TYPE_APPLICATION));
    return result;
  }

  /**
   * Enumerates the waivers applied to a given component in a way that allows to clients to identify at which point in
   * the organizational hierarchy the waiver has been defined.
   */
  public static class AppliedWaivers
  {
    public List<WaiversByOwner> waiversByOwner = new ArrayList<WaiversByOwner>();

    void add(String ownerId, String ownerName, String ownerType, List<PolicyWaiverDTO> waivers) {
      if (waivers == null || waivers.isEmpty()) {
        return;
      }
      for (PolicyWaiver waiver : waivers) {
        waiver.setOwnerId(ownerId);
      }
      WaiversByOwner wbo = new WaiversByOwner();
      wbo.ownerId = ownerId;
      wbo.ownerName = ownerName;
      wbo.ownerType = ownerType;
      wbo.waivers = waivers;
      waiversByOwner.add(wbo);
    }
  }

  /**
   * Enumerates the waivers contributed from a given context (app/org) along with basic identifying info about the
   * context itself, suitable for future REST requests to manage the waivers.
   * If the owner is an application, the ownerId holds the public application ID as expected for REST requests and not
   * the internal ID.
   */
  public static class WaiversByOwner
  {
    public String ownerId;

    public String ownerName;

    public String ownerType;

    public List<PolicyWaiverDTO> waivers;
  }

  /**
   * Describes a waiver in a REST-friendly way.
   */
  public static class PolicyWaiverDTO
      extends PolicyWaiver
  {
    public String policyName;
  }
}

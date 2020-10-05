/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.6
 */
@Named
@Timed
@Path(PolicyWaiverResource.RESOURCE_PATH)
public class PolicyWaiverResource
{
  public static final String SERVICE_BASEPATH = "rest/policyWaiver/";

  public static final String RESOURCE_PATH = SERVICE_BASEPATH
      + "{ownerType: application|organization|repository|repository_container}/{ownerId}";

  private final OwnerDAO ownerDAO = new OwnerDAO();

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  @Audited(AuditEvent.CREATE_WAIVER)
  public PolicyWaiver addPolicyWaiver(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
                                      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
                                      PolicyWaiver policyWaiver)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    if (policyWaiver.getConstraintFactsJson() == null || policyWaiver.getConstraintFactsJson().isEmpty()) {
      throw new BadRequestException("Policy waiver must have constraint facts.");
    }

    policyWaiver.setId(null);
    policyWaiver.setOwnerId(internalOwnerId);
    new PolicyWaiverDAO().insert(policyWaiver);
    auditPolicyWaiver(policyWaiver);
    return policyWaiver;
  }

  /**
   * Supports the "View Waivers" functionality of the UI. Most notably, the returned DTO holds the names of relevant
   * entities and public IDs as opposed to internal IDs to facilitate follow-up REST requests like deletion.
   */
  @GET
  @Path("component/{hash}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public AppliedWaivers getPolicyWaiversByHash(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("hash") String hash)
  {
    AuditData.get().setComponentHash(hash);
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    PolicyDAO policyDAO = new PolicyDAO();
    Map<String, String> policyNamesById = new HashMap<>();
    Function<String, String> policyNameLoader =
        policyId -> policyNamesById.computeIfAbsent(policyId, id -> policyDAO.getById(id).getName());

    AppliedWaivers result = new AppliedWaivers();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      result.add(owner, getApplicableWaivers(owner.getId(), hash, policyNameLoader));
    }

    return result;
  }

  private List<PolicyWaiverDTO> getApplicableWaivers(
      String ownerId,
      String hash,
      Function<String, String> policyNameLoader)
  {
    List<PolicyWaiver> waivers = new PolicyWaiverDAO().getApplicableToComponent(ownerId, hash);
    List<PolicyWaiverDTO> dtos = new ArrayList<>(waivers.size());
    for (PolicyWaiver waiver : waivers) {
      PolicyWaiverDTO dto = new PolicyWaiverDTO();
      dto.setComment(waiver.getComment());
      dto.setCreateTime(waiver.getCreateTime());
      dto.setHash(waiver.getHash());
      dto.setId(waiver.getId());
      dto.setOwnerId(waiver.getOwnerId());
      dto.setPolicyId(waiver.getPolicyId());
      dto.policyName = policyNameLoader.apply(dto.getPolicyId());
      dtos.add(dto);
    }
    return dtos;
  }

  @GET
  @Path("applicable/context/{policyId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public ApplicableContext getApplicableContexts(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("policyId") String policyId)
  {
    Policy policy = new PolicyDAO().getByIdNotNull(policyId);
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    ApplicableContext context = null;
    boolean foundPolicyInHierarchy = false;
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      ApplicableContext currentContext = new ApplicableContext(getRestOwnerId(owner), owner.getName(), owner.getType());
      if (context != null) {
        currentContext.setChildren(new ArrayList<ApplicableContext>());
        currentContext.getChildren().add(context);
      }
      context = currentContext;

      if (owner.getId().equals(policy.getOwnerId())) {
        // only go as high as the owner of the policy
        foundPolicyInHierarchy = true;
        break;
      }
    }

    if (!foundPolicyInHierarchy) {
      Owner owner = ownerDAO.getById(ownerId);
      throw new NotFoundException("Cannot find a policy with ID " + policyId + " for " + owner.getType()
          + " public ID " + owner.getPublicId());
    }

    return context;
  }

  private static String getRestOwnerId(Owner owner) {
    return OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
  }

  /**
   * Enumerates the waivers applied to a given component in a way that allows to clients to identify at which point in
   * the organizational hierarchy the waiver has been defined.
   */
  public static class AppliedWaivers
  {
    public List<WaiversByOwner> waiversByOwner = new ArrayList<>();

    void add(Owner owner, List<PolicyWaiverDTO> waivers) {
      String ownerId = getRestOwnerId(owner);
      if (waivers == null || waivers.isEmpty()) {
        return;
      }
      for (PolicyWaiver waiver : waivers) {
        waiver.setOwnerId(ownerId);
      }
      WaiversByOwner wbo = new WaiversByOwner();
      wbo.ownerId = ownerId;
      wbo.ownerName = owner.getName();
      wbo.ownerType = owner.getType();
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

    public OwnerType ownerType;

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

  private void auditPolicyWaiver(PolicyWaiver policyWaiver) {
    AuditData.get().setData("policyWaiverId", policyWaiver.getId())
        .setPolicy(new PolicyDAO().getByIdNotNull(policyWaiver.getPolicyId()))
        .setComment(policyWaiver.getComment())
        .setComponentHash(policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() != null) {
      AuditData.get().setData("policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    }
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dashboard.PolicyWaiverService;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.6
 */
@Named
@Timed
@Path(PolicyWaiverResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_WAIVERS)
public class PolicyWaiverResource
{
  public static final String SERVICE_BASEPATH = "rest/policyWaiver/";

  public static final String RESOURCE_PATH = SERVICE_BASEPATH
      + "{ownerType: application|organization|repository|repository_container|repository_manager}/{ownerId}";

  private final OwnerDAO ownerDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final OwnerComponentDAO applicationComponentDAO;

  private final PolicyWaiverService policyWaiverService;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  private final PolicyDAO policyDAO;

  private final IdUtils idUtils;

  @Inject
  public PolicyWaiverResource(
      final OwnerDAO ownerDAO,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final OwnerComponentDAO applicationComponentDAO,
      final PolicyWaiverService policyWaiverService,
      final PolicyWaiverDAO policyWaiverDAO,
      final PolicyDAO policyDAO,
      final PolicyWaiverReasonDAO policyWaiverReasonDAO,
      final IdUtils idUtils)
  {
    this.ownerDAO = ownerDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyWaiverService = policyWaiverService;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyDAO = policyDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
    this.idUtils = idUtils;
  }

  private <T extends HasComponentId> ComponentIdentifier getComponentIdentifierFromOwnerIdAndHash(
      String ownerId,
      String hash,
      BiFunction<String, String, List<T>> daoMethod)
  {
    T proxyRepositoryComponent = daoMethod.apply(ownerId, hash).stream().findFirst().orElse(null);
    return proxyRepositoryComponent != null ? proxyRepositoryComponent.getComponentIdentifier() : null;
  }

  private ComponentIdentifier getComponentIdentifierFromOwnerAndHash(Owner owner, String hash) {
    if (owner.getType().equals(OwnerType.REPOSITORY)) {
      return getComponentIdentifierFromOwnerIdAndHash(owner.getId(), hash,
          proxyRepositoryComponentDAO::getByRepositoryIdAndHash);
    }
    else if (owner.getType().equals(OwnerType.APPLICATION)) {
      return getComponentIdentifierFromOwnerIdAndHash(owner.getId(), hash,
          applicationComponentDAO::getByOwnerIdAndHash);
    }
    return null;
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
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    Map<String, String> policyNamesById = new HashMap<>();
    UnaryOperator<String> policyNameLoader =
        policyId -> policyNamesById.computeIfAbsent(policyId, id -> policyDAO.getById(id).getName());

    Map<String, PolicyWaiverReason> policyWaiverReasonMap = policyWaiverReasonDAO
        .getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    AppliedWaivers result = new AppliedWaivers();
    List<Owner> owners = ownerDAO.getOwnersInHierarchy(ownerId, ownerType);

    ComponentIdentifier componentIdentifier = null;
    for (Owner owner : owners) {
      componentIdentifier = getComponentIdentifierFromOwnerAndHash(owner, hash);
      if (componentIdentifier != null) {
        break;
      }
    }
    PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    List<String> ownerIds = owners.stream().map(Owner::getId).collect(Collectors.toList());

    // Applicable and expired waivers are independent point-in-time reads for this read-only view; no snapshot across
    // the two is required.
    Map<String, List<PolicyWaiver>> applicableByOwnerId =
        policyWaiverDAO.getApplicableToComponentIncludingAllVersionsByOwnerIds(ownerIds, hash, purl);
    Map<String, List<PolicyWaiverDTO>> expiredByOwnerId = policyWaiverService.getExpiredWaiversByOwnerIds(
        ownerIds, hash, policyNameLoader, componentIdentifier, policyWaiverReasonMap);

    for (Owner owner : owners) {
      List<PolicyWaiverDTO> applicableWaivers = applicableByOwnerId.getOrDefault(owner.getId(), List.of())
          .stream()
          .map(waiver -> policyWaiverService.mapPolicyWaiverToDTO(waiver, policyNameLoader, policyWaiverReasonMap))
          .collect(Collectors.toList());
      result.add(owner, applicableWaivers);
      result.addExpired(owner, expiredByOwnerId.getOrDefault(owner.getId(), List.of()));
    }

    return result;
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
    Policy policy = policyDAO.getByIdNotNull(policyId);
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    ApplicableContext context = null;
    boolean foundPolicyInHierarchy = false;
    // walkHierarchy stays lazy so we can stop once the policy owner is found
    for (Owner owner : ownerDAO.walkHierarchy(ownerId, ownerType)) {
      ApplicableContext currentContext = new ApplicableContext(getRestOwnerId(owner), owner.getName(), owner.getType());
      if (context != null) {
        currentContext.setChildren(new ArrayList<>());
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

    public List<WaiversByOwner> expiredWaiversByOwner = new ArrayList<>();

    void add(Owner owner, List<PolicyWaiverDTO> waivers) {
      if (waivers == null || waivers.isEmpty()) {
        return;
      }
      WaiversByOwner wbo = mapWaiversToDTO(owner, waivers);
      waiversByOwner.add(wbo);
    }

    void addExpired(Owner owner, List<PolicyWaiverDTO> waivers) {
      if (waivers == null || waivers.isEmpty()) {
        return;
      }
      WaiversByOwner wbo = mapWaiversToDTO(owner, waivers);
      expiredWaiversByOwner.add(wbo);
    }

    private WaiversByOwner mapWaiversToDTO(Owner owner, List<PolicyWaiverDTO> waivers) {
      String ownerId = getRestOwnerId(owner);
      for (PolicyWaiver waiver : waivers) {
        waiver.setOwnerId(ownerId);
      }
      WaiversByOwner wbo = new WaiversByOwner();
      wbo.ownerId = ownerId;
      wbo.ownerName = owner.getName();
      wbo.ownerType = owner.getType();
      wbo.waivers = waivers;
      return wbo;
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

    public String reasonText;

    public String policyWaiverReasonId;

    @JsonIgnore
    @Override
    public String getWaiverReasonId() {
      return null;
    }
  }
}

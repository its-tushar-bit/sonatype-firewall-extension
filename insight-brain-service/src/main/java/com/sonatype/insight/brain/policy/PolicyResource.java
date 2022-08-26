/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.utils.NgUploadResponseGenerator;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.plexus.util.IOUtil;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;

@Named
@Timed
@Path(PolicyResource.RESOURCE_PATH)
public class PolicyResource
{
  public static final String RESOURCE_PATH =
      "rest/policy/{ownerType: application|organization|repository_container}/{ownerId}";

  private static final Logger log = LoggerFactory.getLogger(PolicyResource.class);

  private final PolicyImportExport policyImportExport;

  private final NgUploadResponseGenerator ngUploadResponseGenerator;

  private final OwnerDAO ownerDAO = new OwnerDAO();

  private final ManagementEventService managementEventService;

  @Inject
  public PolicyResource(
      PolicyImportExport policyImportExport,
      NgUploadResponseGenerator ngUploadResponseGenerator,
      final ManagementEventService managementEventService)
  {
    this.policyImportExport = policyImportExport;
    this.ngUploadResponseGenerator = ngUploadResponseGenerator;
    this.managementEventService = managementEventService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public List<Policy> getPolicies(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId)
  {
    log.debug("Received request to get all policies for {} id {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return new PolicyDAO().getByOwnerId(internalOwnerId);
  }

  /**
   * @since 1.6
   */
  @GET
  @Path("applicable")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public ApplicablePolicies getApplicablePolicies(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    log.debug("Received request to get all applicable policies for {} id {}", ownerType, ownerId);

    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    // Get all applicable policies
    List<Policy> policies = new PolicyDAO().getApplicableByOwnerIdWithHierarchy(ownerId);

    // Init the result structure
    PolicyTagDAO policyTagDAO = new PolicyTagDAO();
    Map<String, PoliciesByOwner> policiesByOwnerId = new LinkedHashMap<>();
    for (Owner currentOwner : ownerDAO.walkHierarchy(ownerId)) {
      String currentOwnerId = currentOwner.getId();
      if (currentOwner instanceof Application) {
        policiesByOwnerId.put(currentOwnerId,
            new PoliciesByOwner(currentOwnerId, currentOwner.getName(), currentOwner.getType()));
      }
      else {
        policiesByOwnerId.put(
            currentOwnerId,
            new PoliciesByOwner(currentOwnerId, currentOwner.getName(), currentOwner.getType(), policyTagDAO
                .getByOrganizationId(currentOwnerId)));
      }
    }

    // Add the applicable policies by owner to the result structure
    for (Policy policy : policies) {
      policiesByOwnerId.get(policy.getOwnerId()).policies.add(policy);
    }

    ApplicablePolicies result = new ApplicablePolicies();
    result.policiesByOwner = new ArrayList<>();
    result.policiesByOwner.addAll(policiesByOwnerId.values());
    return result;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.CREATE_POLICY)
  public Policy addPolicy(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
                          @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
                          final Policy policy)
  {
    log.debug("Received request to add {} policy for ownerId {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    policy.setOwnerId(internalOwnerId);
    new PolicyDAO().insert(policy);
    AuditData.get().setPolicyWithDetails(policy);
    managementEventService.postEvent(CREATED, policy);

    return policy;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.UPDATE_POLICY)
  public Policy updatePolicy(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
                             @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
                             final Policy policy)
  {
    log.debug("Received request to update {} policy for ownerId {}, policyId {}", ownerType, ownerId, policy.getId());

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    PolicyDAO policyDAO = new PolicyDAO();
    if (!internalOwnerId.equals(policyDAO.getByIdNotNull(policy.getId()).getOwnerId())) {
      throw new NotFoundException("Cannot find a policy with id " + policy.getId() + " for owner id " + ownerId);
    }

    policy.setOwnerId(internalOwnerId);
    policyDAO.update(policy);
    AuditData.get().setPolicyWithDetails(policy);

    managementEventService.postEvent(UPDATED, policy);

    return policy;
  }

  @PUT
  @Path("{policyId}/actionsOverrides")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.ADD_ACTIONS_OVERRIDE)
  public Policy addActionsOverride(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
      @PathParam("policyId") final String policyId,
      final Map<String, String> actionsOverride)
  {
    log.debug("Received request to addActionsOverride for ownerId {}, policyId {}", ownerId, policyId);

    PolicyDAO policyDAO = new PolicyDAO();
    Policy policy = policyDAO.getByIdNotNull(policyId);
    if (!policy.isPolicyActionsOverrideAllowed()) {
      throw new BadRequestException("Actions override is not allowed for policy with id " + policy.getId());
    }

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    policy.addPolicyActionsOverride(internalOwnerId, actionsOverride);
    policyDAO.update(policy);
    AuditData.get().setPolicy(policy).setData("overridingOwnerId", ownerId)
      .setData("actionsOverride", actionsOverride);

    managementEventService.postEvent(UPDATED, policy);

    return policy;
  }

  @DELETE
  @Path("{policyId}/actionsOverrides")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.REMOVE_ACTIONS_OVERRIDE)
  public Policy deleteActionsOverride(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
      @PathParam("policyId") final String policyId)
  {
    log.debug("Received request to delete {} policy's actions overrides for ownerId {}, policyId {}", ownerType,
        ownerId, policyId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    Policy policy = new PolicyDAO().getByIdNotNull(policyId);

    Map<String, Map<String, String>> policyActionOverrides = policy.getPolicyActionsOverrides();
    if (policyActionOverrides != null && !policyActionOverrides.isEmpty()
        && policyActionOverrides.containsKey(internalOwnerId)) {
      policyActionOverrides.remove(internalOwnerId);
      policy.setPolicyActionsOverrides(policyActionOverrides);
      new PolicyDAO().update(policy);
    }

    AuditData.get().setPolicy(policy).setData("overridingOwnerId", ownerId);
    managementEventService.postEvent(UPDATED, policy);

    return policy;
  }

  @DELETE
  @Path("{policyId}")
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.DELETE_POLICY)
  public void deletePolicy(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
                           @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
                           @PathParam("policyId") final String policyId)
  {
    log.debug("Received request to delete {} policy for ownerId {}, policyId {}", ownerType, ownerId, policyId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    PolicyDAO policyDAO = new PolicyDAO();
    Policy policy = policyDAO.getByIdNotNull(policyId);
    if (!internalOwnerId.equals(policy.getOwnerId())) {
      throw new NotFoundException("Cannot find a policy with ID " + policyId + " for " + ownerType + " ID " + ownerId);
    }

    policyDAO.delete(policy);
    AuditData.get().setPolicyWithDetails(policy);
    managementEventService.postEvent(DELETED, policy);
  }

  @GET
  @Path("export")
  @Produces(MediaType.APPLICATION_JSON)
  public PolicyExportResult exportPolicies(@PathParam("ownerType") final OwnerType ownerType,
                                           @PathParam("ownerId") String ownerId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    if (OwnerType.ORGANIZATION.equals(ownerType)) {
      return policyImportExport.exportOrganization(new OrganizationDAO().getByIdNotNull(internalOwnerId));
    }
    else {
      return policyImportExport.exportApplication(new ApplicationDAO().getByIdNotNull(internalOwnerId));
    }
  }

  @POST
  @Path("import")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.IMPORT)
  public Response importPolicies(@PathParam("ownerType") final OwnerType ownerType,
                                 @PathParam("ownerId") final String ownerId,
                                 @FormDataParam("file") final InputStream is,
                                 @FormDataParam(AntiCsrfFilter.CSRF_HEADER_NAME) String csrfToken,
                                 @Context HttpHeaders headers,
                                 @QueryParam("noFormData") boolean noFormData) throws Exception
  {
    return ngUploadResponseGenerator.run(csrfToken, headers, noFormData, new Callable<PolicyImportResult>()
    {
      @Override
      public PolicyImportResult call() throws Exception {
        return importPolicies(ownerType, ownerId, is);
      }
    });
  }

  private PolicyImportResult importPolicies(OwnerType ownerType, String ownerId, InputStream in) throws IOException {
    if (OwnerType.ORGANIZATION.equals(ownerType)) {
      PolicyExportResult exportDTO = readPolicyExportResult(in);
      return policyImportExport.importOrganization(new OrganizationDAO().getByIdNotNull(ownerId), exportDTO);
    }
    else {
      throw new BadRequestException("Importing policies into an application is no longer supported.");
    }
  }

  private PolicyExportResult readPolicyExportResult(InputStream stream) throws IOException {
    byte[] importBytes;
    try {
      importBytes = IOUtil.toByteArray(stream);
    }
    finally {
      IOUtil.close(stream);
    }
    PolicyExportResult policyExportResult;
    try {
      policyExportResult = JsonUtils.parse(importBytes, PolicyExportResult.class);
    }
    catch (IOException e) {
      log.error("Policy file import failure, unable to marshal from json", e);
      throw new BadRequestException("The file you selected failed to upload correctly, are you certain it is a properly"
        + " formatted policy import json file?");
    }
    // Any random json file can be uploaded and result in an empty PolicyImportResult. It does not make sense to import
    // policies from a file without policies.
    if (CollectionUtils.isEmpty(policyExportResult.policies)) {
      throw new BadRequestException("The file you selected failed to upload correctly, the policy file needs to have at"
        + " least one policy defined.");
    }

    // Ensure that tags are not null. The importer expects non-null fields
    if (policyExportResult.tags == null) {
      policyExportResult.tags = new ArrayList<>();
    }
    if (policyExportResult.policyTags == null) {
      policyExportResult.policyTags = new ArrayList<>();
    }

    return policyExportResult;
  }

  public static class ApplicablePolicies
  {
    public List<PoliciesByOwner> policiesByOwner;
  }

  public static class PoliciesByOwner
  {
    public PoliciesByOwner() {
    }

    public PoliciesByOwner(String ownerId, String ownerName, OwnerType ownerType) {
      this(ownerId, ownerName, ownerType, new ArrayList<PolicyTag>());
    }

    public PoliciesByOwner(String ownerId, String ownerName, OwnerType ownerType, List<PolicyTag> policyTags) {
      this.ownerId = ownerId;
      this.ownerName = ownerName;
      this.ownerType = ownerType;
      this.policyTags = policyTags;
      policies = new ArrayList<>();
    }

    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public List<Policy> policies;

    public List<PolicyTag> policyTags;
  }
}

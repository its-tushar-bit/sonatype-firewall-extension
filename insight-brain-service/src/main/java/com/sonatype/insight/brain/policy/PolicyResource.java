/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.utils.NgUploadResponseGenerator;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.brain.product.license.RequiresEntitlement;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.repository.RepositoryType.proxy;
import static com.sonatype.insight.brain.policy.PolicyMaintenanceTelemetry.Action.CREATE;
import static com.sonatype.insight.brain.policy.PolicyMaintenanceTelemetry.Action.DELETE;
import static com.sonatype.insight.brain.policy.PolicyMaintenanceTelemetry.Action.UPDATE;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

@Named
@Timed
@Path(PolicyResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)
public class PolicyResource
{
  public static final String RESOURCE_PATH =
      "rest/policy/{ownerType: application|organization|repository_container|repository_manager|repository}/{ownerId}";

  static final String NOTIFICATIONS_PATH = "notifications";

  private static final Logger log = LoggerFactory.getLogger(PolicyResource.class);

  private final PolicyImportExport policyImportExport;

  private final NgUploadResponseGenerator ngUploadResponseGenerator;

  private final ManagementEventService managementEventService;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  private final OwnerDAO ownerDAO;

  private final PolicyTagDAO policyTagDAO;

  private final PolicyDAO policyDAO;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final IdUtils idUtils;

  @Inject
  public PolicyResource(
      PolicyImportExport policyImportExport,
      NgUploadResponseGenerator ngUploadResponseGenerator,
      final ManagementEventService managementEventService,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final OwnerDAO ownerDAO,
      final PolicyTagDAO policyTagDAO,
      final PolicyDAO policyDAO,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final IdUtils idUtils)
  {
    this.policyImportExport = policyImportExport;
    this.ngUploadResponseGenerator = ngUploadResponseGenerator;
    this.managementEventService = managementEventService;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
    this.ownerDAO = ownerDAO;
    this.policyTagDAO = policyTagDAO;
    this.policyDAO = policyDAO;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.idUtils = idUtils;
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public List<Policy> getPolicies(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId)
  {
    log.debug("Received request to get all policies for {} id {}", ownerType, ownerId);

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    return policyDAO.getByOwnerId(internalOwnerId);
  }

  /**
   * @since 170
   */
  @GET
  @Path("withProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode")
  @Produces(MediaType.APPLICATION_JSON)
  public ProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodePolicies getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode() {
    checkReadPermission(RepositoryContainer.SINGLETON);
    List<Policy> proprietaryNameConflictPolicies = new ArrayList<>();
    List<Policy> securityVulnerabilityCategoryMaliciousCodePolicies = new ArrayList<>();

    Stream<String> containerAndRootIds =
        Stream.of(RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID);
    Stream<String> repositoryIds = repositoryDAO.getByRepositoryType(proxy).stream().map(Repository::getId);
    Stream<String> repositoryManagerIds = repositoryManagerDAO.getAll().stream().map(RepositoryManager::getId);

    List<Policy> policies = concat(containerAndRootIds, concat(repositoryIds, repositoryManagerIds))
        .collect(collectingAndThen(toSet(), policyDAO::getByOwnerIds));

    for (Policy policy : policies) {
      boolean hasSecurityVulnerabilityCategoryMaliciousCode = false;
      boolean hasProprietaryNameConflict = false;
      for (Iterator<Constraint> iterator =
          policy.getConstraints().iterator(); !hasSecurityVulnerabilityCategoryMaliciousCode
              && !hasProprietaryNameConflict && iterator.hasNext();)
      {
        Constraint constraint = iterator.next();
        for (Condition condition : constraint.getConditions()) {
          if (condition.getConditionTypeId().equals(ProprietaryNameConflictConditionType.ID)
              && condition.getOperator().equals(ProprietaryNameConflictConditionType.OP_IS_PRESENT))
          {
            hasProprietaryNameConflict = true;
          }
          else if (condition.getConditionTypeId().equals(SecurityVulnerabilityCategoryConditionType.ID)
              && condition.getOperator()
                  .equals(
                      ConditionTypes.SecurityVulnerabilityCategoryConditionType.getSupportedOperators().get(0))
              && condition.getValue().equals(SecurityVulnerabilityCategory.MALICIOUS_CODE.getId()))
          {
            hasSecurityVulnerabilityCategoryMaliciousCode = true;
          }
          if (hasProprietaryNameConflict && hasSecurityVulnerabilityCategoryMaliciousCode) {
            break;
          }
        }
      }
      if (hasProprietaryNameConflict) {
        proprietaryNameConflictPolicies.add(policy);
      }
      if (hasSecurityVulnerabilityCategoryMaliciousCode) {
        securityVulnerabilityCategoryMaliciousCodePolicies.add(policy);
      }
    }

    ProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodePolicies result =
        new ProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodePolicies();
    result.proprietaryNameConflictPolicies = new ArrayList<>();
    result.proprietaryNameConflictPolicies.addAll(proprietaryNameConflictPolicies);
    result.securityVulnerabilityCategoryMaliciousCodePolicies = new ArrayList<>();
    result.securityVulnerabilityCategoryMaliciousCodePolicies.addAll(
        securityVulnerabilityCategoryMaliciousCodePolicies);
    return result;
  }

  /**
   * @since 1.6
   */
  @GET
  @Path("applicable")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_READ_ONLY)
  public ApplicablePolicies getApplicablePolicies(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    log.debug("Received request to get all applicable policies for {} id {}", ownerType, ownerId);

    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    // Get all applicable policies
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(ownerId);

    // Init the result structure
    List<Owner> hierarchyOwners = new ArrayList<>();
    ownerDAO.walkHierarchy(ownerId).forEach(hierarchyOwners::add);
    List<String> orgOwnerIds = hierarchyOwners.stream()
        .filter(o -> !(o instanceof Application))
        .map(Owner::getId)
        .collect(toList());
    Map<String, List<PolicyTag>> policyTagsByOrgId = policyTagDAO.getByOrganizationIdsGrouped(orgOwnerIds);
    Map<String, PoliciesByOwner> policiesByOwnerId = new LinkedHashMap<>();
    for (Owner currentOwner : hierarchyOwners) {
      String currentOwnerId = currentOwner.getId();
      if (currentOwner instanceof Application) {
        policiesByOwnerId.put(currentOwnerId,
            new PoliciesByOwner(currentOwnerId, currentOwner.getName(), currentOwner.getType()));
      }
      else {
        policiesByOwnerId.put(
            currentOwnerId,
            new PoliciesByOwner(currentOwnerId, currentOwner.getName(), currentOwner.getType(),
                policyTagsByOrgId.getOrDefault(currentOwnerId, Collections.emptyList())));
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
  @RequiresEntitlement(LicensedFeature.CUSTOM_POLICIES)
  public Policy addPolicy(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
      final Policy policy)
  {
    log.debug("Received request to add {} policy for ownerId {}", ownerType, ownerId);

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    policy.setOwnerId(internalOwnerId);
    policyDAO.insert(policy);
    AuditData.get().setPolicyWithDetails(policy);
    managementEventService.postEvent(CREATED, policy);
    telemetrySender.send(PolicyMaintenanceTelemetry.getTelemetry(CREATE, telemetryUtils.obfuscate(ownerId), policy));
    return policy;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.UPDATE_POLICY)
  public Policy updatePolicy(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
      final Policy policy)
  {
    log.debug("Received request to update {} policy for ownerId {}, policyId {}", ownerType, ownerId, policy.getId());

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    if (!internalOwnerId.equals(policyDAO.getByIdNotNull(policy.getId()).getOwnerId())) {
      throw new NotFoundException("Cannot find a policy with id " + policy.getId() + " for owner id " + ownerId);
    }

    policy.setOwnerId(internalOwnerId);
    policyDAO.update(policy);
    AuditData.get().setPolicyWithDetails(policy);

    managementEventService.postEvent(UPDATED, policy);
    telemetrySender.send(PolicyMaintenanceTelemetry.getTelemetry(UPDATE, telemetryUtils.obfuscate(ownerId), policy));
    return policy;
  }

  @PUT
  @Path(NOTIFICATIONS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.UPDATE_POLICY)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_READ_ONLY)
  public Policy updatePolicyNotifications(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      Policy policy)
  {
    log.debug("Received request to update {} policy notifications for ownerId {}, policyId {}", ownerType, ownerId,
        policy.getId());

    Policy originalPolicy = policyDAO.getById(policy.getId());
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    if (originalPolicy == null || !internalOwnerId.equals(originalPolicy.getOwnerId())) {
      throw new NotFoundException("Cannot find a policy with id " + policy.getId() + " for owner id " + ownerId);
    }

    originalPolicy.setPolicyNotificationsOverrideAllowed(policy.isPolicyNotificationsOverrideAllowed());
    originalPolicy.setPolicyNotificationsOverrides(policy.getPolicyNotificationsOverrides());
    originalPolicy.setNotifications(policy.getNotifications());

    policyDAO.update(originalPolicy);
    AuditData.get().setPolicyWithDetails(originalPolicy);

    managementEventService.postEvent(UPDATED, originalPolicy);
    telemetrySender.send(
        PolicyMaintenanceTelemetry.getTelemetry(UPDATE, telemetryUtils.obfuscate(ownerId), originalPolicy));
    return originalPolicy;
  }

  @PUT
  @Path("{policyId}/overrides")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.UPDATE_OVERRIDES)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_READ_ONLY)
  public Policy updateOverrides(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("policyId") final String policyId,
      JsonNode jsonNode)
  {
    log.debug("Received request to update overrides for ownerId {}, policyId {}", ownerId, policyId);

    AuditData auditData = AuditData.get().setData("overridingOwnerId", ownerId);

    Policy policy = policyDAO.getByIdNotNull(policyId);
    auditData.setPolicy(policy);
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    boolean actionsOverridesUpdateNeeded = jsonNode != null && jsonNode.has("actions");
    boolean notificationsOverridesUpdateNeeded = jsonNode != null && jsonNode.has("notifications");

    if (!actionsOverridesUpdateNeeded && !notificationsOverridesUpdateNeeded) {
      throw new BadRequestException("A policy overrides configuration must be specified.");
    }
    PolicyOverridesDTO policyOverridesDTO;
    try {
      policyOverridesDTO = JsonUtils.asPojo(jsonNode, PolicyOverridesDTO.class);
    }
    catch (IOException e) {
      throw new BadRequestException("The given JSON cannot be deserialized into a policy overrides configuration.");
    }

    if (actionsOverridesUpdateNeeded) {
      if (policyOverridesDTO.actions == null) {
        Map<String, Map<String, String>> policyActionOverrides = policy.getPolicyActionsOverrides();
        if (MapUtils.getObject(policyActionOverrides, internalOwnerId) != null) {
          policyActionOverrides.remove(internalOwnerId);
        }
        auditData.setData("actionsOverride", "null");
      }
      else {
        if (!policy.isPolicyActionsOverrideAllowed()) {
          throw new BadRequestException("Actions override is not allowed for policy with id " + policy.getId());
        }
        policy.addPolicyActionsOverride(internalOwnerId, policyOverridesDTO.actions);
        auditData.setData("actionsOverride", policyOverridesDTO.actions);
      }
    }

    if (notificationsOverridesUpdateNeeded) {
      if (policyOverridesDTO.notifications == null) {
        Map<String, Notifications> policyNotificationsOverrides = policy.getPolicyNotificationsOverrides();
        if (MapUtils.getObject(policyNotificationsOverrides, internalOwnerId) != null) {
          policyNotificationsOverrides.remove(internalOwnerId);
        }
        auditData.setData("notificationsOverride", "null");
      }
      else {
        if (!policy.isPolicyNotificationsOverrideAllowed()) {
          throw new BadRequestException("Notifications override is not allowed for policy with id " + policy.getId());
        }
        policy.addPolicyNotificationsOverride(internalOwnerId, policyOverridesDTO.notifications);
        auditData.setData("notificationsOverride", policyOverridesDTO.notifications);
      }
    }

    policyDAO.update(policy);
    managementEventService.postEvent(UPDATED, policy);
    telemetrySender.send(PolicyMaintenanceTelemetry.getTelemetry(UPDATE, telemetryUtils.obfuscate(ownerId), policy));

    return policy;
  }

  @DELETE
  @Path("{policyId}")
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.DELETE_POLICY)
  @RequiresEntitlement(LicensedFeature.CUSTOM_POLICIES)
  public void deletePolicy(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
      @PathParam("policyId") final String policyId)
  {
    log.debug("Received request to delete {} policy for ownerId {}, policyId {}", ownerType, ownerId, policyId);

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    Policy policy = policyDAO.getByIdNotNull(policyId);
    if (!internalOwnerId.equals(policy.getOwnerId())) {
      throw new NotFoundException("Cannot find a policy with ID " + policyId + " for " + ownerType + " ID " + ownerId);
    }

    policyDAO.delete(policy);
    AuditData.get().setPolicyWithDetails(policy);
    managementEventService.postEvent(DELETED, policy);
    telemetrySender.send(PolicyMaintenanceTelemetry.getTelemetry(DELETE, telemetryUtils.obfuscate(ownerId), policy));
  }

  @GET
  @Path("export")
  @Produces(MediaType.APPLICATION_JSON)
  public PolicyExportResult exportPolicies(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    if (OwnerType.ORGANIZATION.equals(ownerType)) {
      return policyImportExport.exportOrganization(organizationDAO.getByIdNotNull(internalOwnerId));
    }
    else {
      return policyImportExport.exportApplication(applicationDAO.getByIdNotNull(internalOwnerId));
    }
  }

  @POST
  @Path("import")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.IMPORT)
  @RequiresEntitlement(LicensedFeature.CUSTOM_POLICIES)
  public Response importPolicies(
      @PathParam("ownerType") final OwnerType ownerType,
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
      return policyImportExport.importOrganization(organizationDAO.getByIdNotNull(ownerId), exportDTO);
    }
    else {
      throw new BadRequestException("Importing policies into an application is no longer supported.");
    }
  }

  private PolicyExportResult readPolicyExportResult(InputStream stream) throws IOException {
    byte[] importBytes;
    try {
      importBytes = IOUtils.toByteArray(stream);
    }
    finally {
      IOUtils.close(stream);
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

  public static class ProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodePolicies
  {
    public List<Policy> proprietaryNameConflictPolicies;

    public List<Policy> securityVulnerabilityCategoryMaliciousCodePolicies;
  }

  public static class PoliciesByOwner
  {
    public PoliciesByOwner() {
    }

    public PoliciesByOwner(String ownerId, String ownerName, OwnerType ownerType) {
      this(ownerId, ownerName, ownerType, new ArrayList<>());
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

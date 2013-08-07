/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.client.utils.AuditUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.json.store.JsonUtils;

import static com.sonatype.insight.brain.utils.IdUtils.*;

@Named
@Path(PolicyResource.SERVICE_PATH)
public class PolicyResource
{
  public static final String SERVICE_PATH = "rest/policy/{ownerType: application|organization}/{ownerId}";

  private static final Logger log = LoggerFactory.getLogger(PolicyResource.class);

  public static final String ORG_IMPORT_LTG_ERROR = "Organization already has license threat groups besides the default "
      + "ones defined, cannot import data unless the Organization is new.";

  public static final String ORG_IMPORT_LABEL_ERROR = "Organization already has labels defined, cannot import data "
      + "unless the Organization is new.";

  public static final String ORG_IMPORT_POLICY_ERROR = "Organization already has policies defined, cannot import data "
      + "unless the Organization is new.";

  public static final String ORG_IMPORT_APP_ERROR = "Organization already has applications defined, cannot import data "
      + "unless the Organization is new.";

  @Context
  private InsightWork work;

  @Context
  private BaseUrl baseUrl;

  @Inject
  private CLMLicenseManager licenseManager;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Policy> getPolicies(@PathParam("ownerType") final String ownerType,
      @PathParam("ownerId") final String ownerId)
  {
    log.debug("Received request to get all policies for {} id {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return policyDAO().getByOwnerId(internalOwnerId);
  }

  /**
   * @since 1.6
   */
  @GET
  @Path("applicable")
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicablePolicies getApplicablePolicies(@PathParam("ownerType") final String ownerType,
      @PathParam("ownerId") final String ownerId)
  {
    log.debug("Received request to get all applicable policies for {} id {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    ApplicablePolicies result = new ApplicablePolicies();

    result.policiesByOwner = new ArrayList<PoliciesByOwner>();
    String organizationId;
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application application = new ApplicationDAO().getByIdNotNull(internalOwnerId);
      PoliciesByOwner policiesByOwner = new PoliciesByOwner();
      policiesByOwner.ownerId = application.getId();
      policiesByOwner.ownerName = application.getName();
      policiesByOwner.ownerType = IdUtils.TYPE_APPLICATION;
      policiesByOwner.policies = policyDAO().getByOwnerId(application.getId());
      result.policiesByOwner.add(policiesByOwner);
      organizationId = application.getOrganizationId();
    }
    else {
      organizationId = internalOwnerId;
    }
    if (organizationId != null) {
      Organization organization = new OrganizationDAO().getByIdNotNull(organizationId);
      PoliciesByOwner policiesByOwner = new PoliciesByOwner();
      policiesByOwner.ownerId = organization.getId();
      policiesByOwner.ownerName = organization.getName();
      policiesByOwner.ownerType = IdUtils.TYPE_ORGANIZATION;
      policiesByOwner.policies = policyDAO().getByOwnerId(organization.getId());
      result.policiesByOwner.add(policiesByOwner);
    }

    return result;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Policy addPolicy(@PathParam("ownerType") final String ownerType, @PathParam("ownerId") final String ownerId,
      final Policy policy, @QueryParam("user") final String user, @QueryParam("where") final String where,
      @Context final HttpServletRequest request)
  {
    log.debug("Received request to add {} policy for ownerId {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return policyDAO().session(user, AuditUtils.findIP(request), where).insert(internalOwnerId, policy);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Policy updatePolicy(@PathParam("ownerType") final String ownerType,
      @PathParam("ownerId") final String ownerId, final Policy policy, @QueryParam("user") final String user,
      @QueryParam("where") final String where, @Context final HttpServletRequest request)
  {
    log.debug("Received request to update {} policy for ownerId {}, policyId {}", ownerType, ownerId, policy.getId());

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return policyDAO().session(user, AuditUtils.findIP(request), where).update(internalOwnerId, policy);
  }

  @DELETE
  @Path("{policyId}")
  public void deletePolicy(@PathParam("ownerType") final String ownerType, @PathParam("ownerId") final String ownerId,
      @PathParam("policyId") final String policyId, @QueryParam("user") final String user,
      @QueryParam("where") final String where, @Context final HttpServletRequest request)
  {
    log.debug("Received request to delete {} policy for ownerId {}, policyId {}", ownerType, ownerId, policyId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    policyDAO().session(user, AuditUtils.findIP(request), where).delete(internalOwnerId, policyId);
  }

  @GET
  @Path("export")
  @Produces(MediaType.APPLICATION_JSON)
  public PolicyExportResult exportPolicies(@PathParam("ownerType") final String ownerType,
      @PathParam("ownerId") String ownerId)
  {
    if (!TYPE_APPLICATION.equals(ownerType)) {
      throw new BadRequestException("Policy export is only supported for applications");
    }
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    PolicyExportResult exportDTO = new PolicyExportResult();
    exportDTO.policies = policyDAO().getByOwnerId(internalOwnerId);
    exportDTO.labels = new LabelDAO().getByOwnerId(internalOwnerId);
    exportDTO.licenseThreatGroups = new LicenseThreatGroupDAO().getByOwnerId(internalOwnerId);
    exportDTO.licenseThreatGroupLicenses = new LicenseThreatGroupLicenseDAO().getByOwnerId(internalOwnerId);

    return exportDTO;
  }

  private Label getLabelByName(List<Label> labels, String nameLowercase) {
    for (Label label : labels) {
      if (nameLowercase.equals(label.getLabelLowercase())) {
        return label;
      }
    }
    return null;
  }

  @PUT
  @Path("import")
  @Produces(MediaType.APPLICATION_JSON)
  public PolicyImportResult importPolicies(@PathParam("ownerType") final String ownerType,
      @PathParam("ownerId") String ownerId, @Context HttpServletRequest servletRequest) throws IOException
  {
    PolicyExportResult exportDTO = readPolicyExportResult(servletRequest.getInputStream());
    if (!TYPE_APPLICATION.equals(ownerType)) {
      return importFromApplicationToOrganization(ownerId, exportDTO);
    }

    return importApplication(ownerId, exportDTO);
  }

  /**
   * Import an existing Application export to an Organization. Supported only as an update mechanism
   * between v1.5 and v1.6
   * 
   * @since 1.6
   */
  private PolicyImportResult importFromApplicationToOrganization(String orgId, PolicyExportResult exportDTO) {
    // ensure that Org exists and does not already have Apps, Policy, Label or LTGs
    OrganizationDAO organizationDAO = new OrganizationDAO();
    Organization organization = organizationDAO.getByIdNotNull(orgId);
    List<Application> applications = new ApplicationDAO().getByOrganizationId(orgId);
    if (!applications.isEmpty()) {
      throw new BadRequestException(ORG_IMPORT_APP_ERROR);
    }

    PolicyDAO policyDAO = policyDAO();
    List<Policy> policies = policyDAO.getByOwnerId(organization.getId());
    if (!policies.isEmpty()) {
      throw new BadRequestException(ORG_IMPORT_POLICY_ERROR);
    }

    List<Label> labels = new LabelDAO().getByOwnerId(organization.getId());
    if (!labels.isEmpty()) {
      throw new BadRequestException(ORG_IMPORT_LABEL_ERROR);
    }

    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(orgId);

    int size = licenseThreatGroups.size();
    if (size != 4 && size != 0) {
      throw new BadRequestException(ORG_IMPORT_LTG_ERROR);
    }

    EntityManager em = organizationDAO.createEntityManager();
    try {
      em.getTransaction().begin();

      LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
      ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();
      LabelDAO labelDAO = new LabelDAO();

      // Set labels with Org as the owner
      for (Label label : exportDTO.labels) {
        label.setOwnerId(orgId);
        labelDAO.update(em, label);
        for (ComponentLabel componentLabel : componentLabelDAO.getByLabelId(em, label.getId())) {
          componentLabel.setOwnerId(orgId);
          componentLabelDAO.update(em, componentLabel);
        }
      }

      if (!exportDTO.licenseThreatGroups.isEmpty()) {
        // Delete existing(default) LTGs from Organization to prevent conflict with imported LTGs
        for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
          licenseThreatGroupDAO.delete(em, licenseThreatGroup);
        }

        // Set LTGs with Org as the owner
        for (LicenseThreatGroup licenseThreatGroup : exportDTO.licenseThreatGroups) {
          licenseThreatGroup.setOwnerId(orgId);
          licenseThreatGroupDAO.update(em, licenseThreatGroup);
        }
      }

      if (!exportDTO.licenseThreatGroupLicenses.isEmpty()) {
        // Set LTGLs with Org as the owner
        for (LicenseThreatGroupLicense licenseThreatGroupLicense : exportDTO.licenseThreatGroupLicenses) {
          licenseThreatGroupLicense.setOwnerId(orgId);
          licenseThreatGroupLicenseDAO.update(em, licenseThreatGroupLicense);
        }
      }

      em.getTransaction().commit();

      // Create org policies from exportDTO. Since this is not stored in the DB, the strategy of changing the ownerId
      // and
      // updating does not work.
      for (Policy policy : exportDTO.policies) {
        // remove existing policy if it exists
        if (policyDAO.getByOwnerIdAndPolicyId(policy.getOwnerId(), policy.getId()) != null) {
          policyDAO.delete(policy.getOwnerId(), policy.getId());
        }
        policy.setOwnerId(orgId);
        policyDAO.insert(orgId, policy);
      }
    }
    finally {
      OrganizationDAO.close(em);
    }

    PolicyImportResult result = new PolicyImportResult();
    result.applicationName = organization.getName();
    UriBuilder uriBuilder = baseUrl.redirect().path(InsightBrainService.BRAIN_ASSET_PATH).path("index.html")
        .fragment("/management/organization/" + organization.getId());
    result.applicationURL = uriBuilder.build().toString();
    return result;
  }

  /**
   * Import an Application, either by creating a new Application or modifying an existing one.
   */
  private PolicyImportResult importApplication(String appId, PolicyExportResult exportDTO) {
    Application application;
    ApplicationDAO applicationDAO = new ApplicationDAO();
    EntityManager em = applicationDAO.createEntityManager();
    try {
      em.getTransaction().begin();

      LabelDAO labelDAO = new LabelDAO();
      List<Label> oldLabels = new ArrayList<Label>();
      application = applicationDAO.getByPublicId(em, appId);
      if (application == null) {
        // Create an application
        int appLimit = licenseManager.getApplicationCountLimit();
        if (applicationDAO.getAll(em).size() >= appLimit) {
          throw new PaymentRequiredException("You have exceeded the licensed limit of " + appLimit + " applications.");
        }

        application = new Application();
        application.setPublicId(appId);
        application.setName(appId);
        if (applicationDAO.getByName(em, application.getName()) != null) {
          application.setName(application.getName() + " " + System.currentTimeMillis());
        }

        applicationDAO.insert(em, application);
      }
      else {
        // The application already exists. Delete all its license threat groups and policies.
        // Do not delete its labels - labels need to be merged.
        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(em, application.getId());
        for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
          licenseThreatGroupDAO.delete(em, licenseThreatGroup);
        }

        policyDAO().deleteByOwnerId(application.getId());

        oldLabels.addAll(labelDAO.getByOwnerId(em, application.getId()));
      }
      String applicationId = application.getId();

      if (exportDTO.labels.size() > 0) {
        Map<String, String> idMap = new HashMap<String, String>();
        // include any existing org labels, in case they're used in app policies. These are NOT candidates for deletion.
        if (application.getOrganizationId() != null) {
          for (Label label : labelDAO.getByOwnerId(application.getOrganizationId())) {
            idMap.put(label.getId(), label.getId());
          }
        }
        for (Label label : exportDTO.labels) {
          String oldId = label.getId();
          Label existingLabel = getLabelByName(oldLabels, label.getLabelLowercase());
          if (existingLabel != null) {
            oldLabels.remove(existingLabel);
            existingLabel.setLabel(label.getLabel());
            existingLabel.setColor(label.getColor());
            labelDAO.update(em, existingLabel);
            idMap.put(oldId, existingLabel.getId());
          }
          else {
            label.setId(null);
            label.setOwnerId(applicationId);
            labelDAO.insert(em, label);
            idMap.put(oldId, label.getId());
          }
        }
        for (Policy policy : exportDTO.policies) {
          for (Constraint constraint : policy.getConstraints()) {
            for (Condition condition : constraint.getConditions()) {
              if (LabelConditionType.ID.equals(condition.getConditionTypeId())) {
                condition.setValue(idMap.get(condition.getValue()));
              }
            }
          }
        }
      }
      for (Label label : oldLabels) {
        labelDAO.delete(em, label);
      }

      if (exportDTO.licenseThreatGroups.size() > 0) {
        Map<String, String> idMap = new HashMap<String, String>();
        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        for (LicenseThreatGroup licenseThreatGroup : exportDTO.licenseThreatGroups) {
          String oldId = licenseThreatGroup.getId();
          licenseThreatGroup.setId(null);
          licenseThreatGroup.setOwnerId(applicationId);
          licenseThreatGroupDAO.insert(em, licenseThreatGroup);
          idMap.put(oldId, licenseThreatGroup.getId());
        }
        LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
        for (LicenseThreatGroupLicense licenseThreatGroupLicense : exportDTO.licenseThreatGroupLicenses) {
          licenseThreatGroupLicense.setId(null);
          licenseThreatGroupLicense.setOwnerId(applicationId);
          licenseThreatGroupLicense.setLicenseThreatGroupId(idMap.get(licenseThreatGroupLicense
              .getLicenseThreatGroupId()));
          licenseThreatGroupLicenseDAO.insert(em, licenseThreatGroupLicense);
        }
        for (Policy policy : exportDTO.policies) {
          for (Constraint constraint : policy.getConstraints()) {
            for (Condition condition : constraint.getConditions()) {
              if (LicenseThreatGroupConditionType.ID.equals(condition.getConditionTypeId())) {
                condition.setValue(idMap.get(condition.getValue()));
              }
            }
          }
        }
      }
      em.getTransaction().commit();

      // no transactional support here
      PolicyDAO policyDAO = policyDAO();
      for (Policy policy : exportDTO.policies) {
        policyDAO.insert(application.getId(), policy);
      }
    }
    finally {
      ApplicationDAO.close(em);
    }

    PolicyImportResult result = new PolicyImportResult();
    result.applicationName = application.getName();
    UriBuilder uriBuilder = baseUrl.redirect().path(InsightBrainService.BRAIN_ASSET_PATH).path("index.html")
        .fragment("/management/application/" + appId);
    result.applicationURL = uriBuilder.build().toString();

    return result;
  }

  private PolicyExportResult readPolicyExportResult(InputStream stream) throws IOException {
    byte[] importBytes;
    try {
      importBytes = IOUtil.toByteArray(stream);
    }
    finally {
      IOUtil.close(stream);
    }
    return JsonUtils.parse(importBytes, PolicyExportResult.class);
  }

  private PolicyDAO policyDAO() {
    return new PolicyDAO(work.getWorkDir());
  }

  public static class ApplicablePolicies
  {
    public List<PoliciesByOwner> policiesByOwner;
  }

  public static class PoliciesByOwner
  {
    public String ownerId;

    public String ownerName;

    public String ownerType;

    public List<Policy> policies;
  }
}

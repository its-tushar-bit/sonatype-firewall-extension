/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightWork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.utils.IdUtils.TYPE_APPLICATION;
import static com.sonatype.insight.brain.utils.IdUtils.TYPE_ORGANIZATION;

/**
 * @since 1.7
 */
@Named
public class PolicyImporterImpl
    implements PolicyImporter
{
  private static final Logger log = LoggerFactory.getLogger(PolicyImporterImpl.class);

  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private LabelDAO labelDAO = new LabelDAO();

  private final InsightWork work;

  private final BaseUrl baseUrl;


  @Inject
  public PolicyImporterImpl(InsightWork work, BaseUrl baseUrl) {
    this.work = work;
    this.baseUrl = baseUrl;
  }

  @Authorize(permission = Permission.WRITE)
  public PolicyImportResult importApplication(@AuthzContext(Key.APPLICATION) Application application,
                                              PolicyExportResult exportDTO)
  {
    String appId = application.getId();
    String orgId = application.getOrganizationId();
    EntityManager em = applicationDAO.createEntityManager();

    try {
      em.getTransaction().begin();

      deleteAllPolicyFromDatabase(em, appId, TYPE_APPLICATION);
      importAndMergeLabels(em, exportDTO, labelDAO.getByOwnerId(em, appId), appId, orgId);
      importLicenseThreatData(em, exportDTO, appId);

      em.getTransaction().commit();

      // no transactional support here
      policyDAO().deleteByOwnerId(appId);
      for (Policy policy : exportDTO.policies) {
        policyDAO().insert(appId, policy);
      }
    }
    finally {
      ApplicationDAO.close(em);
    }

    return createResult(application.getName(), application.getPublicId(), TYPE_APPLICATION);
  }

  @Override
  @Authorize(permission = Permission.WRITE)
  public PolicyImportResult importOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION) Organization organization,
                                               final PolicyExportResult exportDTO)
  {
    String orgId = organization.getId();
    EntityManager em = organizationDAO.createEntityManager();
    try {
      em.getTransaction().begin();

      deleteAllPolicyFromDatabase(em, orgId, TYPE_ORGANIZATION);
      importAndMergeLabels(em, exportDTO, labelDAO.getByOwnerId(em, orgId), null, orgId);
      importLicenseThreatData(em, exportDTO, orgId);

      em.getTransaction().commit();

      // no transactional support here
      PolicyDAO policyDAO = policyDAO();
      policyDAO.deleteByOwnerId(orgId);
      for (Application application : applicationDAO.getByOrganizationId(orgId)) {
        policyDAO.deleteByOwnerId(application.getId());
      }
      for (Policy policy : exportDTO.policies) {
        policyDAO.insert(orgId, policy);
      }
    }
    finally {
      OrganizationDAO.close(em);
    }

    return createResult(organization.getName(), orgId, TYPE_ORGANIZATION);
  }

  /**
   * Delete all data related to ownerId from the database.
   * For the Org case, all child Applications LTG/Label data is deleted, as well as LTGs on the org itself.
   * For the App case, only LTGs are deleted.
   *
   * @param ownerId Org/App owning policy
   * @param type    Org/App
   */
  private void deleteAllPolicyFromDatabase(EntityManager em, String ownerId, String type) {
    if (type.equals(TYPE_ORGANIZATION)) {
      for (Application application : applicationDAO.getByOrganizationId(em, ownerId)) {
        log.debug("Deleting application policies from: {} during import of organization: {}", application.getName(),
            ownerId);
        deleteAllPolicyFromDatabase(em, application.getId(), TYPE_APPLICATION);
        for (Label label : labelDAO.getByOwnerId(em, application.getId(), false)) {
          log.debug("Deleting application labels from: {} during import of organization: {}", application.getName(),
              ownerId);
          labelDAO.delete(em, label);
        }
      }
    }
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(em, ownerId);
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      log.debug("Deleting licenseThreatGroup: {} during import for ownerId: {}", licenseThreatGroup.getName(), ownerId);
      licenseThreatGroupDAO.delete(em, licenseThreatGroup);
    }
  }

  /**
   * Will import LicenseThreatGroupLicense and LicenseThreatGroup information from the exportDTO.
   *
   * @param em        entityManager for sharing transaction
   * @param exportDTO exportDTO modified by side-effect to update ids from newly saved objects
   * @param ownerId   the org/app id to import to
   */
  private void importLicenseThreatData(EntityManager em, PolicyExportResult exportDTO, String ownerId) {
    if (exportDTO.licenseThreatGroups.size() > 0) {
      Map<String, String> idMap = new HashMap<>();
      for (LicenseThreatGroup licenseThreatGroup : exportDTO.licenseThreatGroups) {
        String oldId = licenseThreatGroup.getId();
        licenseThreatGroup.setId(null);
        licenseThreatGroup.setOwnerId(ownerId);
        licenseThreatGroupDAO.insert(em, licenseThreatGroup);
        idMap.put(oldId, licenseThreatGroup.getId());
      }
      for (LicenseThreatGroupLicense licenseThreatGroupLicense : exportDTO.licenseThreatGroupLicenses) {
        licenseThreatGroupLicense.setId(null);
        licenseThreatGroupLicense.setOwnerId(ownerId);
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
  }

  /**
   * Will import and update existing labels or add new ones mentioned in the exportDTO.
   *
   * @param em             entityManager for sharing transaction
   * @param exportDTO      exportDTO modified by side-effect to update ids from newly saved objects
   * @param oldLabels      already persisted labels
   * @param applicationId  the applicationId owning the labels(may be null if we're processing an organization)
   * @param organizationId the organizationId owning the labels; if applicationId is not set this organization will be
   *                       updated
   */
  private void importAndMergeLabels(final EntityManager em, final PolicyExportResult exportDTO,
                                    final List<Label> oldLabels, final String applicationId,
                                    final String organizationId)
  {
    if (exportDTO.labels.size() > 0) {
      Map<String, String> idMap = new HashMap<>();
      // include any existing org labels, in case they're used in app policies. These are NOT candidates for deletion.
      if (organizationId != null) {
        for (Label label : labelDAO.getByOwnerId(em, organizationId)) {
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
          existingLabel.setDescription(label.getDescription());
          labelDAO.update(em, existingLabel);
          idMap.put(oldId, existingLabel.getId());
        }
        else {
          label.setId(null);
          label.setOwnerId(applicationId != null ? applicationId : organizationId);
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
  }

  private Label getLabelByName(List<Label> labels, String nameLowercase) {
    for (Label label : labels) {
      if (nameLowercase.equals(label.getLabelLowercase())) {
        return label;
      }
    }
    return null;
  }

  private PolicyDAO policyDAO() {
    return new PolicyDAO(work.getWorkDir());
  }

  private PolicyImportResult createResult(String name, String id, String type ) {
    PolicyImportResult result = new PolicyImportResult();
    result.name = name;
    UriBuilder uriBuilder = baseUrl.redirect().path(InsightBrainService.BRAIN_ASSET_PATH).path("index.html")
        .fragment("/management/" + type + "/" + id);
    result.url = uriBuilder.build().toString();
    return result;
  }
}

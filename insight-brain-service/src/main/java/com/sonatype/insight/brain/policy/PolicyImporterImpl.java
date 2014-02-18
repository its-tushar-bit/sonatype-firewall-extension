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
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;

import com.google.common.base.Preconditions;
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

  private PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();

  private final TagDAO tagDAO = new TagDAO();

  private final PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  private final BaseUrl baseUrl;

  @Inject
  public PolicyImporterImpl(BaseUrl baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public PolicyImportResult importApplication(Application application, PolicyExportResult exportDTO) {
    checkAppImportPreconditions(application, exportDTO);

    String appId = application.getId();
    String orgId = application.getOrganizationId();
    EntityManager em = applicationDAO.createEntityManager();

    try {
      em.getTransaction().begin();
      deleteLicenseThreatGroups(em, appId, null);
      deletePolicyWaivers(em, appId, null);
      policyDAO().deleteByOwnerId(em, appId);
      importAndMergeLabels(em, exportDTO, labelDAO.getByOwnerId(em, appId), appId, orgId);
      importLicenseThreatGroups(em, exportDTO, appId);
      // Must commit before inserting the policies because policy insert() calls validate(), which needs to access some
      // db tables for policy condition validations.
      // If everything was done in one transaction here, then all policy related validation methods need to participate
      // in the same transaction, so they would need to take an EntityManager as param, which is not worth it in my
      // opinion.
      em.getTransaction().commit();

      em.getTransaction().begin();
      for (Policy policy : exportDTO.policies) {
        policy.setId(null);
        policy.setOwnerId(appId);
        policyDAO().insert(em, policy);
      }
      em.getTransaction().commit();
    }
    finally {
      ApplicationDAO.close(em);
    }

    return createResult(application.getName(), application.getPublicId(), TYPE_APPLICATION);
  }

  @Override
  public PolicyImportResult importOrganization(Organization organization, PolicyExportResult exportDTO) {
    checkOrgImportPreconditions(organization, exportDTO);

    String orgId = organization.getId();
    EntityManager em = organizationDAO.createEntityManager();
    try {
      em.getTransaction().begin();

      deleteFromOwnedApplications(em, orgId);
      deletePolicyWaivers(em, orgId, null);
      deleteLicenseThreatGroups(em, orgId, null);
      PolicyDAO policyDAO = policyDAO();
      policyDAO.deleteByOwnerId(em, orgId);
      for (Application application : applicationDAO.getByOrganizationId(em, orgId)) {
        policyDAO.deleteByOwnerId(em, application.getId());
      }
      importAndMergeLabels(em, exportDTO, labelDAO.getByOwnerId(em, orgId), null, orgId);
      importLicenseThreatGroups(em, exportDTO, orgId);
      importAndMergeTags(em, exportDTO, orgId);

      Map<String, List<PolicyTag>> policyTagsByPolicyId = getPolicyTagsByPolicyId(exportDTO.policyTags);
      // Must commit before inserting the policies because policy insert() calls validate(), which needs to access some
      // db tables for policy condition validations.
      // If everything was done in one transaction here, then all policy related validation methods need to participate
      // in the same transaction, so they would need to take an EntityManager as param, which is not worth it in my
      // opinion.
      em.getTransaction().commit();

      em.getTransaction().begin();
      for (Policy policy : exportDTO.policies) {
        List<PolicyTag> policyTags = policyTagsByPolicyId.get(policy.getId());
        policy.setId(null);
        policy.setOwnerId(orgId);
        policyDAO.insert(policy);
        importPolicyTags(em, policy.getId(), policyTags);
      }
      em.getTransaction().commit();
    }
    finally {
      OrganizationDAO.close(em);
    }

    return createResult(organization.getName(), orgId, TYPE_ORGANIZATION);
  }

  /**
   * Delete all LTGs and waivers from an organization's child applications.
   */
  private void deleteFromOwnedApplications(EntityManager em, String orgId) {
    for (Application application : applicationDAO.getByOrganizationId(em, orgId)) {
      deleteLicenseThreatGroups(em, application.getId(), orgId);
      deletePolicyWaivers(em, application.getId(), orgId);
    }
  }

  /**
   * Delete all LTGs from the specified owner.
   */
  private void deleteLicenseThreatGroups(EntityManager em, String ownerId, String orgId) {
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(em, ownerId);
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      log.debug("Deleting licenseThreatGroup: {} during import for ownerId: {}", licenseThreatGroup.getName(),
          orgId != null ? orgId : ownerId);
      licenseThreatGroupDAO.delete(em, licenseThreatGroup);
    }
  }

  /**
   * Delete all PolicyWaivers from the specified owner.
   */
  private void deletePolicyWaivers(EntityManager em, String ownerId, final String orgId) {
    for (PolicyWaiver policyWaiver : policyWaiverDAO.getByOwnerId(em, ownerId)) {
      log.debug("Deleting policyWaiver: {} during import for ownerId: {}", policyWaiver.getId(), orgId != null ? orgId
          : ownerId);
      policyWaiverDAO.delete(em, policyWaiver);
    }
  }

  /**
   * Will import LicenseThreatGroup and LicenseThreatGroupLicense information from the exportDTO.
   * 
   * @param em entityManager for sharing transaction
   * @param exportDTO exportDTO modified by side-effect to update ids from newly saved objects
   * @param ownerId the org/app id to import to
   */
  private void importLicenseThreatGroups(EntityManager em, PolicyExportResult exportDTO, String ownerId) {
    if (!exportDTO.licenseThreatGroups.isEmpty()) {
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
        licenseThreatGroupLicense
            .setLicenseThreatGroupId(idMap.get(licenseThreatGroupLicense.getLicenseThreatGroupId()));
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
   * @param em entityManager for sharing transaction
   * @param exportDTO exportDTO modified by side-effect to update ids from newly saved objects
   * @param oldLabels already persisted labels
   * @param applicationId the applicationId owning the labels(may be null if we're processing an organization)
   * @param organizationId the organizationId owning the labels; if applicationId is not set this organization will be
   *          updated
   */
  void importAndMergeLabels(final EntityManager em, final PolicyExportResult exportDTO, final List<Label> oldLabels,
      final String applicationId, final String organizationId)
  {
    if (!exportDTO.labels.isEmpty()) {
      Map<String, String> idMap = new HashMap<>();
      if (applicationId != null && organizationId != null) {
        for (Label label : labelDAO.getByOwnerId(em, organizationId)) {
          idMap.put(label.getId(), label.getId());
        }
      }
      for (Label label : exportDTO.labels) {
        String labelId = label.getId();
        Label existingLabel = getLabelByName(oldLabels, label);
        if (existingLabel != null) {
          // Existing label, update it with new properties.
          existingLabel.setLabel(label.getLabel());
          existingLabel.setColor(label.getColor());
          existingLabel.setDescription(label.getDescription());
          labelDAO.update(em, existingLabel);
          idMap.put(labelId, existingLabel.getId());
        }
        else {
          // New label, create it.
          label.setId(null);
          label.setOwnerId(applicationId != null ? applicationId : organizationId);
          labelDAO.insert(em, label);
          idMap.put(labelId, label.getId());
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
  }

  /**
   * Import/merge the specified Tags.
   * 
   * If the Tag already exists on the specified Org, it is updated to reflect the passed in Tag
   * If the Tag does not exist, it is created
   * 
   * @param em entityManager for sharing transaction
   * @param exportDTO exportDTO modified by side-effect to update ids from newly saved objects
   * @param orgId the organization owning the tags
   */
  void importAndMergeTags(final EntityManager em, final PolicyExportResult exportDTO, final String orgId) {
    if (!exportDTO.tags.isEmpty()) {
      Map<String, String> idMap = new HashMap<>();
      for (Tag tag : exportDTO.tags) {
        String oldId = tag.getId();
        Tag existingTag = tagDAO.getByOrganizationIdAndName(em, orgId, tag.getName());
        if (existingTag != null) {
          // Existing tag, update it
          tag.setId(existingTag.getId());
          tag.setOrganizationId(orgId);
          tagDAO.update(em, tag);
        }
        else {
          // New tag, create it
          tag.setId(null);
          tag.setOrganizationId(orgId);
          tagDAO.insert(em, tag);
        }
        idMap.put(oldId, tag.getId());
      }

      for (PolicyTag policyTag : exportDTO.policyTags) {
        policyTag.setTagId(idMap.get(policyTag.getTagId()));
      }
    }
  }

  /**
   * Import the specified PolicyTags, using the specified policy id.
   * 
   * The id on the passed in PolicyTags is no longer valid, since the policies
   * get new ids when imported
   */
  private void importPolicyTags(EntityManager em, String policyId, List<PolicyTag> policyTags) {
    if (policyTags != null) {
      for (PolicyTag policyTag : policyTags) {
        policyTag.setId(null);
        policyTag.setPolicyId(policyId);
        policyTagDAO.insert(em, policyTag);
      }
    }
  }

  /**
   * Convert the given list of PolicyTags into a map of PolicyTag lists, keyed by policy id
   */
  private Map<String, List<PolicyTag>> getPolicyTagsByPolicyId(List<PolicyTag> policyTags) {
    Map<String, List<PolicyTag>> policyTagMap = new HashMap<>();
    for (PolicyTag policyTag : policyTags) {
      if (policyTagMap.containsKey(policyTag.getPolicyId())) {
        policyTagMap.get(policyTag.getPolicyId()).add(policyTag);
      }
      else {
        List<PolicyTag> policyTagList = new ArrayList<>();
        policyTagList.add(policyTag);
        policyTagMap.put(policyTag.getPolicyId(), policyTagList);
      }
    }
    return policyTagMap;
  }

  /**
   * Search for label in the list, considering a match on case-insensitive comparison
   * of the label name only.
   * 
   * @param labels search candidates
   * @param labelToFind label we're looking for
   */
  private Label getLabelByName(List<Label> labels, Label labelToFind) {
    for (Label label : labels) {
      if (labelToFind.getLabelLowercase().equals(label.getLabelLowercase())) {
        return label;
      }
    }
    return null;
  }

  private PolicyDAO policyDAO() {
    return new PolicyDAO();
  }

  private PolicyImportResult createResult(String name, String id, String type) {
    PolicyImportResult result = new PolicyImportResult();
    result.ownerName = name;
    UriBuilder uriBuilder = baseUrl.redirect().path(InsightBrainService.BRAIN_ASSET_PATH).path("index.html")
        .fragment("/management/" + type + "/" + id);
    result.url = uriBuilder.build().toString();
    return result;
  }

  private void checkAppImportPreconditions(Application app, PolicyExportResult exportDTO) {
    Preconditions.checkNotNull(app, "Import failed. The passed in application was null");
    checkPolicyExportResultPreconditions(exportDTO);
    Preconditions.checkArgument(exportDTO.tags.isEmpty() && exportDTO.policyTags.isEmpty(),
        "Importing policies with applied tags to an application is not supported");
  }

  private void checkOrgImportPreconditions(Organization org, PolicyExportResult exportDTO) {
    Preconditions.checkNotNull(org, "Import failed. The passed in organization was null");
    checkPolicyExportResultPreconditions(exportDTO);
  }

  private void checkPolicyExportResultPreconditions(PolicyExportResult exportDTO) {
    String msg = "Import failed. Part of the PolicyExportResult was null: %s";
    Preconditions.checkNotNull(exportDTO, "Import failed. The passed in PolicyExportResult was null");
    Preconditions.checkNotNull(exportDTO.policies, msg, "policies");
    Preconditions.checkNotNull(exportDTO.labels, msg, "labels");
    Preconditions.checkNotNull(exportDTO.licenseThreatGroups, msg, "licenseThreatGroups");
    Preconditions.checkNotNull(exportDTO.licenseThreatGroupLicenses, msg, "licenseThreatGroupLicenses");
    Preconditions.checkNotNull(exportDTO.tags, msg, "tags");
    Preconditions.checkNotNull(exportDTO.policyTags, msg, "policyTags");
  }
}

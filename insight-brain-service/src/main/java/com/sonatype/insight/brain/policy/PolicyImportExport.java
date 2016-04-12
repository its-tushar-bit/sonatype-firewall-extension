/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

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
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.7
 */
@Named
public class PolicyImportExport
{
  private static final Logger log = LoggerFactory.getLogger(PolicyImportExport.class);

  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private LabelDAO labelDAO = new LabelDAO();

  private PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();

  private final TagDAO tagDAO = new TagDAO();

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  @Inject
  public PolicyImportExport() {
  }

  /**
   * <p>
   * Import policies into an Application. Existing polices are deleted from the application. Application Labels will be
   * merged if they match (case-insensitive by name) existing data; this preserves any related ComponentLabels.
   * </p>
   * <p>
   * License Threat Groups and associated Licenses on this application are all deleted as part of the import. If the
   * imported data contains any LicenseThreatGroups with the same name as one in the parent hierarchy, then it will be
   * discarded and the existing one higher up in the hierarchy will be used in it's place
   * </p>
   * 
   * @param application application to import policy to
   * @param exportDTO data to import
   * @return result embedding the url of the application
   */
  @Authorize(permission = Permission.WRITE)
  PolicyImportResult importApplication(@AuthzContext(AuthzContext.Key.APPLICATION) Application application,
                                       PolicyExportResult exportDTO)
  {
    checkAppImportPreconditions(application, exportDTO);

    String appId = application.getId();
    String orgId = application.getOrganizationId();

    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      deleteLicenseThreatGroups(tx, appId, null);
      deletePolicyWaivers(tx, appId, null);
      policyDAO.deleteByOwnerId(tx, appId);
      importAndMergeLabels(tx, exportDTO, labelDAO.getByOwnerId(tx, appId), appId, orgId);
      importLicenseThreatGroups(tx, exportDTO, appId);
      // Must commit before inserting the policies because policy insert() calls validate(), which needs to access some
      // db tables for policy condition validations.
      // If everything was done in one transaction here, then all policy related validation methods need to participate
      // in the same transaction, so they would need to take a TransactionContext as param, which is not worth it in my
      // opinion.
      tx.commit();

      tx.begin();
      for (Policy policy : exportDTO.policies) {
        policy.setId(null);
        policy.setOwnerId(appId);
        policyDAO.insert(tx, policy);
      }
      tx.commit();
    }

    return createResult(application.getName());
  }

  /**
   * <p>
   * Import policies into an Organization. Existing polices are deleted from the organization and all child
   * applications. This includes deletion of data from child Applications(License Threat Groups and associated
   * Licenses). Organization Labels will be merged if they match (case-insensitive by name) existing data; this
   * preserves any related ComponentLabels.
   * </p>
   * <p>
   * License Threat Groups and associated Licenses on this organization are all deleted as part of the import. If the
   * imported data contains any LicenseThreatGroups with the same name as one in the parent hierarchy, then it will be
   * discarded and the existing one higher up in the hierarchy will be used in it's place
   * </p>
   * 
   * @param organization org to import policy to
   * @param exportDTO data to import
   * @return result embedding the url of the organization
   */
  @Authorize(permission = Permission.WRITE)
  PolicyImportResult importOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION) Organization organization,
                                        PolicyExportResult exportDTO)
  {
    checkOrgImportPreconditions(organization, exportDTO);

    String orgId = organization.getId();
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();

      deleteFromOwnedApplications(tx, orgId);
      deletePolicyWaivers(tx, orgId, null);
      deleteLicenseThreatGroups(tx, orgId, null);
      policyDAO.deleteByOwnerId(tx, orgId);
      for (Application application : applicationDAO.getByOrganizationId(tx, orgId)) {
        policyDAO.deleteByOwnerId(tx, application.getId());
      }
      importAndMergeLabels(tx, exportDTO, labelDAO.getByOwnerId(tx, orgId), null, orgId);
      importLicenseThreatGroups(tx, exportDTO, orgId);
      importAndMergeTags(tx, exportDTO, orgId);

      Map<String, List<PolicyTag>> policyTagsByPolicyId = getPolicyTagsByPolicyId(exportDTO.policyTags);
      // Must commit before inserting the policies because policy insert() calls validate(), which needs to access some
      // db tables for policy condition validations.
      // If everything was done in one transaction here, then all policy related validation methods need to participate
      // in the same transaction, so they would need to take a TransactionContext as param, which is not worth it in my
      // opinion.
      tx.commit();

      tx.begin();
      for (Policy policy : exportDTO.policies) {
        List<PolicyTag> policyTags = policyTagsByPolicyId.get(policy.getId());
        policy.setId(null);
        policy.setOwnerId(orgId);
        policyDAO.insert(policy);
        importPolicyTags(tx, policy.getId(), policyTags);
      }
      tx.commit();
    }

    return createResult(organization.getName());
  }

  /**
   * Delete all LTGs and waivers from an organization's child applications.
   */
  private void deleteFromOwnedApplications(TransactionContext tx, String orgId) {
    for (Application application : applicationDAO.getByOrganizationId(tx, orgId)) {
      deleteLicenseThreatGroups(tx, application.getId(), orgId);
      deletePolicyWaivers(tx, application.getId(), orgId);
    }
  }

  /**
   * Delete all LTGs from the specified owner.
   */
  private void deleteLicenseThreatGroups(TransactionContext tx, String ownerId, String orgId) {
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(tx, ownerId);
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      log.debug("Deleting licenseThreatGroup: {} during import for ownerId: {}", licenseThreatGroup.getName(),
          orgId != null ? orgId : ownerId);
      licenseThreatGroupDAO.delete(tx, licenseThreatGroup);
    }
  }

  /**
   * Delete all PolicyWaivers from the specified owner.
   */
  private void deletePolicyWaivers(TransactionContext tx, String ownerId, final String orgId) {
    for (PolicyWaiver policyWaiver : policyWaiverDAO.getByOwnerId(tx, ownerId)) {
      log.debug("Deleting policyWaiver: {} during import for ownerId: {}", policyWaiver.getId(), orgId != null ? orgId
          : ownerId);
      policyWaiverDAO.delete(tx, policyWaiver);
    }
  }

  /**
   * Will import LicenseThreatGroup and LicenseThreatGroupLicense information from the exportDTO.
   * If the imported data contains any LicenseThreatGroups with the same name as one in the parent hierarchy,
   * then it will be discarded and the existing one higher up in the hierarchy will be used in it's place
   * 
   * @param tx tx for sharing transaction
   * @param exportDTO exportDTO modified by side-effect to update ids from newly saved objects
   * @param ownerId the org/app id to import to
   */
  private void importLicenseThreatGroups(TransactionContext tx, PolicyExportResult exportDTO, String ownerId) {
    if (!exportDTO.licenseThreatGroups.isEmpty()) {
      Map<String, String> idMap = new HashMap<>();
      Set<String> idInheritedSet = new HashSet<>();
      for (LicenseThreatGroup licenseThreatGroup : exportDTO.licenseThreatGroups) {
        licenseThreatGroup.setOwnerId(ownerId);
        String oldId = licenseThreatGroup.getId();
        LicenseThreatGroup inheritedLtg = licenseThreatGroupDAO.getInheritedByName(tx, licenseThreatGroup);
        if (inheritedLtg == null) {
          licenseThreatGroup.setId(null);
          licenseThreatGroupDAO.insert(tx, licenseThreatGroup);
          idMap.put(oldId, licenseThreatGroup.getId());
        }
        else {
          idMap.put(oldId, inheritedLtg.getId());
          idInheritedSet.add(oldId);
        }
      }
      for (LicenseThreatGroupLicense licenseThreatGroupLicense : exportDTO.licenseThreatGroupLicenses) {
        if (idInheritedSet.contains(licenseThreatGroupLicense.getLicenseThreatGroupId())) {
          continue; // Skip as these are already defined in the inherited one
        }
        licenseThreatGroupLicense.setId(null);
        licenseThreatGroupLicense.setOwnerId(ownerId);
        licenseThreatGroupLicense
            .setLicenseThreatGroupId(idMap.get(licenseThreatGroupLicense.getLicenseThreatGroupId()));
        licenseThreatGroupLicenseDAO.insert(tx, licenseThreatGroupLicense);
      }
      for (Policy policy : exportDTO.policies) {
        for (Constraint constraint : policy.getConstraints()) {
          for (Condition condition : constraint.getConditions()) {
            if (LicenseThreatGroupConditionType.ID.equals(condition.getConditionTypeId())) {
              if (!LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID.equals(condition.getValue())) {
                condition.setValue(idMap.get(condition.getValue()));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Will import and update existing labels or add new ones mentioned in the exportDTO.
   * 
   * @param tx tx for sharing transaction
   * @param exportDTO exportDTO modified by side-effect to update ids from newly saved objects
   * @param oldLabels already persisted labels
   * @param applicationId the applicationId owning the labels(may be null if we're processing an organization)
   * @param organizationId the organizationId owning the labels; if applicationId is not set this organization will be
   *          updated
   */
  void importAndMergeLabels(final TransactionContext tx,
                            final PolicyExportResult exportDTO,
                            final List<Label> oldLabels,
                            final String applicationId,
                            final String organizationId)
  {
    if (!exportDTO.labels.isEmpty()) {
      Map<String, String> idMap = new HashMap<>();
      if (applicationId != null && organizationId != null) {
        for (Label label : labelDAO.getByOwnerId(tx, organizationId)) {
          idMap.put(label.getId(), label.getId());
        }
      }
      for (Label label : exportDTO.labels) {
        String labelId = label.getId();
        Label existingLabel = getLabelByName(oldLabels, label);
        if (existingLabel != null) {
          // Existing label, update it with new properties.
          existingLabel.setLabel(label.getLabel());
          existingLabel.setColor(label.getColor().getUpdatedColor());
          existingLabel.setDescription(label.getDescription());
          labelDAO.update(tx, existingLabel);
          idMap.put(labelId, existingLabel.getId());
        }
        else {
          // New label, create it.
          label.setId(null);
          label.setOwnerId(applicationId != null ? applicationId : organizationId);
          label.setColor(label.getColor().getUpdatedColor());
          labelDAO.insert(tx, label);
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
   * @param tx tx for sharing transaction
   * @param exportDTO exportDTO modified by side-effect to update ids from newly saved objects
   * @param orgId the organization owning the tags
   */
  void importAndMergeTags(final TransactionContext tx, final PolicyExportResult exportDTO, final String orgId) {
    if (!exportDTO.tags.isEmpty()) {
      Map<String, String> idMap = new HashMap<>();
      for (Tag tag : exportDTO.tags) {
        String oldId = tag.getId();
        Tag existingTag = tagDAO.getByOrganizationIdAndName(tx, orgId, tag.getName());
        if (existingTag != null) {
          // Existing tag, update it
          tag.setId(existingTag.getId());
          tag.setOrganizationId(orgId);
          tag.setColor(tag.getColor().getUpdatedColor());
          tagDAO.update(tx, tag);
        }
        else {
          // New tag, create it
          tag.setId(null);
          tag.setOrganizationId(orgId);
          tag.setColor(tag.getColor().getUpdatedColor());
          tagDAO.insert(tx, tag);
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
  private void importPolicyTags(TransactionContext tx, String policyId, List<PolicyTag> policyTags) {
    if (policyTags != null) {
      for (PolicyTag policyTag : policyTags) {
        policyTag.setId(null);
        policyTag.setPolicyId(policyId);
        policyTagDAO.insert(tx, policyTag);
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

  private PolicyImportResult createResult(String name) {
    PolicyImportResult result = new PolicyImportResult();
    result.ownerName = name;
    return result;
  }

  private void checkAppImportPreconditions(Application app, PolicyExportResult exportDTO) {
    checkNotNull(app, "Import failed. The passed in application was null.");
    checkPolicyExportResultPreconditions(exportDTO);
    if (!exportDTO.tags.isEmpty() || !exportDTO.policyTags.isEmpty()) {
      throw new BadRequestException(
          "Importing policies with application categories to an application is not supported.");
    }
  }

  private void checkOrgImportPreconditions(Organization org, PolicyExportResult exportDTO) {
    checkNotNull(org, "Import failed. The passed in organization was null.");
    checkPolicyExportResultPreconditions(exportDTO);
  }

  private void checkPolicyExportResultPreconditions(PolicyExportResult exportDTO) {
    String msg = "Import failed. Part of the PolicyExportResult was null: ";
    checkNotNull(exportDTO, "Import failed. The passed in PolicyExportResult was null.");
    checkNotNull(exportDTO.policies, msg, "policies");
    checkNotNull(exportDTO.labels, msg, "labels");
    checkNotNull(exportDTO.licenseThreatGroups, msg, "licenseThreatGroups");
    checkNotNull(exportDTO.licenseThreatGroupLicenses, msg, "licenseThreatGroupLicenses");
    checkNotNull(exportDTO.tags, msg, "tags");
    checkNotNull(exportDTO.policyTags, msg, "policyTags");
  }

  private static void checkNotNull(Object reference, String errorMessage) {
    if (reference == null) {
      throw new BadRequestException(errorMessage);
    }
  }

  private static void checkNotNull(Object reference, String errorMessage, String errorMessageArg) {
    if (reference == null) {
      throw new BadRequestException(errorMessage + errorMessageArg);
    }
  }

  @Authorize(permission = Permission.READ)
  PolicyExportResult exportApplication(@AuthzContext(AuthzContext.Key.APPLICATION) Application application) {
    return export(application.getId());
  }

  @Authorize(permission = Permission.READ)
  PolicyExportResult exportOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION) Organization organization) {
    String orgId = organization.getId();
    PolicyExportResult policyExportResult = export(orgId);
    policyExportResult.policyTags = policyTagDAO.getByOrganizationId(orgId);
    policyExportResult.tags = tagDAO.getAppliedToPolicyByOrganizationId(orgId);
    return policyExportResult;
  }

  private PolicyExportResult export(String ownerId) {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = policyDAO.getByOwnerId(ownerId);
    policyExportResult.labels = labelDAO.getByOwnerId(ownerId);
    policyExportResult.licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(ownerId);
    policyExportResult.licenseThreatGroupLicenses = licenseThreatGroupLicenseDAO.getByOwnerId(ownerId);

    return policyExportResult;
  }
}

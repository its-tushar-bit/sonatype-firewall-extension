/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.db.H2DatabaseBackup;
import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates the "config" from a selected organization to the root organization.
 * The config includes:
 * - policies
 * - policy monitoring
 * - labels
 * - license threat groups
 * - application categories (aka tags)
 * 
 * Not migrated:
 * - applications
 * - policy notifications and policy monitoring notifications that use email addresses (they are simply removed)
 * - security/access configuration
 * - component labels
 * - license overrides
 * - SV overrides
 * - policy waivers
 * 
 * @since 1.18
 */
@Named
public class RootOrganizationConfigMigrator
{
  private static final Logger log = LoggerFactory.getLogger(RootOrganizationConfigMigrator.class);

  static final String ROOT_ORG_NOT_EMPTY_MESSAGE =
      "Cannot migrate root organization config because the root organization has %s.";

  private final InsightConfig config;

  private final RootOrganizationConfigMigrationUtils migrationUtils;

  private ApplicationTagDAO appTagDAO = new ApplicationTagDAO();

  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  private LabelDAO labelDAO = new LabelDAO();

  private LicenseThreatGroupDAO ltgDAO = new LicenseThreatGroupDAO();

  private LicenseThreatGroupLicenseDAO ltglDAO = new LicenseThreatGroupLicenseDAO();

  private OrganizationDAO orgDAO = new OrganizationDAO();

  private OwnerDAO ownerDAO = new OwnerDAO();

  private PolicyDAO policyDAO = new PolicyDAO();

  private PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  private PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  private PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();

  private TagDAO tagDAO = new TagDAO();

  @Inject
  public RootOrganizationConfigMigrator(InsightConfig config, RootOrganizationConfigMigrationUtils migrationUtils) {
    this.config = config;
    this.migrationUtils = migrationUtils;
  }

  boolean migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.debug("Migrating config for root organization...");

    if (migrationUtils.isMigrated()) {
      log.debug("Root organization config already migrated.");
      return false;
    }

    if (config.isShowRootOrganization()) {
      log.info("Root organization is visible. No migration for root organization configuration"
          + " if the root organization is visible.");
      migrationUtils.setMigrated();
      return false;
    }

    if (orgDAO.getAll().size() == 1) {
      log.info("Fresh install. No migration for root organization configuration is needed.");
      migrationUtils.setMigrated();
      return false;
    }

    if (!migrationUtils.isMigrationScheduled()) {
      log.debug("Root organization config migration was not configured yet.");
      return false;
    }

    checkRootOrgIsEmpty();

    createBackup();

    Organization sourceOrg = orgDAO.getByIdNotNull(migrationUtils.getSourceOrganizationId());
    migrate(sourceOrg);

    migrationUtils.setMigrated();
    deleteBackup();

    log.info("Migrated root organization config in {} ms.", System.currentTimeMillis() - start);
    return true;
  }

  private void createBackup() {
    if (OperationalDataStoreProvider.isDatabaseInMemory()) {
      return;
    }

    File dbBackupDir = getDbBackupDir();
    if (dbBackupDir.exists()) {
      throw new IllegalStateException("Cannot migrate config for root organization. The backup directory '"
          + dbBackupDir.getAbsolutePath() + "' already exists, indicating that a previous migration failed. "
          + "Please contact support for further assistance.");
    }

    H2DatabaseBackup h2DatabaseBackup = new H2DatabaseBackup();
    h2DatabaseBackup.backup(OperationalDataStoreProvider.getDatabaseConfig(),
        OperationalDataStoreProvider.getDataSource(), dbBackupDir);
  }

  private void deleteBackup() {
    if (OperationalDataStoreProvider.isDatabaseInMemory()) {
      return;
    }

    File dbBackupDir = getDbBackupDir();
    try {
      new FileCleaner().delete(dbBackupDir);
    }
    catch (FileDeletionException e) {
      throw new RuntimeException("Cannot delete db backup created for root organization config migration: "
          + e.getMessage(), e);
    }
  }

  @VisibleForTesting
  File getDbBackupDir() {
    File databasePath = H2DatabaseUtil.getDatabasePath(OperationalDataStoreProvider.getDatabaseConfig());
    File databaseDir = databasePath.getParentFile();
    return new File(databaseDir, "backup");
  }

  private void checkRootOrgIsEmpty() {
    // Don't check for license threat groups because the root org can have the default license thread groups.
    if (!policyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID).isEmpty()) {
      throw new RuntimeException(String.format(ROOT_ORG_NOT_EMPTY_MESSAGE, "policies"));
    }
    if (policyMonitoringDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID) != null) {
      throw new RuntimeException(String.format(ROOT_ORG_NOT_EMPTY_MESSAGE, "policy monitoring"));
    }
    if (!labelDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID).isEmpty()) {
      throw new RuntimeException(String.format(ROOT_ORG_NOT_EMPTY_MESSAGE, "labels"));
    }
    if (!tagDAO.getByOrganizationId(Organization.ROOT_ORGANIZATION_ID).isEmpty()) {
      throw new RuntimeException(String.format(ROOT_ORG_NOT_EMPTY_MESSAGE, "application categories"));
    }
  }

  @VisibleForTesting
  void migrate(Organization sourceOrg) {
    log.info("Migrating config for root organization from template (source) organization: {} (ID: {}).",
        sourceOrg.getName(), sourceOrg.getId());

    migratePolicyMonitoring(sourceOrg);
    migrateLabels(sourceOrg);
    migrateLicenseThreatGroups(sourceOrg);
    migrateTags(sourceOrg);
    migratePolicies(sourceOrg);
  }

  private void removeEmailNotifications(Policy policy) {
    policy.getNotifications().getUserNotifications().clear();
  }

  private void migratePolicies(Organization sourceOrg) {
    log.info("Migrating policies...");

    for (Policy sourcePolicy : policyDAO.getByOwnerId(sourceOrg.getId())) {
      log.info("Moving policy {} (ID: {}) to root organization.", sourcePolicy.getName(), sourcePolicy.getId());

      // Delete all policies with the same name
      for (Policy sameNamePolicy : policyDAO.getByName(sourcePolicy.getName())) {
        if (sourcePolicy.getId().equals(sameNamePolicy.getId())) {
          // Same policy
          continue;
        }

        log.debug("Will remove policy {} (ID: {}) from owner ID {}.", sameNamePolicy.getName(), sameNamePolicy.getId(),
            sameNamePolicy.getOwnerId());

        // Move the waivers of the sameNamePolicy to the sourcePolicy
        for (PolicyWaiver policyWaiver : policyWaiverDAO.getByPolicyId(sameNamePolicy.getId())) {
          policyWaiver.setPolicyId(sourcePolicy.getId());
          policyWaiverDAO.update(policyWaiver);
        }

        // Move the policy violations of the sameNamePolicy to the sourcePolicy
        int movedViolationCount = policyViolationDAO.replacePolicyId(sameNamePolicy.getId(), sourcePolicy.getId());
        log.debug("Moved {} policy violations from policy ID {} to policy ID {}.", movedViolationCount,
            sameNamePolicy.getId(), sourcePolicy.getId());

        // Delete the sameNamePolicy
        policyDAO.delete(sameNamePolicy);
      }

      // Move the sourcePolicy to root org
      sourcePolicy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
      // Remove email notifications from sourcePolicy
      removeEmailNotifications(sourcePolicy);
      policyDAO.update(sourcePolicy);
    }
  }

  private void migrateLabels(Organization sourceOrg) {
    log.info("Migrating labels...");

    for (Label sourceLabel : labelDAO.getByOwnerId(sourceOrg.getId())) {
      log.info("Moving label {} to root organization.", sourceLabel.getLabel());

      // Move the source label to root org.
      // We need to move it before deleting other labels with the same name because
      // we have to move the ComponentLabels for the labels we delete and the source label
      // must be already applicable in those contexts.
      sourceLabel.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
      // The label name may collide with other labels, so we temporarily change it here
      // and restore it after all labels with the same name are deleted.
      String sourceLabelName = sourceLabel.getLabel();
      String sourceLabelNameLowercase = sourceLabel.getLabelLowercase();
      sourceLabel.setLabel(sourceLabel.getId());
      labelDAO.update(sourceLabel);

      // Delete all labels with the same name and move references to sourceLabel
      for (Label sameNameLabel : labelDAO.getByLabelLowercase(sourceLabelNameLowercase)) {
        if (sourceLabel.getId().equals(sameNameLabel.getId())) {
          // Same label
          continue;
        }

        // Move component-label associations from sameNameLabel to sourceLabel
        for (ComponentLabel componentLabel : componentLabelDAO.getByLabelId(sameNameLabel.getId())) {
          componentLabel.setLabelId(sourceLabel.getId());
          componentLabelDAO.update(componentLabel);
        }

        // Update policy conditions that use sameNameLabel to sourceLabel
        Owner sameNameLabelOwner = ownerDAO.getById(sameNameLabel.getOwnerId());
        updateChildPolicies(sameNameLabelOwner, ConditionTypes.LabelConditionType, sameNameLabel.getId(),
            sourceLabel.getId());

        // Delete the sameNameLabel
        labelDAO.delete(sameNameLabel);
      }

      // Restore the label name
      sourceLabel.setLabel(sourceLabelName);
      labelDAO.update(sourceLabel);
    }
  }

  private void migrateLicenseThreatGroups(Organization sourceOrg) {
    log.info("Migrating license threat groups...");

    for (LicenseThreatGroup sourceLTG : ltgDAO.getByOwnerId(sourceOrg.getId())) {
      log.info("Moving license threat group {} to root organization.", sourceLTG.getName());

      // Move sourceLTG to root org.
      // We need to move it before deleting other LTGs with the same name because
      // we have to move the references for the LTGs we delete and the sourceLTG
      // must be already applicable in those contexts.
      sourceLTG.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
      // The label name may collide with other LTGs, so we temporarily change it here
      // and restore it after all LTGs with the same name are deleted.
      String sourceLTGName = sourceLTG.getName();
      sourceLTG.setName(sourceLTG.getId());
      ltgDAO.update(sourceLTG);

      // Move the LTG-License associations
      for (LicenseThreatGroupLicense ltgl : ltglDAO.getByLicenseThreatGroupId(sourceLTG.getId())) {
        ltgl.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
        ltglDAO.update(ltgl);
      }

      // Delete all LTGs with the same name and move references to sourceLTG
      for (LicenseThreatGroup sameNameLTG : ltgDAO.getByName(sourceLTGName)) {
        if (sourceLTG.getId().equals(sameNameLTG.getId())) {
          // Same license threat group
          continue;
        }

        // Update policy conditions that use sameNameLTG to use sourceLTG
        Owner sameNameLTGOwner = ownerDAO.getById(sameNameLTG.getOwnerId());
        updateChildPolicies(sameNameLTGOwner, ConditionTypes.LicenseThreatGroupConditionType, sameNameLTG.getId(),
            sourceLTG.getId());

        // Delete the sameNameLTG
        ltgDAO.delete(sameNameLTG);
      }

      // Restore the LTG name
      sourceLTG.setName(sourceLTGName);
      ltgDAO.update(sourceLTG);
    }
  }

  private void migrateTags(Organization sourceOrg) {
    log.info("Migrating tags...");

    for (Tag sourceTag : tagDAO.getByOrganizationId(sourceOrg.getId())) {
      log.info("Moving tag {} to root organization.", sourceTag.getName());

      // Move sourceTag to root org.
      // We need to move it before deleting other tags with the same name because
      // we have to move the references for the tags we delete and the sourceTag
      // must be already applicable in those contexts.
      sourceTag.setOrganizationId(Organization.ROOT_ORGANIZATION_ID);
      // The tag name may collide with other tags, so we temporarily change it here
      // and restore it after all tags with the same name are deleted.
      String sourceTagName = sourceTag.getName();
      sourceTag.setName(sourceTag.getId());
      tagDAO.update(sourceTag);

      // Delete all tags with the same name and move references to sourceTag
      for (Tag sameNameTag : tagDAO.getByName(sourceTagName)) {
        if (sourceTag.getId().equals(sameNameTag.getId())) {
          // Same tag
          continue;
        }

        // Move application-tag associations from sameNameTag to sourceTag
        for (ApplicationTag appTag : appTagDAO.getByTagId(sameNameTag.getId())) {
          ApplicationTag newAppTag = new ApplicationTag(appTag.getApplicationId(), sourceTag.getId());
          appTagDAO.insert(newAppTag);
          appTagDAO.delete(appTag);
        }

        // Move policy-tag associations from sameNameTag to sourceTag
        for (PolicyTag policyTag : policyTagDAO.getByTagId(sameNameTag.getId())) {
          // The database has a foreign key constraint from policy_tag.policy_id to policy.policy_id,
          // which means there shouldn't be any PolicyTags that link tags to inexistent policies.
          // Still, I found a customer database that contains such orphan PolicyTags associations.
          // The only way I could get a database in the same state was by creating the foreign key constraint using
          // NOCHECK.
          // We know that this customer had problems with the database in the past and that they tried to fix it
          // themselves, so there is a good chance these orphan associations were caused by the customer db "fixing".
          Policy policy = policyDAO.getById(policyTag.getPolicyId());
          if (policy != null) {
            PolicyTag newPolicyTag = new PolicyTag(policyTag.getPolicyId(), sourceTag.getId());
            policyTagDAO.insert(newPolicyTag);
          }
          policyTagDAO.delete(policyTag);
        }

        // Delete the sameNameTag
        tagDAO.delete(sameNameTag);
      }

      // Restore the tag name
      sourceTag.setName(sourceTagName);
      tagDAO.update(sourceTag);
    }
  }

  private void migratePolicyMonitoring(Organization sourceOrg) {
    log.info("Migrating policy monitoring...");

    PolicyMonitoring sourcePolicyMonitoring = policyMonitoringDAO.getByOwnerId(sourceOrg.getId());
    if (sourcePolicyMonitoring != null) {
      log.info("Moving policy monitoring for stage type id {} to root organization.",
          sourcePolicyMonitoring.getStageTypeId());

      // Delete all monitorings of the same stage as sourcePolicyMonitoring
      for (PolicyMonitoring sameStagePolicyMonitoring : policyMonitoringDAO.getByStageTypeId(sourcePolicyMonitoring
          .getStageTypeId())) {
        if (sourcePolicyMonitoring.getId().equals(sameStagePolicyMonitoring.getId())) {
          // Same monitoring
          continue;
        }

        policyMonitoringDAO.delete(sameStagePolicyMonitoring);
      }

      sourcePolicyMonitoring.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
      policyMonitoringDAO.update(sourcePolicyMonitoring);
    }
  }

  private void updateChildPolicies(Owner owner, ConditionType conditionType, String oldValue, String newValue) {
    for (Policy policy : getChildPolicies(owner)) {
      List<Constraint> constraints = policy.getConstraints();
      for (Constraint constraint : constraints) {
        for (Condition condition : constraint.getConditions()) {
          if (conditionType.getId().equals(condition.getConditionTypeId()) && oldValue.equals(condition.getValue())) {
            condition.setValue(newValue);
            policy.setConstraints(constraints);
            policyDAO.update(policy);
          }
        }
      }
    }
  }

  private List<Policy> getChildPolicies(Owner owner) {
    List<Policy> policies = new ArrayList<>();
    policies.addAll(policyDAO.getByOwnerId(owner.getId()));

    if (!owner.canHaveChildren()) {
      return policies;
    }

    for (Owner childOwner : ownerDAO.getChildOwners(owner)) {
      policies.addAll(getChildPolicies(childOwner));
    }
    return policies;
  }
}

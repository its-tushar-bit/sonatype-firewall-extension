/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
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
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class RootOrganizationConfigMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private RootOrganizationConfigMigrationUtils migrationUtils;

  @Inject
  private InsightConfig config;

  @Inject
  private ComponentLabelDAO componentLabelDAO;

  @Inject
  private LabelDAO labelDAO;

  @Inject
  private LicenseThreatGroupDAO ltgDAO;

  @Inject
  private LicenseThreatGroupLicenseDAO ltglDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyMonitoringDAO policyMonitoringDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private ApplicationTagDAO appTagDAO;

  @Inject
  private PolicyTagDAO policyTagDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private TagDAO tagDAO;

  @Inject
  private RootOrganizationConfigMigrator migrator;

  @Before
  public void before() throws Exception {
    migrationTrackerDAO.deleteById(RootOrganizationConfigMigrationUtils.MIGRATION_ID);
    migrationTrackerDAO.deleteById(RootOrganizationConfigMigrationUtils.MIGRATION_CONFIG_ID);
  }

  @Test
  public void testMigrate_AlreadyMigrated() throws Exception {
    migrationUtils.setMigrated();

    assertThat(migrator.migrate()).isFalse();
  }

  @Test
  public void testMigrate_RootOrgIsVisible() throws Exception {
    config.setShowRootOrganization(true);

    assertThat(migrator.migrate()).isFalse();

    // Hide root org, should not do any migration.
    config.setShowRootOrganization(false);
    assertThat(migrator.migrate()).isFalse();
    assertThat(migrationUtils.isMigrated()).isTrue();
  }

  @Test
  public void testMigrate_FreshInstall() throws Exception {
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP SCHEMA " + OperationalDataStoreProvider.ID);
    }
    DataSourceFactory.clear_ForTestsOnly();
    OperationalDataStoreProvider.initWithoutMigration(null);

    assertThat(migrationUtils.isMigrated()).isTrue();

    assertThat(migrator.migrate()).isFalse();
    assertThat(migrationUtils.isMigrated()).isTrue();
  }

  @Test
  public void testMigrate_MigrationConfigDoesNotExist() throws Exception {
    tempEntity.newOrganization(); // add an organization so it doesn't look like a fresh install
    assertThat(migrator.migrate()).isFalse();
    assertThat(migrationUtils.isMigrated()).isFalse();
    assertThat(migrationUtils.isMigrationScheduled()).isFalse();
  }

  @Test
  public void testMigrate() throws Exception {
    createSourceOrg();
    assertThat(migrator.migrate()).isTrue();
    // Migration should not happen again
    assertThat(migrator.migrate()).isFalse();
  }

  @Test
  public void testMigrate_Policies() throws Exception {
    Organization sourceOrg = createSourceOrg();
    Label sourceOrgLabel = tempEntity.newLabel(sourceOrg.getId());
    LicenseThreatGroup sourceOrgLTG = tempEntity.newLicenseThreatGroup(sourceOrg.getId());
    Tag sourceOrgTag = tempEntity.newTag(sourceOrg.getId());
    // Create the source policy (the policy to be moved) with references to a label, an LTG and a tag,
    // in order to verify that the references remain valid during and after migration.
    String policyName = "My Policy";
    Policy sourcePolicy = tempEntity.newPolicy(sourceOrg.getId(), policyName);
    addConstraintForLabel(sourcePolicy, sourceOrgLabel);
    addConstraintForLTG(sourcePolicy, sourceOrgLTG);
    policyDAO.update(sourcePolicy);
    addEmailNotification(sourcePolicy, BuildStageType.ID);
    addRoleNotification(sourcePolicy, BuildStageType.ID);
    addFailAction(sourcePolicy, ReleaseStageType.ID);
    addMonitoringEmailNotification(sourcePolicy);
    addMonitoringRoleNotification(sourcePolicy);
    tempEntity.newPolicyTag(sourcePolicy.getId(), sourceOrgTag.getId());
    PolicyViolation sourcePolicyViolation = newPolicyViolation(sourceOrg, sourcePolicy);

    Policy sourcePolicyWithoutNotifications = tempEntity.newPolicy(sourceOrg.getId(),
        "sourcePolicyWithoutNotifications");

    Organization otherOrg = tempEntity.newOrganization("otherOrg");
    Policy otherPolicy1 = tempEntity.newPolicy(otherOrg.getId(), policyName);
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(otherPolicy1.getId(), otherOrg.getId());
    PolicyViolation policyViolation1 = newPolicyViolation(otherOrg, otherPolicy1);
    Policy otherPolicy2 = tempEntity.newPolicy(otherOrg.getId(), policyName + " something else");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(otherPolicy2.getId(), otherOrg.getId());
    PolicyViolation policyViolation2 = newPolicyViolation(otherOrg, otherPolicy2);
    addEmailNotification(otherPolicy2, BuildStageType.ID);
    addMonitoringEmailNotification(otherPolicy2);

    assertThat(migrator.migrate()).isTrue();
    // sourcePolicy was moved
    sourcePolicy = policyDAO.getById(sourcePolicy.getId());
    assertThat(sourcePolicy.getOwnerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    sourcePolicyWithoutNotifications = policyDAO.getById(sourcePolicyWithoutNotifications.getId());
    assertThat(sourcePolicyWithoutNotifications.getOwnerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    // email notifications were removed from sourcePolicy, all other actions were preserved
    assertThat(sourcePolicy.getActions().keySet()).hasSize(1);
    assertThat(sourcePolicy.getNotifications().getRoleNotifications()).hasSize(2);
    assertThat(sourcePolicy.getNotifications().getUserNotifications()).hasSize(0);
    // sourcePolicyViolation was not changed
    sourcePolicyViolation = policyViolationDAO.getById(sourcePolicyViolation.getId());
    assertThat(sourcePolicyViolation.getPolicyId()).isEqualTo(sourcePolicy.getId());
    // otherPolicy1 was deleted and its waiver and its policy violation were moved to sourcePolicy
    assertThat(policyDAO.getById(otherPolicy1.getId())).isNull();
    policyWaiver1 = policyWaiverDAO.getById(policyWaiver1.getId());
    assertThat(policyWaiverDAO.getById(policyWaiver1.getId()).getPolicyId()).isEqualTo(sourcePolicy.getId());
    policyViolation1 = policyViolationDAO.getById(policyViolation1.getId());
    assertThat(policyViolation1.getPolicyId()).isEqualTo(sourcePolicy.getId());
    // otherPolicy2, its waiver and its policy violation are unchanged
    otherPolicy2 = policyDAO.getById(otherPolicy2.getId());
    assertThat(otherPolicy2.getOwnerId()).isEqualTo(otherOrg.getId());
    assertThat(policyWaiverDAO.getById(policyWaiver2.getId()).getPolicyId()).isEqualTo(otherPolicy2.getId());
    assertThat(otherPolicy2.getNotifications().getUserNotifications()).hasSize(2);
    policyViolation2 = policyViolationDAO.getById(policyViolation2.getId());
    assertThat(policyViolation2.getPolicyId()).isEqualTo(otherPolicy2.getId());
  }

  private PolicyViolation newPolicyViolation(Organization org, Policy policy) {
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    return tempEntity.newPolicyViolation(policyEvaluation, policy);
  }

  @Test
  public void testMigrate_PolicyMonitoring() throws Exception {
    Organization sourceOrg = createSourceOrg();
    PolicyMonitoring sourcePolicyMonitoring = tempEntity.newPolicyMonitoring(sourceOrg.getId(), BuildStageType.ID);

    Organization otherOrg1 = tempEntity.newOrganization("otherOrg1");
    PolicyMonitoring otherPolicyMonitoring1 = tempEntity.newPolicyMonitoring(otherOrg1.getId(), BuildStageType.ID);
    Organization otherOrg2 = tempEntity.newOrganization("otherOrg2");
    PolicyMonitoring otherPolicyMonitoring2 = tempEntity.newPolicyMonitoring(otherOrg2.getId(), ReleaseStageType.ID);

    assertThat(migrator.migrate()).isTrue();

    // sourcePolicyMonitoring was moved
    assertThat(policyMonitoringDAO.getById(sourcePolicyMonitoring.getId()).getOwnerId())
        .isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    // otherPolicyMonitoring1 was removed because it monitored the same stage as sourcePolicyMonitoring
    assertThat(policyMonitoringDAO.getById(otherPolicyMonitoring1.getId())).isNull();
    assertThat(policyMonitoringDAO.getByOwnerId(otherOrg1.getId())).isNull();
    // otherPolicyMonitoring2 was not removed
    assertThat(policyMonitoringDAO.getById(otherPolicyMonitoring2.getId())).isNotNull();
  }

  @Test
  public void testMigrate_Labels() throws Exception {
    Organization sourceOrg = createSourceOrg();
    String labelName = "My Label";
    Label sourceLabel = tempEntity.newLabel(sourceOrg.getId(), labelName);

    Organization otherOrg = tempEntity.newOrganization("otherOrg");
    Label otherLabel1 = tempEntity.newLabel(otherOrg.getId(), labelName);
    ComponentLabel componentLabel1 = tempEntity.newComponentLabel(otherOrg.getId(), otherLabel1.getId(), "hash");
    Label otherLabel2 = tempEntity.newLabel(otherOrg.getId(), labelName + " something else");
    ComponentLabel componentLabel2 = tempEntity.newComponentLabel(otherOrg.getId(), otherLabel2.getId(), "hash");

    Application app = tempEntity.newApplication(otherOrg.getId());
    Policy orgPolicy1 = newPolicyForLabel(otherOrg, otherLabel1);
    Policy appPolicy1 = newPolicyForLabel(app, otherLabel1);
    Policy orgPolicy2 = newPolicyForLabel(otherOrg, otherLabel2);
    Policy appPolicy2 = newPolicyForLabel(app, otherLabel2);

    assertThat(migrator.migrate()).isTrue();
    // sourceLabel was moved
    sourceLabel = labelDAO.getById(sourceLabel.getId());
    assertThat(sourceLabel.getOwnerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(sourceLabel.getLabel()).isEqualTo(labelName);
    // otherLabel1 was deleted and componentLabel1 was moved to sourceLabel
    assertThat(labelDAO.getById(otherLabel1.getId())).isNull();
    assertThat(componentLabelDAO.getById(componentLabel1.getId()).getLabelId()).isEqualTo(sourceLabel.getId());
    // orgPolicy1 and appPolicy1 were updated to use sourceLabel
    assertThat(policyDAO.getById(orgPolicy1.getId()).getConstraints().get(0).getConditions().get(0).getValue())
        .isEqualTo(sourceLabel.getId());
    assertThat(policyDAO.getById(appPolicy1.getId()).getConstraints().get(0).getConditions().get(0).getValue())
        .isEqualTo(sourceLabel.getId());
    // otherLabel2 and componentLabel2 are unchanged
    otherLabel2 = labelDAO.getById(otherLabel2.getId());
    assertThat(otherLabel2.getOwnerId()).isEqualTo(otherOrg.getId());
    assertThat(componentLabelDAO.getById(componentLabel2.getId()).getLabelId()).isEqualTo(otherLabel2.getId());
    // orgPolicy2 and appPolicy2 are unchanged
    assertThat(policyDAO.getById(orgPolicy2.getId()).getConstraints().get(0).getConditions().get(0).getValue())
        .isEqualTo(otherLabel2.getId());
    assertThat(policyDAO.getById(appPolicy2.getId()).getConstraints().get(0).getConditions().get(0).getValue())
        .isEqualTo(otherLabel2.getId());
  }

  @Test
  public void testMigrate_LicenseThreatGroups() throws Exception {
    Organization sourceOrg = createSourceOrg();
    String ltgName = "My LTG";
    LicenseThreatGroup sourceLTG = tempEntity.newLicenseThreatGroup(sourceOrg.getId(), ltgName, 5);
    LicenseThreatGroupLicense sourceLTGL = tempEntity
        .newLicenseThreatGroupLicense(sourceOrg.getId(), sourceLTG.getId());

    Organization otherOrg = tempEntity.newOrganization("otherOrg");
    LicenseThreatGroup otherLTG1 = tempEntity.newLicenseThreatGroup(otherOrg.getId(), ltgName, 6);
    LicenseThreatGroupLicense otherLTGL1 = tempEntity.newLicenseThreatGroupLicense(otherOrg.getId(), otherLTG1.getId());
    LicenseThreatGroup otherLTG2 = tempEntity.newLicenseThreatGroup(otherOrg.getId(), ltgName + " something else", 7);
    LicenseThreatGroupLicense otherLTGL2 = tempEntity.newLicenseThreatGroupLicense(otherOrg.getId(), otherLTG2.getId());

    Application app = tempEntity.newApplication(otherOrg.getId());
    Policy orgPolicy1 = newPolicyForLTG(otherOrg, otherLTG1);
    Policy appPolicy1 = newPolicyForLTG(app, otherLTG1);
    Policy orgPolicy2 = newPolicyForLTG(otherOrg, otherLTG2);
    Policy appPolicy2 = newPolicyForLTG(app, otherLTG2);

    assertThat(migrator.migrate()).isTrue();

    // sourceLTG and sourceLTGL were moved
    sourceLTG = ltgDAO.getById(sourceLTG.getId());
    assertThat(sourceLTG.getOwnerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(sourceLTG.getName()).isEqualTo(ltgName);
    assertThat(ltglDAO.getById(sourceLTGL.getId()).getOwnerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    // otherLTG1 and otherLTGL1 were deleted
    assertThat(ltgDAO.getById(otherLTG1.getId())).isNull();
    assertThat(ltglDAO.getById(otherLTGL1.getId())).isNull();
    // orgPolicy1 and appPolicy1 were updated to use sourceLTG
    assertThat(policyDAO.getById(orgPolicy1.getId()).getConstraints().get(0).getConditions().get(0).getValue())
        .isEqualTo(sourceLTG.getId());
    assertThat(policyDAO.getById(appPolicy1.getId()).getConstraints().get(0).getConditions().get(0).getValue())
        .isEqualTo(sourceLTG.getId());
    // otherLabel2 and otherLTGL2 are unchanged
    assertThat(ltgDAO.getById(otherLTG2.getId()).getOwnerId()).isEqualTo(otherOrg.getId());
    assertThat(ltglDAO.getById(otherLTGL2.getId()).getOwnerId()).isEqualTo(otherOrg.getId());
    // orgPolicy2 and appPolicy2 are unchanged
    assertThat(policyDAO.getById(orgPolicy2.getId()).getConstraints().get(0).getConditions().get(0).getValue())
        .isEqualTo(otherLTG2.getId());
    assertThat(policyDAO.getById(appPolicy2.getId()).getConstraints().get(0).getConditions().get(0).getValue())
        .isEqualTo(otherLTG2.getId());
  }

  @Test
  public void testMigrate_Tags() throws Exception {
    Organization sourceOrg = createSourceOrg();
    String tagName = "My tag";
    Tag sourceTag = tempEntity.newTag(sourceOrg.getId(), tagName);

    Organization otherOrg = tempEntity.newOrganization("otherOrg");
    Application app = tempEntity.newApplication(otherOrg.getId());
    Policy policy = tempEntity.newPolicy(app);
    Tag otherTag1 = tempEntity.newTag(otherOrg.getId(), tagName);
    tempEntity.newApplicationTag(app.getId(), otherTag1.getId());
    tempEntity.newPolicyTag(policy.getId(), otherTag1.getId());
    Tag otherTag2 = tempEntity.newTag(otherOrg.getId(), tagName + " something else");
    tempEntity.newApplicationTag(app.getId(), otherTag2.getId());
    tempEntity.newPolicyTag(policy.getId(), otherTag2.getId());

    assertThat(migrator.migrate()).isTrue();

    // sourceTag was moved
    sourceTag = tagDAO.getById(sourceTag.getId());
    assertThat(sourceTag.getOrganizationId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(sourceTag.getName()).isEqualTo(tagName);
    // otherTag1 was deleted
    assertThat(tagDAO.getById(otherTag1.getId())).isNull();
    // otherTag2 is unchanged
    assertThat(tagDAO.getById(otherTag2.getId()).getOrganizationId()).isEqualTo(otherOrg.getId());
    // The app tag for otherTag1 was moved to sourceTag and the app tag for otherTag2 was not changed
    assertThat(appTagDAO.getByApplicationId(app.getId())).hasSize(2);
    assertThat(appTagDAO.getByApplicationIdAndTagId(app.getId(), sourceTag.getId())).isNotNull();
    assertThat(appTagDAO.getByApplicationIdAndTagId(app.getId(), otherTag2.getId())).isNotNull();
    // The policy tag for otherTag1 was moved to sourceTag and the policy tag for otherTag2 was not changed
    assertThat(policyTagDAO.getByPolicyId(policy.getId())).hasSize(2);
    assertThat(policyTagDAO.getByPolicyIdAndTagId(policy.getId(), sourceTag.getId())).isNotNull();
    assertThat(policyTagDAO.getByPolicyIdAndTagId(policy.getId(), otherTag2.getId())).isNotNull();
  }

  private void addFailAction(Policy policy, String stageTypeId) {
    policy.setAction(stageTypeId, Action.ID_FAIL);
    policyDAO.update(policy);
  }

  private void addEmailNotification(Policy policy, String stageTypeId) {
    policy.getNotifications().add(new UserNotification("test@sonatype.com", stageTypeId));
    policyDAO.update(policy);
  }

  private void addRoleNotification(Policy policy, String stageTypeId) {
    policy.getNotifications().add(new RoleNotification(Role.POLICY_ADMIN_ROLE_ID, stageTypeId));
    policyDAO.update(policy);
  }

  private void addMonitoringEmailNotification(Policy policy) {
    policy.getNotifications().add(new UserNotification("test@sonatype.com", Notification.CONTINUOUS_MONITORING));
    policyDAO.update(policy);
  }

  private void addMonitoringRoleNotification(Policy policy) {
    policy.getNotifications().add(new RoleNotification(Role.POLICY_ADMIN_ROLE_ID, Notification.CONTINUOUS_MONITORING));
    policyDAO.update(policy);
  }

  private Policy newPolicyForLabel(Owner owner, Label label) {
    Policy policy = new Policy(null /* id */, tempEntity.uuid());
    policy.setOwnerId(owner.getId());
    addConstraintForLabel(policy, label);

    tempEntity.newPolicy(policy);
    return policy;
  }

  private void addConstraintForLabel(Policy policy, Label label) {
    Condition condition = new Condition(LabelConditionType.ID, "is", label.getId());
    Constraint constraint = new Constraint(null /* constraintId */, "constraintForLabel", LogicalOperator.AND);
    constraint.addCondition(condition);
    policy.addConstraint(constraint);
  }

  private Policy newPolicyForLTG(Owner owner, LicenseThreatGroup ltg) {
    Policy policy = new Policy(null /* id */, tempEntity.uuid());
    policy.setOwnerId(owner.getId());
    addConstraintForLTG(policy, ltg);

    tempEntity.newPolicy(policy);
    return policy;
  }

  private void addConstraintForLTG(Policy policy, LicenseThreatGroup ltg) {
    Condition condition = new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId());
    Constraint constraint = new Constraint(null /* constraintId */, "constraintForLTG", LogicalOperator.AND);
    constraint.addCondition(condition);
    policy.addConstraint(constraint);
  }

  @Test
  public void testMigrate_RootOrgHasPolicy() throws Exception {
    createSourceOrg();
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
      migrator.migrate();
    }).withMessage(RootOrganizationConfigMigrator.ROOT_ORG_NOT_EMPTY_MESSAGE, "policies");
  }

  @Test
  public void testMigrate_RootOrgHasTag() throws Exception {
    createSourceOrg();
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
      migrator.migrate();
    }).withMessage(RootOrganizationConfigMigrator.ROOT_ORG_NOT_EMPTY_MESSAGE, "application categories");
  }

  @Test
  public void testMigrate_RootOrgHasPolicyMonitoring() throws Exception {
    createSourceOrg();
    tempEntity.newPolicyMonitoring(Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID);

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
      migrator.migrate();
    }).withMessage(RootOrganizationConfigMigrator.ROOT_ORG_NOT_EMPTY_MESSAGE, "policy monitoring");
  }

  @Test
  public void testMigrate_RootOrgHasLabel() throws Exception {
    createSourceOrg();
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
      migrator.migrate();
    }).withMessage(RootOrganizationConfigMigrator.ROOT_ORG_NOT_EMPTY_MESSAGE, "labels");
  }

  @Test
  public void testMigrate_RootOrgHasLicenseThreatGroup() throws Exception {
    createSourceOrg();
    tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID);

    // Should not throw an exception
    migrator.migrate();
  }

  @Test
  public void testMigrate_InternalDatabase_BackupIsCreated() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();

    try {
      // Create an on-disk database
      File dbDir = new File(tempDir.getRoot(), DatabaseName.ods.name());
      DatabaseConfig odsDatabaseConfig = new DatabaseConfig();
      odsDatabaseConfig.setDriverClassName("org.h2.Driver");
      odsDatabaseConfig.setUrl("jdbc:h2:" + dbDir.getAbsolutePath()
          + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE");
      odsDatabaseConfig.setUsername("sa");
      odsDatabaseConfig.setPassword("");
      odsDatabaseConfig.setMaxConnections(50);
      OperationalDataStoreProvider.init(odsDatabaseConfig, false);
      // Create an organization only to make it look like this is not a fresh install (that would not require a
      // migration).
      tempEntity.newOrganization();

      // Migration will fail because the source org id is invalid.
      // Verify that a backup was created.
      File backupDir = migrator.getDbBackupDir();
      migrationUtils.setSourceOrganizationId("Not a valid org id");
      assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
        migrator.migrate();
      });
      assertThat(backupDir).isDirectory();
      assertThat(new File(backupDir, "ods-db-backup.zip")).isFile();

      // Running the migration again should fail because a backup already exists (i.e. the previous migration failed).
      assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
        migrator.migrate();
      }).withMessageStartingWith("Cannot migrate config for root organization. The backup directory ");

      // Delete the backup dir, fix the source org id and try again.
      // Migration should succeed and there should be no backup left on disk.
      new FileCleaner().delete(backupDir);
      migrationTrackerDAO.deleteById(RootOrganizationConfigMigrationUtils.MIGRATION_CONFIG_ID);
      createSourceOrg();
      migrator.migrate();
      assertThat(backupDir).doesNotExist();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testMigrate_ExternalDatabase_BackupIsNotCreated() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();

    try (PostgresServer postgres = new PostgresServer()) {
      config.setDatabase(new com.sonatype.insight.brain.service.DatabaseConfig());
      // Create a postgres ODS database
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      // Create an organization only to make it look like this is not a fresh install (that would not require a
      // migration).
      tempEntity.newOrganization();

      createSourceOrg();
      assertThat(migrator.migrate()).isTrue();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private Organization createSourceOrg() {
    Organization org = tempEntity.newOrganization("SourceOrg");
    migrationUtils.setSourceOrganizationId(org.getId());
    return org;
  }
}

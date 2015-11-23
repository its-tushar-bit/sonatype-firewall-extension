/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
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
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RootOrganizationConfigMigratorTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private InsightConfig config;

  private InsightWork work;

  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  private LabelDAO labelDAO = new LabelDAO();

  private LicenseThreatGroupDAO ltgDAO = new LicenseThreatGroupDAO();

  private LicenseThreatGroupLicenseDAO ltglDAO = new LicenseThreatGroupLicenseDAO();

  private PolicyDAO policyDAO = new PolicyDAO();

  private PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  private PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();

  private ApplicationTagDAO appTagDAO = new ApplicationTagDAO();

  private PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  private TagDAO tagDAO = new TagDAO();

  private RootOrganizationConfigMigrator migrator;

  private RootOrganizationConfigMigrationUtils migrationUtils;

  @Before
  public void before() throws Exception {
    config = new InsightConfig();
    File workDir = tempDir.newFolder();
    workDir.mkdirs();
    config.setSonatypeWork(workDir.getAbsolutePath());

    work = new InsightWork(config);
    migrationUtils = new RootOrganizationConfigMigrationUtils(work);

    migrator = new RootOrganizationConfigMigrator(config, migrationUtils);
  }

  @Test
  public void testMigrate_AlreadyMigrated() throws Exception {
    migrationUtils.setMigrated();

    assertThat(migrator.migrate(), is(false));
  }

  @Test
  public void testMigrate_RootOrgIsVisible() throws Exception {
    config.setShowRootOrganization(true);

    assertThat(migrator.migrate(), is(false));

    // Hide root org, should not do any migration.
    config.setShowRootOrganization(false);
    assertThat(migrator.migrate(), is(false));
  }

  @Test
  public void testMigrate_MigrationConfigFileDoesNotExist() throws Exception {
    assertThat(migrator.migrate(), is(false));
  }

  @Test
  public void testMigrate() throws Exception {
    createSourceOrg();
    assertThat(migrator.migrate(), is(true));
    // Migration should not happen again
    assertThat(migrator.migrate(), is(false));
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

    Policy sourcePolicyWithoutNotifications = tempEntity.newPolicy(sourceOrg.getId(),
        "sourcePolicyWithoutNotifications");

    Organization otherOrg = tempEntity.newOrganization("otherOrg");
    Policy otherPolicy1 = tempEntity.newPolicy(otherOrg.getId(), policyName);
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(otherPolicy1.getId(), otherOrg.getId());
    Policy otherPolicy2 = tempEntity.newPolicy(otherOrg.getId(), policyName + " something else");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(otherPolicy2.getId(), otherOrg.getId());
    addEmailNotification(otherPolicy2, BuildStageType.ID);
    addMonitoringEmailNotification(otherPolicy2);

    assertThat(migrator.migrate(), is(true));
    // sourcePolicy was moved
    sourcePolicy = policyDAO.getById(sourcePolicy.getId());
    assertThat(sourcePolicy.getOwnerId(), is(Organization.ROOT_ORGANIZATION_ID));
    sourcePolicyWithoutNotifications = policyDAO.getById(sourcePolicyWithoutNotifications.getId());
    assertThat(sourcePolicyWithoutNotifications.getOwnerId(), is(Organization.ROOT_ORGANIZATION_ID));
    // email notifications were removed from sourcePolicy, all other actions were preserved
    List<Action> actions = sourcePolicy.getActions().get(BuildStageType.ID);
    assertThat(actions, hasSize(1));
    assertThat(actions.get(0).getTargetType(), is(NotifyActionType.TARGET_TYPE_ROLE));
    actions = sourcePolicy.getActions().get(ReleaseStageType.ID);
    assertThat(actions, hasSize(1));
    assertThat(actions.get(0).getActionTypeId(), is(Action.ID_FAIL));
    List<NotifyAction> monitoringActions = sourcePolicy.getMonitorNotifyActions();
    assertThat(monitoringActions, hasSize(1));
    assertThat(monitoringActions.get(0).getTargetType(), is(NotifyActionType.TARGET_TYPE_ROLE));
    // otherPolicy1 was deleted and its waiver was moved to sourcePolicy
    assertThat(policyDAO.getById(otherPolicy1.getId()), is(nullValue()));
    policyWaiver1 = policyWaiverDAO.getById(policyWaiver1.getId());
    assertThat(policyWaiverDAO.getById(policyWaiver1.getId()).getPolicyId(), is(sourcePolicy.getId()));
    // otherPolicy2 and its waiver are unchanged
    otherPolicy2 = policyDAO.getById(otherPolicy2.getId());
    assertThat(otherPolicy2.getOwnerId(), is(otherOrg.getId()));
    assertThat(policyWaiverDAO.getById(policyWaiver2.getId()).getPolicyId(), is(otherPolicy2.getId()));
    actions = otherPolicy2.getActions().get(BuildStageType.ID);
    assertThat(actions, hasSize(1));
    assertThat(actions.get(0).getTarget(), is("test@sonatype.com"));
    monitoringActions = otherPolicy2.getMonitorNotifyActions();
    assertThat(monitoringActions, hasSize(1));
    assertThat(monitoringActions.get(0).getTarget(), is("test@sonatype.com"));
  }

  @Test
  public void testMigrate_PolicyMonitoring() throws Exception {
    Organization sourceOrg = createSourceOrg();
    PolicyMonitoring sourcePolicyMonitoring = tempEntity.newPolicyMonitoring(sourceOrg.getId(), BuildStageType.ID);

    Organization otherOrg1 = tempEntity.newOrganization("otherOrg1");
    PolicyMonitoring otherPolicyMonitoring1 = tempEntity.newPolicyMonitoring(otherOrg1.getId(), BuildStageType.ID);
    Organization otherOrg2 = tempEntity.newOrganization("otherOrg2");
    PolicyMonitoring otherPolicyMonitoring2 = tempEntity.newPolicyMonitoring(otherOrg2.getId(), ReleaseStageType.ID);

    assertThat(migrator.migrate(), is(true));

    // sourcePolicyMonitoring was moved
    assertThat(policyMonitoringDAO.getById(sourcePolicyMonitoring.getId()).getOwnerId(),
        is(Organization.ROOT_ORGANIZATION_ID));
    // otherPolicyMonitoring1 was removed because it monitored the same stage as sourcePolicyMonitoring
    assertThat(policyMonitoringDAO.getById(otherPolicyMonitoring1.getId()), is(nullValue()));
    assertThat(policyMonitoringDAO.getByOwnerId(otherOrg1.getId()), is(nullValue()));
    // otherPolicyMonitoring2 was not removed
    assertThat(policyMonitoringDAO.getById(otherPolicyMonitoring2.getId()), is(notNullValue()));
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

    assertThat(migrator.migrate(), is(true));
    // sourceLabel was moved
    sourceLabel = labelDAO.getById(sourceLabel.getId());
    assertThat(sourceLabel.getOwnerId(), is(Organization.ROOT_ORGANIZATION_ID));
    assertThat(sourceLabel.getLabel(), is(labelName));
    // otherLabel1 was deleted and componentLabel1 was moved to sourceLabel
    assertThat(labelDAO.getById(otherLabel1.getId()), is(nullValue()));
    assertThat(componentLabelDAO.getById(componentLabel1.getId()).getLabelId(), is(sourceLabel.getId()));
    // orgPolicy1 and appPolicy1 were updated to use sourceLabel
    assertThat(policyDAO.getById(orgPolicy1.getId()).getConstraints().get(0).getConditions().get(0).getValue(),
        is(sourceLabel.getId()));
    assertThat(policyDAO.getById(appPolicy1.getId()).getConstraints().get(0).getConditions().get(0).getValue(),
        is(sourceLabel.getId()));
    // otherLabel2 and componentLabel2 are unchanged
    otherLabel2 = labelDAO.getById(otherLabel2.getId());
    assertThat(otherLabel2.getOwnerId(), is(otherOrg.getId()));
    assertThat(componentLabelDAO.getById(componentLabel2.getId()).getLabelId(), is(otherLabel2.getId()));
    // orgPolicy2 and appPolicy2 are unchanged
    assertThat(policyDAO.getById(orgPolicy2.getId()).getConstraints().get(0).getConditions().get(0).getValue(),
        is(otherLabel2.getId()));
    assertThat(policyDAO.getById(appPolicy2.getId()).getConstraints().get(0).getConditions().get(0).getValue(),
        is(otherLabel2.getId()));
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

    assertThat(migrator.migrate(), is(true));

    // sourceLTG and sourceLTGL were moved
    sourceLTG = ltgDAO.getById(sourceLTG.getId());
    assertThat(sourceLTG.getOwnerId(), is(Organization.ROOT_ORGANIZATION_ID));
    assertThat(sourceLTG.getName(), is(ltgName));
    assertThat(ltglDAO.getById(sourceLTGL.getId()).getOwnerId(), is(Organization.ROOT_ORGANIZATION_ID));
    // otherLTG1 and otherLTGL1 were deleted
    assertThat(ltgDAO.getById(otherLTG1.getId()), is(nullValue()));
    assertThat(ltglDAO.getById(otherLTGL1.getId()), is(nullValue()));
    // orgPolicy1 and appPolicy1 were updated to use sourceLTG
    assertThat(policyDAO.getById(orgPolicy1.getId()).getConstraints().get(0).getConditions().get(0).getValue(),
        is(sourceLTG.getId()));
    assertThat(policyDAO.getById(appPolicy1.getId()).getConstraints().get(0).getConditions().get(0).getValue(),
        is(sourceLTG.getId()));
    // otherLabel2 and otherLTGL2 are unchanged
    assertThat(ltgDAO.getById(otherLTG2.getId()).getOwnerId(), is(otherOrg.getId()));
    assertThat(ltglDAO.getById(otherLTGL2.getId()).getOwnerId(), is(otherOrg.getId()));
    // orgPolicy2 and appPolicy2 are unchanged
    assertThat(policyDAO.getById(orgPolicy2.getId()).getConstraints().get(0).getConditions().get(0).getValue(),
        is(otherLTG2.getId()));
    assertThat(policyDAO.getById(appPolicy2.getId()).getConstraints().get(0).getConditions().get(0).getValue(),
        is(otherLTG2.getId()));
  }

  @Test
  public void testMigrate_Tags() throws Exception {
    Organization sourceOrg = createSourceOrg();
    String tagName = "My tag";
    Tag sourceTag = tempEntity.newTag(sourceOrg.getId(), tagName);

    Organization otherOrg = tempEntity.newOrganization("otherOrg");
    Application app = tempEntity.newApplication(otherOrg.getId());
    Policy policy = tempEntity.newPolicy(app.getId(), "name");
    Tag otherTag1 = tempEntity.newTag(otherOrg.getId(), tagName);
    tempEntity.newApplicationTag(app.getId(), otherTag1.getId());
    tempEntity.newPolicyTag(policy.getId(), otherTag1.getId());
    Tag otherTag2 = tempEntity.newTag(otherOrg.getId(), tagName + " something else");
    tempEntity.newApplicationTag(app.getId(), otherTag2.getId());
    tempEntity.newPolicyTag(policy.getId(), otherTag2.getId());

    assertThat(migrator.migrate(), is(true));

    // sourceTag was moved
    sourceTag = tagDAO.getById(sourceTag.getId());
    assertThat(sourceTag.getOrganizationId(), is(Organization.ROOT_ORGANIZATION_ID));
    assertThat(sourceTag.getName(), is(tagName));
    // otherTag1 was deleted
    assertThat(tagDAO.getById(otherTag1.getId()), is(nullValue()));
    // otherTag2 is unchanged
    assertThat(tagDAO.getById(otherTag2.getId()).getOrganizationId(), is(otherOrg.getId()));
    // The app tag for otherTag1 was moved to sourceTag and the app tag for otherTag2 was not changed
    assertThat(appTagDAO.getByApplicationId(app.getId()), hasSize(2));
    assertThat(appTagDAO.getByApplicationIdAndTagId(app.getId(), sourceTag.getId()), is(notNullValue()));
    assertThat(appTagDAO.getByApplicationIdAndTagId(app.getId(), otherTag2.getId()), is(notNullValue()));
    // The policy tag for otherTag1 was moved to sourceTag and the policy tag for otherTag2 was not changed
    assertThat(policyTagDAO.getByPolicyId(policy.getId()), hasSize(2));
    assertThat(policyTagDAO.getByPolicyIdAndTagId(policy.getId(), sourceTag.getId()), is(notNullValue()));
    assertThat(policyTagDAO.getByPolicyIdAndTagId(policy.getId(), otherTag2.getId()), is(notNullValue()));
  }

  private void addFailAction(Policy policy, String stageTypeId) {
    Action action = new Action(Action.ID_FAIL);
    policy.addAction(stageTypeId, action);
    policyDAO.update(policy);
  }

  private void addEmailNotification(Policy policy, String stageTypeId) {
    Action action = new Action(NotifyActionType.ID, "test@sonatype.com");
    policy.addAction(stageTypeId, action);
    policyDAO.update(policy);
  }

  private void addRoleNotification(Policy policy, String stageTypeId) {
    Action action = new Action(NotifyActionType.ID, Role.CLM_ADMIN_ROLE_ID);
    action.setTargetType(NotifyActionType.TARGET_TYPE_ROLE);
    policy.addAction(stageTypeId, action);
    policyDAO.update(policy);
  }

  private void addMonitoringEmailNotification(Policy policy) {
    NotifyAction action = new NotifyAction("test@sonatype.com", null /* targetType */);
    policy.addMonitorNotifyAction(action);
    policyDAO.update(policy);
  }

  private void addMonitoringRoleNotification(Policy policy) {
    NotifyAction action = new NotifyAction(Role.CLM_ADMIN_ROLE_ID, NotifyActionType.TARGET_TYPE_ROLE);
    policy.addMonitorNotifyAction(action);
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
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy name");

    try {
      migrator.migrate();
      fail("Expected exception");
    }
    catch (RuntimeException expected) {
      assertThat(expected.getMessage(),
          is(String.format(RootOrganizationConfigMigrator.ROOT_ORG_NOT_EMPTY_MESSAGE, "policies")));
    }
  }

  @Test
  public void testMigrate_RootOrgHasTag() throws Exception {
    createSourceOrg();
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);

    try {
      migrator.migrate();
      fail("Expected exception");
    }
    catch (RuntimeException expected) {
      assertThat(expected.getMessage(),
          is(String.format(RootOrganizationConfigMigrator.ROOT_ORG_NOT_EMPTY_MESSAGE, "application categories")));
    }
  }

  @Test
  public void testMigrate_RootOrgHasPolicyMonitoring() throws Exception {
    createSourceOrg();
    tempEntity.newPolicyMonitoring(Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID);

    try {
      migrator.migrate();
      fail("Expected exception");
    }
    catch (RuntimeException expected) {
      assertThat(expected.getMessage(),
          is(String.format(RootOrganizationConfigMigrator.ROOT_ORG_NOT_EMPTY_MESSAGE, "policy monitoring")));
    }
  }

  @Test
  public void testMigrate_RootOrgHasLabel() throws Exception {
    createSourceOrg();
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);

    try {
      migrator.migrate();
      fail("Expected exception");
    }
    catch (RuntimeException expected) {
      assertThat(expected.getMessage(),
          is(String.format(RootOrganizationConfigMigrator.ROOT_ORG_NOT_EMPTY_MESSAGE, "labels")));
    }
  }

  @Test
  public void testMigrate_RootOrgHasLicenseThreatGroup() throws Exception {
    createSourceOrg();
    tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID);

    // Should not throw an exception
    migrator.migrate();
  }

  @Test
  public void testBackup() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();

    try {
      // Create an on-disk database
      File dbDir = new File(tempDir.getRoot(), "ods");
      DatabaseConfig odsDatabaseConfig = new DatabaseConfig();
      odsDatabaseConfig.setDriverClassName("org.h2.Driver");
      odsDatabaseConfig.setUrl("jdbc:h2:" + dbDir.getAbsolutePath()
          + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
      odsDatabaseConfig.setUsername("sa");
      odsDatabaseConfig.setPassword("");
      odsDatabaseConfig.setMaxConnections(50);
      OperationalDataStoreProvider.init(odsDatabaseConfig);

      // Migration will fail because the source org id is invalid.
      // Verify that a backup was created.
      File backupDir = migrator.getDbBackupDir();
      migrationUtils.setSourceOrganizationId("Not a valid org id");
      try {
        migrator.migrate();
        fail("Expected exception");
      }
      catch (NotFoundException expected) {
        assertThat(backupDir.isDirectory(), is(true));
        assertThat(new File(backupDir, "ods-db-backup.zip").isFile(), is(true));
      }

      // Running the migration again should fail because a backup already exists (i.e. the previous migration failed).
      try {
        migrator.migrate();
        fail("Expected exception");
      }
      catch (IllegalStateException expected) {
        assertThat(expected.getMessage(),
            startsWith("Cannot migrate config for root organization. The backup directory "));
      }

      // Delete the backup dir, fix the source org id and try again.
      // Migration should succeed and there should be no backup left on disk.
      new FileCleaner().delete(backupDir);
      createSourceOrg();
      migrator.migrate();
      assertThat(backupDir.exists(), is(false));
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private Organization createSourceOrg() throws IOException {
    Organization org = tempEntity.newOrganization("SourceOrg");
    migrationUtils.setSourceOrganizationId(org.getId());
    return org;
  }
}

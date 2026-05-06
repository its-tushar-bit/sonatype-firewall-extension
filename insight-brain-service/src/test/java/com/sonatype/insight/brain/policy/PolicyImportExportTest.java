/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionValidator;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.ConstraintValidator;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyValidator;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.NotificationsValidator;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.UserNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotificationValidator;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 1.7
 */
public class PolicyImportExportTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private LabelDAO labelDAO;

  @Inject
  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Inject
  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  @Inject
  private ComponentLabelDAO componentLabelDAO;

  @Inject
  private TagDAO tagDAO;

  @Inject
  private PolicyTagDAO policyTagDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private RoleDAO roleDAO;

  private Organization fromOrg;

  private Application fromApp;

  @Inject
  private PolicyImportExport policyImportExport;

  @Before
  public void before() {
    fromOrg = tempEntity.newOrganization();
    fromApp = tempEntity.newApplication(fromOrg.getId());
  }

  private void deleteFromOrg() {
    applicationDAO.delete(fromApp);
    organizationDAO.delete(fromOrg);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testImportAndMergeLabelsForOrg() throws Exception {
    List<Label> orgLabels = createLabels(fromOrg.getId());
    createPolicy(fromOrg.getId(), orgLabels.get(0).getId(), "Org Policy");
    PolicyExportResult exportDTO = policyImportExport.exportOrganization(fromOrg);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();

    // change the color to a legacy value
    exportDTO.labels.get(0).setColor(Color.black);
    exportDTO.labels.get(1).setColor(Color.blue);

    Organization toOrg = tempEntity.newOrganization();

    Label oldLabelToUpdate = new Label(toOrg.getId(), orgLabels.get(0).getLabel().toLowerCase(), Color.light_green);
    oldLabelToUpdate.setDescription("anything");
    labelDAO.insert(oldLabelToUpdate);

    Label oldLabelToKeep = new Label(toOrg.getId(), "keepMe", Color.dark_red);
    labelDAO.insert(oldLabelToKeep);

    List<Label> oldLabels = Lists.newArrayList(oldLabelToUpdate, oldLabelToKeep);

    try (TransactionContext tx = labelDAO.createTransactionContext()) {
      tx.begin();
      policyImportExport.importAndMergeLabels(tx, exportDTO, oldLabels, toOrg);
      tx.commit();
    }

    List<Label> labels = labelDAO.getByOwnerId(toOrg.getId());
    assertThat(labels).hasSize(3);

    Label keptLabel = labels.get(0);
    assertThat(keptLabel.getColor()).isEqualTo(Color.dark_red);
    assertThat(keptLabel.getLabel()).isEqualTo("keepMe");

    Label updatedLabel = labels.get(1);
    assertThat(updatedLabel.getColor()).isEqualTo(Color.dark_purple); // updated
    assertThat(updatedLabel.getLabel()).isEqualTo("LABEL1"); // updated from the lowercase version
    assertThat(updatedLabel.getId()).isEqualTo(oldLabelToUpdate.getId()); // id remains the same
    assertThat(updatedLabel.getDescription()).isNull(); // existing description is removed

    Label importedLabel = labels.get(2);
    assertThat(importedLabel.getColor()).isEqualTo(Color.dark_blue);
    assertThat(importedLabel.getLabel()).isEqualTo("LABEL2");

    assertThat(exportDTO.policies.get(0).getConstraints().get(0).getConditions().get(0).getValue())
        .isEqualTo(oldLabelToUpdate.getId());
  }

  @Test
  public void testDeletionOfPolicyWaiversFromOrg() {
    DateTime now = DateTime.now();
    Organization toOrg = tempEntity.newOrganization();
    Application toApp = tempEntity.newApplication(toOrg.getId());
    Policy orgPolicy = tempEntity.newPolicy(toOrg);
    Policy appPolicy = tempEntity.newPolicy(toApp);
    tempEntity.newWaiver(orgPolicy.getId(), toOrg.getId());
    tempEntity.newWaiver("expiring", orgPolicy.getId(), toOrg.getId(), null, "comment",
        now.toDate(), now.plusHours(1).toDate());
    tempEntity.newWaiver("expired", orgPolicy.getId(), toOrg.getId(), null, "comment",
        now.toDate(), now.toDate());
    tempEntity.newWaiver(appPolicy.getId(), toApp.getId());
    tempEntity.newWaiver("expiring", appPolicy.getId(), toApp.getId(), null, "comment",
        now.toDate(), now.plusHours(1).toDate());
    tempEntity.newWaiver("expired", appPolicy.getId(), toApp.getId(), null, "comment",
        now.toDate(), now.toDate());

    // only interested in the deletion so import an empty DTO
    policyImportExport.importOrganization(toOrg, new PolicyExportResult());

    assertThat(policyWaiverDAO.getByOwnerId(toOrg.getId())).isEmpty();
    assertThat(policyWaiverDAO.getByOwnerId(toApp.getId())).isEmpty();
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testImportAndMergeTags_UpdateTag() throws Exception {
    Tag fromTag = tempEntity.newTag(fromOrg.getId(), "tagname", Color.dark_purple);
    Policy policy = tempEntity.newPolicy(fromOrg);
    tempEntity.newPolicyTag(policy.getId(), fromTag.getId());

    PolicyExportResult exportDTO = policyImportExport.exportOrganization(fromOrg);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();
    exportDTO.tags.get(0).setColor(Color.black); // change the color to a legacy value

    Organization toOrg = tempEntity.newOrganization();
    Tag toTag = tempEntity.newTag(toOrg.getId(), "TAG NAME", Color.yellow);

    try (TransactionContext tx = tagDAO.createTransactionContext()) {
      tx.begin();
      policyImportExport.importAndMergeTags(tx, exportDTO, toOrg);
      tx.commit();
    }

    assertTag(fromTag, tagDAO.getById(toTag.getId()));
    assertThat(exportDTO.policyTags.get(0).getTagId()).isEqualTo(toTag.getId());
  }

  @Test
  public void testImportAndMergeTags_NewTag() throws Exception {
    Tag tag = tempEntity.newTag(fromOrg.getId(), "Tag Name", Color.dark_purple);
    Policy policy = tempEntity.newPolicy(fromOrg);
    tempEntity.newPolicyTag(policy.getId(), tag.getId());

    PolicyExportResult exportDTO = policyImportExport.exportOrganization(fromOrg);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();

    Organization toOrg = tempEntity.newOrganization();

    try (TransactionContext tx = tagDAO.createTransactionContext()) {
      tx.begin();
      policyImportExport.importAndMergeTags(tx, exportDTO, toOrg);
      tx.commit();
    }

    List<Tag> tags = tagDAO.getByOrganizationId(toOrg.getId());
    assertThat(tags).hasSize(1);
    assertTag(tag, tags.get(0));
    assertThat(tags.get(0).getId()).isNotEqualTo(tag.getId());
    assertThat(exportDTO.policyTags.get(0).getTagId()).isEqualTo(tags.get(0).getId());
  }

  @Test
  public void testImport_LicenseThreatGroupUnassignedCondition() throws Exception {
    Policy policy = new Policy("testPolicyId", "testPolicyName");
    policy.setOwnerId(fromOrg.getId());
    Constraint constraint = new Constraint("testConstraintId", "testConstraintName", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is",
        LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    PolicyExportResult exportDTO = policyImportExport.exportOrganization(fromOrg);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();

    Organization toOrg = tempEntity.newOrganization();

    policyImportExport.importOrganization(toOrg, exportDTO);

    List<Policy> importedPolicies = policyDAO.getByOwnerId(toOrg.getId());
    assertThat(importedPolicies).hasSize(1);
    policy = importedPolicies.get(0);
    List<Constraint> constraints = policy.getConstraints();
    assertThat(constraints).hasSize(1);
    List<Condition> conditions = constraints.get(0).getConditions();
    assertThat(conditions).hasSize(1);
    Condition condition = conditions.get(0);
    assertThat(condition.getConditionTypeId()).isEqualTo(LicenseThreatGroupConditionType.ID);
    assertThat(condition.getOperator()).isEqualTo("is");
    assertThat(condition.getValue()).isEqualTo(LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID);
  }

  @Test
  public void testImport_ToRootOrganizationDeletesAllLtgsWaiversPolicies() {
    final Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final Policy rootOrganizationPolicy = tempEntity.newPolicy(rootOrganization);
    final PolicyWaiver rootOrganizationPolicyWaiver = tempEntity.newWaiver(rootOrganizationPolicy.getId(),
        rootOrganization.getId());
    final LicenseThreatGroup rootOrganizationLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(rootOrganization.getId());

    final Organization organizationOne = tempEntity.newOrganization("organizationOne");
    final Policy organizationOnePolicy = tempEntity.newPolicy(organizationOne);
    final PolicyWaiver organizationOnePolicyWaiver = tempEntity.newWaiver(rootOrganizationPolicy.getId(),
        organizationOne.getId());
    final LicenseThreatGroup organizationOneLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(organizationOne.getId());
    final Application applicationOne = tempEntity.newApplication(organizationOne.getId());
    tempEntity.newPolicy(applicationOne);
    final PolicyWaiver applicationOnePolicyWaiver = tempEntity.newWaiver(organizationOnePolicy.getId(),
        applicationOne.getId());
    final LicenseThreatGroup applicationOneLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(applicationOne.getId());

    final Organization organizationTwo = tempEntity.newOrganization("organizationTwo");
    final Policy organizationTwoPolicy = tempEntity.newPolicy(organizationTwo);
    final PolicyWaiver organizationTwoPolicyWaiver = tempEntity.newWaiver(rootOrganizationPolicy.getId(),
        organizationTwo.getId());
    final LicenseThreatGroup organizationTwoLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(organizationTwo.getId());
    final Application applicationTwo = tempEntity.newApplication(organizationTwo.getId());
    tempEntity.newPolicy(applicationTwo);
    final PolicyWaiver applicationTwoPolicyWaiver = tempEntity.newWaiver(organizationTwoPolicy.getId(),
        applicationTwo.getId());
    final LicenseThreatGroup applicationTwoLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(applicationTwo.getId());

    final Repository repository = tempEntity.newRepository();
    final PolicyWaiver repositoryPolicyWaiver = tempEntity.newWaiver(rootOrganizationPolicy.getId(),
        repository.getId());

    policyImportExport.importOrganization(rootOrganization, new PolicyExportResult());

    assertThat(policyDAO.getAll()).isEmpty();
    assertThat(policyWaiverDAO.getById(rootOrganizationPolicyWaiver.getId())).isNull();
    assertThat(policyWaiverDAO.getById(organizationOnePolicyWaiver.getId())).isNull();
    assertThat(policyWaiverDAO.getById(organizationTwoPolicyWaiver.getId())).isNull();
    assertThat(policyWaiverDAO.getById(applicationOnePolicyWaiver.getId())).isNull();
    assertThat(policyWaiverDAO.getById(applicationTwoPolicyWaiver.getId())).isNull();
    assertThat(policyWaiverDAO.getById(repositoryPolicyWaiver.getId())).isNull();
    assertThat(licenseThreatGroupDAO.getById(rootOrganizationLicenseThreatGroup.getId())).isNull();
    assertThat(licenseThreatGroupDAO.getById(organizationOneLicenseThreatGroup.getId())).isNull();
    assertThat(licenseThreatGroupDAO.getById(organizationTwoLicenseThreatGroup.getId())).isNull();
    assertThat(licenseThreatGroupDAO.getById(applicationOneLicenseThreatGroup.getId())).isNull();
    assertThat(licenseThreatGroupDAO.getById(applicationTwoLicenseThreatGroup.getId())).isNull();
  }

  @Test
  public void testImport_ToChildOrganizationDoesNotDeleteAllLtgsWaiversPolicies() {
    final Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final Policy rootOrganizationPolicy = tempEntity.newPolicy(rootOrganization);
    final PolicyWaiver rootOrganizationPolicyWaiver = tempEntity.newWaiver(rootOrganizationPolicy.getId(),
        rootOrganization.getId());
    final LicenseThreatGroup rootOrganizationLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(rootOrganization.getId());

    final Organization organizationOne = tempEntity.newOrganization("organizationOne");
    final Policy organizationOnePolicy = tempEntity.newPolicy(organizationOne);
    final PolicyWaiver organizationOnePolicyWaiver = tempEntity.newWaiver(rootOrganizationPolicy.getId(),
        organizationOne.getId());
    final LicenseThreatGroup organizationOneLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(organizationOne.getId());
    final Application applicationOne = tempEntity.newApplication(organizationOne.getId());
    final Policy applicationOnePolicy = tempEntity.newPolicy(applicationOne);
    final PolicyWaiver applicationOnePolicyWaiver = tempEntity.newWaiver(organizationOnePolicy.getId(),
        applicationOne.getId());
    final LicenseThreatGroup applicationOneLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(applicationOne.getId());

    final Organization organizationTwo = tempEntity.newOrganization("organizationTwo");
    final Policy organizationTwoPolicy = tempEntity.newPolicy(organizationTwo);
    final PolicyWaiver organizationTwoPolicyWaiver = tempEntity.newWaiver(rootOrganizationPolicy.getId(),
        organizationTwo.getId());
    final LicenseThreatGroup organizationTwoLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(organizationTwo.getId());
    final Application applicationTwo = tempEntity.newApplication(organizationTwo.getId());
    final Policy applicationTwoPolicy = tempEntity.newPolicy(applicationTwo);
    final PolicyWaiver applicationTwoPolicyWaiver = tempEntity.newWaiver(organizationTwoPolicy.getId(),
        applicationTwo.getId());
    final LicenseThreatGroup applicationTwoLicenseThreatGroup = tempEntity
        .newLicenseThreatGroup(applicationTwo.getId());

    final Repository repository = tempEntity.newRepository();
    final PolicyWaiver repositoryPolicyWaiver = tempEntity.newWaiver(rootOrganizationPolicy.getId(),
        repository.getId());

    policyImportExport.importOrganization(organizationOne, new PolicyExportResult());

    assertThat(policyDAO.getById(rootOrganizationPolicy.getId())).isNotNull();
    assertThat(policyDAO.getById(organizationOnePolicy.getId())).isNull();
    assertThat(policyDAO.getById(organizationTwoPolicy.getId())).isNotNull();
    assertThat(policyDAO.getById(applicationOnePolicy.getId())).isNull();
    assertThat(policyDAO.getById(applicationTwoPolicy.getId())).isNotNull();
    assertThat(policyWaiverDAO.getById(rootOrganizationPolicyWaiver.getId())).isNotNull();
    assertThat(policyWaiverDAO.getById(organizationOnePolicyWaiver.getId())).isNull();
    assertThat(policyWaiverDAO.getById(organizationTwoPolicyWaiver.getId())).isNotNull();
    assertThat(policyWaiverDAO.getById(applicationOnePolicyWaiver.getId())).isNull();
    assertThat(policyWaiverDAO.getById(applicationTwoPolicyWaiver.getId())).isNotNull();
    assertThat(policyWaiverDAO.getById(repositoryPolicyWaiver.getId())).isNotNull();
    assertThat(licenseThreatGroupDAO.getById(rootOrganizationLicenseThreatGroup.getId())).isNotNull();
    assertThat(licenseThreatGroupDAO.getById(organizationOneLicenseThreatGroup.getId())).isNull();
    assertThat(licenseThreatGroupDAO.getById(organizationTwoLicenseThreatGroup.getId())).isNotNull();
    assertThat(licenseThreatGroupDAO.getById(applicationOneLicenseThreatGroup.getId())).isNull();
    assertThat(licenseThreatGroupDAO.getById(applicationTwoLicenseThreatGroup.getId())).isNotNull();
  }

  /**
   * Serialize the PolicyExportResult through json to ensure that all objects are detached from the db and that they are
   * created fresh as they are when imported from json.
   */
  private PolicyExportResult detachObjects(PolicyExportResult policyExportResult) throws IOException {
    String s = JsonUtils.format(policyExportResult);
    return JsonUtils.parse(s, PolicyExportResult.class);
  }

  private List<Policy> createPolicy(String ownerId, String labelId, String policyName) {
    Policy policy = new Policy(null, policyName);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, TemporaryEntity.uuid(), LogicalOperator.AND);
    constraint.addCondition(new Condition(LabelConditionType.ID, "is", labelId));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);
    return Lists.newArrayList(policy);
  }

  private List<Label> createLabels(String ownerId) {
    Label label1 = tempEntity.newLabel(ownerId, "LABEL1", Color.dark_purple);
    Label label2 = tempEntity.newLabel(ownerId, "LABEL2", Color.dark_blue);
    return Lists.newArrayList(label1, label2);
  }

  public static void assertTag(Tag expected, Tag actual) {
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getNameLowercaseNoWhitespace()).isEqualTo(expected.getNameLowercaseNoWhitespace());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getColor()).isEqualTo(expected.getColor());
  }

  @Test
  public void testExportImport_Update() throws Exception {
    Organization org = tempEntity.newOrganization();

    String orgId = org.getId();

    Label label1 = tempEntity.newLabel(orgId, "label1", Color.dark_blue);
    Label label2 = tempEntity.newLabel(orgId, "label2", Color.dark_red);
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(orgId);
    LicenseThreatGroupLicense licenseThreatGroupLicense = tempEntity.newLicenseThreatGroupLicense(orgId,
        licenseThreatGroup.getId());

    Policy policy = new Policy();
    policy.setOwnerId(orgId);
    policy.setName("Policy1");
    Constraint constraint1 = new Constraint();
    constraint1.setName("Constraint1");
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label1.getId()));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint();
    constraint2.setName("Constraint2");
    constraint2.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", licenseThreatGroup.getId()));
    policy.addConstraint(constraint2);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    tempEntity.newPolicy(policy);

    // Export
    PolicyExportResult policyExportResult = policyImportExport.exportOrganization(org);
    assertThat(policyExportResult).isNotNull();
    assertThat(policyExportResult.policies).isNotEmpty();
    assertThat(policyExportResult.labels).isNotEmpty();
    assertThat(policyExportResult.licenseThreatGroups).isNotEmpty();
    assertThat(policyExportResult.licenseThreatGroupLicenses).isNotEmpty();

    // Delete and re-create one label - it should be reset by import (matched by label case insensitive)
    labelDAO.delete(label1);
    policy.getConstraints().remove(constraint1); // the label was assosiated with this constraint
    policyDAO.update(policy);

    label1 = tempEntity.newLabel(orgId, label1.getLabel().toUpperCase(Locale.ENGLISH), Color.dark_purple);
    // Delete one label - it should be re-created by the import.
    labelDAO.delete(label2);
    // Add a new label - it should be retained through the import.
    tempEntity.newLabel(orgId, "label3", Color.dark_red);

    // Import
    policyExportResult.tags = Collections.emptyList();
    policyExportResult.policyTags = Collections.emptyList();
    policyExportResult = detachObjects(policyExportResult);
    PolicyImportResult policyImportResult = policyImportExport.importOrganization(org, policyExportResult);
    assertThat(policyImportResult).isNotNull();
    assertThat(policyImportResult.ownerName).isEqualTo(org.getName());
    List<Label> labels = labelDAO.getByOwnerId(orgId);
    // All labels retained.
    assertThat(labels).hasSize(3);
    assertThat(labels.get(0).getId()).isEqualTo(label1.getId());
    assertThat(labels.get(0).getLabel()).isEqualTo("label1");
    assertThat(labels.get(0).getColor()).isEqualTo(Color.dark_blue);
    assertThat(labels.get(1).getId()).isNotEqualTo(label2.getId());
    assertThat(labels.get(1).getLabel()).isEqualTo(label2.getLabel());
    assertThat(labels.get(1).getColor()).isEqualTo(label2.getColor());
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(orgId);
    assertThat(licenseThreatGroups).hasSize(1);
    assertThat(licenseThreatGroups.get(0).getName()).isEqualTo(licenseThreatGroup.getName());
    assertThat(licenseThreatGroups.get(0).getId()).isNotEqualTo(licenseThreatGroup.getId());
    List<LicenseThreatGroupLicense> licenseThreatGroupLicenses = licenseThreatGroupLicenseDAO.getByOwnerId(orgId);
    assertThat(licenseThreatGroupLicenses).hasSize(1);
    assertThat(licenseThreatGroupLicenses.get(0).getLicenseId()).isEqualTo(licenseThreatGroupLicense.getLicenseId());
    assertThat(licenseThreatGroupLicenses.get(0).getId()).isNotEqualTo(licenseThreatGroupLicense.getId());
    List<Policy> policies = policyDAO.getByOwnerId(orgId);
    assertThat(policies).hasSize(1);
    assertThat(policies.get(0).getName()).isEqualTo(policy.getName());
    assertThat(policies.get(0).getId()).isNotEqualTo(policy.getId());

    ConditionValidator conditionValidator = new ConditionValidator();
    ConstraintValidator constraintValidator = new ConstraintValidator(conditionValidator);
    UserNotificationValidator userNotificationValidator = new UserNotificationValidator();
    RoleNotificationValidator roleNotificationValidator =
        new RoleNotificationValidator(() -> roleDAO);
    JiraNotificationValidator jiraNotificationValidator = new JiraNotificationValidator();
    WebhookNotificationValidator webhookNotificationValidator = new WebhookNotificationValidator();
    NotificationsValidator notificationsValidator =
        new NotificationsValidator(userNotificationValidator, roleNotificationValidator, jiraNotificationValidator,
            webhookNotificationValidator);
    PolicyValidator policyValidator = new PolicyValidator(constraintValidator, notificationsValidator);

    ValidationResult policyValidationResult = policyValidator.validate(null, policies.get(0), orgId);
    assertThat(policyValidationResult.isValid()).as(policyValidationResult.toMessageString()).isTrue();
  }

  @Test
  public void testExportOfTags() {
    Policy policy1 = tempEntity.newPolicy(fromOrg);
    Policy policy2 = tempEntity.newPolicy(fromOrg);

    Tag tag1 = tempEntity.newTag(fromOrg.getId(), "tag1");
    tempEntity.newPolicyTag(policy1.getId(), tag1.getId());

    Tag tag2 = tempEntity.newTag(fromOrg.getId(), "tag2");
    tempEntity.newPolicyTag(policy2.getId(), tag2.getId());

    Tag tag3 = tempEntity.newTag(fromOrg.getId(), "tag3");
    tempEntity.newPolicyTag(policy1.getId(), tag3.getId());
    tempEntity.newPolicyTag(policy2.getId(), tag3.getId());

    // Export
    PolicyExportResult policyExportResult = policyImportExport.exportOrganization(fromOrg);
    assertThat(policyExportResult).isNotNull();
    assertThat(policyExportResult.tags).hasSize(3);
    assertThat(policyExportResult.policyTags).hasSize(4);
  }

  @Test
  public void testExportOfUnreferencedApplicationCategory() {
    tempEntity.newTag(fromOrg.getId(), "tagNotReferencedByPolicy");

    // Export
    PolicyExportResult policyExportResult = policyImportExport.exportOrganization(fromOrg);
    assertThat(policyExportResult.tags).hasSize(1);
  }

  @Test
  public void testImportDeletionOfExistingOrgPolicy() {
    tempEntity.newPolicy(fromOrg);
    Label orgLabel = tempEntity.newLabel(fromOrg.getId(), fromOrg.getId(), Color.light_green);
    tempEntity.newComponentLabel(fromOrg.getId(), orgLabel.getId());

    tempEntity.newPolicy(fromApp);
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(fromApp.getId());
    tempEntity.newLicenseThreatGroupLicense(fromApp.getId(), licenseThreatGroup.getId());
    Label appLabel = tempEntity.newLabel(fromApp.getId(), Color.light_green);
    tempEntity.newComponentLabel(fromApp.getId(), appLabel.getId());

    // import an empty PolicyExportResult to the org
    policyImportExport.importOrganization(fromOrg, new PolicyExportResult());

    // verify that we delete all data from the org
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(fromOrg.getId())).isEmpty();
    assertThat(licenseThreatGroupDAO.getByOwnerId(fromOrg.getId())).isEmpty();
    assertThat(policyDAO.getByOwnerId(fromOrg.getId())).isEmpty();

    // verify that org label data is preserved.
    assertThat(labelDAO.getByOwnerId(fromOrg.getId())).hasSize(1);
    assertThat(componentLabelDAO.getByOwnerId(fromOrg.getId())).hasSize(1);

    // verify that we delete all data from the app
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(fromApp.getId())).isEmpty();
    assertThat(licenseThreatGroupDAO.getByOwnerId(fromApp.getId())).isEmpty();
    assertThat(policyDAO.getByOwnerId(fromApp.getId())).isEmpty();

    // verify that the app label data is preserved.
    assertThat(componentLabelDAO.getByOwnerId(fromApp.getId())).hasSize(1);
    assertThat(labelDAO.getByOwnerId(fromApp.getId())).hasSize(1);
  }

  @Test
  public void testImportToOrg() throws Exception {
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroupDAO.getByOwnerId(fromOrg.getId())) {
      licenseThreatGroupDAO.delete(licenseThreatGroup);
    }
    Policy fromOrgPolicy = tempEntity.newPolicy(fromOrg);
    LicenseThreatGroup fromOrgLtg = tempEntity.newLicenseThreatGroup(fromOrg.getId());
    tempEntity.newLicenseThreatGroupLicense(fromOrg.getId(), fromOrgLtg.getId());
    Label fromOrgLabel = tempEntity.newLabel(fromOrg.getId(), fromOrg.getId(), Color.light_green);
    tempEntity.newComponentLabel(fromOrg.getId(), fromOrgLabel.getId());
    Tag fromOrgTag = tempEntity.newTag(fromOrg.getId());
    tempEntity.newPolicyTag(fromOrgPolicy.getId(), fromOrgTag.getId());

    tempEntity.newPolicy(fromApp.getId(), "App Policy");
    LicenseThreatGroup fromAppLtg = tempEntity.newLicenseThreatGroup(fromApp.getId());
    tempEntity.newLicenseThreatGroupLicense(fromApp.getId(), fromAppLtg.getId());
    Label fromAppLabel = tempEntity.newLabel(fromApp.getId(), Color.light_green);
    tempEntity.newComponentLabel(fromApp.getId(), fromAppLabel.getId());

    PolicyExportResult policyExportResult = policyImportExport.exportOrganization(fromOrg);
    policyExportResult = detachObjects(policyExportResult);
    deleteFromOrg();

    Organization toOrg = tempEntity.newOrganization("To Org");
    Policy toOrgPolicy = tempEntity.newPolicy(toOrg);
    LicenseThreatGroup toOrgLtg = tempEntity.newLicenseThreatGroup(toOrg.getId());
    LicenseThreatGroupLicense toOrgLtgl = tempEntity.newLicenseThreatGroupLicense(toOrg.getId(), toOrgLtg.getId());
    Label toOrgLabel = tempEntity.newLabel(toOrg.getId(), toOrg.getId(), Color.light_green);
    tempEntity.newComponentLabel(toOrg.getId(), toOrgLabel.getId());

    Application toApp = tempEntity.newApplication(toOrg.getId());
    tempEntity.newPolicy(toApp);
    LicenseThreatGroup toAppLtg = tempEntity.newLicenseThreatGroup(toApp.getId());
    tempEntity.newLicenseThreatGroupLicense(toApp.getId(), toAppLtg.getId());
    Label toAppLabel = tempEntity.newLabel(toApp.getId(), Color.light_green);
    tempEntity.newComponentLabel(toApp.getId(), toAppLabel.getId());

    policyImportExport.importOrganization(toOrg, policyExportResult);

    // verify that org data is as expected
    List<LicenseThreatGroupLicense> ltgls = licenseThreatGroupLicenseDAO.getByOwnerId(toOrg.getId());
    assertThat(ltgls).hasSize(1);
    assertThat(ltgls.get(0).getId()).isNotEqualTo(toOrgLtgl.getId());
    List<LicenseThreatGroup> ltgs = licenseThreatGroupDAO.getByOwnerId(toOrg.getId());
    assertThat(ltgs).hasSize(1);
    assertThat(ltgs.get(0).getId()).isNotEqualTo(toOrgLtg.getId());
    assertThat(ltgs.get(0).getName()).isEqualTo(fromOrgLtg.getName());
    List<Policy> policies = policyDAO.getByOwnerId(toOrg.getId());
    assertThat(policies).hasSize(1);
    assertThat(policies.get(0).getId()).isNotEqualTo(toOrgPolicy.getId());
    assertThat(policies.get(0).getName()).isEqualTo(fromOrgPolicy.getName());
    List<Label> labels = labelDAO.getByOwnerId(toOrg.getId());
    assertThat(labels).hasSize(2);
    Label newLabel = findLabel(labels, fromOrgLabel.getLabel());
    assertThat(newLabel.getId()).isNotEqualTo(toOrgLabel.getId());
    assertThat(newLabel.getColor()).isEqualTo(fromOrgLabel.getColor());
    // preserved by import of labels
    assertThat(componentLabelDAO.getByOwnerId(toOrg.getId())).hasSize(1);
    List<Tag> tags = tagDAO.getByOrganizationId(toOrg.getId());
    assertThat(tags).hasSize(1);
    assertThat(tags.get(0).getId()).isNotEqualTo(fromOrgTag.getId());
    assertThat(tags.get(0).getName()).isEqualTo(fromOrgTag.getName());
    assertThat(tags.get(0).getDescription()).isEqualTo(fromOrgTag.getDescription());
    assertThat(tags.get(0).getColor()).isEqualTo(fromOrgTag.getColor());
    List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(policies.get(0).getId());
    assertThat(policyTags).hasSize(1);
    assertThat(policyTags.get(0).getTagId()).isEqualTo(tags.get(0).getId());

    // verify that we delete all data from the app
    assertThat(licenseThreatGroupDAO.getByOwnerId(toApp.getId())).isEmpty();
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(toApp.getId())).isEmpty();
    assertThat(policyDAO.getByOwnerId(toApp.getId())).isEmpty();

    // verify that app label data was preserved during import.
    assertThat(componentLabelDAO.getByOwnerId(toApp.getId())).hasSize(1);
    assertThat(labelDAO.getByOwnerId(toApp.getId())).hasSize(1);
  }

  @Test
  public void testImportToOrgWithConflictingInheritedLicenseThreatGroups() {
    Organization toOrg = tempEntity.newOrganization("To Org");
    String parentId = toOrg.getParentOrganizationId();
    LicenseThreatGroup parentLTG = tempEntity.newLicenseThreatGroup(parentId, "DummyLTG", 10, "Apache-2.0");

    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.licenseThreatGroups = new ArrayList<>();
    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup();
    licenseThreatGroup.setId("ltg-id");
    licenseThreatGroup.setName(parentLTG.getName());
    licenseThreatGroup.setOwnerId(toOrg.getId());
    licenseThreatGroup.setThreatLevel(5);
    policyExportResult.licenseThreatGroups.add(licenseThreatGroup);

    policyExportResult.licenseThreatGroupLicenses = new ArrayList<>();
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setId("ltglId");
    licenseThreatGroupLicense.setLicenseId("Apache-2.0");
    licenseThreatGroupLicense.setLicenseThreatGroupId(licenseThreatGroup.getId());
    licenseThreatGroupLicense.setOwnerId(toOrg.getId());
    policyExportResult.licenseThreatGroupLicenses.add(licenseThreatGroupLicense);

    policyExportResult.policies = new ArrayList<>();
    Policy policy = new Policy();
    policy.setId("policyId");
    policy.setName("DummyPolicy");
    policy.setOwnerId(toOrg.getId());
    policy.setThreatLevel(5);
    List<Constraint> constraints = new ArrayList<>();
    Constraint constraint = new Constraint();
    constraint.setId("constraintId");
    constraint.setName("DummyConstraint");
    List<Condition> conditions = new ArrayList<>();
    Condition condition = new Condition("License Threat Group", "is", licenseThreatGroup.getId());
    conditions.add(condition);
    constraint.setConditions(conditions);
    constraints.add(constraint);
    policy.setConstraints(constraints);
    policyExportResult.policies.add(policy);

    PolicyImportResult policyImportResult = policyImportExport.importOrganization(toOrg, policyExportResult);
    assertThat(policyImportResult.ownerName).isEqualTo(toOrg.getName());

    // must be null as the value in the parent should be used
    assertThat(licenseThreatGroupDAO.getByOwnerIdAndName(toOrg.getId(), parentLTG.getName())).isNull();
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(toOrg.getId())).isEmpty();

    List<Policy> policies = policyDAO.getByOwnerId(toOrg.getId());
    assertThat(policies).hasSize(1);
    Policy retrievedPolicy = policies.get(0);
    assertThat(retrievedPolicy.getName()).isEqualTo("DummyPolicy");
    List<Constraint> retrievedConstraints = retrievedPolicy.getConstraints();
    assertThat(retrievedConstraints).hasSize(1);
    Constraint retrievedConstraint = retrievedConstraints.get(0);
    assertThat(retrievedConstraint.getName()).isEqualTo("DummyConstraint");
    List<Condition> retrievedConditions = retrievedConstraint.getConditions();
    assertThat(retrievedConditions).hasSize(1);
    Condition retrievedCondition = retrievedConditions.get(0);
    assertThat(retrievedCondition.getConditionTypeId()).isEqualTo("License Threat Group");
    assertThat(retrievedCondition.getOperator()).isEqualTo("is");
    // must use the parent ltg
    assertThat(retrievedCondition.getValue()).isEqualTo(parentLTG.getId());
  }

  private Label findLabel(List<Label> labels, String name) {
    for (Label label : labels) {
      if (name.equals(label.getLabel())) {
        return label;
      }
    }
    return null;
  }

  @Test
  public void testImport_EmptyLicenseThreatGroup() {
    Organization toOrg = tempEntity.newOrganization("To Org");
    PolicyExportResult policyExportResult = new PolicyExportResult();
    LicenseThreatGroup emptyLTG = new LicenseThreatGroup(null, "Test LTG", 3);
    policyExportResult.licenseThreatGroups = Collections.singletonList(emptyLTG);

    policyImportExport.importOrganization(toOrg, policyExportResult);
    assertThat(licenseThreatGroupDAO.getByOwnerIdAndName(toOrg.getId(), emptyLTG.getName())).isNotNull();
  }

  // ==================== exportWithInheritance Tests ====================

  @Test
  public void testExportWithInheritance_SingleLevel() {
    // Given: Organization with policies only at its level
    Policy policy = tempEntity.newPolicy(fromOrg);

    // When: Exporting with inheritance (single level)
    PolicyExportResult result = policyImportExport.exportWithInheritance(
        OwnerType.ORGANIZATION, fromOrg.getId());

    // Then: Returns direct policies only
    assertThat(result.policies).hasSize(1);
    assertThat(result.policies.get(0).getId()).isEqualTo(policy.getId());
  }

  @Test
  public void testExportWithInheritance_TwoLevels_ApplicationToOrg() {
    // Given: Organization with Policy A, Application with Policy B
    Policy orgPolicy = tempEntity.newPolicy(fromOrg);
    Policy appPolicy = tempEntity.newPolicy(fromApp);

    // When: Exporting application with inheritance
    PolicyExportResult result = policyImportExport.exportWithInheritance(
        OwnerType.APPLICATION, fromApp.getId());

    // Then: Returns both application and organization policies
    assertThat(result.policies).hasSize(2);
    List<String> policyIds = result.policies.stream().map(Policy::getId).collect(Collectors.toList());
    assertThat(policyIds).containsExactlyInAnyOrder(orgPolicy.getId(), appPolicy.getId());

    // And: Policies preserve their original ownerId
    Policy resultAppPolicy =
        result.policies.stream().filter(p -> p.getId().equals(appPolicy.getId())).findFirst().orElse(null);
    Policy resultOrgPolicy =
        result.policies.stream().filter(p -> p.getId().equals(orgPolicy.getId())).findFirst().orElse(null);
    assertThat(resultAppPolicy.getOwnerId()).isEqualTo(fromApp.getId());
    assertThat(resultOrgPolicy.getOwnerId()).isEqualTo(fromOrg.getId());
  }

  @Test
  public void testExportWithInheritance_LabelsFromMultipleOwners() {
    // Given: Organization with Label A, Application with Label B
    Label orgLabel = tempEntity.newLabel(fromOrg.getId(), "org-label");
    Label appLabel = tempEntity.newLabel(fromApp.getId(), "app-label");

    // When: Exporting application with inheritance
    PolicyExportResult result = policyImportExport.exportWithInheritance(
        OwnerType.APPLICATION, fromApp.getId());

    // Then: Returns both labels
    assertThat(result.labels).hasSize(2);
    List<String> labelIds = result.labels.stream().map(Label::getId).collect(Collectors.toList());
    assertThat(labelIds).containsExactlyInAnyOrder(orgLabel.getId(), appLabel.getId());
  }

  @Test
  public void testExportWithInheritance_TagsOnlyFromOrganizations() {
    // Given: Organization with tags and policy-tag associations
    Policy orgPolicy = tempEntity.newPolicy(fromOrg);
    Tag tag = tempEntity.newTag(fromOrg.getId(), "tag1");
    PolicyTag policyTag = tempEntity.newPolicyTag(orgPolicy.getId(), tag.getId());

    // When: Exporting application with inheritance
    PolicyExportResult result = policyImportExport.exportWithInheritance(
        OwnerType.APPLICATION, fromApp.getId());

    // Then: Tags are included (from parent org)
    assertThat(result.tags).hasSize(1);
    assertThat(result.tags.get(0).getId()).isEqualTo(tag.getId());
    assertThat(result.policyTags).hasSize(1);
    assertThat(result.policyTags.get(0).getId()).isEqualTo(policyTag.getId());
  }

  @Test
  public void testExportWithInheritance_MultiplePolicies() {
    // Given: Multiple unique policies at the org level
    Policy policy1 = tempEntity.newPolicy(fromOrg);
    Policy policy2 = tempEntity.newPolicy(fromOrg);

    // When: Exporting with inheritance
    PolicyExportResult result = policyImportExport.exportWithInheritance(
        OwnerType.ORGANIZATION, fromOrg.getId());

    // Then: Both policies are included (ROOT org policies may also be present)
    List<String> policyIds = result.policies.stream()
        .map(Policy::getId)
        .collect(Collectors.toList());
    assertThat(policyIds).contains(policy1.getId(), policy2.getId());
  }

  @Test
  public void testExportWithInheritance_EmptyResult() {
    // Given: Organization with no policies, labels, etc.
    Organization emptyOrg = tempEntity.newOrganization();

    // When: Exporting with inheritance
    PolicyExportResult result = policyImportExport.exportWithInheritance(
        OwnerType.ORGANIZATION, emptyOrg.getId());

    // Then: Returns empty lists for policies, labels, tags (but may have inherited LTGs from root)
    assertThat(result.policies).isEmpty();
    assertThat(result.labels).isEmpty();
    assertThat(result.tags).isEmpty();
    assertThat(result.policyTags).isEmpty();
  }

  @Test
  public void testExportWithInheritance_OrganizationNotFound() {
    // Given: A non-existent organization ID
    String nonExistentId = "non-existent-org-id-" + System.currentTimeMillis();

    // When/Then: NotFoundException is thrown
    assertThatThrownBy(() -> policyImportExport.exportWithInheritance(
        OwnerType.ORGANIZATION, nonExistentId))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("organization not found");
  }

  @Test
  public void testExportWithInheritance_ApplicationNotFound() {
    // Given: A non-existent application ID
    String nonExistentId = "non-existent-app-id-" + System.currentTimeMillis();

    // When/Then: NotFoundException is thrown
    assertThatThrownBy(() -> policyImportExport.exportWithInheritance(
        OwnerType.APPLICATION, nonExistentId))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("application not found");
  }

  @Test
  public void testExportWithInheritance_Repository() {
    // Given: Repository hierarchy - Repository -> RepositoryManager -> RepositoryContainer -> ROOT Org
    // This test verifies the repository hierarchy chain is correctly walked
    Repository repository = tempEntity.newRepository();
    Policy repoPolicy = tempEntity.newPolicy(repository);

    // When: Exporting with inheritance from repository
    PolicyExportResult result = policyImportExport.exportWithInheritance(
        OwnerType.REPOSITORY, repository.getId());

    // Then: Policies from repository and its hierarchy are returned
    // Verify the repository's own policy is included
    List<String> resultPolicyIds = result.policies.stream()
        .map(Policy::getId)
        .collect(Collectors.toList());
    assertThat(resultPolicyIds).contains(repoPolicy.getId());
    // Note: ROOT org policies may also be included due to hierarchy inheritance
  }
}

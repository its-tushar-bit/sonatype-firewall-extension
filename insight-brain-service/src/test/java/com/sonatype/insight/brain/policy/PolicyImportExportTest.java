/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.UriInfo;

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
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 1.7
 */
public class PolicyImportExportTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Mock
  private UriInfo uriInfo;

  private PolicyImportExport policyImportExport;

  private Organization fromOrg;

  private Application fromApp;

  private InsightConfig insightConfig;

  private PolicyDAO policyDAO = new PolicyDAO();
  private LabelDAO labelDAO = new LabelDAO();
  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();
  private TagDAO tagDAO = new TagDAO();
  private PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    insightConfig = new InsightConfig();
    insightConfig.setBaseUrl("base");
    insightConfig.setSonatypeWork(temporaryFolder.getRoot().getAbsolutePath());
    policyImportExport = new PolicyImportExport(new BaseUrl(insightConfig, uriInfo));
    fromOrg = tempEntity.newOrganization();
    fromApp = tempEntity.newApplication(fromOrg.getId());
    when(uriInfo.getRequestUri()).thenReturn(URI.create("whatever"));
  }

  private void deleteFromOrg() {
    new ApplicationDAO().delete(fromApp);
    new OrganizationDAO().delete(fromOrg);
  }

  @Test
  public void testImportAndMergeLabelsForOrg() throws Exception {
    List<Label> orgLabels = createLabels(fromOrg.getId());
    createPolicy(fromOrg.getId(), orgLabels.get(0).getId(), "Org Policy");
    PolicyExportResult exportDTO = policyImportExport.exportOrganization(fromOrg);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();

    Organization toOrg = tempEntity.newOrganization();

    Label oldLabelToUpdate = new Label(toOrg.getId(), orgLabels.get(0).getLabel().toLowerCase(), Color.white);
    oldLabelToUpdate.setDescription("anything");
    labelDAO.insert(oldLabelToUpdate);

    Label oldLabelToKeep = new Label(toOrg.getId(), "keepMe", Color.red);
    labelDAO.insert(oldLabelToKeep);
    
    List<Label> oldLabels = Lists.newArrayList(oldLabelToUpdate, oldLabelToKeep);

    try (TransactionContext tx = labelDAO.createTransactionContext()) {
      tx.begin();
      policyImportExport.importAndMergeLabels(tx, exportDTO, oldLabels, null, toOrg.getId());
      tx.commit();
    }

    List<Label> labels = labelDAO.getByOwnerId(toOrg.getId());
    assertThat(labels, hasSize(3));
    
    Label keptLabel = labels.get(0);
    assertThat(keptLabel.getColor(), is(Color.red));
    assertThat(keptLabel.getLabel(), is("keepMe"));
    
    Label updatedLabel = labels.get(1);
    assertThat(updatedLabel.getColor(), is(Color.black)); // updated
    assertThat(updatedLabel.getLabel(), is("LABEL1")); // updated from the lowercase version
    assertThat(updatedLabel.getId(), is(oldLabelToUpdate.getId())); // id remains the same
    assertThat(updatedLabel.getDescription(), nullValue()); // existing description is removed

    Label importedLabel = labels.get(2);
    assertThat(importedLabel.getColor(), is(Color.blue));
    assertThat(importedLabel.getLabel(), is("LABEL2"));

    assertThat(exportDTO.policies.get(0).getConstraints().get(0).getConditions().get(0).getValue(),
        is(oldLabelToUpdate.getId()));
  }

  @Test
  public void testImportAndMergeLabelsForApp() throws Exception {
    List<Label> appLabels = createLabels(fromApp.getId());
    Label fromOrgLabel = tempEntity.newLabel(fromOrg.getId(), "orgLabel", Color.black);

    createPolicy(fromApp.getId(), appLabels.get(0).getId(), "App Policy 1");
    createPolicy(fromApp.getId(), fromOrgLabel.getId(), "App Policy 2");
    PolicyExportResult exportDTO = policyImportExport.exportApplication(fromApp);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();

    Organization toOrg = tempEntity.newOrganization();
    Application toApp = tempEntity.newApplication(toOrg.getId());

    tempEntity.newLabel(toOrg.getId(), "orgLabel", Color.black);

    Label oldLabelToUpdate = new Label(toApp.getId(), appLabels.get(0).getLabel().toLowerCase(), Color.white);
    oldLabelToUpdate.setDescription("anything");
    labelDAO.insert(oldLabelToUpdate);

    Label oldLabelToKeep = tempEntity.newLabel(toApp.getId(), "keepMe", Color.red);
    
    List<Label> oldLabels = Lists.newArrayList(oldLabelToUpdate, oldLabelToKeep);

    try (TransactionContext tx = labelDAO.createTransactionContext()) {
      tx.begin();
      policyImportExport.importAndMergeLabels(tx, exportDTO, oldLabels, toApp.getId(), toOrg.getId());
      tx.commit();
    }

    List<Label> labels = labelDAO.getByOwnerId(toApp.getId());
    assertThat(labels, hasSize(3));
    
    Label keptLabel = labels.get(0);
    assertThat(keptLabel.getColor(), is(Color.red));
    assertThat(keptLabel.getLabel(), is("keepMe"));
    
    Label updatedLabel = labels.get(1);
    assertThat(updatedLabel.getColor(), is(Color.black)); // updated
    assertThat(updatedLabel.getLabel(), is("LABEL1")); // updated from the lowercase version
    assertThat(updatedLabel.getId(), is(oldLabelToUpdate.getId())); // id remains the same
    assertThat(updatedLabel.getDescription(), nullValue()); // existing description is removed

    Label importedLabel = labels.get(2);
    assertThat(importedLabel.getColor(), is(Color.blue));
    assertThat(importedLabel.getLabel(), is("LABEL2"));

    assertThat(exportDTO.policies.get(0).getConstraints().get(0).getConditions().get(0).getValue(),
        is(oldLabelToUpdate.getId()));
    assertThat(exportDTO.policies.get(1).getConstraints().get(0).getConditions().get(0).getValue(), is(nullValue()));
  }

  @Test
  public void testDeletionOfPolicyWaiversFromApp(){
    Organization toOrg = tempEntity.newOrganization();
    Application toApp = tempEntity.newApplication(toOrg.getId());
    
    Policy appPolicy = tempEntity.newPolicy(toApp.getId(), "Policy Name");
    tempEntity.newWaiver("hash", appPolicy.getId(), toApp.getId());

    //only interested in the deletion so import an empty DTO
    policyImportExport.importApplication(toApp, emptyExportDTO());

    verify(uriInfo).getRequestUri();
    assertThat(new PolicyWaiverDAO().getByOwnerId(toApp.getId()), is(empty()));
  }

  @Test
  public void testDeletionOfPolicyWaiversFromOrg(){
    Organization toOrg = tempEntity.newOrganization();
    Application toApp = tempEntity.newApplication(toOrg.getId());
    Policy orgPolicy = tempEntity.newPolicy(toOrg.getId(), "Org Policy Name");
    Policy appPolicy = tempEntity.newPolicy(toApp.getId(), "App Policy Name");
    tempEntity.newWaiver("hash", orgPolicy.getId(), toOrg.getId());
    tempEntity.newWaiver("hash", appPolicy.getId(), toApp.getId());
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();

    //only interested in the deletion so import an empty DTO
    policyImportExport.importOrganization(toOrg, emptyExportDTO());

    verify(uriInfo).getRequestUri();
    assertThat(policyWaiverDAO.getByOwnerId(toOrg.getId()), is(empty()));
    assertThat(policyWaiverDAO.getByOwnerId(toApp.getId()), is(empty()));
  }

  @Test
  public void testImportAndMergeTags_UpdateTag() throws Exception {
    Tag fromTag = tempEntity.newTag(fromOrg.getId(), "tagname", Color.black);
    Policy policy = tempEntity.newPolicy(fromOrg.getId(), "Policy Name");
    tempEntity.newPolicyTag(policy.getId(), fromTag.getId());

    PolicyExportResult exportDTO = policyImportExport.exportOrganization(fromOrg);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();

    Organization toOrg = tempEntity.newOrganization();
    Tag toTag = tempEntity.newTag(toOrg.getId(), "TAG NAME", Color.yellow);

    try (TransactionContext tx = tagDAO.createTransactionContext()) {
      tx.begin();
      policyImportExport.importAndMergeTags(tx, exportDTO, toOrg.getId());
      tx.commit();
    }

    assertTag(fromTag, tagDAO.getById(toTag.getId()));
    assertThat(exportDTO.policyTags.get(0).getTagId(), is(toTag.getId()));
  }

  @Test
  public void testImportAndMergeTags_NewTag() throws Exception {
    Tag tag = tempEntity.newTag(fromOrg.getId(), "Tag Name", Color.black);
    Policy policy = tempEntity.newPolicy(fromOrg.getId(), "Policy Name");
    tempEntity.newPolicyTag(policy.getId(), tag.getId());

    PolicyExportResult exportDTO = policyImportExport.exportOrganization(fromOrg);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();

    Organization toOrg = tempEntity.newOrganization();

    try (TransactionContext tx = tagDAO.createTransactionContext()) {
      tx.begin();
      policyImportExport.importAndMergeTags(tx, exportDTO, toOrg.getId());
      tx.commit();
    }

    List<Tag> tags = tagDAO.getByOrganizationId(toOrg.getId());
    assertThat(tags, hasSize(1));
    assertTag(tag, tags.get(0));
    assertThat(tags.get(0).getId(), is(not(tag.getId())));
    assertThat(exportDTO.policyTags.get(0).getTagId(), is(tags.get(0).getId()));
  }

  @Test
  public void testImportPolicyTagsToApplication() throws IOException {
    // Remove the license threat groups because we don't want errors about them.
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroupDAO.getByOwnerId(fromOrg.getId())) {
      licenseThreatGroupDAO.delete(licenseThreatGroup);
    }

    Tag tag = tempEntity.newTag(fromOrg.getId(), "tagName");
    Policy policy = tempEntity.newPolicy(fromOrg.getId(), "Policy Name");
    tempEntity.newPolicyTag(policy.getId(), tag.getId());
    PolicyExportResult policyExportResult = policyImportExport.exportOrganization(fromOrg);
    policyExportResult = detachObjects(policyExportResult);

    try {
      policyImportExport.importApplication(fromApp, policyExportResult);
      fail("Import should have thrown an exception due to tag data");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Importing policies with applied tags to an application is not supported."));
    }
  }

  @Test
  public void testImport_LicenseThreatGroupUnassignedCondition() throws Exception {
    Policy policy = new Policy("testPolicyId", "testPolicyName");
    policy.setOwnerId(fromOrg.getId());
    Constraint constraint = new Constraint("testConstraintId", "testConstraintName", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is",
        LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);

    PolicyExportResult exportDTO = policyImportExport.exportOrganization(fromOrg);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();

    Organization toOrg = tempEntity.newOrganization();

    policyImportExport.importOrganization(toOrg, exportDTO);

    List<Policy> importedPolicies = policyDAO.getByOwnerId(toOrg.getId());
    assertThat(importedPolicies, hasSize(1));
    policy = importedPolicies.get(0);
    List<Constraint> constraints = policy.getConstraints();
    assertThat(constraints, hasSize(1));
    List<Condition> conditions = constraints.get(0).getConditions();
    assertThat(conditions, hasSize(1));
    Condition condition = conditions.get(0);
    assertThat(condition.getConditionTypeId(), is(LicenseThreatGroupConditionType.ID));
    assertThat(condition.getOperator(), is("is"));
    assertThat(condition.getValue(), is(LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID));
  }

  /**
   * Serialize the PolicyExportResult through json to ensure that all objects are detached from the db and that they are
   * created fresh as they are when imported from json.
   */
  private PolicyExportResult detachObjects(PolicyExportResult policyExportResult) throws IOException {
    String s = JsonUtils.format(policyExportResult);
    return JsonUtils.parse(s, PolicyExportResult.class);
  }

  private PolicyExportResult emptyExportDTO() {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.licenseThreatGroups = Collections.emptyList();
    policyExportResult.licenseThreatGroupLicenses = Collections.emptyList();
    policyExportResult.labels = Collections.emptyList();
    policyExportResult.policies = Collections.emptyList();
    policyExportResult.tags = Collections.emptyList();
    policyExportResult.policyTags = Collections.emptyList();
    return policyExportResult;
  }

  private List<Policy> createPolicy(String ownerId, String labelId, String policyName) {
    Policy policy = new Policy(null, policyName);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, tempEntity.uuid(), LogicalOperator.AND);
    constraint.addCondition(new Condition(LabelConditionType.ID, "is", labelId));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);
    return Lists.newArrayList(policy);
  }

  private List<Label> createLabels(String ownerId) {
    Label label1 = tempEntity.newLabel(ownerId, "LABEL1", Color.black);
    Label label2 = tempEntity.newLabel(ownerId, "LABEL2", Color.blue);
    return Lists.newArrayList(label1, label2);
  }

  public static void assertTag(Tag expected, Tag actual) {
    assertThat(actual.getName(), is(expected.getName()));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(expected.getNameLowercaseNoWhitespace()));
    assertThat(actual.getDescription(), is(expected.getDescription()));
    assertThat(actual.getColor(), is(expected.getColor()));
  }

  @Test
  public void testExportImport_Update() throws Exception {
    String appId = fromApp.getId();

    Label label1 = tempEntity.newLabel(appId, "label1", Color.blue);
    Label label2 = tempEntity.newLabel(appId, "label2", Color.red);
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(appId);
    LicenseThreatGroupLicense licenseThreatGroupLicense = tempEntity.newLicenseThreatGroupLicense(appId,
        licenseThreatGroup.getId());

    Policy policy = new Policy();
    policy.setOwnerId(appId);
    policy.setName("Policy1");
    Constraint constraint1 = new Constraint();
    constraint1.setName("Constraint1");
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label1.getId()));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint();
    constraint2.setName("Constraint2");
    constraint2.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", licenseThreatGroup.getId()));
    policy.addConstraint(constraint2);
    policy.addAction(BuildStageType.ID, new Action(Action.ID_FAIL));
    policyDAO.insert(policy);

    // Export
    PolicyExportResult policyExportResult = policyImportExport.exportApplication(fromApp);
    assertNotNull(policyExportResult);
    assertTrue(!policyExportResult.policies.isEmpty());
    assertTrue(!policyExportResult.labels.isEmpty());
    assertTrue(!policyExportResult.licenseThreatGroups.isEmpty());
    assertTrue(!policyExportResult.licenseThreatGroupLicenses.isEmpty());

    // Delete and re-create one label - it should be reset by import (matched by label case insensitive)
    labelDAO.delete(label1);
    label1 = tempEntity.newLabel(appId, label1.getLabel().toUpperCase(Locale.ENGLISH), Color.black);
    // Delete one label - it should be re-created by the import.
    labelDAO.delete(label2);
    // Add a new label - it should be retained through the import.
    tempEntity.newLabel(appId, "label3", Color.red);

    // Import
    policyExportResult.tags = Collections.emptyList();
    policyExportResult.policyTags = Collections.emptyList();
    policyExportResult = detachObjects(policyExportResult);
    PolicyImportResult policyImportResult = policyImportExport.importApplication(fromApp, policyExportResult);
    assertNotNull(policyImportResult);
    Assert.assertEquals(fromApp.getName(), policyImportResult.ownerName);
    assertThat(policyImportResult.url, endsWith("index.html#/management/application/" + fromApp.getPublicId()));
    List<Label> labels = labelDAO.getByOwnerId(appId);
    // All labels retained.
    Assert.assertEquals(3, labels.size());
    Assert.assertEquals(label1.getId(), labels.get(0).getId());
    Assert.assertEquals("label1", labels.get(0).getLabel());
    Assert.assertEquals(Color.blue, labels.get(0).getColor());
    Assert.assertNotEquals(label2.getId(), labels.get(1).getId());
    Assert.assertEquals(label2.getLabel(), labels.get(1).getLabel());
    Assert.assertEquals(label2.getColor(), labels.get(1).getColor());
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(appId);
    Assert.assertEquals(1, licenseThreatGroups.size());
    Assert.assertEquals(licenseThreatGroup.getName(), licenseThreatGroups.get(0).getName());
    Assert.assertNotEquals(licenseThreatGroup.getId(), licenseThreatGroups.get(0).getId());
    List<LicenseThreatGroupLicense> licenseThreatGroupLicenses = licenseThreatGroupLicenseDAO.getByOwnerId(appId);
    Assert.assertEquals(1, licenseThreatGroupLicenses.size());
    Assert.assertEquals(licenseThreatGroupLicense.getLicenseId(), licenseThreatGroupLicenses.get(0).getLicenseId());
    Assert.assertNotEquals(licenseThreatGroupLicense.getId(), licenseThreatGroupLicenses.get(0).getId());
    List<Policy> policies = policyDAO.getByOwnerId(appId);
    Assert.assertEquals(1, policies.size());
    Assert.assertEquals(policy.getName(), policies.get(0).getName());
    Assert.assertNotEquals(policy.getId(), policies.get(0).getId());
    ValidationResult policyValidationResult = policies.get(0).validate(appId);
    assertTrue(policyValidationResult.toMessageString(), policyValidationResult.isValid());
  }

  @Test
  public void testExportOfTags() throws Exception {
    Policy policy1 = tempEntity.newPolicy(fromOrg.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(fromOrg.getId(), "policy2");

    Tag tag1 = tempEntity.newTag(fromOrg.getId(), "tag1");
    tempEntity.newPolicyTag(policy1.getId(), tag1.getId());

    Tag tag2 = tempEntity.newTag(fromOrg.getId(), "tag2");
    tempEntity.newPolicyTag(policy2.getId(), tag2.getId());

    Tag tag3 = tempEntity.newTag(fromOrg.getId(), "tag3");
    tempEntity.newPolicyTag(policy1.getId(), tag3.getId());
    tempEntity.newPolicyTag(policy2.getId(), tag3.getId());

    // Export
    PolicyExportResult policyExportResult = policyImportExport.exportOrganization(fromOrg);
    assertThat(policyExportResult, notNullValue());
    assertThat(policyExportResult.tags, notNullValue());
    assertThat(policyExportResult.tags, hasSize(3));
    assertThat(policyExportResult.policyTags, notNullValue());
    assertThat(policyExportResult.policyTags, hasSize(4));
  }

  @Test
  public void testImportDeletionOfExistingAppPolicy() throws Exception {
    tempEntity.newPolicy(fromOrg.getId(), "Org Policy");
    Label orgLabel = tempEntity.newLabel(fromOrg.getId(), fromOrg.getId(), Color.white);
    tempEntity.newComponentLabel(fromOrg.getId(), orgLabel.getId());

    tempEntity.newPolicy(fromApp.getId(), "App Policy");
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(fromApp.getId());
    tempEntity.newLicenseThreatGroupLicense(fromApp.getId(), licenseThreatGroup.getId());
    Label appLabel = tempEntity.newLabel(fromApp.getId(), Color.white);
    tempEntity.newComponentLabel(fromApp.getId(), appLabel.getId());

    // import a policy with no data to the app
    policyImportExport.importApplication(fromApp, emptyExportDTO());

    // verify that org data is untouched
    // 127 at time of writing, should only break if we remove many
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(fromOrg.getId()).size(), is(greaterThan(100)));
    assertThat(licenseThreatGroupDAO.getByOwnerId(fromOrg.getId()), hasSize(4));
    assertThat(policyDAO.getByOwnerId(fromOrg.getId()), hasSize(1));
    assertThat(labelDAO.getByOwnerId(fromOrg.getId()), hasSize(1));
    assertThat(componentLabelDAO.getByOwnerId(fromOrg.getId()), hasSize(1));

    // verify that we delete all data from the app
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(fromApp.getId()), is(empty()));
    assertThat(licenseThreatGroupDAO.getByOwnerId(fromApp.getId()), is(empty()));
    assertThat(policyDAO.getByOwnerId(fromApp.getId()), is(empty()));

    // Verify that app label data is retained.
    assertThat(componentLabelDAO.getByOwnerId(fromApp.getId()), hasSize(1));
    assertThat(labelDAO.getByOwnerId(fromApp.getId()), hasSize(1));
  }

  @Test
  public void testImportDeletionOfExistingOrgPolicy() throws Exception {
    tempEntity.newPolicy(fromOrg.getId(), "Org Policy");
    Label orgLabel = tempEntity.newLabel(fromOrg.getId(), fromOrg.getId(), Color.white);
    tempEntity.newComponentLabel(fromOrg.getId(), orgLabel.getId());

    tempEntity.newPolicy(fromApp.getId(), "App Policy");
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(fromApp.getId());
    tempEntity.newLicenseThreatGroupLicense(fromApp.getId(), licenseThreatGroup.getId());
    Label appLabel = tempEntity.newLabel(fromApp.getId(), Color.white);
    tempEntity.newComponentLabel(fromApp.getId(), appLabel.getId());

    // import a policy with no data to the org
    policyImportExport.importOrganization(fromOrg, emptyExportDTO());

    // verify that we delete all data from the org
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(fromOrg.getId()), is(empty()));
    assertThat(licenseThreatGroupDAO.getByOwnerId(fromOrg.getId()), is(empty()));
    assertThat(policyDAO.getByOwnerId(fromOrg.getId()), is(empty()));

    // verify that org label data is preserved.
    assertThat(labelDAO.getByOwnerId(fromOrg.getId()), hasSize(1));
    assertThat(componentLabelDAO.getByOwnerId(fromOrg.getId()), hasSize(1));

    // verify that we delete all data from the app
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(fromApp.getId()), is(empty()));
    assertThat(licenseThreatGroupDAO.getByOwnerId(fromApp.getId()), is(empty()));
    assertThat(policyDAO.getByOwnerId(fromApp.getId()), is(empty()));

    // verify that the app label data is preserved.
    assertThat(componentLabelDAO.getByOwnerId(fromApp.getId()), hasSize(1));
    assertThat(labelDAO.getByOwnerId(fromApp.getId()), hasSize(1));
  }

  @Test
  public void testImportToOrg() throws Exception {
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroupDAO.getByOwnerId(fromOrg.getId())) {
      licenseThreatGroupDAO.delete(licenseThreatGroup);
    }
    Policy fromOrgPolicy = tempEntity.newPolicy(fromOrg.getId(), "Org Policy");
    LicenseThreatGroup fromOrgLtg = tempEntity.newLicenseThreatGroup(fromOrg.getId());
    tempEntity.newLicenseThreatGroupLicense(fromOrg.getId(), fromOrgLtg.getId());
    Label fromOrgLabel = tempEntity.newLabel(fromOrg.getId(), fromOrg.getId(), Color.white);
    tempEntity.newComponentLabel(fromOrg.getId(), fromOrgLabel.getId());
    Tag fromOrgTag = tempEntity.newTag(fromOrg.getId());
    tempEntity.newPolicyTag(fromOrgPolicy.getId(), fromOrgTag.getId());

    tempEntity.newPolicy(fromApp.getId(), "App Policy");
    LicenseThreatGroup fromAppLtg = tempEntity.newLicenseThreatGroup(fromApp.getId());
    tempEntity.newLicenseThreatGroupLicense(fromApp.getId(), fromAppLtg.getId());
    Label fromAppLabel = tempEntity.newLabel(fromApp.getId(), Color.white);
    tempEntity.newComponentLabel(fromApp.getId(), fromAppLabel.getId());

    PolicyExportResult policyExportResult = policyImportExport.exportOrganization(fromOrg);
    policyExportResult = detachObjects(policyExportResult);
    deleteFromOrg();
    
    Organization toOrg = tempEntity.newOrganization("To Org", false /* createLicenseThreatGroups */);
    Policy toOrgPolicy = tempEntity.newPolicy(toOrg.getId(), "Org Policy");
    LicenseThreatGroup toOrgLtg = tempEntity.newLicenseThreatGroup(toOrg.getId());
    LicenseThreatGroupLicense toOrgLtgl = tempEntity.newLicenseThreatGroupLicense(toOrg.getId(), toOrgLtg.getId());
    Label toOrgLabel = tempEntity.newLabel(toOrg.getId(), toOrg.getId(), Color.white);
    tempEntity.newComponentLabel(toOrg.getId(), toOrgLabel.getId());

    Application toApp = tempEntity.newApplication(toOrg.getId());
    tempEntity.newPolicy(toApp.getId(), "App Policy");
    LicenseThreatGroup toAppLtg = tempEntity.newLicenseThreatGroup(toApp.getId());
    tempEntity.newLicenseThreatGroupLicense(toApp.getId(), toAppLtg.getId());
    Label toAppLabel = tempEntity.newLabel(toApp.getId(), Color.white);
    tempEntity.newComponentLabel(toApp.getId(), toAppLabel.getId());

    policyImportExport.importOrganization(toOrg, policyExportResult);

    // verify that org data is as expected
    List<LicenseThreatGroupLicense> ltgls = licenseThreatGroupLicenseDAO.getByOwnerId(toOrg.getId());
    assertThat(ltgls, hasSize(1));
    assertThat(ltgls.get(0).getId(), is(not(toOrgLtgl.getId())));
    List<LicenseThreatGroup> ltgs = licenseThreatGroupDAO.getByOwnerId(toOrg.getId());
    assertThat(ltgs, hasSize(1));
    assertThat(ltgs.get(0).getId(), is(not(toOrgLtg.getId())));
    assertThat(ltgs.get(0).getName(), is(fromOrgLtg.getName()));
    List<Policy> policies = policyDAO.getByOwnerId(toOrg.getId());
    assertThat(policies, hasSize(1));
    assertThat(policies.get(0).getId(), is(not(toOrgPolicy.getId())));
    assertThat(policies.get(0).getName(), is(fromOrgPolicy.getName()));
    List<Label> labels = labelDAO.getByOwnerId(toOrg.getId());
    assertThat(labels, hasSize(2));
    Label newLabel = findLabel(labels, fromOrgLabel.getLabel());
    assertThat(newLabel.getId(), is(not(toOrgLabel.getId())));
    assertThat(newLabel.getColor(), is(fromOrgLabel.getColor()));
    // preserved by import of labels
    assertThat(componentLabelDAO.getByOwnerId(toOrg.getId()), hasSize(1));
    List<Tag> tags = tagDAO.getByOrganizationId(toOrg.getId());
    assertThat(tags, hasSize(1));
    assertThat(tags.get(0).getId(), is(not(fromOrgTag.getId())));
    assertThat(tags.get(0).getName(), is(fromOrgTag.getName()));
    assertThat(tags.get(0).getDescription(), is(fromOrgTag.getDescription()));
    assertThat(tags.get(0).getColor(), is(fromOrgTag.getColor()));
    List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(policies.get(0).getId());
    assertThat(policyTags, hasSize(1));
    assertThat(policyTags.get(0).getTagId(), is(tags.get(0).getId()));

    // verify that we delete all data from the app
    assertThat(licenseThreatGroupDAO.getByOwnerId(toApp.getId()), is(empty()));
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(toApp.getId()), is(empty()));
    assertThat(policyDAO.getByOwnerId(toApp.getId()), is(empty()));

    // verify that app label data was preserved during import.
    assertThat(componentLabelDAO.getByOwnerId(toApp.getId()), hasSize(1));
    assertThat(labelDAO.getByOwnerId(toApp.getId()), hasSize(1));
  }

  private Label findLabel(List<Label> labels, String name) {
    for (Label label : labels) {
      if (name.equals(label.getLabel())) {
        return label;
      }
    }
    return null;
  }
}

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

import javax.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
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

    LabelDAO labelDAO = new LabelDAO();
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

    LabelDAO labelDAO = new LabelDAO();
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

    TagDAO tagDAO = new TagDAO();
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

    TagDAO tagDAO = new TagDAO();
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
    LicenseThreatGroupDAO ltgDAO = new LicenseThreatGroupDAO();
    for (LicenseThreatGroup licenseThreatGroup : ltgDAO.getByOwnerId(fromOrg.getId())) {
      ltgDAO.delete(licenseThreatGroup);
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
    new PolicyDAO().insert(policy);

    PolicyExportResult exportDTO = policyImportExport.exportOrganization(fromOrg);
    exportDTO = detachObjects(exportDTO);
    deleteFromOrg();

    Organization toOrg = tempEntity.newOrganization();

    policyImportExport.importOrganization(toOrg, exportDTO);

    List<Policy> importedPolicies = new PolicyDAO().getByOwnerId(toOrg.getId());
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
    new PolicyDAO().insert(policy);
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
}

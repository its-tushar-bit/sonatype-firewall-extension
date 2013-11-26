/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @since 1.7
 */
public class PolicyImporterTest
{

  public static final String TEST_ORG = "testOrg";

  public static final String TEST_APP = "testApp";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private LabelDAO labelDAO;

  @Mock
  private EntityManager entityManager;

  @Captor
  private ArgumentCaptor<Label> newLabel;

  private PolicyImporterImpl policyImporter;

  private Organization org;

  private Application app;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.getRoot().getAbsolutePath());
    policyImporter = new PolicyImporterImpl(new InsightWork(insightConfig), new BaseUrl(insightConfig));
    policyImporter.setLabelDAO(labelDAO);
    org = new Organization(TEST_ORG);
    org.setId(TEST_ORG);
    app = new Application(TEST_APP, TEST_APP, org.getId());
    app.setId(TEST_APP);
  }

  @After
  public void after() {
    verifyNoMoreInteractions(labelDAO, entityManager);
  }

  @Test
  public void testImportAndMergeLabelsForOrg() {
    PolicyExportResult exportDTO = new PolicyExportResult();
    exportDTO.labels = createLabels(TEST_ORG);
    for (Label label : exportDTO.labels) {
      label.setId(TEST_ORG);
    }
    exportDTO.policies = createPolicies(TEST_ORG, exportDTO.labels.get(0).getId());

    Label oldLabelToUpdate = new Label("whoCares?", exportDTO.labels.get(0).getLabel().toLowerCase(), Color.white);
    oldLabelToUpdate.setDescription("anything");
    oldLabelToUpdate.setId("label1Old");

    Label oldLabelToDelete = new Label("deleteMe", "deleteMe", Color.red);
    oldLabelToDelete.setId("deleteMe");
    List<Label> oldLabels = Lists.newArrayList(oldLabelToUpdate, oldLabelToDelete);

    policyImporter.importAndMergeLabels(entityManager, exportDTO, oldLabels, null, org.getId());

    verify(labelDAO).update(entityManager, oldLabelToUpdate);
    verify(labelDAO).insert(eq(entityManager), newLabel.capture());
    verify(labelDAO).delete(entityManager, oldLabelToDelete);

    assertThat(oldLabelToUpdate.getColor(), is(Color.black));  // updated
    assertThat(oldLabelToUpdate.getLabel(), is("LABEL1"));     // updated from the lowercase version
    assertThat(oldLabelToUpdate.getId(), is("label1Old"));     // id remains the same
    assertThat(oldLabelToUpdate.getDescription(), nullValue()); // existing description is removed

    assertThat(newLabel.getValue().getOwnerId(), is(org.getId()));
    assertThat(newLabel.getValue().getLabel(), is(exportDTO.labels.get(1).getLabel()));

    assertThat(exportDTO.policies.get(0).getConstraints().get(0).getConditions().get(0).getValue(), nullValue());
  }

  @Test
  public void testImportAndMergeLabelsForApp() {
    PolicyExportResult exportDTO = new PolicyExportResult();
    exportDTO.labels = createLabels(TEST_APP);
    for (Label label : exportDTO.labels) {
      label.setId(TEST_APP);
    }

    exportDTO.policies = createPolicies(TEST_APP, exportDTO.labels.get(0).getId());
    exportDTO.policies.addAll(createPolicies(TEST_APP, "orgLabelId")); // add policy referring to an org label

    Label orgLabel = new Label("orgLabel", "orgLabel", Color.black);
    orgLabel.setId("orgLabelId");

    Label oldLabelToUpdate = new Label("whoCares?", exportDTO.labels.get(0).getLabel().toLowerCase(), Color.white);
    oldLabelToUpdate.setDescription("anything");
    oldLabelToUpdate.setId("label1Old");

    Label oldLabelToDelete = new Label("deleteMe", "deleteMe", Color.red);
    oldLabelToDelete.setId("deleteMe");
    List<Label> oldLabels = Lists.newArrayList(oldLabelToUpdate, oldLabelToDelete);

    when(labelDAO.getByOwnerId(entityManager, app.getOrganizationId())).thenReturn(Lists.newArrayList(orgLabel));

    policyImporter.importAndMergeLabels(entityManager, exportDTO, oldLabels, app.getId(), org.getId());

    verify(labelDAO).getByOwnerId(entityManager, app.getOrganizationId());
    verify(labelDAO).update(entityManager, oldLabelToUpdate);
    verify(labelDAO).insert(eq(entityManager), newLabel.capture());
    verify(labelDAO).delete(entityManager, oldLabelToDelete);

    assertThat(oldLabelToUpdate.getColor(), is(Color.black));  // updated
    assertThat(oldLabelToUpdate.getLabel(), is("LABEL1"));     // updated from the lowercase version
    assertThat(oldLabelToUpdate.getId(), is("label1Old"));     // id remains the same
    assertThat(oldLabelToUpdate.getDescription(), nullValue()); // existing description is removed

    assertThat(newLabel.getValue().getOwnerId(), is(app.getId()));
    assertThat(newLabel.getValue().getLabel(), is(exportDTO.labels.get(1).getLabel()));

    assertThat(exportDTO.policies.get(0).getConstraints().get(0).getConditions().get(0).getValue(), nullValue());
    assertThat(exportDTO.policies.get(1).getConstraints().get(0).getConditions().get(0).getValue(), is(orgLabel.getId()));
  }

  private List<Policy> createPolicies(String ownerId, String labelId) {
    Policy policy = new Policy(ownerId, ownerId);
    Constraint constraint = new Constraint(ownerId, ownerId, LogicalOperator.AND);
    constraint.addCondition(new Condition(LabelConditionType.ID, "", labelId));
    policy.addConstraint(constraint);
    return Lists.newArrayList(policy);
  }

  private List<Label> createLabels(String ownerId) {
    Label label1 = new Label(ownerId, "LABEL1", Color.black);
    label1.setId("label1");
    Label label2 = new Label(ownerId, "LABEL2", Color.blue);
    label2.setId("label2");
    return Lists.newArrayList(label1, label2);
  }
}

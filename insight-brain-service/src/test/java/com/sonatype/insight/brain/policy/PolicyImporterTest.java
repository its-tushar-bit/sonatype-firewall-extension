/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
    exportDTO.labels = createLabels();
    for (Label label : exportDTO.labels) {
      label.setId(TEST_ORG);
    }
    exportDTO.policies = createPolicies(TEST_ORG, exportDTO.labels.get(0).getId());

    Label oldLabelToUpdate = new Label("whoCares?", exportDTO.labels.get(0).getLabel().toLowerCase(), Color.white);
    oldLabelToUpdate.setDescription("anything");
    oldLabelToUpdate.setId("label1Old");
    oldLabelToUpdate.setColor(Color.white);

    Label oldLabelToDelete = new Label("deleteMe", "deleteMe", Color.red);
    oldLabelToDelete.setId("deleteMe");
    List<Label> oldLabels = new ArrayList<>(Arrays.asList(oldLabelToUpdate, oldLabelToDelete));

    //when(labelDAO.getByOwnerId(entityManager, org.getId())).thenReturn(oldLabels);
    policyImporter.importAndMergeLabels(entityManager, exportDTO, oldLabels, null, org.getId());

    ArgumentCaptor<Label> updatedLabel = ArgumentCaptor.forClass(Label.class);
    ArgumentCaptor<Label> newLabel = ArgumentCaptor.forClass(Label.class);
    ArgumentCaptor<Label> deletedLabel = ArgumentCaptor.forClass(Label.class);

    //verify(labelDAO).getByOwnerId(entityManager, org.getId());
    verify(labelDAO).update(any(EntityManager.class), updatedLabel.capture());
    verify(labelDAO).insert(any(EntityManager.class), newLabel.capture());
    verify(labelDAO).delete(any(EntityManager.class), deletedLabel.capture());

    assertThat(updatedLabel.getValue().getColor(), is(Color.black));
    assertThat(updatedLabel.getValue().getLabel(), is("LABEL1"));
    assertThat(updatedLabel.getValue().getId(), is("label1Old"));
    assertThat(updatedLabel.getValue().getDescription(), nullValue());

    assertThat(newLabel.getValue().getOwnerId(), is(org.getId()));
    assertThat(newLabel.getValue().getLabel(), is(exportDTO.labels.get(1).getLabel()));

    assertThat(deletedLabel.getValue().getId(), is(oldLabelToDelete.getId()));

    assertThat(exportDTO.policies.get(0).getConstraints().get(0).getConditions().get(0).getValue(), nullValue());
  }

  private List<Policy> createPolicies(String ownerId, String labelId) {
    Policy policy = new Policy(ownerId, ownerId);
    Constraint constraint = new Constraint(ownerId, ownerId, LogicalOperator.AND);
    constraint.addCondition(new Condition(LabelConditionType.ID, "", labelId));
    policy.addConstraint(constraint);
    return Lists.newArrayList(policy);
  }

  private List<Label> createLabels() {
    Label label1 = new Label(TEST_ORG, "LABEL1", Color.black);
    label1.setId("label1");
    Label label2 = new Label(TEST_ORG, "LABEL2", Color.blue);
    label2.setId("label2");
    return Lists.newArrayList(label1, label2);
  }
}

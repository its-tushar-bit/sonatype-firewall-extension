/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.TemporaryEntity;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @since 1.7
 */
public class PolicyImporterTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private PolicyImporterImpl policyImporter;

  private Organization fromOrg;

  private Application fromApp;

  @Before
  public void setUp() {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.getRoot().getAbsolutePath());
    policyImporter = new PolicyImporterImpl(new InsightWork(insightConfig), new BaseUrl(insightConfig));
    fromOrg = tempEntity.newOrganization();
    fromApp = tempEntity.newApplication(fromOrg.getId());
  }

  @Test
  public void testImportAndMergeLabelsForOrg() {
    PolicyExportResult exportDTO = new PolicyExportResult();
    exportDTO.labels = createLabels(fromOrg.getId());
    exportDTO.policies = createPolicies(fromOrg.getId(), exportDTO.labels.get(0).getId());

    Organization toOrg = tempEntity.newOrganization();

    LabelDAO labelDAO = new LabelDAO();
    Label oldLabelToUpdate = new Label(toOrg.getId(), exportDTO.labels.get(0).getLabel().toLowerCase(), Color.white);
    oldLabelToUpdate.setDescription("anything");
    oldLabelToUpdate.setId("label1Old");
    labelDAO.insert(oldLabelToUpdate);

    Label oldLabelToDelete = new Label(toOrg.getId(), "deleteMe", Color.red);
    labelDAO.insert(oldLabelToDelete);
    List<Label> oldLabels = Lists.newArrayList(oldLabelToUpdate, oldLabelToDelete);

    EntityManager em = labelDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      policyImporter.importAndMergeLabels(em, exportDTO, oldLabels, null, toOrg.getId());
      em.getTransaction().commit();
    }
    finally {
      LabelDAO.close(em);
    }

    List<Label> labels = labelDAO.getByOwnerId(toOrg.getId());
    assertThat(labels, hasSize(2));
    Label oldlabel = labels.get(0);
    assertThat(oldlabel.getColor(), is(Color.black)); // updated
    assertThat(oldlabel.getLabel(), is("LABEL1")); // updated from the lowercase version
    assertThat(oldlabel.getId(), is("label1Old")); // id remains the same
    assertThat(oldlabel.getDescription(), nullValue()); // existing description is removed

    Label newLabel = labels.get(1);
    assertThat(newLabel.getColor(), is(Color.blue));
    assertThat(newLabel.getLabel(), is("LABEL2"));

    assertThat(exportDTO.policies.get(0).getConstraints().get(0).getConditions().get(0).getValue(), is("label1Old"));
  }

  @Test
  public void testImportAndMergeLabelsForApp() {
    PolicyExportResult exportDTO = new PolicyExportResult();
    exportDTO.labels = createLabels(fromApp.getId());

    exportDTO.policies = createPolicies(fromApp.getId(), exportDTO.labels.get(0).getId());
    exportDTO.policies.addAll(createPolicies(fromApp.getId(), "orgLabelId")); // add policy referring to an org label

    Organization toOrg = tempEntity.newOrganization();
    Application toApp = tempEntity.newApplication(toOrg.getId());

    LabelDAO labelDAO = new LabelDAO();
    Label orgLabel = new Label(toOrg.getId(), "orgLabel", Color.black);
    orgLabel.setId("orgLabelId");
    labelDAO.insert(orgLabel);

    Label oldLabelToUpdate = new Label(toApp.getId(), exportDTO.labels.get(0).getLabel().toLowerCase(), Color.white);
    oldLabelToUpdate.setDescription("anything");
    oldLabelToUpdate.setId("label1Old");
    labelDAO.insert(oldLabelToUpdate);

    Label oldLabelToDelete = new Label(toApp.getId(), "deleteMe", Color.red);
    labelDAO.insert(oldLabelToDelete);
    List<Label> oldLabels = Lists.newArrayList(oldLabelToUpdate, oldLabelToDelete);

    EntityManager em = labelDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      policyImporter.importAndMergeLabels(em, exportDTO, oldLabels, toApp.getId(), toOrg.getId());
      em.getTransaction().commit();
    }
    finally {
      LabelDAO.close(em);
    }

    List<Label> labels = labelDAO.getByOwnerId(toApp.getId());
    assertThat(labels, hasSize(2));
    Label oldlabel = labels.get(0);

    assertThat(oldlabel.getColor(), is(Color.black)); // updated
    assertThat(oldlabel.getLabel(), is("LABEL1")); // updated from the lowercase version
    assertThat(oldlabel.getId(), is("label1Old")); // id remains the same
    assertThat(oldlabel.getDescription(), nullValue()); // existing description is removed

    Label newLabel = labels.get(1);
    assertThat(newLabel.getColor(), is(Color.blue));
    assertThat(newLabel.getLabel(), is("LABEL2"));

    assertThat(exportDTO.policies.get(0).getConstraints().get(0).getConditions().get(0).getValue(), is("label1Old"));
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

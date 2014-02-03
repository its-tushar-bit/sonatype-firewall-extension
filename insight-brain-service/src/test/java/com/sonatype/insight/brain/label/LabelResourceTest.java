/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.label.LabelResource.ApplicableLabels;
import com.sonatype.insight.brain.label.LabelResource.LabelsByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class LabelResourceTest
    extends AbstractResourceTest
{
  private static final String APP = "application";

  private static final String ORG = "organization";

  @Test
  public void testApplicationCRUD() throws Exception {
    // Create an application
    String appPublicId = "LabelResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    // Get all labels
    Response response = AuthedRestAccess.get(getServiceURLForApplication(appPublicId));
    assertResponseStatus(200, response);
    Label[] labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(0, labels.length);

    // Add a label
    Label label = new Label();
    label.setLabel("MyLabel");
    response = AuthedRestAccess.post(getServiceURLForApplication(appPublicId), JsonHelpers.asJson(label));
    assertResponseStatus(200, response);
    label = JsonHelpers.fromJson(response.getResponseBody(), Label.class);
    assertLabel(application.getId(), "MyLabel", null /* color */, label);

    // Get all labels
    response = AuthedRestAccess.get(getServiceURLForApplication(appPublicId));
    assertResponseStatus(200, response);
    labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(application.getId(), "MyLabel", null /* color */, labels[0]);

    // Update a label
    label.setLabel("MyUpdatedLabel");
    response = AuthedRestAccess.put(getServiceURLForApplication(appPublicId), JsonHelpers.asJson(label));
    assertResponseStatus(200, response);
    label = JsonHelpers.fromJson(response.getResponseBody(), Label.class);
    assertLabel(application.getId(), "MyUpdatedLabel", null /* color */, label);

    // Get all labels
    response = AuthedRestAccess.get(getServiceURLForApplication(appPublicId));
    assertResponseStatus(200, response);
    labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(application.getId(), "MyUpdatedLabel", null /* color */, labels[0]);

    // Delete a label
    response = AuthedRestAccess.delete(getServiceURLForApplication(appPublicId) + "/" + label.getId());
    assertResponseStatus(204, response);

    // Get all labels
    response = AuthedRestAccess.get(getServiceURLForApplication(appPublicId));
    assertResponseStatus(200, response);
    labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(0, labels.length);
  }

  @Test
  public void testDeleteAppLabel_UsedInPolicyCondition() throws Exception {
    // Create an application with one label
    String appPublicId = "LabelResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);
    Label label = new Label();
    label.setColor(Color.blue);
    label.setLabel("MyLabel");
    Response response = AuthedRestAccess.post(getServiceURLForApplication(appPublicId), JsonHelpers.asJson(label));
    assertResponseStatus(200, response);
    label = JsonHelpers.fromJson(response.getResponseBody(), Label.class);

    // Create a policy that uses the label
    Condition condition = new Condition(LabelConditionType.ID, "is", label.getId());
    Constraint constraint = new Constraint("ConstraintId1", "Constraint name 1", LogicalOperator.AND);
    constraint.addCondition(condition);
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    response = AuthedRestAccess.post(getRestUrl(PolicyResource.SERVICE_PATH, "application", appPublicId),
        JsonHelpers.asJson(policy));
    assertResponseStatus(200, response);

    // Try to delete the label
    response = AuthedRestAccess.delete(getServiceURLForApplication(appPublicId) + "/" + label.getId());
    assertResponseStatus(400, response);
    Assert.assertEquals("Cannot delete the label because it is used in a condition for the 'Policy Name 1' policy",
        response.getResponseBody());
    // Verify that the label was not deleted
    response = AuthedRestAccess.get(getServiceURLForApplication(appPublicId));
    assertResponseStatus(200, response);
    Label[] labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(application.getId(), "MyLabel", Color.blue, labels[0]);
  }

  @Test
  public void testDeleteAppLabel_Nonexistant() throws Exception {
    String appPublicId = "LabelResourceTest_AppId";
    tempEntity.newApplicationWithParent(appPublicId);

    Response response = AuthedRestAccess.delete(getServiceURLForApplication(appPublicId) + "/YettiId");
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a label with id YettiId", response.getResponseBody());
  }

  @Test
  public void testDeleteAppLabel_OwnerIdMismatch() throws Exception {
    String appPublicId1 = "LabelResourceTest_AppId1";
    Application application1 = tempEntity.newApplicationWithParent(appPublicId1);
    String appPublicId2 = "LabelResourceTest_AppId2";
    tempEntity.newApplicationWithParent(appPublicId2);

    Label label = new Label();
    label.setColor(Color.blue);
    label.setLabel("MyLabel");
    Response response = AuthedRestAccess.post(getServiceURLForApplication(appPublicId1), JsonHelpers.asJson(label));
    assertResponseStatus(200, response);
    label = JsonHelpers.fromJson(response.getResponseBody(), Label.class);

    response = AuthedRestAccess.delete(getServiceURLForApplication(appPublicId2) + "/" + label.getId());
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a label with id " + label.getId() + " for application id " + appPublicId2,
        response.getResponseBody());
    // Verify that the label was not deleted
    response = AuthedRestAccess.get(getServiceURLForApplication(appPublicId1));
    assertResponseStatus(200, response);
    Label[] labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(application1.getId(), "MyLabel", Color.blue, labels[0]);
  }

  @Test
  public void testOrganizationCRUD() throws Exception {
    // Create an organization
    String orgName = "LabelResourceTestOrgName";
    Organization organization = tempEntity.newOrganization(orgName);

    // Get all labels
    Response response = AuthedRestAccess.get(getServiceURL(ORG, organization.getId()));
    assertResponseStatus(200, response);
    Label[] labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(0, labels.length);

    // Add a label
    Label label = new Label();
    label.setLabel("MyLabel");
    response = AuthedRestAccess.post(getServiceURL(ORG, organization.getId()), JsonHelpers.asJson(label));
    assertResponseStatus(200, response);
    label = JsonHelpers.fromJson(response.getResponseBody(), Label.class);
    assertLabel(organization.getId(), "MyLabel", null /* color */, label);

    // Get all labels
    response = AuthedRestAccess.get(getServiceURL(ORG, organization.getId()));
    assertResponseStatus(200, response);
    labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(organization.getId(), "MyLabel", null /* color */, labels[0]);

    // Update a label
    label.setLabel("MyUpdatedLabel");
    response = AuthedRestAccess.put(getServiceURL(ORG, organization.getId()), JsonHelpers.asJson(label));
    assertResponseStatus(200, response);
    label = JsonHelpers.fromJson(response.getResponseBody(), Label.class);
    assertLabel(organization.getId(), "MyUpdatedLabel", null /* color */, label);

    // Get all labels
    response = AuthedRestAccess.get(getServiceURL(ORG, organization.getId()));
    assertResponseStatus(200, response);
    labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(organization.getId(), "MyUpdatedLabel", null /* color */, labels[0]);

    // Delete a label
    response = AuthedRestAccess.delete(getServiceURL(ORG, organization.getId()) + "/" + label.getId());
    assertResponseStatus(204, response);

    // Get all labels
    response = AuthedRestAccess.get(getServiceURL(ORG, organization.getId()));
    assertResponseStatus(200, response);
    labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(0, labels.length);
  }

  @Test
  public void testDeleteOrgLabel_UsedInPolicyCondition() throws Exception {
    // Create an organization with one label
    String orgName = "LabelResourceTestOrgName";
    Organization organization = tempEntity.newOrganization(orgName);
    Label label = new Label();
    label.setColor(Color.blue);
    label.setLabel("MyLabel");
    Response response = AuthedRestAccess.post(getServiceURLForOrganization(organization.getId()), JsonHelpers.asJson(label));
    assertResponseStatus(200, response);
    label = JsonHelpers.fromJson(response.getResponseBody(), Label.class);

    // Create a policy that uses the label
    Condition condition = new Condition(LabelConditionType.ID, "is", label.getId());
    Constraint constraint = new Constraint("ConstraintId1", "Constraint name 1", LogicalOperator.AND);
    constraint.addCondition(condition);
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    response = AuthedRestAccess.post(
        getRestUrl(PolicyResource.SERVICE_PATH, "organization", organization.getId()),
        JsonHelpers.asJson(policy));
    assertResponseStatus(200, response);

    // Try to delete the label
    response = AuthedRestAccess.delete(getServiceURLForOrganization(organization.getId()) + "/" + label.getId());
    assertResponseStatus(400, response);
    Assert.assertEquals("Cannot delete the label because it is used in a condition for the 'Policy Name 1' policy",
        response.getResponseBody());
    // Verify that the label was not deleted
    response = AuthedRestAccess.get(getServiceURLForOrganization(organization.getId()));
    assertResponseStatus(200, response);
    Label[] labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(organization.getId(), "MyLabel", Color.blue, labels[0]);
  }

  @Test
  public void testDeleteOrgLabel_UsedInAppPolicyCondition() throws Exception {
    // Create an application
    String appPublicId = "LabelResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId, "Application Name 1");
    String organizationId = application.getOrganizationId();

    // Create an organization label
    Label label = new Label();
    label.setColor(Color.blue);
    label.setLabel("MyLabel");
    Response response = AuthedRestAccess.post(getServiceURLForOrganization(organizationId), JsonHelpers.asJson(label));
    assertResponseStatus(200, response);
    label = JsonHelpers.fromJson(response.getResponseBody(), Label.class);

    // Create an app policy that uses the label
    Condition condition = new Condition(LabelConditionType.ID, "is", label.getId());
    Constraint constraint = new Constraint("ConstraintId1", "Constraint name 1", LogicalOperator.AND);
    constraint.addCondition(condition);
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    response = AuthedRestAccess.post(getRestUrl(PolicyResource.SERVICE_PATH, "application", appPublicId),
        JsonHelpers.asJson(policy));
    assertResponseStatus(200, response);

    // Try to delete the label
    response = AuthedRestAccess.delete(getServiceURLForOrganization(organizationId) + "/" + label.getId());
    assertResponseStatus(400, response);
    Assert.assertEquals("Cannot delete the label because it is used in a condition for the 'Policy Name 1' policy"
        + " in application 'Application Name 1'", response.getResponseBody());

    // Verify that the label was not deleted
    response = AuthedRestAccess.get(getServiceURLForOrganization(organizationId));
    assertResponseStatus(200, response);
    Label[] labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(organizationId, "MyLabel", Color.blue, labels[0]);
  }

  @Test
  public void testDeleteOrgLabel_Nonexistant() throws Exception {
    String orgName = "LabelResourceTestOrgName";
    Organization organization = tempEntity.newOrganization(orgName);

    Response response = AuthedRestAccess.delete(getServiceURLForOrganization(organization.getId()) + "/YettiId");
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a label with id YettiId", response.getResponseBody());
  }

  @Test
  public void testDeleteOrgLabel_OwnerIdMismatch() throws Exception {
    String orgName1 = "LabelResourceTestOrgName1";
    Organization organization1 = tempEntity.newOrganization(orgName1);
    String orgName2 = "LabelResourceTestOrgName2";
    Organization organization2 = tempEntity.newOrganization(orgName2);

    Label label = new Label();
    label.setColor(Color.blue);
    label.setLabel("MyLabel");
    Response response = AuthedRestAccess.post(getServiceURLForOrganization(organization1.getId()), JsonHelpers.asJson(label));
    assertResponseStatus(200, response);
    label = JsonHelpers.fromJson(response.getResponseBody(), Label.class);

    response = AuthedRestAccess.delete(getServiceURLForOrganization(organization2.getId()) + "/" + label.getId());
    assertResponseStatus(404, response);
    Assert.assertEquals(
        "Cannot find a label with id " + label.getId() + " for organization id " + organization2.getId(),
        response.getResponseBody());
    // Verify that the label was not deleted
    response = AuthedRestAccess.get(getServiceURLForOrganization(organization1.getId()));
    assertResponseStatus(200, response);
    Label[] labels = JsonHelpers.fromJson(response.getResponseBody(), Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(organization1.getId(), "MyLabel", Color.blue, labels[0]);
  }

  /**
   * Tests for {@link LabelResource#getApplicableLabels(java.lang.String, java.lang.String)}.
   */
  @Test
  public void testGetApplicableLabels() throws Exception {
    // Create an organization and an application
    String orgName = "testGetApplicableLabelsOrg";
    Organization organization = tempEntity.newOrganization(orgName);
    String orgId = organization.getId();
    String appName = "testGetApplicableLabelsApp";
    String appPublicId = "testGetApplicableLabelsApp";
    Application app = super.tempEntity.newApplication(appPublicId, appPublicId, organization.getId());
    String appId = app.getId();

    // Verify the applicable labels for the application
    Response response = AuthedRestAccess.get(getServiceURL(APP, appPublicId) + "/applicable");
    assertResponseStatus(200, response);
    assertResponseStatus(200, response);
    ApplicableLabels applicableLabels = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    Assert.assertEquals(2, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(appId, appName, "application", 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(orgId, orgName, "organization", 0, applicableLabels.labelsByOwner.get(1));

    // Verify the applicable labels for the organization
    response = AuthedRestAccess.get(getServiceURL(ORG, orgId) + "/applicable");
    assertResponseStatus(200, response);
    applicableLabels = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    Assert.assertEquals(1, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(orgId, orgName, "organization", 0, applicableLabels.labelsByOwner.get(0));

    // Create a label for the application
    Label appLabel = new Label();
    appLabel.setLabel("testGetApplicableLabels_App_label");
    response = AuthedRestAccess.post(getServiceURL(APP, appPublicId), JsonHelpers.asJson(appLabel));
    assertResponseStatus(200, response);
    appLabel = JsonHelpers.fromJson(response.getResponseBody(), Label.class);

    // Verify the applicable labels for the application
    response = AuthedRestAccess.get(getServiceURL(APP, appPublicId) + "/applicable");
    assertResponseStatus(200, response);
    applicableLabels = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    Assert.assertEquals(2, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(appId, appName, "application", 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(orgId, orgName, "organization", 0, applicableLabels.labelsByOwner.get(1));
    Assert.assertEquals(appLabel.getId(), applicableLabels.labelsByOwner.get(0).labels.get(0).getId());

    // Verify the applicable labels for the organization
    response = AuthedRestAccess.get(getServiceURL(ORG, orgId) + "/applicable");
    assertResponseStatus(200, response);
    applicableLabels = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    Assert.assertEquals(1, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(orgId, orgName, "organization", 0, applicableLabels.labelsByOwner.get(0));

    // Create a label for the organization
    Label orgLabel = new Label();
    orgLabel.setLabel("testGetApplicableLabels_Org_label");
    response = AuthedRestAccess.post(getServiceURL(ORG, orgId), JsonHelpers.asJson(orgLabel));
    assertResponseStatus(200, response);
    orgLabel = JsonHelpers.fromJson(response.getResponseBody(), Label.class);

    // Verify the applicable labels for the application
    response = AuthedRestAccess.get(getServiceURL(APP, appPublicId) + "/applicable");
    assertResponseStatus(200, response);
    applicableLabels = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    Assert.assertEquals(2, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(appId, appName, "application", 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(orgId, orgName, "organization", 1, applicableLabels.labelsByOwner.get(1));
    Assert.assertEquals(appLabel.getId(), applicableLabels.labelsByOwner.get(0).labels.get(0).getId());
    Assert.assertEquals(orgLabel.getId(), applicableLabels.labelsByOwner.get(1).labels.get(0).getId());

    // Verify the applicable labels for the organization
    response = AuthedRestAccess.get(getServiceURL(ORG, orgId) + "/applicable");
    assertResponseStatus(200, response);
    applicableLabels = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    Assert.assertEquals(1, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(orgId, orgName, "organization", 1, applicableLabels.labelsByOwner.get(0));
    Assert.assertEquals(orgLabel.getId(), applicableLabels.labelsByOwner.get(0).labels.get(0).getId());
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    Application app = tempEntity.newApplication("appName", "appPublicId", org.getId());
    Label orgLabel = new Label(org.getId(), "orgLabel", null);
    new LabelDAO().insert(orgLabel);
    Label appLabel = new Label(app.getId(), "appLabel", null);
    new LabelDAO().insert(appLabel);

    Response response = AuthedRestAccess.get(getContextsURL(APP, app.getPublicId(), appLabel.getId()));
    assertResponseStatus(200, response);
    ApplicableContext context = JsonHelpers.fromJson(response.getResponseBody(), ApplicableContext.class);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(app.getPublicId()));
    Assert.assertThat(context.getName(), is(app.getName()));
    Assert.assertThat(context.getType(), is(APP));
    Assert.assertThat(context.getChildren(), is(nullValue()));

    response = AuthedRestAccess.get(getContextsURL(ORG, org.getId(), orgLabel.getId()));
    assertResponseStatus(200, response);
    context = JsonHelpers.fromJson(response.getResponseBody(), ApplicableContext.class);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(org.getId()));
    Assert.assertThat(context.getName(), is(org.getName()));
    Assert.assertThat(context.getType(), is(ORG));
    Assert.assertThat(context.getChildren(), is(notNullValue()));
    Assert.assertThat(context.getChildren(), hasSize(1));
    context = context.getChildren().get(0);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(app.getPublicId()));
    Assert.assertThat(context.getName(), is(app.getName()));
    Assert.assertThat(context.getType(), is(APP));
    Assert.assertThat(context.getChildren(), is(nullValue()));

    response = AuthedRestAccess.get(getContextsURL(ORG, org.getId(), appLabel.getId()));
    assertResponseStatus(404, response);
    Assert.assertThat(response.getResponseBody(), is("Cannot find a label with id " + appLabel.getId()
        + " for organization id " + org.getId()));
  }

  private void assertLabel(String ownerId, String label, Color color, Label actual) {
    Assert.assertEquals(ownerId, actual.getOwnerId());
    Assert.assertEquals(label, actual.getLabel());
    Assert.assertEquals(label.toLowerCase(Locale.ENGLISH), actual.getLabelLowercase());
    Assert.assertEquals(color, actual.getColor());
  }

  private void assertLabelsByOwner(String ownerId, String ownerName, String ownerType, int labelsCount,
      LabelsByOwner actual)
  {
    Assert.assertEquals(ownerId, actual.ownerId);
    Assert.assertEquals(ownerName, actual.ownerName);
    Assert.assertEquals(ownerType, actual.ownerType);
    Assert.assertEquals(labelsCount, actual.labels.size());
  }

  private String getServiceURLForApplication(final String appId) {
    return getServiceURL(APP, appId);
  }

  private String getServiceURLForOrganization(final String orgId) {
    return getServiceURL(ORG, orgId);
  }

  private String getServiceURL(final String ownerType, final String ownerId) {
    return getRestBaseUrl() + LabelResource.SERVICE_BASEPATH + ownerType + "/" + ownerId;
  }

  private String getContextsURL(final String ownerType, final String ownerId, final String labelId) {
    return getServiceURL(ownerType, ownerId) + "/applicable/context/" + labelId;
  }
}

/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.List;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.label.ComponentLabelResource.AppliedLabels;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ComponentLabelResourceTest
    extends AbstractResourceTest
{

  private String componentHash = "bababababa";

  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  private Application app;

  private Label appLabel;

  private Label orgLabel;

  @Before
  public void init() throws Exception {
    app = tempEntity.newApplicationWithParent("test-app", "Test");
    appLabel = new Label(app.getId(), "app", null);
    orgLabel = new Label(app.getOrganizationId(), "org", null);
    LabelDAO labelDAO = new LabelDAO();
    labelDAO.insert(appLabel);
    labelDAO.insert(orgLabel);
  }

  @Test
  public void testGetComponentLabels_AppLevel() throws Exception {
    Response response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_APPLICATION, app.getPublicId(), componentHash));
    assertResponseStatus(200, response);
    AppliedLabels componentLabels = JsonHelpers.fromJson(response.getResponseBody(), AppliedLabels.class);
    Assert.assertThat(componentLabels, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.size(), is(0));

    componentLabelDAO.insert(new ComponentLabel(app.getId(), appLabel.getId(), componentHash));
    componentLabelDAO.insert(new ComponentLabel(app.getOrganizationId(), orgLabel.getId(), componentHash));

    response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_APPLICATION, app.getPublicId(), componentHash));
    assertResponseStatus(200, response);
    componentLabels = JsonHelpers.fromJson(response.getResponseBody(), AppliedLabels.class);
    Assert.assertThat(componentLabels, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.size(), is(2));
    Assert.assertThat(componentLabels.labelsByOwner.get(0), is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).ownerId, is(app.getPublicId()));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).ownerName, is("Test"));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).ownerType, is(IdUtils.TYPE_APPLICATION));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).labels, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).labels.size(), is(1));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).labels.get(0).getId(), is(appLabel.getId()));
    Assert.assertThat(componentLabels.labelsByOwner.get(1), is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.get(1).ownerId, is(app.getOrganizationId()));
    Assert.assertThat(componentLabels.labelsByOwner.get(1).ownerName, is("Test"));
    Assert.assertThat(componentLabels.labelsByOwner.get(1).ownerType, is(IdUtils.TYPE_ORGANIZATION));
    Assert.assertThat(componentLabels.labelsByOwner.get(1).labels, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.get(1).labels.size(), is(1));
    Assert.assertThat(componentLabels.labelsByOwner.get(1).labels.get(0).getId(), is(orgLabel.getId()));
  }

  @Test
  public void testGetComponentLabels_OrgLevel() throws Exception {
    Response response = AuthedRestAccess
        .get(getServiceURL(IdUtils.TYPE_ORGANIZATION, app.getOrganizationId(), componentHash));
    assertResponseStatus(200, response);
    AppliedLabels componentLabels = JsonHelpers.fromJson(response.getResponseBody(), AppliedLabels.class);
    Assert.assertThat(componentLabels, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.size(), is(0));

    componentLabelDAO.insert(new ComponentLabel(app.getId(), appLabel.getId(), componentHash));
    componentLabelDAO.insert(new ComponentLabel(app.getOrganizationId(), orgLabel.getId(), componentHash));

    response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_ORGANIZATION, app.getOrganizationId(), componentHash));
    assertResponseStatus(200, response);
    componentLabels = JsonHelpers.fromJson(response.getResponseBody(), AppliedLabels.class);
    Assert.assertThat(componentLabels, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.size(), is(1));
    Assert.assertThat(componentLabels.labelsByOwner.get(0), is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).ownerId, is(app.getOrganizationId()));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).ownerName, is("Test"));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).ownerType, is(IdUtils.TYPE_ORGANIZATION));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).labels, is(notNullValue()));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).labels.size(), is(1));
    Assert.assertThat(componentLabels.labelsByOwner.get(0).labels.get(0).getId(), is(orgLabel.getId()));
  }

  @Test
  public void testSetComponentLabel_AppLevel() throws Exception {
    Response response = AuthedRestAccess.post(getServiceURL(IdUtils.TYPE_APPLICATION, app.getPublicId(), componentHash),
        JsonHelpers.asJson(appLabel));
    assertResponseStatus(204, response);

    List<ComponentLabel> componentLabels = componentLabelDAO.getByOwnerIdAndHash(app.getId(), componentHash);
    Assert.assertThat(componentLabels, is(notNullValue()));
    Assert.assertThat(componentLabels.size(), is(1));
    Assert.assertThat(componentLabels.get(0).getLabelId(), is(appLabel.getId()));
    componentLabels = componentLabelDAO.getByOwnerIdAndHash(app.getOrganizationId(), componentHash);
    Assert.assertThat(componentLabels, is(notNullValue()));
    Assert.assertThat(componentLabels.size(), is(0));
  }

  @Test
  public void testSetComponentLabel_OrgLevel() throws Exception {
    Response response = AuthedRestAccess.post(
        getServiceURL(IdUtils.TYPE_ORGANIZATION, app.getOrganizationId(), componentHash), JsonHelpers.asJson(orgLabel));
    assertResponseStatus(204, response);

    List<ComponentLabel> componentLabels = componentLabelDAO
        .getByOwnerIdAndHash(app.getOrganizationId(), componentHash);
    Assert.assertThat(componentLabels, is(notNullValue()));
    Assert.assertThat(componentLabels.size(), is(1));
    Assert.assertThat(componentLabels.get(0).getLabelId(), is(orgLabel.getId()));
  }

  @Test
  public void testRemoveComponentLabel_AppLevel() throws Exception {
    ComponentLabel appComponentLabel = new ComponentLabel(app.getId(), appLabel.getId(), componentHash);
    ComponentLabel orgComponentLabel = new ComponentLabel(app.getOrganizationId(), orgLabel.getId(), componentHash);
    componentLabelDAO.insert(appComponentLabel);
    componentLabelDAO.insert(orgComponentLabel);

    Response response = AuthedRestAccess.delete(getServiceURL(IdUtils.TYPE_APPLICATION, app.getPublicId(), componentHash)
        + "/" + appLabel.getId());
    assertResponseStatus(204, response);

    Assert.assertThat(componentLabelDAO.getById(appComponentLabel.getId()), is(nullValue()));

    response = AuthedRestAccess.delete(getServiceURL(IdUtils.TYPE_APPLICATION, app.getPublicId(), componentHash) + "/"
        + orgLabel.getId());
    assertResponseStatus(404, response);
    Assert.assertThat(response.getResponseBody(), is("Cannot find the label with id " + orgLabel.getId()
        + " for application id test-app on the component " + componentHash));
  }

  @Test
  public void testRemoveComponentLabel_OrgLevel() throws Exception {
    ComponentLabel appComponentLabel = new ComponentLabel(app.getId(), appLabel.getId(), componentHash);
    ComponentLabel orgComponentLabel = new ComponentLabel(app.getOrganizationId(), orgLabel.getId(), componentHash);
    componentLabelDAO.insert(appComponentLabel);
    componentLabelDAO.insert(orgComponentLabel);

    Response response = AuthedRestAccess.delete(getServiceURL(IdUtils.TYPE_ORGANIZATION, app.getOrganizationId(),
        componentHash) + "/" + orgLabel.getId());
    assertResponseStatus(204, response);

    Assert.assertThat(componentLabelDAO.getById(orgComponentLabel.getId()), is(nullValue()));

    response = AuthedRestAccess.delete(getServiceURL(IdUtils.TYPE_ORGANIZATION, app.getOrganizationId(), componentHash) + "/"
        + appLabel.getId());
    assertResponseStatus(404, response);
    Assert.assertThat(response.getResponseBody(), is("Cannot find the label with id " + appLabel.getId()
        + " for organization id " + app.getOrganizationId() + " on the component " + componentHash));
  }

  private String getServiceURL(String ownerType, String ownerId, String hash) {
    return getRestBaseUrl() + ComponentLabelResource.SERVICE_BASEPATH + ownerType + "/" + ownerId + "/" + hash;
  }

}

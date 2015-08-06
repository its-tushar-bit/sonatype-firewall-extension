/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.label.ComponentLabelService.AppliedLabels;
import com.sonatype.insight.brain.label.ComponentLabelService.LabelsByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ComponentLabelResourceTest
    extends AbstractResourceTest
{

  private String componentHash = "bababababa";

  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  private Organization org;

  private Application app;

  private Label appLabel;

  private Label orgLabel;

  private HttpRequest restRequest(OwnerType ownerType, String ownerId, String hash) {
    return restRequest().path(ComponentLabelResource.SERVICE_PATH).parameter(ownerType, ownerId, hash);
  }

  @Before
  public void init() throws Exception {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication("Test", "test-app", org.getId());
    appLabel = tempEntity.newLabel(app.getId(), "app");
    orgLabel = tempEntity.newLabel(org.getId(), "org");
  }

  private void assertLabelsByOwner(LabelsByOwner labelsByOwner, Owner owner, String labelId) {
    assertThat(labelsByOwner, is(notNullValue()));
    assertThat(labelsByOwner.ownerId, is(owner.getPublicId()));
    assertThat(labelsByOwner.ownerName, is(owner.getName()));
    assertThat(labelsByOwner.ownerType, is(owner.getType()));
    assertThat(labelsByOwner.labels, is(notNullValue()));
    assertThat(labelsByOwner.labels, hasSize(1));
    assertThat(labelsByOwner.labels.get(0).getId(), is(labelId));
  }

  @Test
  public void testGetComponentLabels() throws Exception {
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    Label parentOrgLabel = tempEntity.newLabel(parentOrg.getId(), "parentOrg");

    // No labels applied to componentHash
    // Verify app level
    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId(), componentHash).get();
    assertResponseStatus(200, response);
    AppliedLabels componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner, hasSize(0));
    // Verify org level
    response = restRequest(OwnerType.ORGANIZATION, org.getId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner, hasSize(0));
    // Verify parent org level
    response = restRequest(OwnerType.ORGANIZATION, org.getParentOrganizationId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner, hasSize(0));

    // Labels applied to componentHash at all levels
    tempEntity.newComponentLabel(app.getId(), appLabel.getId(), componentHash);
    tempEntity.newComponentLabel(org.getId(), orgLabel.getId(), componentHash);
    tempEntity.newComponentLabel(parentOrg.getId(), parentOrgLabel.getId(), componentHash);

    // Verify app level
    response = restRequest(OwnerType.APPLICATION, app.getPublicId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner, hasSize(3));
    assertLabelsByOwner(componentLabels.labelsByOwner.get(0), app, appLabel.getId());
    assertLabelsByOwner(componentLabels.labelsByOwner.get(1), org, orgLabel.getId());
    assertLabelsByOwner(componentLabels.labelsByOwner.get(2), parentOrg, parentOrgLabel.getId());
    // Verify org level
    response = restRequest(OwnerType.ORGANIZATION, org.getId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner, hasSize(2));
    assertLabelsByOwner(componentLabels.labelsByOwner.get(0), org, orgLabel.getId());
    assertLabelsByOwner(componentLabels.labelsByOwner.get(1), parentOrg, parentOrgLabel.getId());
    // Verify parent org level
    response = restRequest(OwnerType.ORGANIZATION, parentOrg.getId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner, hasSize(1));
    assertLabelsByOwner(componentLabels.labelsByOwner.get(0), parentOrg, parentOrgLabel.getId());
  }

  @Test
  public void testSetComponentLabel_AppLevel() throws Exception {
    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId(), componentHash).body(appLabel).post();
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
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, app.getOrganizationId(), componentHash).body(orgLabel)
        .post();
    assertResponseStatus(204, response);

    List<ComponentLabel> componentLabels = componentLabelDAO
        .getByOwnerIdAndHash(app.getOrganizationId(), componentHash);
    Assert.assertThat(componentLabels, is(notNullValue()));
    Assert.assertThat(componentLabels.size(), is(1));
    Assert.assertThat(componentLabels.get(0).getLabelId(), is(orgLabel.getId()));
  }

  @Test
  public void testDeleteComponentLabel_AppLevel() throws Exception {
    ComponentLabel appComponentLabel = tempEntity.newComponentLabel(app.getId(), appLabel.getId(), componentHash);
    tempEntity.newComponentLabel(app.getOrganizationId(), orgLabel.getId(), componentHash);
    
    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId(), componentHash).path(appLabel.getId())
        .delete();
    assertResponseStatus(204, response);

    Assert.assertThat(componentLabelDAO.getById(appComponentLabel.getId()), is(nullValue()));

    response = restRequest(OwnerType.APPLICATION, app.getPublicId(), componentHash).path(orgLabel.getId()).delete();
    assertResponseStatus(404, response);
    Assert.assertThat(response.getBodyText(), is("Cannot find the label with ID " + orgLabel.getId()
        + " for application ID test-app on the component " + componentHash + "."));
  }

  @Test
  public void testDeleteComponentLabel_OrgLevel() throws Exception {
    tempEntity.newComponentLabel(app.getId(), appLabel.getId(), componentHash);
    ComponentLabel orgComponentLabel = tempEntity.newComponentLabel(app.getOrganizationId(), orgLabel.getId(), componentHash);

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, app.getOrganizationId(), componentHash).path(
        orgLabel.getId()).delete();
    assertResponseStatus(204, response);

    Assert.assertThat(componentLabelDAO.getById(orgComponentLabel.getId()), is(nullValue()));

    response = restRequest(OwnerType.ORGANIZATION, app.getOrganizationId(), componentHash).path(appLabel.getId())
        .delete();
    assertResponseStatus(404, response);
    Assert.assertThat(response.getBodyText(), is("Cannot find the label with ID " + appLabel.getId()
        + " for organization ID " + app.getOrganizationId() + " on the component " + componentHash + "."));
  }
}

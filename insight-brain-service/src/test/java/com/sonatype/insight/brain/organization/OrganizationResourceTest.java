/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OrganizationResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(OrganizationResource.SERVICE_PATH);
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    Organization organization = new Organization("OrganizationResourceTest");

    HttpResponse response = restRequest().body(organization).post();
    assertResponseStatus(200, response);
    organization = fromJson(response, Organization.class);
    assertNotNull(organization);
    assertNotNull(organization.getId());
    assertEquals("OrganizationResourceTest", organization.getName());
    String organizationId = organization.getId();

    // Get
    response = restRequest().get();
    assertResponseStatus(200, response);
    Organization[] organizations = fromJson(response, Organization[].class);
    assertNotNull(organizations);
    assertEquals(1, organizations.length);
    organization = organizations[0];
    assertNotNull(organization);
    assertEquals(organizationId, organization.getId());
    assertEquals("OrganizationResourceTest", organization.getName());

    // Add invalid icon
    byte[] defaultIconByteArray = IconUtils.loadInvalidIcon();
    response = restRequest().path(OrganizationResource.ICON_PATH).part("organizationId", organizationId)
        .part("hasRobotSource", "false").part("file", "defaulticon_organization.png", defaultIconByteArray).post();
    assertResponseStatus(400, response);
    Assert.assertEquals("defaulticon_organization.png is not a valid image.", response.getResponseBody());

    // Get icon (default icon)
    HttpResponse iconResponse = restRequest().path(OrganizationResource.GET_ICON_PATH).parameter(organizationId).get();
    assertResponseStatus(307, iconResponse);
    Assert.assertEquals(getRestBaseUrl() + "assets/img/defaulticon_organization.png",
        iconResponse.getHeader("Location"));

    // Add icon
    defaultIconByteArray = loadDefaultIcon();
    response = restRequest().path(OrganizationResource.ICON_PATH).part("organizationId", organizationId)
        .part("hasRobotSource", "false").part("file", "defaulticon_organization.png", defaultIconByteArray).post();
    assertResponseStatus(204, response);

    // Get icon
    iconResponse = restRequest().path(OrganizationResource.GET_ICON_PATH).parameter(organizationId).get();
    assertResponseStatus(200, iconResponse);
    BufferedImage icon = null;
    try (InputStream iconStream = iconResponse.getResponseBodyAsStream()) {
      icon = ImageIO.read(iconStream);
    }
    Assert.assertNotNull(icon);
    Assert.assertEquals(420, icon.getHeight());
    Assert.assertEquals(420, icon.getWidth());

    // Update
    organization.setName("OrganizationResourceTest updated");
    response = restRequest().body(organization).put();
    assertResponseStatus(200, response);
    organization = fromJson(response, Organization.class);
    assertNotNull(organization);
    assertEquals(organizationId, organizationId);
    assertEquals("OrganizationResourceTest updated", organization.getName());

    // Update icon
    response = restRequest().path(OrganizationResource.ICON_PATH).part("organizationId", organizationId)
        .part("hasRobotSource", "false").post();
    assertResponseStatus(204, response);

    // now create related objects to test delete cascades

    // application
    final ApplicationDAO applicationDAO = new ApplicationDAO();
    final Application application = new Application("testapp", "testapp", organization.getId());
    applicationDAO.insert(application);

    // Delete
    response = restRequest().path(organizationId).delete();
    assertResponseStatus(204, response);
    Assert.assertNull(new OrganizationDAO().getById(organizationId));
    iconResponse = restRequest().path(OrganizationResource.GET_ICON_PATH).parameter(organizationId).get();
    assertResponseStatus(404, iconResponse);
    // assert related objects were deleted
    Assert.assertNull(applicationDAO.getById(application.getId()));
  }

  @Test
  public void testAddOrganization_Unlicensed() throws Exception {
    uninstallLicense();
    Organization organization = new Organization("OrganizationResourceTest");

    HttpResponse response = restRequest().body(organization).post();
    assertResponseStatus(402, response);
  }

  @Test
  public void testUpdateOrganization_Unlicensed() throws Exception {
    Organization organization = new Organization("OrganizationResourceTest");
    HttpResponse response = restRequest().body(organization).post();
    assertResponseStatus(200, response);
    organization = fromJson(response, Organization.class);
    tempEntity.register(organization);
    uninstallLicense();
    response = restRequest().body(organization).put();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetAll_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = restRequest().get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGenerateIcon() throws Exception {
    String hashcode = "abababababababababab";
    String saasUrl = "rest/application/icon/generate/" + hashcode;
    setSaasResponseForURI(saasUrl, loadDefaultIcon(), 200);
    HttpResponse response = restRequest().path(OrganizationResource.GENERATE_ICON_PATH).parameter(hashcode).get();
    assertResponseStatus(200, response);
    Assert.assertNotNull(response.getResponseBodyAsBytes());
  }

  private byte[] loadDefaultIcon() throws IOException {
    return IconUtils.loadIconFromProductAssets("defaulticon_organization.png");
  }
}

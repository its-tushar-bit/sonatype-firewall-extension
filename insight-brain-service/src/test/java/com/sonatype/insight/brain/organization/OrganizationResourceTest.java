/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.StringPart;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OrganizationResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    // Create
    Organization organization = new Organization("OrganizationResourceTest");

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(200, response);
    organization = JsonHelpers.fromJson(response.getResponseBody(), Organization.class);
    assertNotNull(organization);
    assertNotNull(organization.getId());
    assertEquals("OrganizationResourceTest", organization.getName());
    String organizationId = organization.getId();

    // Get
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    Organization[] organizations = JsonHelpers.fromJson(response.getResponseBody(), Organization[].class);
    assertNotNull(organizations);
    assertEquals(1, organizations.length);
    organization = organizations[0];
    assertNotNull(organization);
    assertEquals(organizationId, organization.getId());
    assertEquals("OrganizationResourceTest", organization.getName());

    // Add invalid icon
    byte[] defaultIconByteArray = IconUtils.loadInvalidIcon();
    AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(getSetIconServiceUrl());
    builder.addBodyPart(new StringPart("organizationId", organizationId));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("defaulticon_organization.png",
        defaultIconByteArray)));

    response = AuthedRestAccess.execute(builder);
    assertResponseStatus(400, response);
    Assert.assertEquals("defaulticon_organization.png is not a valid image.", response.getResponseBody());

    // Get icon (default icon)
    defaultIconByteArray = loadDefaultIcon();
    Response iconResponse = AuthedRestAccess.get(getGetIconServiceUrl(organizationId));
    assertResponseStatus(307, iconResponse);
    Assert.assertEquals(getRestBaseUrl() + "assets/img/defaulticon_organization.png",
        iconResponse.getHeader("Location"));

    // Add icon
    builder = AuthedRestAccess.getClient().preparePost(getSetIconServiceUrl());
    builder.addBodyPart(new StringPart("organizationId", organizationId));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("defaulticon_organization.png",
        defaultIconByteArray)));
    response = AuthedRestAccess.execute(builder);
    assertResponseStatus(204, response);

    // Get icon
    iconResponse = AuthedRestAccess.get(getGetIconServiceUrl(organizationId));
    assertResponseStatus(200, iconResponse);
    InputStream iconStream = iconResponse.getResponseBodyAsStream();
    BufferedImage icon = null;
    try {
      icon = ImageIO.read(iconStream);
    }
    finally {
      iconStream.close();
    }
    Assert.assertNotNull(icon);
    Assert.assertEquals(420, icon.getHeight());
    Assert.assertEquals(420, icon.getWidth());

    // Update
    organization.setName("OrganizationResourceTest updated");
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(200, response);
    organization = JsonHelpers.fromJson(response.getResponseBody(), Organization.class);
    assertNotNull(organization);
    assertEquals(organizationId, organizationId);
    assertEquals("OrganizationResourceTest updated", organization.getName());

    // Update icon
    builder = AuthedRestAccess.getClient().preparePost(getSetIconServiceUrl());
    builder.addBodyPart(new StringPart("organizationId", organizationId));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    response = AuthedRestAccess.execute(builder);
    assertResponseStatus(204, response);

    // now create related objects to test delete cascades

    // application
    final ApplicationDAO applicationDAO = new ApplicationDAO();
    final Application application = new Application("testapp", "testapp", organization.getId());
    applicationDAO.insert(application);

    // Delete
    response = AuthedRestAccess.delete(getServiceURL() + "/" + organizationId);
    assertResponseStatus(204, response);
    Assert.assertNull(new OrganizationDAO().getById(organizationId));
    iconResponse = AuthedRestAccess.get(getServiceURL() + "/icon/" + organizationId);
    assertResponseStatus(404, iconResponse);
    // assert related objects were deleted
    Assert.assertNull(applicationDAO.getById(application.getId()));
  }

  @Test
  public void testAddOrganization_Unlicensed() throws Exception {
    uninstallLicense();
    Organization organization = new Organization("OrganizationResourceTest");

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(402, response);
  }

  @Test
  public void testUpdateOrganization_Unlicensed() throws Exception {
    Organization organization = new Organization("OrganizationResourceTest");
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(200, response);
    organization = JsonHelpers.fromJson(response.getResponseBody(), Organization.class);
    tempEntity.register(organization);
    uninstallLicense();
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetAll_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(402, response);
  }

  @Test
  public void testGenerateIcon() throws Exception {
    String hashcode = "abababababababababab";
    String url = getGenerateIconServiceUrl(hashcode);
    String saasUrl = "rest/application/icon/generate/" + hashcode;
    setSaasResponseForURI(saasUrl, 200, loadDefaultIcon());
    Response response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    Assert.assertNotNull(response.getResponseBodyAsBytes());
  }

  private String getServiceURL() {
    return getRestBaseUrl() + OrganizationResource.SERVICE_PATH;
  }

  private String getGenerateIconServiceUrl(String hashcode) {
    return getServiceURL() + "/" + OrganizationResource.GENERATE_ICON_PATH.replace("{hashcode}", hashcode);
  }

  private String getGetIconServiceUrl(String organizationId) {
    return getServiceURL() + "/" + OrganizationResource.GET_ICON_PATH.replace("{organizationId}", organizationId);
  }

  private String getSetIconServiceUrl() {
    return getServiceURL() + "/" + OrganizationResource.ICON_PATH;
  }

  private byte[] loadDefaultIcon() throws IOException {
    return IconUtils.loadIconFromProductAssets("defaulticon_organization.png");
  }
}

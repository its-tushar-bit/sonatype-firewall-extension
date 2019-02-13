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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(OrganizationResource.RESOURCE_PATH);
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    Organization organization = new Organization("OrganizationResourceTest");

    HttpResponse response = restRequest().body(organization).post();
    assertResponseStatus(200, response);
    organization = response.getBody(Organization.class);
    assertThat(organization).isNotNull();
    assertThat(organization.getId()).isNotNull();
    tempEntity.register(organization);
    assertThat(organization.getName()).isEqualTo("OrganizationResourceTest");
    String organizationId = organization.getId();

    // Get
    response = restRequest().get();
    assertResponseStatus(200, response);
    Organization[] organizations = response.getBody(Organization[].class);
    // One that was saved and one for the root org
    assertThat(organizations).hasSize(2);
    organization = organizations[0];
    assertThat(organization).isNotNull();
    if (Organization.ROOT_ORGANIZATION_ID.equals(organization.getId())) {
      organization = organizations[1];
    }
    assertThat(organization.getId()).isEqualTo(organizationId);
    assertThat(organization.getName()).isEqualTo("OrganizationResourceTest");

    // Add invalid icon
    byte[] defaultIconByteArray = IconUtils.loadInvalidIcon();
    response = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organizationId)
        .part("hasRobotSource", "false").part("file", "defaulticon_organization.png", defaultIconByteArray).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("defaulticon_organization.png is not a valid image."
        + " Make sure the image is in PNG, JPEG, GIF, BMP, or WBMP format.");

    // Get icon (default icon)
    HttpResponse iconResponse = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organizationId).get();
    assertResponseStatus(307, iconResponse);
    assertThat(iconResponse.getHeader("Location"))
        .isEqualTo(getRestBaseUrl() + "assets/img/defaulticon_organization.png");

    // Get icon (default Root Org icon)
    iconResponse = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(Organization.ROOT_ORGANIZATION_ID).get();
    assertResponseStatus(307, iconResponse);
    assertThat(iconResponse.getHeader("Location")).isEqualTo(getRestBaseUrl() + "assets/img/defaulticon_root_org.png");

    // Add icon
    defaultIconByteArray = loadDefaultIcon();
    response = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organizationId)
        .part("hasRobotSource", "false").part("file", "defaulticon_organization.png", defaultIconByteArray).post();
    assertResponseStatus(200, response);

    // Get icon
    iconResponse = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organizationId).get();
    assertResponseStatus(200, iconResponse);
    BufferedImage icon;
    try (InputStream iconStream = iconResponse.getBodyStream()) {
      icon = ImageIO.read(iconStream);
    }
    assertThat(icon).isNotNull();
    assertThat(icon.getHeight()).isEqualTo(420);
    assertThat(icon.getWidth()).isEqualTo(420);

    // Update
    organization.setName("OrganizationResourceTest updated");
    response = restRequest().body(organization).put();
    assertResponseStatus(200, response);
    organization = response.getBody(Organization.class);
    assertThat(organization).isNotNull();
    assertThat(organization.getId()).isEqualTo(organizationId);
    assertThat(organization.getName()).isEqualTo("OrganizationResourceTest updated");

    // Update icon
    response = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organizationId)
        .part("hasRobotSource", "false").post();
    assertResponseStatus(200, response);

    // now create related objects to test delete cascades

    // application
    final ApplicationDAO applicationDAO = new ApplicationDAO();
    final Application application = new Application("testapp", "testapp", organization.getId());
    applicationDAO.insert(application);

    // Delete
    response = restRequest().path(organizationId).delete();
    assertResponseStatus(204, response);
    assertThat(new OrganizationDAO().getById(organizationId)).isNull();
    iconResponse = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organizationId).get();
    assertResponseStatus(404, iconResponse);
    // assert related objects were deleted
    assertThat(applicationDAO.getById(application.getId())).isNull();
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
    uninstallLicense();
    Organization organization = tempEntity.newOrganization();

    HttpResponse response = restRequest().body(organization).put();
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
    HttpResponse response = restRequest().path(OrganizationResource.GENERATE_ICON_PATH).parameter("hash").get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyBytes()).isNotNull();
  }

  private byte[] loadDefaultIcon() throws IOException {
    return IconUtils.loadIconFromProductAssets("defaulticon_organization.png");
  }
}

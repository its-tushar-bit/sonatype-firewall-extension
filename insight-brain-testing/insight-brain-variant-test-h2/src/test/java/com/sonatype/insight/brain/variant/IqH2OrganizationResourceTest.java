/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import jakarta.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationError;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.IconUtils;
import com.sonatype.insight.brain.organization.OrganizationResource;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2OrganizationResourceTest
{
  private IqTestContext ctx;

  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  private ApplicationDAO applicationDAO;

  private OrganizationDAO organizationDAO;

  @BeforeEach
  void setUp() {
    automaticApplicationsConfigurationDAO = ctx.lookup(AutomaticApplicationsConfigurationDAO.class);
    applicationDAO = ctx.lookup(ApplicationDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(OrganizationResource.RESOURCE_PATH);
  }

  @Test
  void testCRUD() throws Exception {
    // Create
    Organization organization = new Organization("OrganizationResourceTest");

    HttpResponse response = restRequest().body(organization).post();
    ctx.assertResponseStatus(200, response);
    organization = response.getBody(Organization.class);
    assertThat(organization).isNotNull();
    assertThat(organization.getId()).isNotNull();
    assertThat(organization.getName()).isEqualTo("OrganizationResourceTest");
    String organizationId = organization.getId();

    // Get
    response = restRequest().get();
    ctx.assertResponseStatus(200, response);
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
    response = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organizationId)
        .part("hasRobotSource", "false")
        .part("file", "defaulticon_organization.png", defaultIconByteArray)
        .post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("defaulticon_organization.png is not a valid image."
        + " Make sure the image is in PNG, JPEG, GIF, BMP, or WBMP format.");

    // Get icon (default icon)
    HttpResponse iconResponse = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organizationId)
        .get();
    ctx.assertResponseStatus(307, iconResponse);
    String normalizedRestBaseUrl = ctx.restBaseUrl().replaceFirst("/+$", "");
    assertThat(iconResponse.getHeader("Location"))
        .isEqualTo(normalizedRestBaseUrl + "/assets/img/defaulticon_organization.png");

    // Get icon (default Root Org icon)
    iconResponse = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(Organization.ROOT_ORGANIZATION_ID)
        .get();
    ctx.assertResponseStatus(307, iconResponse);
    assertThat(iconResponse.getHeader("Location"))
        .isEqualTo(normalizedRestBaseUrl + "/assets/img/defaulticon_root_org.png");

    // Add icon
    defaultIconByteArray = loadDefaultIcon();
    response = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organizationId)
        .part("hasRobotSource", "false")
        .part("file", "defaulticon_organization.png", defaultIconByteArray)
        .post();
    ctx.assertResponseStatus(200, response);

    // Get icon
    iconResponse = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organizationId).get();
    ctx.assertResponseStatus(200, iconResponse);
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
    ctx.assertResponseStatus(200, response);
    organization = response.getBody(Organization.class);
    assertThat(organization).isNotNull();
    assertThat(organization.getId()).isEqualTo(organizationId);
    assertThat(organization.getName()).isEqualTo("OrganizationResourceTest updated");

    // Update icon
    response = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organizationId)
        .part("hasRobotSource", "false")
        .post();
    ctx.assertResponseStatus(200, response);

    // now create related objects to test delete cascades

    // application
    final Application application = new Application("testapp", "testapp", organization.getId());
    applicationDAO.insert(application);

    // Delete
    response = restRequest().path(organizationId).delete();
    ctx.assertResponseStatus(204, response);
    assertThat(organizationDAO.getById(organizationId)).isNull();
    iconResponse = restRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organizationId).get();
    ctx.assertResponseStatus(404, iconResponse);
    // assert related objects were deleted
    assertThat(applicationDAO.getById(application.getId())).isNull();
  }

  @Test
  void testDeleteOrganization_NLevel() throws Exception {
    List<Organization> testList = ctx.tempEntity().newRelatedOrganizationsAsList(1, 8, 2);
    HttpResponse response;

    // Delete
    response = restRequest().path(testList.get(testList.size() - 2).getId()).delete();
    ctx.assertResponseStatus(204, response);

    List<Organization> removedOrgs = testList.subList(0, testList.size() - 2);
    for (Organization org : removedOrgs) {
      String orgId = org.getId();
      assertThat(organizationDAO.getById(orgId)).isNull();
    }
  }

  @Test
  void testDeleteOrganization_NLevel_partialDeletionExceptionMessage() throws Exception {
    List<Organization> testList = ctx.tempEntity().newRelatedOrganizationsAsList(1, 7, 0);
    HttpResponse response;

    Organization organization = testList.get(4);
    String organizationId = organization.getId();
    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId(organizationId);

    // Delete
    response = restRequest().path(organizationId).delete();
    ctx.assertResponseStatus(500, response);

    String expectedErrorResponse = "The delete operation was partially successful." +
        " Some sub-Orgs and applications of this Org were deleted," +
        " while some failed with error(s) below." +
        "\n" + "Cannot delete the parent organization for automatic application creation: " +
        organization.getName() + ".";
    assertThat(StringUtils.contains(response.getBodyText(), expectedErrorResponse)).isTrue();
  }

  @Test
  void testDeleteOrganization_NLevel_originalExceptionMessage() throws Exception {
    List<Organization> testList = ctx.tempEntity().newRelatedOrganizationsAsList(1, 7, 0);
    HttpResponse response;

    Organization organization = testList.get(0);
    String organizationId = organization.getId();
    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId(organizationId);

    // Delete
    response = restRequest().path(organizationId).delete();
    ctx.assertResponseStatus(400, response);

    String expectedErrorResponse = "Cannot delete the parent organization for automatic application creation: " +
        organization.getName() + ".";
    assertThat(StringUtils.contains(response.getBodyText(), expectedErrorResponse)).isTrue();
  }

  @Test
  void testAddOrganization_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    Organization organization = new Organization("OrganizationResourceTest");

    HttpResponse response = restRequest().body(organization).post();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testUpdateOrganization_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    Organization organization = ctx.tempEntity().newOrganization();

    HttpResponse response = restRequest().body(organization).put();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testGetAll_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testGetOrganization() throws Exception {
    Organization testOrg = ctx.tempEntity().newOrganization();
    HttpResponse response = restRequest().path(OrganizationResource.GET_ORGANIZATION_PATH)
        .parameter(testOrg.getId())
        .get();
    ctx.assertResponseStatus(200, response);

    Organization responseOrg = response.getBody(Organization.class);
    assertThat(responseOrg).isNotNull();
    assertThat(responseOrg.getName()).isEqualTo(testOrg.getName());
    assertThat(responseOrg.getId()).isEqualTo(testOrg.getId());
  }

  @Test
  void testGetOrganization_idDoesNotExist() throws Exception {
    HttpResponse response = restRequest().path(OrganizationResource.GET_ORGANIZATION_PATH)
        .parameter("ID_IS_NOT_REAL")
        .get();
    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testGenerateIcon() throws Exception {
    HttpResponse response = restRequest().path(OrganizationResource.GENERATE_ICON_PATH).parameter("hash").get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyBytes()).isNotNull();
  }

  @Test
  void testMoveOrganizationErrorsExport() throws Exception {
    List<Organization> organizations = ctx.tempEntity().newRelatedOrganizationsAsList(1, 3, 0);
    Organization organization = ctx.tempEntity().newOrganization();
    Application application = ctx.tempEntity().newApplication(organizations.get(0).getId());

    // create a bunch of validation errors to assert in csv
    // tags
    Tag tag1 = ctx.tempEntity().newTag(organizations.get(1).getId());
    Tag tag2 = ctx.tempEntity().newTag(organizations.get(1).getId());
    ctx.tempEntity().newApplicationTag(application.getId(), tag1.getId());
    ctx.tempEntity().newApplicationTag(application.getId(), tag2.getId());

    // labels
    Label label1 = ctx.tempEntity().newLabel(organizations.get(1).getId());
    Label label2 = ctx.tempEntity().newLabel(organizations.get(1).getId());
    Condition condition1 = new Condition(LabelConditionType.ID, "is", label1.getId());
    ctx.tempEntity().newPolicy(application.getId(), "Policy 1", condition1);
    Condition condition2 = new Condition(LabelConditionType.ID, "is", label2.getId());
    ctx.tempEntity().newPolicy(application.getId(), "Policy 2", condition2);

    // ltg
    LicenseThreatGroup ltg = ctx.tempEntity().newLicenseThreatGroup(organizations.get(1).getId());
    Condition ltgCondition = new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId());
    ctx.tempEntity().newPolicy(application.getId(), "PolName", ltgCondition);

    HttpResponse response = restRequest()
        .path(OrganizationResource.MOVE_ORGANIZATION_ERRORS_EXPORT_PATH)
        .parameter(organizations.get(0).getId(), organization.getId())
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).startsWith("text/csv");
    String dispositionHeader = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
    String headerStart = "attachment; filename=\"move_organization_errors";
    assertThat(dispositionHeader).startsWith(headerStart);

    String[] lines = response.getBodyText().split("\r\n");

    String expectedFirstLine = String.format("TAG," + "\"Missing application categories for new parent org %s: %s,%s\"",
        organization.getName(), tag2.getName(), tag1.getName());
    String expectedFirstLineOrderAlter =
        String.format("TAG," + "\"Missing application categories for new parent org %s: %s,%s\"",
            organization.getName(), tag1.getName(), tag2.getName());

    String expectedSecondLine = String.format("LABEL," + "\"Missing labels for new parent org %s: %s,%s\"",
        organization.getName(), label2.getLabel(), label1.getLabel());
    String expectedSecondLineOrderAlter = String.format("LABEL," + "\"Missing labels for new parent org %s: %s,%s\"",
        organization.getName(), label1.getLabel(), label2.getLabel());

    String expectedThirdLine =
        String.format("LICENSE_THREAT_GROUP," + "Missing license threat groups for new parent org %s: %s",
            organization.getName(), ltg.getName());

    // As the variables used to build messages are contained in a set, their order is not stable
    // We check there is at least one instance of the message, with the values in whatever order
    assertThat(lines).hasSize(4);
    assertThat(lines[0]).isEqualTo(ValidationError.getCsvHeader());
    assertThat(lines).containsAnyElementsOf(Arrays.asList(expectedFirstLine, expectedFirstLineOrderAlter));
    assertThat(lines).containsAnyElementsOf(Arrays.asList(expectedSecondLine, expectedSecondLineOrderAlter));
    assertThat(lines).containsOnlyOnce(expectedThirdLine);
  }

  @Test
  void testMoveOrganizationErrorsExport_Unlicensed() throws Exception {
    ctx.uninstallLicense();

    HttpResponse response = restRequest()
        .path(OrganizationResource.MOVE_ORGANIZATION_ERRORS_EXPORT_PATH)
        .parameter("org-id-does-not-matter", "destination-org-id-does-not-matter")
        .query("destinationId", "destination-org-id-does-not-matter")
        .get();
    ctx.assertResponseStatus(HttpStatus.SC_PAYMENT_REQUIRED, response);
  }

  private byte[] loadDefaultIcon() throws IOException {
    return IconUtils.loadIconFromProductAssets("defaulticon_organization.png");
  }
}

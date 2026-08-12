/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.imageio.ImageIO;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class ApplicationResourceTest
{
  private IqTestContext ctx;

  private ApplicationDAO applicationDAO;

  @BeforeEach
  void setUp() {
    applicationDAO = ctx.lookup(ApplicationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ApplicationResource.RESOURCE_PATH);
  }

  private HttpRequest evalRequest(String appId, String scanId, Stage stage) {
    return ctx.restRequest()
        .path(PolicyEvaluateResource.RESOURCE_PATH)
        .query("scanId", scanId)
        .parameter(appId)
        .body(stage);
  }

  @Test
  void testValidate() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-testValidate-AppId";
    Application application = applicationDAO.getByPublicId(applicationPublicId);
    assertThat(application).isNull();

    application = ctx.tempEntity()
        .newApplicationWithParent(applicationPublicId,
            "ApplicationResourceTest-testValidate-AppName", "ApplicationResourceTest-testValidate-OrgName");

    HttpResponse response = restRequest().path(ApplicationResource.VALIDATE_PATH).parameter(applicationPublicId).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isEqualTo("OK");

    applicationDAO.delete(application);

    // validate service always returns 200, the actual result is in the response body
    response = restRequest().path(ApplicationResource.VALIDATE_PATH).parameter(applicationPublicId).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid application ID " + applicationPublicId + ".");
  }

  @Test
  void testCRUD() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    Organization organization = ctx.tempEntity().newOrganization("ApplicationResourceTest");

    // Test Add Application
    Application application = new Application(applicationPublicId, applicationName, organization.getId());
    application.setContactInternalName("admin");

    HttpResponse response = restRequest().body(application).post();

    ctx.assertResponseStatus(200, response);

    ApplicationDTO applicationResult = response.getBody(ApplicationDTO.class);

    application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    assertThat(applicationResult.getId()).isEqualTo(application.getId());
    assertThat(applicationResult.getPublicId()).isEqualTo(applicationPublicId);
    assertThat(applicationResult.getName()).isEqualTo(applicationName);
    assertThat(applicationResult.getOrganizationId()).isEqualTo(application.getOrganizationId());
    assertThat(applicationResult.getOrganizationName()).isEqualTo(organization.getName());

    ContactDTO expectedContact = new ContactDTO("admin", "Admin BuiltIn", "admin@localhost", "IQ Server");
    ContactDTO contact = applicationResult.getContact();
    assertContact(contact, expectedContact);

    // Test Add Invalid Icon
    byte[] defaultIconByteArray = IconUtils.loadInvalidIcon();
    response = restRequest().path(ApplicationResource.SET_APPLICATION_ICON_PATH)
        .parameter(application.getId())
        .part("hasRobotSource", "false")
        .part("file", "defaulticon_application.png", defaultIconByteArray)
        .post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("defaulticon_application.png is not a valid image."
        + " Make sure the image is in PNG, JPEG, GIF, BMP, or WBMP format.");

    // Test Get Icon (default icon)
    HttpResponse iconResponse = restRequest().path(ApplicationResource.GET_APPLICATION_ICON_PATH)
        .parameter(applicationPublicId)
        .get();
    ctx.assertResponseStatus(307, iconResponse);
    String expectedDefaultIconLocation =
        ctx.restBaseUrl().replaceFirst("/+$", "") + "/assets/img/defaulticon_application.png";
    assertThat(iconResponse.getHeader("Location")).isEqualTo(expectedDefaultIconLocation);

    // Test Add Application Icon
    defaultIconByteArray = loadDefaultIcon();
    response = restRequest().path(ApplicationResource.SET_APPLICATION_ICON_PATH)
        .parameter(application.getId())
        .part("hasRobotSource", "false")
        .part("file", "defaulticon_application.png", defaultIconByteArray)
        .post();
    ctx.assertResponseStatus(200, response);

    // Test Get Icon (from added application)
    iconResponse = restRequest().path(ApplicationResource.GET_APPLICATION_ICON_PATH)
        .parameter(applicationPublicId)
        .get();
    testValidIconResponse(iconResponse);

    // Test application update
    application.setName(applicationName + "updated");
    response = restRequest().body(application).put();
    ctx.assertResponseStatus(200, response);
    applicationResult = response.getBody(ApplicationDTO.class);
    assertThat(applicationResult.getId()).isEqualTo(application.getId());
    assertThat(applicationResult.getPublicId()).isEqualTo(applicationPublicId);
    assertThat(applicationResult.getName()).isEqualTo(applicationName + "updated");

    // Test icon update
    response = restRequest().path(ApplicationResource.SET_APPLICATION_ICON_PATH)
        .parameter(application.getId())
        .part("hasRobotSource", "false")
        .post();
    ctx.assertResponseStatus(200, response);

    // Verify invalid name fails
    application.setName("Invalid Name !!!!!");

    response = restRequest().body(application).put();

    ctx.assertResponseStatus(400, response);

    // Test delete
    response = restRequest().path(applicationPublicId).delete();
    ctx.assertResponseStatus(204, response);
    application = applicationDAO.getByPublicId(applicationPublicId);
    assertThat(application).isNull();
    iconResponse = restRequest().path(ApplicationResource.GET_APPLICATION_ICON_PATH)
        .parameter(applicationPublicId)
        .get();
    ctx.assertResponseStatus(404, iconResponse);
  }

  private byte[] loadDefaultIcon() throws IOException {
    return IconUtils.loadIconFromProductAssets("defaulticon_application.png");
  }

  private void testValidIconResponse(HttpResponse iconResponse) throws Exception {
    ctx.assertResponseStatus(200, iconResponse);
    assertThat(iconResponse.getBodyBytes()).isNotEmpty();
    BufferedImage icon;
    try (InputStream iconStream = iconResponse.getBodyStream()) {
      icon = ImageIO.read(iconStream);
    }
    assertThat(icon).isNotNull();
    assertThat(icon.getHeight()).isPositive();
  }

  @Test
  void testAddApplication_exceedsLicense() throws Exception {
    ((com.sonatype.insight.brain.product.license.TestProductLicense) ctx.lookup(
        com.sonatype.insight.brain.product.license.ProductLicense.class)).setMaxApplications(1);

    ctx.tempEntity().newApplicationWithParent("testAddApplication_exceedsLicense_id");

    // Test Add Application, which should fail with 402 since we exceeded the limit
    Application application = new Application();
    application.setName("testAddApplication_exceedsLicense_id_new_name");
    application.setPublicId("testAddApplication_exceedsLicense_id_new_id");

    HttpResponse response = restRequest().body(application).post();
    ctx.assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo("You have exceeded the licensed limit of 1 applications.");
  }

  @Test
  void testGetApplications() throws Exception {
    // Create an application
    final String applicationPublicId = "ApplicationResourceTest-getApplicationsTest-AppId";
    final String applicationName = "ApplicationResourceTest-getApplicationsTest-Name";

    Application application = ctx.tempEntity().newApplicationWithParent(applicationPublicId, applicationName);

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    ApplicationDTO[] applications = response.getBody(ApplicationDTO[].class);

    assertThat(applications).hasSize(1);
    assertThat(applications[0].getId()).isEqualTo(application.getId());
    assertThat(applications[0].getName()).isEqualTo(application.getName());

    // Test GetApplication
    response = restRequest().path(ApplicationResource.GET_APPLICATION_PATH).parameter(applicationPublicId).get();
    ctx.assertResponseStatus(200, response);

    ApplicationDTO applicationSummary = response.getBody(ApplicationDTO.class);
    assertThat(applicationSummary).isNotNull();
    assertThat(applicationSummary.getId()).isEqualTo(application.getId());
    assertThat(applicationSummary.getName()).isEqualTo(application.getName());
  }

  @Test
  void testGetApplications_excludeFirewallForDocker() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();

    // Create an app with both a related repository manager and repository
    Organization orgWithRelatedRepo = ctx.tempEntity().newOrganizationWithRepositoryManager("org-with-repo");
    ctx.tempEntity().newApplication(orgWithRelatedRepo.getId());

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    ApplicationDTO[] applications = response.getBody(ApplicationDTO[].class);

    assertThat(applications).hasSize(1);
    assertThat(applications).extracting(ApplicationDTO::getId).containsExactly(application.getId());
  }

  private void assertApplicationManagementSummaryDTO(
      ApplicationManagementSummaryDTO actual,
      Application app,
      Organization org,
      int policyEvaluationCount)
  {
    assertThat(actual.getId()).isEqualTo(app.getId());
    assertThat(actual.getPublicId()).isEqualTo(app.getPublicId());
    assertThat(actual.getName()).isEqualTo(app.getName());
    assertThat(actual.getOrganizationId()).isEqualTo(org.getId());
    assertThat(actual.getOrganizationName()).isEqualTo(org.getName());
    assertThat(actual.getPolicyEvaluations()).hasSize(policyEvaluationCount);
  }

  @Test
  void testGetApplicationSummaries() throws Exception {
    // Create an application
    final String applicationPublicId = "ApplicationResourceTest-getApplicationsTest-AppId";
    final String applicationName = "ApplicationResourceTest-getApplicationsTest-Name";
    final String organizationName = "OrgName";

    Organization organization = ctx.tempEntity().newOrganization(organizationName);

    Application application =
        ctx.tempEntity().newApplication(applicationName, applicationPublicId, organization.getId());
    application.setContactInternalName("admin");
    applicationDAO.update(application);

    // Create policy
    ctx.tempEntity().newPolicy(application);
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    ctx.createScanFile(application.getId(), scanId1);
    ctx.createScanFile(application.getId(), scanId2);
    ctx.createScanFile(application.getId(), scanId3);
    ctx.mockReport(scanId1, "/" + getClass().getSimpleName() + "/report");
    ctx.mockReport(scanId2, "/" + getClass().getSimpleName() + "/report");
    ctx.mockReport(scanId3, "/" + getClass().getSimpleName() + "/report");

    // Eval policy
    HttpResponse response = evalRequest(applicationPublicId, scanId1, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);
    response = evalRequest(applicationPublicId, scanId3, new Stage(Stage.ID_RELEASE)).post();
    ctx.assertResponseStatus(200, response);
    response = evalRequest(applicationPublicId, scanId2, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);

    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES)
        .query("page", "1")
        .query("pageSize", "50")
        .get();
    ctx.assertResponseStatus(200, response);

    ApplicationManagementSummaryDTO[] applications = response.getBody(ApplicationManagementSummaryDTO[].class);

    assertThat(applications).hasSize(1);
    assertApplicationManagementSummaryDTO(applications[0], application, organization, 2);

    Map<String, com.sonatype.insight.brain.model.policy.PolicyEvaluation> policyEvaluations = applications[0]
        .getPolicyEvaluations();
    String[] stageTypeIds = policyEvaluations.keySet().toArray(new String[0]);

    assertThat(policyEvaluations).hasSize(2);
    assertThat(stageTypeIds[0]).isEqualTo(Stage.ID_BUILD);
    assertThat(policyEvaluations.get(stageTypeIds[0]).getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(policyEvaluations.get(stageTypeIds[0]).getScanId()).isEqualTo(scanId2);
    assertThat(stageTypeIds[1]).isEqualTo(Stage.ID_RELEASE);
    assertThat(policyEvaluations.get(stageTypeIds[1]).getStageTypeId()).isEqualTo(Stage.ID_RELEASE);
    assertThat(policyEvaluations.get(stageTypeIds[1]).getScanId()).isEqualTo(scanId3);

    Map<String, PolicyEvaluationResult> policyEvaluationsResults = applications[0].getPolicyEvaluationsResults();
    stageTypeIds = policyEvaluationsResults.keySet().toArray(new String[0]);

    assertThat(policyEvaluationsResults).hasSize(2);
    assertThat(stageTypeIds[0]).isEqualTo(Stage.ID_BUILD);
    assertThat(policyEvaluationsResults.get(stageTypeIds[0]).getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationsResults.get(stageTypeIds[0]).getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationsResults.get(stageTypeIds[0]).getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationsResults.get(stageTypeIds[0]).getSevereComponentCount()).isEqualTo(7);
    assertThat(stageTypeIds[1]).isEqualTo(Stage.ID_RELEASE);
    assertThat(policyEvaluationsResults.get(stageTypeIds[1]).getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationsResults.get(stageTypeIds[1]).getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationsResults.get(stageTypeIds[1]).getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationsResults.get(stageTypeIds[1]).getSevereComponentCount()).isEqualTo(7);

    // Scans count
    makeScanReceipt();

    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES)
        .query("page", "1")
        .query("pageSize", "50")
        .get();
    ctx.assertResponseStatus(200, response);

    applications = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(applications).isNotNull();

    // Test GetApplication
    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARY)
        .parameter(applicationPublicId)
        .get();
    ctx.assertResponseStatus(200, response);

    ApplicationManagementSummaryDTO applicationSummary = response.getBody(ApplicationManagementSummaryDTO.class);
    assertApplicationManagementSummaryDTO(applicationSummary, application, organization, 2);

    policyEvaluations = applicationSummary.getPolicyEvaluations();
    stageTypeIds = policyEvaluations.keySet().toArray(new String[0]);

    assertThat(policyEvaluations).hasSize(2);
    assertThat(stageTypeIds[0]).isEqualTo(Stage.ID_BUILD);
    assertThat(policyEvaluations.get(stageTypeIds[0]).getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(applications[0].getPolicyEvaluations().get(stageTypeIds[0]).getScanId()).isEqualTo(scanId2);
    assertThat(stageTypeIds[1]).isEqualTo(Stage.ID_RELEASE);
    assertThat(policyEvaluations.get(stageTypeIds[1]).getStageTypeId()).isEqualTo(Stage.ID_RELEASE);
    assertThat(applications[0].getPolicyEvaluations().get(stageTypeIds[1]).getScanId()).isEqualTo(scanId3);

    policyEvaluationsResults = applicationSummary.getPolicyEvaluationsResults();
    stageTypeIds = policyEvaluationsResults.keySet().toArray(new String[0]);

    assertThat(policyEvaluationsResults).isEmpty();
  }

  @Test
  void testGetApplicationSummaries_QueryParameters() throws Exception {
    Organization org = ctx.tempEntity().newOrganization("org");
    Application app1 = ctx.tempEntity().newApplication("app1", "publicId1", org.getId());
    Application app2 = ctx.tempEntity().newApplication("app2", "publicId2", org.getId());

    // Uses default params for name filter and order
    HttpResponse response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES)
        .query("page", "1")
        .query("pageSize", "2")
        .get();
    ctx.assertResponseStatus(200, response);
    ApplicationManagementSummaryDTO[] dtos = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(dtos).extracting(ApplicationManagementSummaryDTO::getName)
        .containsExactly(app1.getName(), app2.getName());

    // Uses given name filter
    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES)
        .query("nameFilter", "app2")
        .query("page", "1")
        .query("pageSize", "2")
        .get();
    ctx.assertResponseStatus(200, response);
    dtos = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(dtos).extracting(ApplicationManagementSummaryDTO::getName).containsExactly(app2.getName());

    // Uses given order
    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES)
        .query("order", "APP_NAME_DESC")
        .query("page", "1")
        .query("pageSize", "2")
        .get();
    ctx.assertResponseStatus(200, response);
    dtos = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(dtos).extracting(ApplicationManagementSummaryDTO::getName)
        .containsExactly(app2.getName(), app1.getName());

    Application app3 = ctx.tempEntity().newApplication("app3", "publicId3", org.getId());

    // Uses given page
    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES)
        .query("page", "2")
        .query("pageSize", "2")
        .get();
    ctx.assertResponseStatus(200, response);
    dtos = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(dtos).extracting(ApplicationManagementSummaryDTO::getName).containsExactly(app3.getName());

    // Uses given page size
    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES)
        .query("page", "1")
        .query("pageSize", "3")
        .get();
    ctx.assertResponseStatus(200, response);
    dtos = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(dtos).extracting(ApplicationManagementSummaryDTO::getName)
        .containsExactly(app1.getName(), app2.getName(), app3.getName());
  }

  @Test
  void testGetApplications_DoesNotContactHdsAndPotentiallyBlockToGetLastPolicyAlerts() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-AppId";
    final String applicationName = "ApplicationResourceTest-Name";
    final String appId = ctx.tempEntity().newApplicationWithParent(applicationPublicId, applicationName).getId();
    final String scanId = "ApplicationResourceTest-ScanId";

    // create eval log entry pointing at missing report
    ctx.tempEntity().newPolicyEvaluation(appId, Stage.ID_BUILD, scanId);
    ctx.hdsRespondWith("Not Found").andStatus(404).atUri("/rest/ci/report?scanId=" + scanId);

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    ApplicationManagementSummaryDTO[] applications = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(applications).hasSize(1);
  }

  @Test
  void testGetApplicationNames() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-getApplicationNamesTest-AppId";
    final String applicationName = "ApplicationResourceTest-getApplicationNamesTest-Name";
    ctx.tempEntity().newApplicationWithParent(applicationPublicId, applicationName);

    HttpResponse response = restRequest().path(ApplicationResource.GET_APPLICATION_NAMES).get();
    ctx.assertResponseStatus(200, response);

    @SuppressWarnings("unchecked")
    Map<String, String> applicationNames = response.getBody(Map.class);
    assertThat(applicationNames).hasSize(1).containsEntry(applicationPublicId, applicationName);
  }

  @Test
  void testGenerateIcon() throws Exception {
    HttpResponse response = restRequest().path(ApplicationResource.GENERATE_ICON_PATH).parameter("hash").get();
    testValidIconResponse(response);
  }

  @Test
  void testGetApplicationByPublicIdForLegalReviewer() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();

    HttpResponse response = restRequest()
        .path(ApplicationResource.GET_APPLICATION_LEGAL_REVIEWER_PATH)
        .parameter(application.getPublicId())
        .get();
    ctx.assertResponseStatus(200, response);
    ApplicationDTO dto = response.getBody(ApplicationDTO.class);

    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(application.getId());
    assertThat(dto.getPublicId()).isEqualTo(application.getPublicId());
  }

  @Test
  void testGetLatestReportInformation() throws Exception {
    final Application application = ctx.tempEntity().newApplicationWithParent();

    final HttpResponse response = restRequest()
        .path(ApplicationResource.GET_LATEST_REPORT_INFO_PATH)
        .parameter(application.getPublicId(), "build")
        .get();

    ctx.assertResponseStatus(200, response);

    final com.sonatype.insight.brain.organization.LatestReportInformation latestReportInformation =
        response.getBody(com.sonatype.insight.brain.organization.LatestReportInformation.class);
    assertThat(latestReportInformation).isNotNull();
    assertThat(latestReportInformation)
        .isEqualTo(new com.sonatype.insight.brain.organization.LatestReportInformation(null, false));
  }

  private void makeScanReceipt() {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44d");
    scanReceipt.setTimeToReport(30L);
    ctx.hdsRespondWith(scanReceipt).atUri(com.sonatype.insight.brain.hds.ScanUploader.HDS_PATH);
  }

  private void assertContact(ContactDTO actualContact, ContactDTO expectedContact) {
    assertThat(actualContact).isNotNull();
    assertThat(actualContact.getInternalName()).isEqualTo(expectedContact.getInternalName());
    assertThat(actualContact.getDisplayName()).isEqualTo(expectedContact.getDisplayName());
    assertThat(actualContact.getEmail()).isEqualTo(expectedContact.getEmail());
    assertThat(actualContact.getRealm()).isEqualTo(expectedContact.getRealm());
  }
}

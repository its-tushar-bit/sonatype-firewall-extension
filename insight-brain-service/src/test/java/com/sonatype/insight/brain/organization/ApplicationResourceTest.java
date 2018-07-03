/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import javax.imageio.ImageIO;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.CIResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApplicationResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ApplicationResource.RESOURCE_PATH);
  }

  private HttpRequest evalRequest(String appId, String scanId, Stage stage) {
    return super.restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).query("scanId", scanId).parameter(appId)
        .body(stage);
  }

  @Test
  public void testValidate() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-testValidate-AppId";
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = applicationDAO.getByPublicId(applicationPublicId);
    Assert.assertNull(application);

    application = tempEntity.newApplicationWithParent(applicationPublicId,
        "ApplicationResourceTest-testValidate-AppName", "ApplicationResourceTest-testValidate-OrgName");

    HttpResponse response = restRequest().path(ApplicationResource.VALIDATE_PATH).parameter(applicationPublicId).get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText(), equalTo("OK"));

    applicationDAO.delete(application);

    // validate service always returns 200, the actual result is in the response body
    response = restRequest().path(ApplicationResource.VALIDATE_PATH).parameter(applicationPublicId).get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText(), equalTo("Invalid application ID " + applicationPublicId + "."));
  }

  @Test
  public void testValidate_Anonymous() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-testValidate-AppId";
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = applicationDAO.getByPublicId(applicationPublicId);
    Assert.assertNull(application);

    application = tempEntity.newApplicationWithParent(applicationPublicId,
        "ApplicationResourceTest-testValidate-AppName");

    HttpResponse response = restRequest().path(ApplicationResource.VALIDATE_PATH).parameter(applicationPublicId).get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText(), equalTo("OK"));

    applicationDAO.delete(application);

    // validate service always returns 200, the actual result is in the response body
    response = restRequest().path(ApplicationResource.VALIDATE_PATH).parameter(applicationPublicId).get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText(), equalTo("Invalid application ID " + applicationPublicId + "."));
  }

  @Test
  public void testCRUD() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    Organization organization = tempEntity.newOrganization("ApplicationResourceTest");

    // Test Add Application
    Application application = new Application(applicationPublicId, applicationName, organization.getId());
    application.setContactInternalName("admin");

    HttpResponse response = restRequest().body(application).post();

    assertResponseStatus(200, response);

    ApplicationDTO applicationResult = response.getBody(ApplicationDTO.class);

    ApplicationDAO applicationDAO = new ApplicationDAO();
    application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    tempEntity.register(application);

    Assert.assertNotNull(application);
    Assert.assertEquals(application.getId(), applicationResult.getId());
    Assert.assertEquals(applicationPublicId, applicationResult.getPublicId());
    Assert.assertEquals(applicationName, applicationResult.getName());
    Assert.assertEquals(application.getOrganizationId(), applicationResult.getOrganizationId());
    Assert.assertEquals(organization.getName(), applicationResult.getOrganizationName());

    ContactDTO expectedContact = new ContactDTO("admin", "Admin BuiltIn", "admin@localhost", "IQ Server");
    ContactDTO contact = applicationResult.getContact();
    assertContact(contact, expectedContact);

    // Test Add Invalid Icon
    byte[] defaultIconByteArray = IconUtils.loadInvalidIcon();
    response = restRequest().path(ApplicationResource.ICON_PATH).part("applicationId", application.getId())
        .part("hasRobotSource", "false").part("file", "defaulticon_application.png", defaultIconByteArray).post();
    assertResponseStatus(400, response);
    Assert
        .assertEquals(
            "defaulticon_application.png is not a valid image. Make sure the image is in PNG, JPEG, GIF, BMP, or WBMP format.",
            response.getBodyText());

    // Test Get Icon (default icon)
    HttpResponse iconResponse = restRequest().path(ApplicationResource.GET_APPLICATION_ICON_PATH)
        .parameter(applicationPublicId).get();
    assertResponseStatus(307, iconResponse);
    Assert
        .assertEquals(getRestBaseUrl() + "assets/img/defaulticon_application.png", iconResponse.getHeader("Location"));

    // Test Add Application Icon
    defaultIconByteArray = loadDefaultIcon();
    response = restRequest().path(ApplicationResource.ICON_PATH).part("applicationId", application.getId())
        .part("hasRobotSource", "false").part("file", "defaulticon_application.png", defaultIconByteArray).post();
    assertResponseStatus(200, response);

    // Test Get Icon (from added application)
    iconResponse = restRequest().path(ApplicationResource.GET_APPLICATION_ICON_PATH).parameter(applicationPublicId)
        .get();
    testValidIconResponse(iconResponse);

    // Test application update
    application.setName(applicationName + "updated");
    response = restRequest().body(application).put();
    assertResponseStatus(200, response);
    applicationResult = response.getBody(ApplicationDTO.class);
    Assert.assertEquals(application.getId(), applicationResult.getId());
    Assert.assertEquals(applicationPublicId, applicationResult.getPublicId());
    Assert.assertEquals(applicationName + "updated", applicationResult.getName());

    // Test icon update
    response = restRequest().path(ApplicationResource.ICON_PATH).part("applicationId", application.getId())
        .part("hasRobotSource", "false").post();
    assertResponseStatus(200, response);

    // Verify invalid name fails
    application.setName("Invalid Name !!!!!");

    response = restRequest().body(application).put();

    assertResponseStatus(400, response);

    // Test delete
    response = restRequest().path(applicationPublicId).delete();
    assertResponseStatus(204, response);
    application = applicationDAO.getByPublicId(applicationPublicId);
    Assert.assertNull(application);
    iconResponse = restRequest().path(ApplicationResource.GET_APPLICATION_ICON_PATH).parameter(applicationPublicId)
        .get();
    assertResponseStatus(404, iconResponse);
  }

  private byte[] loadDefaultIcon() throws IOException {
    return IconUtils.loadIconFromProductAssets("defaulticon_application.png");
  }

  private void testValidIconResponse(HttpResponse iconResponse) throws Exception {
    assertResponseStatus(200, iconResponse);
    Assert.assertNotNull(iconResponse.getBodyBytes());
    BufferedImage icon;
    try (InputStream iconStream = iconResponse.getBodyStream()) {
      icon = ImageIO.read(iconStream);
    }
    Assert.assertNotNull(icon);
    Assert.assertTrue(icon.getHeight() > 0);
  }

  @Test
  public void testDeleteApplicationWithData() throws Exception {
    final ApplicationDAO applicationDAO = new ApplicationDAO();
    final PolicyDAO policyDAO = new PolicyDAO();

    final String applicationPublicId = "testDeleteApplicationWithScan_PublicId";
    final String applicationName = "testDeleteApplicationWithScanAppName";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId, applicationName);

    final String licenseFingerprint = "testDeleteApplicationWithScan_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    makeScanReceipt();

    HttpResponse response = super.restRequest().path(CIResource.RESOURCE_PATH, CIResource.SCAN_PATH)
        .parameter(applicationPublicId).body("").put();

    assertResponseStatus(200, response);

    final String applicationId = application.getId();

    // TODO ideally, need to create these directories by calling into appropriate REST endpoints
    final InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(getCLMServer().getWorkDir().getAbsolutePath());
    final InsightWork insightWork = new InsightWork(insightConfig);
    createDirectory(insightWork.getScanDir(applicationId));
    createDirectory(insightWork.getAuditDir(applicationId));
    createDirectory(insightWork.getReportDir(applicationId));

    response = restRequest().path(applicationPublicId).delete();
    application = applicationDAO.getByPublicId(applicationPublicId);

    assertResponseStatus(204, response);
    Assert.assertNull(application);

    Assert.assertEquals(0, policyDAO.getByOwnerId(applicationId).size());
    Assert.assertFalse(insightWork.getScanDir(applicationId).exists());
    Assert.assertFalse(insightWork.getAuditDir(applicationId).exists());
    Assert.assertFalse(insightWork.getReportDir(applicationId).exists());
  }

  @Test
  public void testDeleteNonExistingApplication() throws Exception {
    ApplicationDAO applicationDAO = new ApplicationDAO();

    final String applicationPublicId = "testDeleteApplicationWithScan_PublicId";
    final String applicationName = "testDeleteApplicationWithScanAppName";

    Application application = tempEntity.newApplicationWithParent(applicationPublicId, applicationName);

    HttpResponse response = restRequest().path(applicationPublicId).delete();
    application = applicationDAO.getByPublicId(applicationPublicId);

    assertResponseStatus(204, response);
    Assert.assertNull(application);

    response = restRequest().path(applicationPublicId).delete();

    assertResponseStatus(404, response);
    Assert.assertEquals("Could not find an application with public ID " + applicationPublicId + ".",
        response.getBodyText());
  }

  @Test
  public void testAddApplication_exceedsLicense() throws Exception {
    setApplicationLimit(1);

    tempEntity.newApplicationWithParent("testAddApplication_exceedsLicense_id");

    // Test Add Application, which should fail with 402 since we exceeded the limit
    Application application = new Application();
    application.setName("testAddApplication_exceedsLicense_id_new_name");
    application.setPublicId("testAddApplication_exceedsLicense_id_new_id");

    HttpResponse response = restRequest().body(application).post();
    assertResponseStatus(402, response);
    Assert.assertEquals("You have exceeded the licensed limit of 1 applications.", response.getBodyText());
  }

  @Test
  public void testGetApplications() throws Exception {
    // Create an application
    final String applicationPublicId = "ApplicationResourceTest-getApplicationsTest-AppId";
    final String applicationName = "ApplicationResourceTest-getApplicationsTest-Name";
    final String licenseFingerprint = "ApplicationResourceTest-getApplicationsTest-LicenseFingerprint";

    Application application = tempEntity.newApplicationWithParent(applicationPublicId, applicationName);
    setLicenseFingerprint(licenseFingerprint);

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    ApplicationDTO[] applications = response.getBody(ApplicationDTO[].class);
    Assert.assertNotNull(applications);

    Assert.assertEquals(Arrays.asList(applications).toString(), 1, applications.length);
    Assert.assertEquals(application.getId(), applications[0].getId());
    Assert.assertEquals(application.getName(), applications[0].getName());

    // Test GetApplication
    response = restRequest().path(ApplicationResource.GET_APPLICATION_PATH).parameter(applicationPublicId).get();
    assertResponseStatus(200, response);

    ApplicationDTO applicationSummary = response.getBody(ApplicationDTO.class);
    Assert.assertNotNull(applicationSummary);
    Assert.assertEquals(application.getId(), applicationSummary.getId());
    Assert.assertEquals(application.getName(), applicationSummary.getName());
  }

  private void assertApplicationManagementSummaryDTO(ApplicationManagementSummaryDTO actual,
                                                     Application app,
                                                     Organization org,
                                                     int policyEvaluationCount,
                                                     ContactDTO contact)
  {
    assertThat(actual.getId(), is(app.getId()));
    assertThat(actual.getPublicId(), is(app.getPublicId()));
    assertThat(actual.getName(), is(app.getName()));
    assertThat(actual.getOrganizationId(), is(org.getId()));
    assertThat(actual.getOrganizationName(), is(org.getName()));
    assertThat(actual.getPolicyEvaluations().size(), is(policyEvaluationCount));
    assertContact(actual.getContact(), contact);
  }

  @Test
  public void testGetApplicationSummaries() throws Exception {
    // Create an application
    final String applicationPublicId = "ApplicationResourceTest-getApplicationsTest-AppId";
    final String applicationName = "ApplicationResourceTest-getApplicationsTest-Name";
    final String licenseFingerprint = "ApplicationResourceTest-getApplicationsTest-LicenseFingerprint";
    final String organizationName = "OrgName";

    Organization organization = tempEntity.newOrganization(organizationName);

    Application application = tempEntity.newApplication(applicationName, applicationPublicId, organization.getId());
    application.setContactInternalName("admin");
    new ApplicationDAO().update(application);
    setLicenseFingerprint(licenseFingerprint);

    // Create policy
    tempEntity.newPolicy(application.getId(), "policy 1");
    final String scanId1 = "ScanId1", scanId2 = "ScanId2", scanId3 = "ScanId3";
    mockReport(scanId1, "/PolicyEvaluateResourceTest/report.zip");
    mockReport(scanId2, "/PolicyEvaluateResourceTest/report.zip");
    mockReport(scanId3, "/PolicyEvaluateResourceTest/report.zip");

    // Eval policy
    HttpResponse response = evalRequest(applicationPublicId, scanId1, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);
    response = evalRequest(applicationPublicId, scanId3, new Stage(Stage.ID_RELEASE)).post();
    assertResponseStatus(200, response);
    response = evalRequest(applicationPublicId, scanId2, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES).get();
    assertResponseStatus(200, response);

    ApplicationManagementSummaryDTO[] applications = response.getBody(ApplicationManagementSummaryDTO[].class);
    Assert.assertNotNull(applications);

    Assert.assertEquals(Arrays.asList(applications).toString(), 1, applications.length);
    ContactDTO expectedContact = new ContactDTO("admin", "Admin BuiltIn", "admin@localhost", "IQ Server");
    assertApplicationManagementSummaryDTO(applications[0], application, organization, 2, expectedContact);

    Map<String, com.sonatype.insight.brain.model.policy.PolicyEvaluation> policyEvaluations = applications[0]
        .getPolicyEvaluations();
    String[] stageTypeIds = policyEvaluations.keySet().toArray(new String[0]);

    Assert.assertNotNull(policyEvaluations);
    Assert.assertEquals(2, policyEvaluations.size());
    Assert.assertEquals(Stage.ID_BUILD, stageTypeIds[0]);
    Assert.assertEquals(Stage.ID_BUILD, policyEvaluations.get(stageTypeIds[0]).getStageTypeId());
    Assert.assertEquals(scanId2, policyEvaluations.get(stageTypeIds[0]).getScanId());
    Assert.assertEquals(Stage.ID_RELEASE, stageTypeIds[1]);
    Assert.assertEquals(Stage.ID_RELEASE, policyEvaluations.get(stageTypeIds[1]).getStageTypeId());
    Assert.assertEquals(scanId3, policyEvaluations.get(stageTypeIds[1]).getScanId());

    Map<String, PolicyEvaluationResult> policyEvaluationsResults = applications[0].getPolicyEvaluationsResults();
    stageTypeIds = policyEvaluationsResults.keySet().toArray(new String[0]);

    Assert.assertNotNull(policyEvaluationsResults);
    Assert.assertEquals(2, policyEvaluationsResults.size());
    Assert.assertEquals(Stage.ID_BUILD, stageTypeIds[0]);
    Assert.assertEquals(7, policyEvaluationsResults.get(stageTypeIds[0]).getAffectedComponentCount());
    Assert.assertEquals(0, policyEvaluationsResults.get(stageTypeIds[0]).getCriticalComponentCount());
    Assert.assertEquals(0, policyEvaluationsResults.get(stageTypeIds[0]).getModerateComponentCount());
    Assert.assertEquals(7, policyEvaluationsResults.get(stageTypeIds[0]).getSevereComponentCount());
    Assert.assertEquals(Stage.ID_RELEASE, stageTypeIds[1]);
    Assert.assertEquals(7, policyEvaluationsResults.get(stageTypeIds[1]).getAffectedComponentCount());
    Assert.assertEquals(0, policyEvaluationsResults.get(stageTypeIds[1]).getCriticalComponentCount());
    Assert.assertEquals(0, policyEvaluationsResults.get(stageTypeIds[1]).getModerateComponentCount());
    Assert.assertEquals(7, policyEvaluationsResults.get(stageTypeIds[1]).getSevereComponentCount());

    // Scans count
    makeScanReceipt();

    super.restRequest().path(CIResource.RESOURCE_PATH, CIResource.SCAN_PATH).parameter(applicationPublicId).body("")
        .put();

    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES).get();
    assertResponseStatus(200, response);

    applications = response.getBody(ApplicationManagementSummaryDTO[].class);
    Assert.assertNotNull(applications);

    // Test GetApplication
    response = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARY)
        .parameter(applicationPublicId).get();
    assertResponseStatus(200, response);

    ApplicationManagementSummaryDTO applicationSummary = response.getBody(ApplicationManagementSummaryDTO.class);
    assertApplicationManagementSummaryDTO(applicationSummary, application, organization, 2, expectedContact);

    policyEvaluations = applicationSummary.getPolicyEvaluations();
    stageTypeIds = policyEvaluations.keySet().toArray(new String[0]);

    Assert.assertNotNull(policyEvaluations);
    Assert.assertEquals(2, policyEvaluations.size());
    Assert.assertEquals(Stage.ID_BUILD, stageTypeIds[0]);
    Assert.assertEquals(Stage.ID_BUILD, policyEvaluations.get(stageTypeIds[0]).getStageTypeId());
    Assert.assertEquals(scanId2, applications[0].getPolicyEvaluations().get(stageTypeIds[0]).getScanId());
    Assert.assertEquals(Stage.ID_RELEASE, stageTypeIds[1]);
    Assert.assertEquals(Stage.ID_RELEASE, policyEvaluations.get(stageTypeIds[1]).getStageTypeId());
    Assert.assertEquals(scanId3, applications[0].getPolicyEvaluations().get(stageTypeIds[1]).getScanId());

    policyEvaluationsResults = applicationSummary.getPolicyEvaluationsResults();
    stageTypeIds = policyEvaluationsResults.keySet().toArray(new String[0]);

    Assert.assertNotNull(policyEvaluationsResults);
    Assert.assertEquals(0, policyEvaluationsResults.size());
  }

  @Test(timeout = 10000)
  public void testGetApplications_DoesNotContactHdsAndPotentiallyBlockToGetLastPolicyAlerts() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-AppId";
    final String applicationName = "ApplicationResourceTest-Name";
    final String appId = tempEntity.newApplicationWithParent(applicationPublicId, applicationName).getId();
    final String scanId = "ApplicationResourceTest-ScanId";

    // create eval log entry pointing at missing report
    tempEntity.newPolicyEvaluation(appId, Stage.ID_BUILD, scanId);
    setHdsResponseForURI("/rest/ci/report?scanId=" + scanId, "Not Found", 404);

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    ApplicationManagementSummaryDTO[] applications = response.getBody(ApplicationManagementSummaryDTO[].class);
    Assert.assertNotNull(applications);
    Assert.assertEquals(1, applications.length);
  }

  @Test
  public void testGetApplicationNames() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-getApplicationNamesTest-AppId";
    final String applicationName = "ApplicationResourceTest-getApplicationNamesTest-Name";
    tempEntity.newApplicationWithParent(applicationPublicId, applicationName);

    HttpResponse response = restRequest().path(ApplicationResource.GET_APPLICATION_NAMES).get();
    assertResponseStatus(200, response);

    @SuppressWarnings("unchecked")
    Map<String, String> applicationNames = response.getBody(Map.class);
    Assert.assertNotNull(applicationNames);

    Assert.assertEquals(applicationNames.toString(), 1, applicationNames.size());
    Assert.assertTrue(applicationNames.containsKey(applicationPublicId));
    Assert.assertTrue(applicationNames.containsValue(applicationName));
  }

  @Test
  @ManualServerInit
  public void testGetApplicationNamesForEvaluateComponent_Anonymous_AnonymousClientAccessAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    final String applicationPublicId = "ApplicationResourceTest-getApplicationNamesTest-AppId";
    final String applicationName = "ApplicationResourceTest-getApplicationNamesTest-Name";
    tempEntity.newApplicationWithParent(applicationPublicId, applicationName);

    HttpResponse response = restRequest().path(ApplicationResource.GET_APPLICATION_NAMES).anon().get();
    assertResponseStatus(200, response);
    @SuppressWarnings("unchecked")
    Map<String, String> applicationNames = response.getBody(Map.class);
    Assert.assertNotNull(applicationNames);

    Assert.assertEquals(applicationNames.toString(), 1, applicationNames.size());
    Assert.assertTrue(applicationNames.containsKey(applicationPublicId));
    Assert.assertTrue(applicationNames.containsValue(applicationName));
  }

  @Test
  public void testAddApplication_NoOrganization() throws Exception {
    String applicationPublicId = "testAddApplication_NoOrganization";
    String applicationName = "testAddApplication-NoOrganization";

    Application application = new Application();
    application.setName(applicationName);
    application.setPublicId(applicationPublicId);

    HttpResponse response = restRequest().body(application).post();
    assertResponseStatus(400, response);
    Assert.assertEquals("Application must have a parent organization.", response.getBodyText());
  }

  @Test
  public void testUpdateApplication_NoOrganization() throws Exception {
    Application application = tempEntity.newApplicationWithParent("testUpdateApplication_NoOrganization");

    application.setOrganizationId(null);

    HttpResponse response = restRequest().body(application).put();
    assertResponseStatus(400, response);
    Assert.assertEquals("Cannot change the parent organization of an application.", response.getBodyText());
  }

  @Test
  public void testUpdateApplication_ChangeOrganization() throws Exception {
    Application application = tempEntity.newApplicationWithParent("testUpdateApplication_ChangeOrganization");

    application.setOrganizationId("newOrganizationId");

    HttpResponse response = restRequest().body(application).put();
    assertResponseStatus(400, response);
    Assert.assertEquals("Cannot change the parent organization of an application.", response.getBodyText());
  }

  @Test
  public void testGenerateIcon() throws Exception {
    HttpResponse response = restRequest().path(ApplicationResource.GENERATE_ICON_PATH).parameter("hash").get();
    testValidIconResponse(response);
  }

  @Test
  public void testRevertGrandfatheringOnNextEvaluation() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ApplicationDAO applicationDAO = new ApplicationDAO();
    application.setPolicyViolationGrandfatheringEnabled(true);
    applicationDAO.update(application);

    HttpResponse response = restRequest().path(ApplicationResource.REVERT_GRANDFATHERING_PATH)
        .parameter(application.getPublicId()).put();
    assertResponseStatus(204, response);
    assertThat(applicationDAO.getByPublicId(application.getPublicId()).isPolicyViolationGrandfatheringEnabled(),
        is(false));
  }

  @Test
  public void testRevertGrandfatheringOnNextEvaluation_BadAppId() throws Exception {
    HttpResponse response = restRequest().path(ApplicationResource.REVERT_GRANDFATHERING_PATH)
        .parameter("badAppId").put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText(), is("Could not find an application with public ID badAppId."));
  }

  private void createDirectory(File dir) {
    if (!dir.isDirectory()) {
      Assert.assertTrue("create directory " + dir.getAbsolutePath(), dir.mkdirs());
    }
  }

  private void makeScanReceipt() throws Exception {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44d");
    scanReceipt.setTimeToReport(30L);
    mockScanReceipt(scanReceipt);
  }

  private void assertContact(ContactDTO actualContact, ContactDTO expectedContact) {

    Assert.assertThat(actualContact, notNullValue());
    Assert.assertThat(actualContact.getInternalName(), is(expectedContact.getInternalName()));
    Assert.assertThat(actualContact.getDisplayName(), is(expectedContact.getDisplayName()));
    Assert.assertThat(actualContact.getEmail(), is(expectedContact.getEmail()));
    Assert.assertThat(actualContact.getRealm(), is(expectedContact.getRealm()));
  }
}

/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.saas.CIResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.StringPart;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApplicationResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testValidate() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-testValidate-AppId";
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = applicationDAO.getByPublicId(applicationPublicId);
    Assert.assertNull(application);

    application = tempEntity.newApplicationWithParent(applicationPublicId, "ApplicationResourceTest-testValidate-AppName");

    Response response = AuthedRestAccess.get(getValidateApplicationIdServiceURL(applicationPublicId));
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), equalTo("OK"));

    applicationDAO.delete(application);

    // validate service always returns 200, the actual result is in the response body
    response = AuthedRestAccess.get(getValidateApplicationIdServiceURL(applicationPublicId));
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), equalTo("Invalid application id " + applicationPublicId));
  }

  @Test
  public void testCRUD() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    Organization organization = tempEntity.newOrganization("ApplicationResourceTest");

    // Test Add Application
    Application application = new Application(applicationPublicId, applicationName, organization.getId());
    application.setContactInternalName("admin");

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(application));

    assertResponseStatus(200, response);

    ApplicationDTO applicationResult = JsonHelpers.fromJson(response.getResponseBody(), ApplicationDTO.class);

    ApplicationDAO applicationDAO = new ApplicationDAO();
    application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    tempEntity.register(application);

    Assert.assertNotNull(application);
    Assert.assertEquals(application.getId(), applicationResult.getId());
    Assert.assertEquals(applicationPublicId, applicationResult.getPublicId());
    Assert.assertEquals(applicationName, applicationResult.getName());
    Assert.assertEquals(application.getOrganizationId(), applicationResult.getOrganizationId());
    Assert.assertEquals(organization.getName(), applicationResult.getOrganizationName());

    ContactDTO expectedContact = new ContactDTO("admin", "Admin BuiltIn", "admin@localhost", "CLM");
    ContactDTO contact = applicationResult.getContact();
    assertContact(contact, expectedContact);

    // Test Add Invalid Icon
    byte[] defaultIconByteArray = IconUtils.loadInvalidIcon();
    AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(getSetIconServiceUrl());
    builder.addBodyPart(new StringPart("applicationId", application.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("defaulticon_application.png",
        defaultIconByteArray)));

    response = AuthedRestAccess.execute(builder);
    assertResponseStatus(400, response);
    Assert.assertEquals("defaulticon_application.png is not a valid image.", response.getResponseBody());

    // Test Get Icon (default icon)
    defaultIconByteArray = loadDefaultIcon();
    Response iconResponse = AuthedRestAccess.get(getGetIconServiceUrl(applicationPublicId));
    assertResponseStatus(307, iconResponse);
    Assert
        .assertEquals(getRestBaseUrl() + "assets/img/defaulticon_application.png", iconResponse.getHeader("Location"));

    // Test Add Application Icon
    builder = AuthedRestAccess.getClient().preparePost(getSetIconServiceUrl());
    builder.addBodyPart(new StringPart("applicationId", application.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("defaulticon_application.png",
        defaultIconByteArray)));
    response = AuthedRestAccess.execute(builder);
    assertResponseStatus(204, response);

    // Test Get Icon (from added application)
    iconResponse = AuthedRestAccess.get(getGetIconServiceUrl(applicationPublicId));
    testValidIconResponse(iconResponse);

    // Test application update
    application.setName(applicationName + "updated");
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(application));
    assertResponseStatus(200, response);
    applicationResult = JsonHelpers.fromJson(response.getResponseBody(), ApplicationDTO.class);
    Assert.assertEquals(application.getId(), applicationResult.getId());
    Assert.assertEquals(applicationPublicId, applicationResult.getPublicId());
    Assert.assertEquals(applicationName + "updated", applicationResult.getName());

    // Test icon update
    builder = AuthedRestAccess.getClient().preparePost(getSetIconServiceUrl());
    builder.addBodyPart(new StringPart("applicationId", application.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    response = AuthedRestAccess.execute(builder);
    assertResponseStatus(204, response);

    // Verify non alpha numeric name fails
    application.setName("Non Alphanumeric Name !!!!!");

    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(application));

    assertResponseStatus(400, response);

    // Test delete
    response = AuthedRestAccess.delete(getServiceURL() + "/" + applicationPublicId);
    assertResponseStatus(204, response);
    application = applicationDAO.getByPublicId(applicationPublicId);
    Assert.assertNull(application);
    iconResponse = AuthedRestAccess.get(getServiceURL() + "/icon/" + applicationPublicId);
    assertResponseStatus(404, iconResponse);
  }

  @Test
  public void testSyncIcon() throws Exception {
    final String applicationPublicId = "testID";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    byte[] defaultIconByteArray = loadDefaultIcon();

    // Test Sync Update Application Icon
    AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(getSetSyncIconServiceUrl());
    builder.addBodyPart(new StringPart("applicationId", application.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("defaulticon_application.png",
        defaultIconByteArray)));
    Response response = AuthedRestAccess.execute(builder);
    assertResponseStatus(200, response);
    Assert.assertEquals("", response.getResponseBody());

    // Test Sync Fail Update Application Icon
    builder = AuthedRestAccess.getClient().preparePost(getSetSyncIconServiceUrl());
    builder.addBodyPart(new StringPart("applicationId", application.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("defaulticon_application.png", IconUtils
        .loadInvalidIcon())));
    response = AuthedRestAccess.execute(builder);
    assertResponseStatus(200, response);
    Assert.assertEquals("defaulticon_application.png is not a valid image.", response.getResponseBody());
  }

  private byte[] loadDefaultIcon() throws IOException {
    return IconUtils.loadIconFromProductAssets("defaulticon_application.png");
  }

  private void testValidIconResponse(Response iconResponse) throws Exception {
    assertResponseStatus(200, iconResponse);
    Assert.assertNotNull(iconResponse.getResponseBodyAsBytes());
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

    File saasScanFile = getScanResponseFile(licenseFingerprint);
    makeScanReceipt(saasScanFile);

    Response response = AuthedRestAccess.put(getScanURL(applicationPublicId), "");

    assertResponseStatus(200, response);

    final String applicationId = application.getId();

    // TODO ideally, need to create these directories by calling into appropriate REST endpoints
    final InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(brain.getWorkDir().getAbsolutePath());
    final InsightWork insightWork = new InsightWork(insightConfig);
    createDirectory(insightWork.getScanDir(applicationId));
    createDirectory(insightWork.getAuditDir(applicationId));
    createDirectory(insightWork.getReportDir(applicationId));

    response = AuthedRestAccess.delete(getServiceURL() + "/" + applicationPublicId);
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

    Response response = AuthedRestAccess.delete(getServiceURL() + "/" + applicationPublicId);
    application = applicationDAO.getByPublicId(applicationPublicId);

    assertResponseStatus(204, response);
    Assert.assertNull(application);

    response = AuthedRestAccess.delete(getServiceURL() + "/" + applicationPublicId);

    assertResponseStatus(404, response);
    Assert.assertEquals("Could not find an application with public id " + applicationPublicId + ".",
        response.getResponseBody());
  }

  @Test
  public void testAddApplication_exceedsLicense() throws Exception {
    setApplicationLimit(1);

    tempEntity.newApplicationWithParent("testAddApplication_exceedsLicense_id");

    // Test Add Application, which should fail with 402 since we exceeded the limit
    Application application = new Application();
    application.setName("testAddApplication_exceedsLicense_id_new_name");
    application.setPublicId("testAddApplication_exceedsLicense_id_new_id");

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(application));
    assertResponseStatus(402, response);
    Assert.assertEquals("You have exceeded the licensed limit of 1 applications.", response.getResponseBody());
  }

  @Test
  public void testGetApplications() throws Exception {
    // Create an application
    final String applicationPublicId = "ApplicationResourceTest-getApplicationsTest-AppId";
    final String applicationName = "ApplicationResourceTest-getApplicationsTest-Name";
    final String licenseFingerprint = "ApplicationResourceTest-getApplicationsTest-LicenseFingerprint";

    Application application = tempEntity.newApplicationWithParent(applicationPublicId, applicationName);
    setLicenseFingerprint(licenseFingerprint);

    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);

    ApplicationDTO[] applications = JsonHelpers.fromJson(response.getResponseBody(), ApplicationDTO[].class);
    Assert.assertNotNull(applications);

    Assert.assertEquals(Arrays.asList(applications).toString(), 1, applications.length);
    Assert.assertEquals(application.getId(), applications[0].getId());
    Assert.assertEquals(application.getName(), applications[0].getName());

    // Test GetApplication
    response = AuthedRestAccess.get(getApplicationServiceUrl(applicationPublicId));
    assertResponseStatus(200, response);

    ApplicationDTO applicationSummary = JsonHelpers.fromJson(response.getResponseBody(), ApplicationDTO.class);
    Assert.assertNotNull(applicationSummary);
    Assert.assertEquals(application.getId(), applicationSummary.getId());
    Assert.assertEquals(application.getName(), applicationSummary.getName());
  }

  @Test
  public void testGetApplicationManagementSummary() throws Exception {
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

    final String scanId1 = "ScanId1", scanId2 = "ScanId2";
    final File saasReportFile1 = getReportResponseFile(licenseFingerprint, scanId1);
    FileUtils.copyURLToFile(getClass().getResource("/PolicyEvaluateResourceTest/report.zip"), saasReportFile1);
    FileUtils.copyFile(saasReportFile1, getReportResponseFile(licenseFingerprint, scanId2));

    final long startTime = System.currentTimeMillis();
    Response response = AuthedRestAccess.post(getEvalURL(applicationPublicId, scanId1),
        JsonHelpers.asJson(new Stage(Stage.ID_BUILD)));
    assertResponseStatus(200, response);
    response = AuthedRestAccess.post(getEvalURL(applicationPublicId, scanId2),
        JsonHelpers.asJson(new Stage(Stage.ID_RELEASE)));
    assertResponseStatus(200, response);

    // Verify Response for scan 1
    response = AuthedRestAccess.get(getApplicationManagementSummaryUrl(application.getPublicId(), scanId1));
    assertResponseStatus(200, response);
    ApplicationManagementSummaryDTO summary = JsonHelpers.fromJson(response.getResponseBody(),
        ApplicationManagementSummaryDTO.class);

    Assert.assertEquals(application.getName(), summary.getName());
    Assert.assertEquals(application.getId(), summary.getId());
    Assert.assertEquals(1, summary.getPolicyEvaluations().size());
    Assert.assertTrue(summary.getPolicyEvaluations().containsKey(Stage.ID_BUILD));

    PolicyEvaluation evaluation = summary.getPolicyEvaluations().get(Stage.ID_BUILD);
    Assert.assertEquals(scanId1, evaluation.getScanId());
    Assert.assertTrue(evaluation.getTime().getTime() > startTime);

    // Verify Response for scan 2
    response = AuthedRestAccess.get(getApplicationManagementSummaryUrl(application.getPublicId(), scanId2));
    assertResponseStatus(200, response);

    summary = JsonHelpers.fromJson(response.getResponseBody(), ApplicationManagementSummaryDTO.class);

    Assert.assertEquals(application.getName(), summary.getName());
    Assert.assertEquals(application.getId(), summary.getId());
    Assert.assertEquals(1, summary.getPolicyEvaluations().size());
    Assert.assertTrue(summary.getPolicyEvaluations().containsKey(Stage.ID_RELEASE));
    Assert.assertEquals(organization.getName(), summary.getOrganizationName());

    ContactDTO expectedContact = new ContactDTO("admin", "Admin BuiltIn", "admin@localhost", "CLM");
    ContactDTO summaryContact = summary.getContact();
    assertContact(summaryContact, expectedContact);

    evaluation = summary.getPolicyEvaluations().get(Stage.ID_RELEASE);
    Assert.assertEquals(scanId2, evaluation.getScanId());
    Assert.assertTrue(evaluation.getTime().getTime() > startTime);

    // 1-800-DIAL-A-SCAN
    response = AuthedRestAccess.get(getApplicationManagementSummaryUrl(application.getPublicId(), "12345678"));
    assertResponseStatus(404, response);
    Assert.assertEquals("Unable to locate requested scan", response.getResponseBody());
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
    final File saasReportFile1 = getReportResponseFile(licenseFingerprint, scanId1);
    FileUtils.copyURLToFile(getClass().getResource("/PolicyEvaluateResourceTest/report.zip"), saasReportFile1);
    FileUtils.copyFile(saasReportFile1, getReportResponseFile(licenseFingerprint, scanId2));
    FileUtils.copyFile(saasReportFile1, getReportResponseFile(licenseFingerprint, scanId3));

    // Eval policy
    Response response = AuthedRestAccess.post(getEvalURL(applicationPublicId, scanId1),
        JsonHelpers.asJson(new Stage(Stage.ID_BUILD)));
    assertResponseStatus(200, response);
    response = AuthedRestAccess.post(getEvalURL(applicationPublicId, scanId3),
        JsonHelpers.asJson(new Stage(Stage.ID_RELEASE)));
    assertResponseStatus(200, response);
    response = AuthedRestAccess.post(getEvalURL(applicationPublicId, scanId2), JsonHelpers.asJson(new Stage(Stage.ID_BUILD)));
    assertResponseStatus(200, response);

    response = AuthedRestAccess.get(getSummariesURL());
    assertResponseStatus(200, response);

    ApplicationManagementSummaryDTO[] applications = JsonHelpers.fromJson(response.getResponseBody(),
        ApplicationManagementSummaryDTO[].class);
    Assert.assertNotNull(applications);

    Assert.assertEquals(Arrays.asList(applications).toString(), 1, applications.length);
    Assert.assertEquals(application.getId(), applications[0].getId());
    Assert.assertEquals(application.getName(), applications[0].getName());
    Assert.assertEquals(organization.getName(), applications[0].getOrganizationName());


    ContactDTO expectedContact = new ContactDTO("admin", "Admin BuiltIn", "admin@localhost", "CLM");
    ContactDTO applicationContact = applications[0].getContact();
    assertContact(applicationContact, expectedContact);

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
    final File saasScanFile = getScanResponseFile(licenseFingerprint);
    makeScanReceipt(saasScanFile);

    AuthedRestAccess.put(getScanURL(applicationPublicId), "");

    response = AuthedRestAccess.get(getSummariesURL());
    assertResponseStatus(200, response);

    applications = JsonHelpers.fromJson(response.getResponseBody(), ApplicationManagementSummaryDTO[].class);
    Assert.assertNotNull(applications);
    Assert.assertEquals(1, applications[0].getScansCount());

    // Test GetApplication
    response = AuthedRestAccess.get(getSummaryURL(applicationPublicId));
    assertResponseStatus(200, response);

    ApplicationManagementSummaryDTO applicationSummary = JsonHelpers.fromJson(response.getResponseBody(),
        ApplicationManagementSummaryDTO.class);
    Assert.assertNotNull(applicationSummary);
    Assert.assertEquals(application.getId(), applicationSummary.getId());
    Assert.assertEquals(application.getName(), applicationSummary.getName());
    Assert.assertEquals(organization.getName(), applicationSummary.getOrganizationName());

    ContactDTO summaryContact = applicationSummary.getContact();
    assertContact(summaryContact, expectedContact);

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
  }

  @Test(timeout = 10000)
  public void testGetApplications_DoesNotContactSaasAndPotentiallyBlockToGetLastPolicyAlerts() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-AppId";
    final String applicationName = "ApplicationResourceTest-Name";
    final String appId = tempEntity.newApplicationWithParent(applicationPublicId, applicationName).getId();
    final String scanId = "ApplicationResourceTest-ScanId";

    // create eval log entry pointing at missing report
    tempEntity.newPolicyEvaluation(appId, Stage.ID_BUILD, scanId);
    setSaasResponseForURI("/rest/ci/report?scanId=" + scanId, "Not Found", 404);

    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);

    ApplicationManagementSummaryDTO[] applications = JsonHelpers.fromJson(response.getResponseBody(),
        ApplicationManagementSummaryDTO[].class);
    Assert.assertNotNull(applications);
    Assert.assertEquals(1, applications.length);
  }

  @Test
  public void testGetApplicationNames() throws Exception {
    final String applicationPublicId = "ApplicationResourceTest-getApplicationNamesTest-AppId";
    final String applicationName = "ApplicationResourceTest-getApplicationNamesTest-Name";
    tempEntity.newApplicationWithParent(applicationPublicId, applicationName);

    Response response = AuthedRestAccess.get(getServiceURL() + "/services/names");
    assertResponseStatus(200, response);

    @SuppressWarnings("unchecked")
    Map<String, String> applicationNames = JsonHelpers.fromJson(response.getResponseBody(), Map.class);
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

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(application));
    assertResponseStatus(400, response);
    Assert.assertEquals("Applications must have a parent organization.", response.getResponseBody());
  }

  @Test
  public void testUpdateApplication_NoOrganization() throws Exception {
    Application application = tempEntity.newApplicationWithParent("testUpdateApplication_NoOrganization");

    application.setOrganizationId(null);

    Response response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(application));
    assertResponseStatus(400, response);
    Assert.assertEquals("Applications must have a parent organization.", response.getResponseBody());
  }

  @Test
  public void testUpdateApplication_ChangeOrganization() throws Exception {
    Application application = tempEntity.newApplicationWithParent("testUpdateApplication_ChangeOrganization");

    application.setOrganizationId("newOrganizationId");

    Response response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(application));
    assertResponseStatus(400, response);
    Assert.assertEquals("Cannot change the parent organization of an application.", response.getResponseBody());
  }

  @Test
  public void testGenerateIcon() throws Exception {
    String hashcode = "abababababababababab";
    String url = getGenerateIconServiceUrl(hashcode);
    String saasUrl = "rest/application/icon/generate/" + hashcode;
    setSaasResponseForURI(saasUrl, 200, loadDefaultIcon());
    Response response = AuthedRestAccess.get(url);
    testValidIconResponse(response);
  }

  private void createDirectory(File dir) {
    if (!dir.isDirectory()) {
      Assert.assertTrue("create directory " + dir.getAbsolutePath(), dir.mkdirs());
    }
  }

  private String getValidateApplicationIdServiceURL(String applicationPublicId) {
    return getServiceURL() + '/'
        + ApplicationResource.VALIDATE_PATH.replace("{applicationPublicId}", applicationPublicId);
  }

  private String getApplicationServiceUrl(String applicationPublicId) {
    return getServiceURL() + '/'
        + ApplicationResource.GET_APPLICATION_PATH.replace("{applicationPublicId}", applicationPublicId);
  }

  private String getApplicationManagementSummaryUrl(final String appId, final String scanId) {
    return getRestBaseUrl()
        + ApplicationResource.SERVICE_PATH
        + "/"
        + ApplicationResource.GET_SCAN_APPLICATION_MANAGEMENT_SUMMARY.replace("{applicationPublicId}", appId).replace(
            "{scanId}", scanId);
  }

  private String getGetIconServiceUrl(String applicationPublicId) {
    return getServiceURL() + "/"
        + ApplicationResource.GET_APPLICATION_ICON_PATH.replace("{applicationPublicId}", applicationPublicId);
  }

  private String getSetIconServiceUrl() {
    return getServiceURL() + "/" + ApplicationResource.ICON_PATH;
  }

  private String getSetSyncIconServiceUrl() {
    return getServiceURL() + "/" + ApplicationResource.ICON_PATH_SYNC;
  }

  private String getGenerateIconServiceUrl(String hashcode) {
    return getServiceURL() + "/" + ApplicationResource.GENERATE_ICON_PATH.replace("{hashcode}", hashcode);
  }

  private String getServiceURL() {
    return getRestBaseUrl() + ApplicationResource.SERVICE_PATH;
  }

  private String getSummariesURL() {
    return getServiceURL() + "/" + ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES;
  }

  private String getSummaryURL(String applicationPublicId) {
    return getServiceURL() + "/" + ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARY.replace("{applicationPublicId}", applicationPublicId);
  }

  private String getEvalURL(final String appId, final String scanId) {
    return getRestBaseUrl() + PolicyEvaluateResource.SERVICE_PATH.replace("{applicationPublicId}", appId) + "?scanId="
        + scanId;
  }

  private String getScanURL(final String appId) {
    return getRestBaseUrl() + CIResource.SERVICE_PATH + "/scan/" + appId;
  }

  private void makeScanReceipt(File saasScanFile) throws Exception {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44d");
    scanReceipt.setTimeToReport(30L);
    saasScanFile.delete();
    saasScanFile.getParentFile().mkdirs();
    FileUtils.fileWrite(saasScanFile, "UTF-8", toJson(scanReceipt));
  }

  private void assertContact(ContactDTO actualContact, ContactDTO expectedContact) {

    Assert.assertThat(actualContact, notNullValue());
    Assert.assertThat(actualContact.getInternalName(), is(expectedContact.getInternalName()));
    Assert.assertThat(actualContact.getDisplayName(), is(expectedContact.getDisplayName()));
    Assert.assertThat(actualContact.getEmail(), is(expectedContact.getEmail()));
    Assert.assertThat(actualContact.getRealm(), is(expectedContact.getRealm()));
  }
}

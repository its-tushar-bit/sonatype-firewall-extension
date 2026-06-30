/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import jakarta.mail.Message;
import jakarta.ws.rs.core.HttpHeaders;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.hds.TestNamedComponentDetails;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.util.HashUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.asynchttpclient.uri.Uri;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.sonatype.insight.brain.test.MailboxTestUtil;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DATA_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_THREATS;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SECURITY_JSON;
import static com.sonatype.insight.brain.report.ReportResource.BROWSE_PATH;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static com.sonatype.insight.mock.hds.HdsMockServer.RestServlet.SCAN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class ReportResourceTest
    extends AbstractResourceTest
{
  private MailConfigurationDAO mailConfigurationDAO;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyViolationDAO policyViolationDAO;

  private PolicyDAO policyDAO;

  private ApplicationDAO applicationDAO;

  private Application app;

  @Before
  public void before() {
    mailConfigurationDAO = lookup(MailConfigurationDAO.class);
    policyEvaluationDAO = lookup(PolicyEvaluationDAO.class);
    policyDAO = lookup(PolicyDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);

    MailboxTestUtil.clearAll();

    app = tempEntity.newApplicationWithParent("ReportResourceTest_AppId");
  }

  @After
  public void resetAsyncPollingFlag() {
    // The AsyncReevaluationService singleton (and this flag) is shared across the class via the static test server,
    // so reset it after each test that flipped it for fast polling, to avoid leaking the testing interval into a
    // later test that relies on the production polling interval.
    lookup(AsyncReevaluationService.class).disablePollingIntervalForTesting = false;
  }

  private HttpRequest restRequest(String appId, String scanId) {
    return restRequest().path(ReportResource.RESOURCE_PATH).parameter(appId, scanId);
  }

  @Test
  public void testBrowseReportEntryExpirationDate() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    HttpRequest request = restRequest(app.getPublicId(), scanId).path("{scanId}/browseReport");

    createReportFile(app.getId(), scanId, "/ReportResourceTest/report");

    TimeZone gmt = TimeZone.getTimeZone("GMT");
    final Calendar calendar = Calendar.getInstance(gmt);
    final SimpleDateFormat expirationHeaderFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm", Locale.ENGLISH);
    expirationHeaderFormat.setTimeZone(gmt);

    calendar.setTime(new Date());
    calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 365);
    HttpResponse response = request.subpath("insight.js").get();
    assertResponseStatus(200, response);
    String expiresHeader = response.getHeader("Expires");
    assertThat(expiresHeader).isNotNull();
    Date expires = expirationHeaderFormat.parse(expiresHeader);
    assertThat(Math.abs(calendar.getTimeInMillis() - expires.getTime()))
        .as("insight.js expires in 365 days: " + expires + " vs " + calendar.getTime())
        .isLessThan(2 * 60 * 1000);

    calendar.setTime(new Date());
    response = request.subpath(DATA_JSON.getName()).get();
    assertResponseStatus(200, response);
    expiresHeader = response.getHeader("Expires");
    expires = expirationHeaderFormat.parse(expiresHeader);
    assertThat(Math.abs(calendar.getTimeInMillis() - expires.getTime()))
        .as(DATA_JSON.getName() + " expires immediately: " + expires + " vs " + calendar.getTime())
        .isLessThan(2 * 60 * 1000);

    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
    String ifModifiedSinceHeader = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).format(calendar
        .getTime());
    response = request.subpath("data.json").header("If-Modified-Since", ifModifiedSinceHeader).get();
    assertResponseStatus(304, response);
  }

  @Test
  public void testBrowseSecurityReport() throws Exception {
    assertSecurityReport("security", 2);
  }

  @Test
  public void testBrowseSecurity_And_ThirdParty_Report() throws Exception {
    assertSecurityReport("security-thirdparty", 3);
  }

  @Test
  public void testBrowseSecurity_ThirdParty_Report() throws Exception {
    assertSecurityReport("security-only-thirdparty", 1);
  }

  @Test
  public void testBrowseReport() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    HttpRequest request = restRequest(app.getPublicId(), scanId).path("{scanId}/browseReport");

    String reportResource = "/ReportResourceTest/report";
    mockReport(scanId, reportResource);
    createScanFile(app.getId(), scanId);

    // This will trigger two legacy violations upon evaluation.
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    Constraint constraint = new Constraint(null /* constraintId */, "Constraint Coordinates", LogicalOperator.OR);
    Condition condition1 = new Condition(CoordinatesConditionType.ID, "match",
        ComponentIdentifier.FORMAT_MAVEN + ":tomcat:tomcat-util:5.5.23");
    Condition condition2 = new Condition(CoordinatesConditionType.ID, "match",
        ComponentIdentifier.FORMAT_MAVEN + ":commons-pool:commons-pool:1.4");
    constraint.addCondition(condition1);
    constraint.addCondition(condition2);
    Policy policy = new Policy();
    policy.setOwnerId(app.getId());
    policy.addConstraint(constraint);
    policy.setName("testPolicy");
    policy.setAction(BuildStageType.ID, WarnActionType.ID);
    policy.setLegacyViolationAllowed(true);
    tempEntity.newPolicy(policy);

    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .query("scanId", scanId)
        .parameter(app.getPublicId())
        .body(new Stage(Stage.ID_BUILD))
        .post();
    assertResponseStatus(200, response);

    URL reportResourceUrl = getClass().getResource(reportResource);
    File reportResourceDir = new File(reportResourceUrl.toURI());

    int verifiedFileCount = 0;
    for (File file : reportResourceDir.listFiles()) {
      if (file.isDirectory()) {
        continue;
      }

      verifiedFileCount++;

      String entry = file.getName();

      response = request.subpath(entry).get();
      assertResponseStatus(200, response);

      final String contentType = response.getContentType().replace(" ", "");
      if (entry.endsWith(".html")) {
        assertThat(contentType).isEqualToIgnoringCase("text/html;charset=UTF-8");
      }
      else if (entry.endsWith(".css")) {
        assertThat(contentType).isEqualToIgnoringCase("text/css;charset=UTF-8");
      }
      else if (entry.endsWith(".json")) {
        assertThat(contentType).isEqualToIgnoringCase("application/json");
      }
      else if (entry.endsWith(".png")) {
        assertThat(contentType).isEqualToIgnoringCase("image/png");
      }

      if (DATA_JSON.getName().equals(entry)) {
        String actual = response.getBodyText();
        testDataJsonApplyChanges(actual);
      }
      else if ("licenses.json".equals(entry)) {
        String actual = response.getBodyText();

        testLicensesJsonApplyChanges(actual);
        testJsonApplyComponentChanges(actual);
      }
      else if ("licensethreats.json".equals(entry)) {
        String actual = response.getBodyText();

        testLicenseThreatsJsonApplyChanges(actual);
      }
      else if ("partialmatched.json".equals(entry)) {
        String actual = response.getBodyText();

        testPartialMatchedJsonApplyChanges(actual);
      }
      else if ("security.json".equals(entry)) {
        JsonNode actual = JsonUtils.parse(response.getBodyText());
        JsonNode expected = JsonUtils.parse(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
        for (JsonNode node : expected.get("aaData")) {
          ComponentIdentifierAdapter.injectComponentIdentifier((ObjectNode) node);
          ComponentDisplayNameUtil.injectDisplayName((ObjectNode) node);
        }
        assertThat(actual).isEqualTo(expected);
      }
      else if ("index.html".equals(entry)) {
        String actual = response.getBodyText();
        assertThat(actual).contains("applicationId = '" + app.getPublicId() + "'");
      }
      else if ("bom.json".equals(entry)) {
        String actual = response.getBodyText();
        testJsonApplyComponentChanges(actual);
      }
      else if (contentType.startsWith("text") || contentType.endsWith("json")) {
        assertThat(response.getBodyText())
            .isEqualToIgnoringWhitespace(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
      }
      else {
        assertThat(response.getBodyBytes()).as("Unexpected content for " + entry)
            .isEqualTo(org.apache.commons.io.FileUtils.readFileToByteArray(file));
      }
    }

    assertThat(verifiedFileCount).isEqualTo(109);
    assertResponseStatus(200, request.subpath("/").get());
  }

  @Test
  public void testBrowseReportRemovesPathNames() throws Exception {
    long originalThreshold = ReportResource.FILE_SIZE_THRESHOLD;
    ReportResource.FILE_SIZE_THRESHOLD = 1L;
    try {
      final String scanId = "ReportResourceTest_ScanId";
      HttpRequest request = restRequest(app.getPublicId(), scanId).path("{scanId}/browseReport");

      String reportResource = "/ReportResourceTest/report";
      mockReport(scanId, reportResource);
      createScanFile(app.getId(), scanId);

      HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
          .query("scanId", scanId)
          .parameter(app.getPublicId())
          .body(new Stage(Stage.ID_BUILD))
          .post();
      assertResponseStatus(200, response);

      response = request.subpath("bom.json").get();
      assertResponseStatus(200, response);
      testAllPathNamesEntriesAreEmpty(response.getBodyText());
    }
    finally {
      ReportResource.FILE_SIZE_THRESHOLD = originalThreshold;
    }
  }

  @Test
  public void testBrowseReport_SharedResources() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    HttpRequest request = restRequest(app.getPublicId(), scanId).path("{scanId}/browseReport");

    String reportResource = "/ReportResourceTest/report_sharedResources";
    mockReport(scanId, reportResource);
    createScanFile(app.getId(), scanId);

    // This will trigger two legacy violations upon evaluation.
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    Constraint constraint = new Constraint(null /* constraintId */, "Constraint Coordinates", LogicalOperator.OR);
    Condition condition1 = new Condition(CoordinatesConditionType.ID, "match",
        ComponentIdentifier.FORMAT_MAVEN + ":tomcat:tomcat-util:5.5.23");
    Condition condition2 = new Condition(CoordinatesConditionType.ID, "match",
        ComponentIdentifier.FORMAT_MAVEN + ":commons-pool:commons-pool:1.4");
    constraint.addCondition(condition1);
    constraint.addCondition(condition2);
    Policy policy = new Policy();
    policy.setOwnerId(app.getId());
    policy.addConstraint(constraint);
    policy.setName("testPolicy");
    policy.setAction(BuildStageType.ID, WarnActionType.ID);
    policy.setLegacyViolationAllowed(true);
    tempEntity.newPolicy(policy);

    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .query("scanId", scanId)
        .parameter(app.getPublicId())
        .body(new Stage(Stage.ID_BUILD))
        .post();
    assertResponseStatus(200, response);

    URL reportResourceUrl = getClass().getResource(reportResource);
    File reportResourceDir = new File(reportResourceUrl.toURI());

    int verifiedFileCount = 0;
    for (File file : reportResourceDir.listFiles()) {
      if (file.isDirectory()) {
        continue;
      }

      verifiedFileCount++;

      String entry = file.getName();

      response = request.subpath(entry).get();
      assertResponseStatus(200, response);

      final String contentType = response.getContentType().replace(" ", "");
      if (entry.endsWith(".html")) {
        assertThat(contentType).isEqualToIgnoringCase("text/html;charset=UTF-8");
      }
      else if (entry.endsWith(".css")) {
        assertThat(contentType).isEqualToIgnoringCase("text/css;charset=UTF-8");
      }
      else if (entry.endsWith(".json")) {
        assertThat(contentType).isEqualToIgnoringCase("application/json");
      }
      else if (entry.endsWith(".png")) {
        assertThat(contentType).isEqualToIgnoringCase("image/png");
      }

      if (DATA_JSON.getName().equals(entry)) {
        String actual = response.getBodyText();
        testDataJsonApplyChanges(actual);
      }
      else if ("licenses.json".equals(entry)) {
        String actual = response.getBodyText();

        testLicensesJsonApplyChanges(actual);
        testJsonApplyComponentChanges(actual);
      }
      else if ("licensethreats.json".equals(entry)) {
        String actual = response.getBodyText();

        testLicenseThreatsJsonApplyChanges(actual);
      }
      else if ("partialmatched.json".equals(entry)) {
        String actual = response.getBodyText();

        testPartialMatchedJsonApplyChanges(actual);
      }
      else if ("security.json".equals(entry)) {
        JsonNode actual = JsonUtils.parse(response.getBodyText());
        JsonNode expected = JsonUtils.parse(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
        for (JsonNode node : expected.get("aaData")) {
          ComponentIdentifierAdapter.injectComponentIdentifier((ObjectNode) node);
          ComponentDisplayNameUtil.injectDisplayName((ObjectNode) node);
        }
        assertThat(actual).isEqualTo(expected);
      }
      else if ("index.html".equals(entry)) {
        String actual = response.getBodyText();
        assertThat(actual).contains("applicationId = '" + app.getPublicId() + "'");
      }
      else if ("bom.json".equals(entry)) {
        String actual = response.getBodyText();
        testJsonApplyComponentChanges(actual);
      }
      else if (contentType.startsWith("text") || contentType.endsWith("json")) {
        assertThat(response.getBodyText())
            .isEqualToIgnoringWhitespace(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
      }
      else {
        assertThat(response.getBodyBytes()).as("Unexpected content for " + entry)
            .isEqualTo(org.apache.commons.io.FileUtils.readFileToByteArray(file));
      }
    }

    assertThat(verifiedFileCount).isEqualTo(12);
    assertResponseStatus(200, request.subpath("/").get());
  }

  @Test
  public void testBrowseReport_NoDirectoryTraversal() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    File reportDir = getCLMServer().getInstance(InsightWork.class).getReportDir(app.getId(), scanId);
    reportDir.mkdirs();
    new File(reportDir, "restricted.txt").createNewFile();

    HttpRequest request = restRequest(app.getPublicId(), scanId).path("{scanId}/browseReport");

    // In the latest version of async-http-client they implemented their own UriParser which strips out any dots by
    // default. There is no way to customize this behaviour so we are forced to override using a static mock
    // https://github.com/AsyncHttpClient/async-http-client/blob/main/client/src/main/java/org/
    // asynchttpclient/uri/UriParser.java#L293
    try (MockedStatic<Uri> mockUri = Mockito.mockStatic(Uri.class, Mockito.CALLS_REAL_METHODS)) {
      mockUri.when(() -> Uri.create(any(), any())).thenAnswer(i -> {
        String urlString = i.getArgument(1);

        URL url = new URL(urlString);
        return new Uri(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
            url.getQuery(), url.getRef());
      });

      HttpResponse response = request.subpath("../restricted.txt").get();
      assertResponseStatus(400, response);

      response = request.subpath("%2E%2E/restricted.txt").get();
      assertResponseStatus(400, response);

      response = request.subpath("%2E%2E%5Crestricted.txt").get();
      assertResponseStatus(400, response);
    }
  }

  @Test
  public void testPrintReport() throws Exception {
    String scanId = "ReportResourceTest_ScanId";
    createReportFile(app.getId(), scanId, "/ReportResourceTest/sample-report");
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);

    HttpResponse response = restRequest(app.getPublicId(), scanId).path("{scanId}/printReport").get();
    assertResponseStatus(200, response);
    assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION))
        .containsSubsequence("attachment; filename=\"" + app.getName() + "-Build-", ".pdf\"");

    // validate content type and check the actual content is really a PDF
    assertThat(response.getContentType()).isEqualTo("application/pdf;charset=UTF-8");
    assertThat(new String(response.getBodyBytes(), 0, 1024, "US-ASCII")).contains("%PDF-");
  }

  @Test
  public void testPrintSbomReport() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    String scanId = "scanId";

    Path originalSbom = mockOriginalSbom(this.getClass(), "/original-sbom/cdx-test-bom.xml",
        getCLMServer().getInstance(InsightWork.class).getSbomDir(app.getId()).toPath());
    createReportFile(app.getId(), "scanId", "/ReportResourceTest/sample-report");
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile(originalSbom.getFileName().toString());
    tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
            thirdPartyFile.getFilename());

    HttpResponse response =
        restRequest(app.getPublicId(), sbomMetadata.getSbomVersion())
            .path("sbom/{sbomVersion}/printReport")
            .get();
    assertResponseStatus(200, response);
    assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION))
        .containsSubsequence("attachment; filename=\"" + app.getName() + "-" + sbomMetadata.getSbomVersion(), ".pdf\"");

    // validate content type and check the actual content is really a PDF
    assertThat(response.getContentType()).isEqualTo("application/pdf;charset=UTF-8");
    assertThat(new String(response.getBodyBytes(), 0, 1024, "US-ASCII")).contains("%PDF-");
  }

  @Test
  public void testGetSbomPolicyViolationReport() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";

    String reportResource = "/ReportResourceTest/report";
    mockReport(scanId, reportResource);
    createScanFile(app.getId(), scanId);

    HttpResponse response = restRequest()
        .path(PolicyEvaluateResource.RESOURCE_PATH)
        .query("scanId", scanId)
        .parameter(app.getPublicId())
        .body(new Stage(Stage.ID_COMPLIANCE))
        .post();
    assertResponseStatus(200, response);

    String sbomVersion = "sbomVersion1";
    tempEntity.newSbomEvaluation(
        app, sbomVersion, "spec1",
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"),
        "hash1", scanId, true, ACTIVE);

    setFeatures(LicensedFeature.SBOM_MANAGER);
    response = restRequest()
        .path(ReportResource.RESOURCE_PATH + "/sbom/{sbomVersion}/sbomPolicyViolationReport")
        .parameter(app.getPublicId(), sbomVersion)
        .get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetSbomPolicyViolationReport_WithFileCoordinateId() throws Exception {
    testGetSbomPolicyViolationReport("1249e25aebb15358bedd", "86163fcc32524261bfd2bdbedb7eae43", null);
  }

  @Test
  public void testGetSbomPolicyViolationReport_WithComponentRef() throws Exception {
    testGetSbomPolicyViolationReport("1249e25aebb15358bedd", null, HashUtils.hash(
        "pkg:maven/com.h2database/h2@1.4.200?type=jar", HashUtils.SHA1));
  }

  @Test
  public void testGetSbomPolicyViolationReport_WithHash() throws Exception {
    testGetSbomPolicyViolationReport("1249e25aebb15358bedd", "some-nonexistent-id", null);
  }

  private void testGetSbomPolicyViolationReport(
      String hash,
      String fileCoordinateId,
      String componentRef) throws Exception
  {
    String scanId = "ReportResourceTest_ScanId";

    String reportResource = "/ReportResourceTest/report-bom";
    mockReport(scanId, reportResource);
    createScanFile(app.getId(), scanId);

    URL zippedReport = ReportHelper.zipReport(reportResource, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(app.getId(), scanId);

    FileUtils.copyURLToFile(zippedReport, reportDestination);

    String sbomVersion = "sbomVersion1";
    tempEntity.newSbomEvaluation(app, sbomVersion, "spec1",
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"), hash, scanId, true, ACTIVE);

    setFeatures(LicensedFeature.SBOM_MANAGER);
    HttpResponse response = restRequest()
        .path(ReportResource.RESOURCE_PATH + "/" + ReportResource.SBOM_POLICY_VIOLATION_REPORT)
        .query("hash", hash)
        .query("fileCoordinateId", fileCoordinateId)
        .query("componentRef", componentRef)
        .parameter(app.getPublicId(), sbomVersion)
        .get();

    assertResponseStatus(200, response);

    PolicyThreats.Component result = response.getBody(PolicyThreats.Component.class);
    assertThat(result).isNotNull();
    assertThat(result.hash).isEqualTo(hash);
    assertThat(result.allViolations).hasSize(1);
  }

  @Test
  public void testGetSbomPolicyViolationReport_NotFound() throws Exception {
    String scanId = "ReportResourceTest_ScanId";

    String reportResource = "/ReportResourceTest/report-bom-no-policythreats";
    mockReport(scanId, reportResource);
    createScanFile(app.getId(), scanId);

    URL zippedReport = ReportHelper.zipReport(reportResource, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(app.getId(), scanId);

    FileUtils.copyURLToFile(zippedReport, reportDestination);

    String sbomVersion = "sbomVersion1";
    tempEntity.newSbomEvaluation(app, sbomVersion, "spec1",
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"), "hash1", scanId, true, ACTIVE);

    setFeatures(LicensedFeature.SBOM_MANAGER);

    HttpResponse response = restRequest()
        .path(ReportResource.RESOURCE_PATH + "/" + ReportResource.SBOM_POLICY_VIOLATION_REPORT)
        .query("fileCoordinateId", "some-file-coordinate-id")
        .parameter(app.getPublicId(), "sbomVersion1")
        .get();

    assertResponseStatus(404, response);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetSbomPolicyViolationReport_NoPolicyViolations() throws Exception {
    String scanId = "ReportResourceTest_ScanId";

    String reportResource = "/ReportResourceTest/report-bom";
    mockReport(scanId, reportResource);
    createScanFile(app.getId(), scanId);

    URL zippedReport = ReportHelper.zipReport(reportResource, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(app.getId(), scanId);

    FileUtils.copyURLToFile(zippedReport, reportDestination);

    String sbomVersion = "sbomVersion1";

    tempEntity.newSbomEvaluation(app, sbomVersion, "spec1",
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"), "hash1", scanId, true, ACTIVE);

    setFeatures(LicensedFeature.SBOM_MANAGER);

    HttpResponse response = restRequest()
        .path(ReportResource.RESOURCE_PATH + "/" + ReportResource.SBOM_POLICY_VIOLATION_REPORT)
        .query("hash", "some-hash")
        .query("componentRef", "some-ref")
        .parameter(app.getPublicId(), "sbomVersion1")
        .get();

    assertResponseStatus(200, response);
    assertThat(response.getBody(Map.class)).isEmpty();
  }

  private void configureMail() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("127.0.0.1");
    mailConfiguration.setPort(587);
    mailConfiguration.setSystemEmail("NexusIQServer@localhost");
    mailConfigurationDAO.set(mailConfiguration);
  }

  @Test
  public void testReevaluateReport() throws Exception {
    configureMail();

    String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    // Mock the HDS report for the new scan
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    createScanFile(app.getId(), scanId);

    PolicyEvaluation policyEvaluation = policyEvaluationDAO
        .getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation).isNull();

    final Constraint constraint = new Constraint("C1", "testReevaluateReport constraint 1", LogicalOperator.AND);
    final Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint.addCondition(condition);
    final Policy policy = new Policy("P1", "testReevaluateReport policy1");
    policy.setOwnerId(app.getId());
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    policy.getNotifications().add(new UserNotification("manager@test.corp", Stage.ID_BUILD));
    tempEntity.newPolicy(policy);
    final Stage stage = new Stage(Stage.ID_BUILD);

    List<Message> notifications = MailboxTestUtil.get("manager@test.corp");

    // Evaluate policy
    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", scanId)
        .body(stage)
        .post();
    assertResponseStatus(200, response);

    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getScanId()).isEqualTo(scanId);
    assertThat(policyEvaluation.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(System.currentTimeMillis() - policyEvaluation.getTime().getTime()).isLessThan(60 * 1000);
    assertThat(policyEvaluation.isReevaluation()).isFalse();

    assertNotifications(notifications, 1, 5000);
    notifications.clear();

    Thread.sleep(1);

    // ReEvaluate
    policy.setThreatLevel(policy.getThreatLevel() - 1);
    policyDAO.update(policy);
    response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy").post();
    assertResponseStatus(200, response);

    PolicyEvaluation policyReEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(),
        scanId);
    assertThat(policyReEvaluation).isNotNull();
    assertThat(policyReEvaluation.getScanId()).isEqualTo(scanId);
    assertThat(policyReEvaluation.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(policyReEvaluation.getTime().getTime()).isGreaterThan(policyEvaluation.getTime().getTime());
    assertThat(policyReEvaluation.isReevaluation()).isTrue();

    assertNotifications(notifications, 0, 5000);

    // Evaluate the policy for a new scan for the same app. It should send notifications since this is not a
    // reevaluation.
    scanId = "ReportResourceTest_ScanId1";
    mockReport(scanId, "/ReportResourceTest/report");
    createScanFile(app.getId(), scanId);
    response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", scanId)
        .body(stage)
        .post();
    assertResponseStatus(200, response);

    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getScanId()).isEqualTo(scanId);
    assertThat(policyEvaluation.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(System.currentTimeMillis() - policyEvaluation.getTime().getTime()).isLessThan(60 * 1000);
    assertThat(policyEvaluation.isReevaluation()).isFalse();

    assertNotifications(notifications, 1, 5000);
  }

  @Test
  public void testReevaluateReport_async() throws Exception {
    configureMail();

    String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    // Mock the HDS report for the re-uploaded scan
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    createScanFile(app.getId(), scanId);

    // Avoid the polling-interval back-off so the background task completes quickly under test.
    lookup(AsyncReevaluationService.class).disablePollingIntervalForTesting = true;
    PolicyEvaluationHelper policyEvaluationHelper = lookup(PolicyEvaluationHelper.class);

    final Constraint constraint = new Constraint("C1", "async reeval constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    final Policy policy = new Policy("P1", "async reeval policy");
    policy.setOwnerId(app.getId());
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    policy.getNotifications().add(new UserNotification("manager@test.corp", Stage.ID_BUILD));
    tempEntity.newPolicy(policy);
    final Stage stage = new Stage(Stage.ID_BUILD);

    List<Message> notifications = MailboxTestUtil.get("manager@test.corp");

    // Initial (synchronous) evaluation so there is something to re-evaluate; this one DOES notify.
    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", scanId)
        .body(stage)
        .post();
    assertResponseStatus(200, response);

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.isReevaluation()).isFalse();

    assertNotifications(notifications, 1, 5000);
    notifications.clear();

    // Asynchronous re-evaluation returns 202 Accepted immediately with a status id.
    response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy")
        .query("async", "true")
        .post();
    assertResponseStatus(202, response);
    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isNotNull();

    // The background task transitions the polling result to COMPLETED.
    policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

    HttpResponse statusResponse = restRequest().path(ReportResource.RESOURCE_PATH)
        .path("reevaluatePolicy/status/{statusId}")
        .parameter(app.getPublicId(), receipt.getStatusId())
        .get();
    assertResponseStatus(200, statusResponse);
    PolicyEvaluationPollingResultDTO pollingResult = statusResponse.getBody(PolicyEvaluationPollingResultDTO.class);
    assertThat(pollingResult.status).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(pollingResult.reason).isNull();
    assertThat(pollingResult.result).isNotNull();

    // The re-evaluation actually ran on the background thread and was persisted.
    PolicyEvaluation policyReEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyReEvaluation.isReevaluation()).isTrue();
    assertThat(policyReEvaluation.getTime().getTime())
        .isGreaterThanOrEqualTo(policyEvaluation.getTime().getTime());

    // Re-evaluation must not send policy notifications (unlike the initial evaluation above).
    assertNotifications(notifications, 0, 5000);
  }

  @Test
  public void testReevaluateReport_async_failed() throws Exception {
    String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    createScanFile(app.getId(), scanId);

    lookup(AsyncReevaluationService.class).disablePollingIntervalForTesting = true;
    PolicyEvaluationHelper policyEvaluationHelper = lookup(PolicyEvaluationHelper.class);

    // No prior policy evaluation exists for this scan, so the background reUploadScanToHds fails and
    // the polling result must transition to FAILED with a reason rather than throwing on the request thread.
    HttpResponse response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy")
        .query("async", "true")
        .post();
    assertResponseStatus(202, response);
    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isNotNull();

    policyEvaluationHelper.awaitEvaluationFailed(app.getId(), receipt.getStatusId());

    HttpResponse statusResponse = restRequest().path(ReportResource.RESOURCE_PATH)
        .path("reevaluatePolicy/status/{statusId}")
        .parameter(app.getPublicId(), receipt.getStatusId())
        .get();
    assertResponseStatus(200, statusResponse);
    PolicyEvaluationPollingResultDTO pollingResult = statusResponse.getBody(PolicyEvaluationPollingResultDTO.class);
    assertThat(pollingResult.status).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(pollingResult.reason).isNotBlank();

    // The failed re-evaluation must not have persisted a policy evaluation for the scan.
    assertThat(policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId)).isNull();
  }

  @Test
  public void testReevaluateReport_async_containerImageEvaluationDisabled_rejectedAtTrigger() throws Exception {
    // Make the application RM-backed (container) so the status endpoint's container-image gate would apply, then
    // disable that feature. The trigger must reject up front with the same InvalidLicenseException (402) the status
    // endpoint enforces, so the caller never receives a 202 whose status it could not poll.
    OrganizationDAO organizationDAO = lookup(OrganizationDAO.class);
    Organization organization = organizationDAO.getById(app.getOrganizationId());
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);

    String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    createScanFile(app.getId(), scanId);

    boolean originalEnabled = SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled();
    try {
      SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

      HttpResponse response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy")
          .query("async", "true")
          .post();

      // 402 from InvalidLicenseException, not a 202 with a receipt.
      assertResponseStatus(402, response);

      // No polling-result row should have been created for a rejected trigger.
      assertThat(policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId)).isNull();
    }
    finally {
      SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(originalEnabled);
    }
  }

  @Test
  public void testReevaluateReport_sync_containerImageEvaluationDisabled_stillAllowed() throws Exception {
    // Regression guard: the container-image gate applies only to the async path. A synchronous re-evaluation
    // (async omitted) for an RM-backed app with the feature disabled must still succeed (200) — the gate must not
    // expand to the sync path.
    OrganizationDAO organizationDAO = lookup(OrganizationDAO.class);
    Organization organization = organizationDAO.getById(app.getOrganizationId());
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);

    String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    createScanFile(app.getId(), scanId);

    final Constraint constraint = new Constraint("C1", "sync ungated constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    final Policy policy = new Policy("P1", "sync ungated policy");
    policy.setOwnerId(app.getId());
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);
    final Stage stage = new Stage(Stage.ID_BUILD);

    boolean originalEnabled = SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled();
    try {
      SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

      // Initial evaluation so there is a scan to re-evaluate.
      HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
          .parameter(app.getPublicId())
          .query("scanId", scanId)
          .body(stage)
          .post();
      assertResponseStatus(200, response);

      // Synchronous re-evaluation (no async flag) must NOT be gated by the container-image feature.
      response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy").post();
      assertResponseStatus(200, response);

      PolicyEvaluation policyReEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
      assertThat(policyReEvaluation.isReevaluation()).isTrue();
    }
    finally {
      SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(originalEnabled);
    }
  }

  @Test
  public void testReevaluatePolicyStatus_statusIdScopedToApplication_otherAppGets404() throws Exception {
    String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    createScanFile(app.getId(), scanId);

    lookup(AsyncReevaluationService.class).disablePollingIntervalForTesting = true;

    final Constraint constraint = new Constraint("C1", "cross-app isolation constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    final Policy policy = new Policy("P1", "cross-app isolation policy");
    policy.setOwnerId(app.getId());
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);
    final Stage stage = new Stage(Stage.ID_BUILD);

    // Initial evaluation so there is a scan to re-evaluate, then trigger an async re-evaluation on app A.
    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", scanId)
        .body(stage)
        .post();
    assertResponseStatus(200, response);

    response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy")
        .query("async", "true")
        .post();
    assertResponseStatus(202, response);
    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt.getStatusId()).isNotNull();

    // App A can read its own status.
    HttpResponse ownStatus = restRequest().path(ReportResource.RESOURCE_PATH)
        .path("reevaluatePolicy/status/{statusId}")
        .parameter(app.getPublicId(), receipt.getStatusId())
        .get();
    assertResponseStatus(200, ownStatus);

    // A different application cannot read app A's statusId: the (applicationId, statusId) lookup yields 404.
    Application otherApp = tempEntity.newApplicationWithParent("ReportResourceTest_OtherAppId");
    HttpResponse crossAppStatus = restRequest().path(ReportResource.RESOURCE_PATH)
        .path("reevaluatePolicy/status/{statusId}")
        .parameter(otherApp.getPublicId(), receipt.getStatusId())
        .get();
    assertResponseStatus(404, crossAppStatus);
  }

  @Test
  public void testReevaluateReport_async_containerImageEvaluationEnabled_completes() throws Exception {
    // RM-backed (container) app with the container-image feature enabled: the async happy path. The trigger passes
    // the container gate, returns 202, and the status poll resolves through the EVALUATE_COMPONENT /
    // pollEvaluationResultCheckEvaluateComponent path (distinct from the plain-application path).
    OrganizationDAO organizationDAO = lookup(OrganizationDAO.class);
    Organization organization = organizationDAO.getById(app.getOrganizationId());
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);

    String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    createScanFile(app.getId(), scanId);

    lookup(AsyncReevaluationService.class).disablePollingIntervalForTesting = true;
    PolicyEvaluationHelper policyEvaluationHelper = lookup(PolicyEvaluationHelper.class);

    final Constraint constraint = new Constraint("C1", "container async constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    final Policy policy = new Policy("P1", "container async policy");
    policy.setOwnerId(app.getId());
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);
    final Stage stage = new Stage(Stage.ID_BUILD);

    boolean originalEnabled = SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled();
    try {
      SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

      // Initial (synchronous) evaluation so there is a scan to re-evaluate.
      HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
          .parameter(app.getPublicId())
          .query("scanId", scanId)
          .body(stage)
          .post();
      assertResponseStatus(200, response);

      // Async re-evaluation returns 202 (the container gate passes because the feature is enabled).
      response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy")
          .query("async", "true")
          .post();
      assertResponseStatus(202, response);
      PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
      assertThat(receipt.getStatusId()).isNotNull();

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

      // The status poll resolves to COMPLETED through the EVALUATE_COMPONENT path for RM-backed apps.
      HttpResponse statusResponse = restRequest().path(ReportResource.RESOURCE_PATH)
          .path("reevaluatePolicy/status/{statusId}")
          .parameter(app.getPublicId(), receipt.getStatusId())
          .get();
      assertResponseStatus(200, statusResponse);
      PolicyEvaluationPollingResultDTO pollingResult = statusResponse.getBody(PolicyEvaluationPollingResultDTO.class);
      assertThat(pollingResult.status).isEqualTo(PolicyEvaluationStatus.COMPLETED);
      assertThat(pollingResult.result).isNotNull();

      PolicyEvaluation policyReEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
      assertThat(policyReEvaluation.isReevaluation()).isTrue();
    }
    finally {
      SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(originalEnabled);
    }
  }

  @Test
  public void testReevaluateReport_async_hostedScan_ignoresAsyncAndReturns200() throws Exception {
    // Hosted scans re-evaluate synchronously regardless of async=true: reevaluatePolicy returns at the
    // isHostedScan early-return (before the async branch), so the response is 200 OK with an empty body and no
    // statusId. This guards that guard order against future reordering.
    RepositoryComponentDAO repositoryComponentDAO = lookup(RepositoryComponentDAO.class);

    String scanId = "ReportResourceTest_HostedScanId";
    Repository repository = tempEntity.newRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId());
    // isHostedScan(scanId) resolves the component by scanId, so bind this component to the scan under test.
    component.setScanId(scanId);
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      repositoryComponentDAO.update(tx, component);
      tx.commit();
    }

    // A real report file must exist for the scan so the synchronous hosted re-evaluation's saveOverlayFiles can
    // read bom.json back out.
    mockReport(scanId, "/ReportResourceTest/report");
    createScanFile(app.getId(), scanId);
    createReportFile(app.getId(), scanId, "/ReportResourceTest/report");

    // The hosted re-evaluation calls the repository policy evaluator, which fetches component details from HDS.
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentEvaluationData hdsComponent = new ComponentEvaluationData();
    hdsComponent.hash = component.getHash();
    hdsComponent.matchState = MatchState.EXACT.getId();
    hdsComponent.declaredLicenses = new HashSet<>();
    hdsComponent.observedLicenses = new HashSet<>();
    hdsResult.components.add(hdsComponent);
    getHdsServer().respondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);

    // async=true must be ignored: a hosted scan returns 200 (not 202) with an empty body (no receipt/statusId).
    HttpResponse response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy")
        .query("async", "true")
        .post();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isNullOrEmpty();
  }

  @Test
  public void testReevaluateReport_withoutSkippingAutoWaivers() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    // Mock the HDS report for the new scan
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    mockGetVersionsByComponentCI();
    createScanFile(app.getId(), scanId);

    PolicyEvaluation policyEvaluation = policyEvaluationDAO
        .getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation).isNull();

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(app.getId(), 8, false, true);
    final Constraint constraint = new Constraint("C1", "testReevaluateReport constraint 1", LogicalOperator.AND);
    final Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint.addCondition(condition);
    final Policy policy = new Policy("P1", "testReevaluateReport policy1");
    policy.setOwnerId(app.getId());
    policy.setThreatLevel(6);
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);
    final Stage stage = new Stage(Stage.ID_BUILD);

    // Evaluate policy
    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", scanId)
        .body(stage)
        .post();
    assertResponseStatus(200, response);

    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getScanId()).isEqualTo(scanId);
    assertThat(policyEvaluation.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(System.currentTimeMillis() - policyEvaluation.getTime().getTime()).isLessThan(60 * 1000);
    assertThat(policyEvaluation.isReevaluation()).isFalse();

    List<PolicyViolation> autoWaivedPolicyViolations =
        policyViolationDAO.getAutoWaivedByApplicationIdAndStageId(app.getId(),
            Stage.ID_BUILD);

    Thread.sleep(1);

    /*
     * Creating exclusions for all waived violations
     */
    autoWaivedPolicyViolations.forEach(policyViolation -> {
      tempEntity.newAutoPolicyWaiverExclusionForAllVersions(app.getId(),
          autoPolicyWaiver.getId(), scanId,
          PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()).getPackageUrl());
    });

    // ReEvaluate
    response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy").post();
    assertResponseStatus(200, response);

    PolicyEvaluation policyReEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(),
        scanId);
    assertThat(policyReEvaluation).isNotNull();
    assertThat(policyReEvaluation.getScanId()).isEqualTo(scanId);
    assertThat(policyReEvaluation.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(policyReEvaluation.getTime().getTime()).isGreaterThan(policyEvaluation.getTime().getTime());
    assertThat(policyReEvaluation.isReevaluation()).isTrue();

    autoWaivedPolicyViolations =
        policyViolationDAO.getAutoWaivedByApplicationIdAndStageId(app.getId(),
            Stage.ID_BUILD);
    /*
     * Because auto-waivers were not skipped then auto-waived violations should not be present
     * since a exclusion was added for all of them.
     */
    assertThat(autoWaivedPolicyViolations).isEmpty();
  }

  @Test
  public void testReevaluateReport_skippingAutoWaivers() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    // Mock the HDS report for the new scan
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    mockGetVersionsByComponentCI();
    createScanFile(app.getId(), scanId);

    PolicyEvaluation policyEvaluation = policyEvaluationDAO
        .getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation).isNull();

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(app.getId(), 8, false, true);
    final Constraint constraint = new Constraint("C1", "testReevaluateReport constraint 1", LogicalOperator.AND);
    final Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint.addCondition(condition);
    final Policy policy = new Policy("P1", "testReevaluateReport policy1");
    policy.setOwnerId(app.getId());
    policy.setThreatLevel(6);
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);
    final Stage stage = new Stage(Stage.ID_BUILD);

    // Evaluate policy
    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", scanId)
        .body(stage)
        .post();
    assertResponseStatus(200, response);

    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getScanId()).isEqualTo(scanId);
    assertThat(policyEvaluation.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(System.currentTimeMillis() - policyEvaluation.getTime().getTime()).isLessThan(60 * 1000);
    assertThat(policyEvaluation.isReevaluation()).isFalse();

    List<PolicyViolation> autoWaivedPolicyViolations =
        policyViolationDAO.getAutoWaivedByApplicationIdAndStageId(app.getId(),
            Stage.ID_BUILD);

    Thread.sleep(1);

    /*
     * Creating exclusions for all waived violations
     */
    autoWaivedPolicyViolations.forEach(policyViolation -> {
      tempEntity.newAutoPolicyWaiverExclusionForAllVersions(app.getId(),
          autoPolicyWaiver.getId(), scanId,
          PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()).getPackageUrl());
    });

    // ReEvaluate
    response =
        restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy")
            .query("skipAutoWaivers", true)
            .post();
    assertResponseStatus(200, response);

    PolicyEvaluation policyReEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(),
        scanId);
    assertThat(policyReEvaluation).isNotNull();
    assertThat(policyReEvaluation.getScanId()).isEqualTo(scanId);
    assertThat(policyReEvaluation.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(policyReEvaluation.getTime().getTime()).isGreaterThan(policyEvaluation.getTime().getTime());
    assertThat(policyReEvaluation.isReevaluation()).isTrue();

    List<PolicyViolation> reevaluatedAutoPolicyViolations =
        policyViolationDAO.getAutoWaivedByApplicationIdAndStageId(app.getId(),
            Stage.ID_BUILD);

    /*
     * Because auto-waivers were skipped then same auto-waived violations should be present
     * the exclusion logic wouldn't be applied in this case and everything is copied from
     * last report
     */
    assertThat(reevaluatedAutoPolicyViolations).hasSize(autoWaivedPolicyViolations.size());
  }

  @Test
  public void testRedirection() throws Exception {
    String path = "index.html?x=y&a=b";
    HttpResponse response = restRequest("appId", "scanId").path("{scanId}/brain", "index.html").query("x=y&a=b").get();
    assertResponseStatus(307, response);
    assertThat(response.getHeader("Location")).isEqualTo(getRestBaseUrl() + path);
  }

  @Test
  public void testDownloadBundle_LegacyFormat() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";

    mockReport(scanId, "/ReportResourceTest/standalone-legacy");
    createScanFile(app.getId(), scanId);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("commons-httpclient",
        "commons-httpclient", "3.1.SONATYPE");
    HashComponentIdentifier claimedComponent = tempEntity.newClaimedComponent("f0776db1593e215146d2",
        componentIdentifier);
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(app.getId(),
        claimedComponent.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0");
    LicenseOverride licenseOverride2 = tempEntity.newLicenseOverride(app.getId(),
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"),
        LicenseOverrideStatus.OVERRIDDEN, "EPL-1.0");
    Policy policy = tempEntity.newPolicy(app);

    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", scanId)
        .body(new Stage(Stage.ID_BUILD))
        .post();
    assertResponseStatus(200, response);

    response = restRequest(app.getPublicId(), scanId).path(ReportResource.DOWNLOAD_BUNDLE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("application/zip");
    assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("filename=");
    File temp = tempDir.newFile();
    Files.write(temp.toPath(), response.getBodyBytes());
    try (ZipFile zip = new ZipFile(temp)) {
      assertThat(zip.getEntry("data/report.pdf")).isNotNull();
      assertThat(zip.getEntry("detail.rptdesign")).isNull();
      assertThat(zip.getEntry("data/index.html")).isNull();
      // select legacy report artifacts:
      assertThat(zip.getEntry("appcheck.js")).isNotNull();
      assertThat(zip.getEntry("expand.gif")).isNotNull();
      assertThat(zip.getEntry("public/blue.png")).isNotNull();

      assertThat(zip.getEntry("data/components.json")).isNotNull();
      assertThat(zip.getEntry("data/release-graph/tomcat/tomcat-util/5.5.23.png")).isNotNull();
      assertThat(zip.getEntry("data/" + POLICY_THREATS.getName())).isNotNull();

      assertThat(zip.getEntry("cip/details/f0776db1593e215146d2.json")).isNull();
      ComponentDetails details = JsonUtils.parse(
          zip.getInputStream(zip.getEntry("data/cip/details/f0776db1593e215146d2.json")), ComponentDetails.class);
      assertThat(details.getMatchState()).isEqualTo("exact");
      assertComponentIdentifier(details, claimedComponent.getComponentIdentifier());
      assertThat(details.getComponentIdentifier()).isEqualTo(claimedComponent.getComponentIdentifier());
      assertThat(details.getCatalogDate()).isEqualTo(claimedComponent.getCreateTimeLong());
      assertThat(details.getOverriddenLicenses()).hasSize(1);
      assertThat(details.getOverriddenLicenses().iterator().next().getLicenseId())
          .isEqualTo(licenseOverride.getLicenseIds().iterator().next());
      assertThat(details.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Copyleft");
      assertThat(details.getLicenseThreatLevel()).isEqualTo(9);
      assertThat(details.getIdentificationSource()).isEqualTo(IdentificationSource.MANUAL.getId());
      assertThat(details.getIdentificationSourceComment()).isEqualTo(claimedComponent.getComment());
      ComponentDetailsList list = JsonUtils.parse(
          zip.getInputStream(zip.getEntry("data/cip/list/"
              + componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID) + "/"
              + componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) + "/"
              + componentIdentifier.get(ComponentIdentifier.VERSION) + ".json")),
          ComponentDetailsList.class);
      assertThat(list.getList()).isEmpty();

      details = JsonUtils.parse(zip.getInputStream(zip.getEntry("data/cip/details/1249e25aebb15358bedd.json")),
          ComponentDetails.class);
      assertThat(details.getMatchState()).isEqualTo("exact");
      assertThat(details.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
      assertThat(details.getIdentificationSourceComment()).isNull();
      assertThat(details.getPolicyAlerts()).hasSize(4);
      for (PolicyAlert policyAlert : details.getPolicyAlerts()) {
        assertThat(policyAlert.getTrigger().getPolicyId()).isEqualTo(policy.getId());
        assertThat(policyAlert.getTrigger().getComponentFacts()).hasSize(1);
      }

      list = JsonUtils.parse(zip.getInputStream(zip.getEntry("data/cip/list/tomcat/tomcat-util/5.5.23.json")),
          ComponentDetailsList.class);
      details = findDetailsForComponent(list,
          ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"));
      assertThat(details).isNotNull();
      assertThat(details.getOverriddenLicenses()).hasSize(1);
      assertThat(details.getOverriddenLicenses().iterator().next().getLicenseId()).isEqualTo(licenseOverride2
          .getLicenseIds()
          .iterator()
          .next());
      assertThat(details.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Weak Copyleft");
      assertThat(details.getLicenseThreatLevel()).isEqualTo(2);
    }
  }

  @SuppressWarnings("deprecation")
  private void assertComponentIdentifier(ComponentDetails actual, ComponentIdentifier expected) {
    assertThat(actual.getComponentIdentifier()).isEqualTo(expected);
    assertThat(actual.getGroupId()).isEqualTo(expected.get(ComponentIdentifier.MAVEN_GROUP_ID));
    assertThat(actual.getArtifactId()).isEqualTo(expected.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
    assertThat(actual.getVersion()).isEqualTo(expected.get(ComponentIdentifier.VERSION));
  }

  @Test
  public void testDownloadBundle_v2() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/standalone-v2");
    createScanFile(app.getId(), scanId);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("commons-httpclient",
        "commons-httpclient", "3.1.SONATYPE", "", "jar");
    HashComponentIdentifier claimedComponent = tempEntity.newClaimedComponent("f0776db1593e215146d2",
        componentIdentifier);
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(app.getId(),
        claimedComponent.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0");
    LicenseOverride licenseOverride2 = tempEntity.newLicenseOverride(app.getId(),
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"), LicenseOverrideStatus.OVERRIDDEN,
        "EPL-1.0");
    Policy policy = tempEntity.newPolicy(app);

    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", scanId)
        .body(new Stage(Stage.ID_BUILD))
        .post();
    assertResponseStatus(200, response);

    response = restRequest(app.getPublicId(), scanId).path(ReportResource.DOWNLOAD_BUNDLE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("application/zip");
    assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("filename=");
    File temp = tempDir.newFile();
    Files.write(temp.toPath(), response.getBodyBytes());
    try (ZipFile zip = new ZipFile(temp)) {
      assertThat(zip.getEntry("data/report.pdf")).isNotNull();
      assertThat(zip.getEntry("detail.rptdesign")).isNull();
      assertThat(zip.getEntry("data/index.html")).isNull();
      // select legacy report artifacts:
      assertThat(zip.getEntry("appcheck.js")).isNotNull();
      assertThat(zip.getEntry("expand.gif")).isNotNull();
      assertThat(zip.getEntry("public/blue.png")).isNotNull();

      ZipEntry componentEntry = zip.getEntry("data/components.json");
      assertThat(componentEntry).isNotNull();
      ApiReportRawDataDTOV2 components = JsonUtils
          .parse(zip.getInputStream(componentEntry), ApiReportRawDataDTOV2.class);

      assertThat(components.matchSummary.knownComponentCount).isEqualTo(5);
      assertThat(components.matchSummary.totalComponentCount).isEqualTo(29);
      assertComponent("tomcat", "tomcat-util", "5.5.23", "Weak Copyleft", 2, components.components);

      assertThat(zip.getEntry("data/release-graph/maven/"
          + "artifactId=tomcat-util/classifier=/extension=jar/groupId=tomcat/version=5.5.23/releases.png"))
              .isNotNull();
      assertThat(zip.getEntry("data/" + POLICY_THREATS.getName())).isNotNull();

      assertThat(zip.getEntry("cip/details/f0776db1593e215146d2.json")).isNull();
      TestNamedComponentDetails details = JsonUtils.parse(
          zip.getInputStream(zip.getEntry("data/cip/details/f0776db1593e215146d2.json")),
          TestNamedComponentDetails.class);
      assertThat(details.getMatchState()).isEqualTo("exact");
      assertComponentIdentifier(details, claimedComponent.getComponentIdentifier());
      assertThat(details.getDisplayName().toString())
          .isEqualTo("commons-httpclient : commons-httpclient : 3.1.SONATYPE");
      assertThat(details.getCatalogDate()).isEqualTo(claimedComponent.getCreateTimeLong());
      assertThat(details.getOverriddenLicenses()).hasSize(1);
      assertThat(details.getOverriddenLicenses().iterator().next().getLicenseId())
          .isEqualTo(licenseOverride.getLicenseIds().iterator().next());
      assertThat(details.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Copyleft");
      assertThat(details.getLicenseThreatLevel()).isEqualTo(9);
      assertThat(details.getIdentificationSource()).isEqualTo(IdentificationSource.MANUAL.getId());
      assertThat(details.getIdentificationSourceComment()).isEqualTo(claimedComponent.getComment());
      ComponentDetailsList list = JsonUtils.parse(
          zip.getInputStream(zip.getEntry("data/cip/list/maven/artifactId="
              + componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) + "/classifier="
              + componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER) + "/extension="
              + componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION) + "/groupId="
              + componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID) + "/version="
              + componentIdentifier.get(ComponentIdentifier.VERSION) + "/list.json")),
          ComponentDetailsList.class);
      assertThat(list.getList()).isEmpty();

      details = JsonUtils.parse(zip.getInputStream(zip.getEntry("data/cip/details/1249e25aebb15358bedd.json")),
          TestNamedComponentDetails.class);
      assertThat(details.getMatchState()).isEqualTo("exact");
      assertThat(details.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
      assertThat(details.getIdentificationSourceComment()).isNull();
      assertThat(details.getPolicyAlerts()).hasSize(4);
      for (PolicyAlert policyAlert : details.getPolicyAlerts()) {
        assertThat(policyAlert.getTrigger().getPolicyId()).isEqualTo(policy.getId());
        assertThat(policyAlert.getTrigger().getComponentFacts()).hasSize(1);
      }

      list = JsonUtils.parse(zip.getInputStream(zip.getEntry("data/cip/list/maven/"
          + "artifactId=tomcat-util/classifier=/extension=jar/groupId=tomcat/version=5.5.23/list.json")),
          ComponentDetailsList.class);
      ComponentDetails detailsFromList = findDetailsForComponent(list,
          ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"));
      assertThat(detailsFromList).isNotNull();
      assertThat(detailsFromList.getOverriddenLicenses()).hasSize(1);
      assertThat(detailsFromList.getOverriddenLicenses().iterator().next().getLicenseId()).isEqualTo(licenseOverride2
          .getLicenseIds()
          .iterator()
          .next());
      assertThat(detailsFromList.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Weak Copyleft");
      assertThat(detailsFromList.getLicenseThreatLevel()).isEqualTo(2);
    }
  }

  @Test
  public void testDownloadBundle_v3() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/standalone-v3/");
    createScanFile(app.getId(), scanId);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("org.webjars.npm",
        "reactivex:rxjs", "5.0.0-alpha.7", "", "jar");
    Policy policy = tempEntity.newPolicy(app);

    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", scanId)
        .body(new Stage(Stage.ID_BUILD))
        .post();
    assertResponseStatus(200, response);

    response = restRequest(app.getPublicId(), scanId).path(ReportResource.DOWNLOAD_BUNDLE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("application/zip");
    assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("filename=");
    File temp = tempDir.newFile();
    Files.write(temp.toPath(), response.getBodyBytes());
    try (ZipFile zip = new ZipFile(temp)) {
      assertThat(zip.getEntry("data/report.pdf")).isNotNull();
      assertThat(zip.getEntry("detail.rptdesign")).isNull();
      assertThat(zip.getEntry("data/index.html")).isNull();
      // select legacy report artifacts:
      assertThat(zip.getEntry("appcheck.js")).isNotNull();
      assertThat(zip.getEntry("expand.gif")).isNotNull();
      assertThat(zip.getEntry("public/blue.png")).isNotNull();

      ZipEntry componentEntry = zip.getEntry("data/components.json");
      assertThat(componentEntry).isNotNull();
      ApiReportRawDataDTOV2 components = JsonUtils
          .parse(zip.getInputStream(componentEntry), ApiReportRawDataDTOV2.class);

      assertThat(components.matchSummary.knownComponentCount).isEqualTo(1);
      assertThat(components.matchSummary.totalComponentCount).isEqualTo(481); // Jar has a lot of JS in it
      assertComponent("org.webjars.npm", "reactivex:rxjs", "5.0.0-alpha.7", "Sonatype Special Licenses", 5,
          components.components);

      assertThat(zip.getEntry("data/sv/maven/"
          + "artifactId=reactivex%3arxjs/classifier=/extension=jar/groupId=org.webjars.npm/version=5.0.0-alpha.7/"
          + "9276b9bfccfcd3614dc2.cve.CVE-2013-1624.json")).isNotNull();
      assertThat(zip.getEntry("data/release-graph/maven/"
          + "artifactId=reactivex%3arxjs/classifier=/extension=jar/groupId=org.webjars.npm/version=5.0.0-alpha.7/"
          + "releases.png")).isNotNull();
      assertThat(zip.getEntry("data/" + POLICY_THREATS.getName())).isNotNull();

      assertThat(zip.getEntry("cip/details/9276b9bfccfcd3614dc2.json")).isNull();
      TestNamedComponentDetails details = JsonUtils.parse(
          zip.getInputStream(zip.getEntry("data/cip/details/9276b9bfccfcd3614dc2.json")),
          TestNamedComponentDetails.class);
      assertThat(details.getMatchState()).isEqualTo("exact");
      assertComponentIdentifier(details, componentIdentifier);
      assertThat(details.getDisplayName().toString()).isEqualTo("org.webjars.npm : reactivex:rxjs : 5.0.0-alpha.7");
      assertThat(details.getCatalogDate()).isEqualTo(1447958674000L);
      assertThat(details.getOverriddenLicenses()).isEmpty();
      assertThat(details.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Sonatype Special Licenses");
      assertThat(details.getLicenseThreatLevel()).isEqualTo(5);
      assertThat(details.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
      assertThat(details.getPolicyAlerts()).hasSize(1);
      assertThat(details.getPolicyAlerts().get(0).getTrigger().getPolicyId()).isEqualTo(policy.getId());
      assertThat(details.getPolicyAlerts().get(0).getTrigger().getComponentFacts()).hasSize(1);

      ComponentDetailsList list = JsonUtils.parse(zip.getInputStream(zip.getEntry("data/cip/list/maven/"
          + "artifactId=reactivex%3arxjs/classifier=/extension=jar/groupId=org.webjars.npm/version=5.0.0-alpha.7"
          + "/list.json")), ComponentDetailsList.class);
      assertThat(list.getList()).hasSize(1);
    }
  }

  @Test
  public void testToDataPathV3_invalidCharacters() {
    for (char c = 0; c < 16; c++) {
      assertThat(ReportResource.toDataPathV3(identifier(c))).isEqualTo("bb/x=%0" + Integer.toHexString(c));
    }

    for (char c = 16; c < 31; c++) {
      assertThat(ReportResource.toDataPathV3(identifier(c))).isEqualTo("bb/x=%" + Integer.toHexString(c));
    }

    assertThat(ReportResource.toDataPathV3(identifier('*'))).isEqualTo("bb/x=%2a");
    assertThat(ReportResource.toDataPathV3(identifier('\\'))).isEqualTo("bb/x=%5c");
    assertThat(ReportResource.toDataPathV3(identifier('/'))).isEqualTo("bb/x=%2f");
    assertThat(ReportResource.toDataPathV3(identifier('?'))).isEqualTo("bb/x=%3f");
    assertThat(ReportResource.toDataPathV3(identifier(':'))).isEqualTo("bb/x=%3a");
    assertThat(ReportResource.toDataPathV3(identifier('|'))).isEqualTo("bb/x=%7c");
    assertThat(ReportResource.toDataPathV3(identifier('"'))).isEqualTo("bb/x=%22");
    assertThat(ReportResource.toDataPathV3(identifier('<'))).isEqualTo("bb/x=%3c");
    assertThat(ReportResource.toDataPathV3(identifier('>'))).isEqualTo("bb/x=%3e");
  }

  @Test
  public void testGetReportMetadata() throws Exception {
    final String scanId = "ScanId";

    createReportFile(app.getId(), scanId, "/ReportResourceTest/report-expanded_coverage_false");

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    // Verify Response for scan
    HttpResponse response = restRequest(app.getPublicId(), scanId).path(ReportResource.METADATA_PATH).get();
    assertResponseStatus(200, response);
    ReportMetadataDTO metadata = response.getBody(ReportMetadataDTO.class);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getReportTitle()).isEqualTo("Build Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval.getTime());

    // Unknown scan id
    response = restRequest(app.getPublicId(), "12345678").path(ReportResource.METADATA_PATH).get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Could not find a report with ID 12345678");
  }

  private static ComponentIdentifier identifier(Character c) {
    return new ComponentIdentifier("bb", Collections.singletonMap("x", String.valueOf(c)));
  }

  @Test
  public void testAppendCacheBustingParams() {
    String indexContent = "<script type='text/javascript' src='../brain/policy-assets/js/brain.client.js'></script>"
        + "<script type='text/javascript' src='../brain/policy-assets/js/cip-loader.js'></script>";
    String expectedIndexContent = "<script type='text/javascript' src='../brain/policy-assets/js/brain.client.js?1.0'>"
        + "</script><script type='text/javascript' src='../brain/policy-assets/js/cip-loader.js?1.0'></script>";

    ReportEntry entry =
        new ReportEntry("index.html", System.currentTimeMillis(), indexContent.getBytes(StandardCharsets.UTF_8));
    entry = ReportResource.appendCacheBustingParams(entry, "1.0");

    assertThat(entry.buf).isEqualTo(expectedIndexContent.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertComponent(
      String groupId,
      String artifactId,
      String version,
      String threatGroup,
      int threatLevel,
      List<ApiReportComponentDTOV2> components)
  {
    for (ApiReportComponentDTOV2 candidate : components) {
      Map<String, String> coordinates = candidate.componentIdentifier == null
          ? null
          : candidate.componentIdentifier
              .getCoordinates();

      if (coordinates != null && groupId.equals(coordinates.get("groupId"))
          && artifactId.equals(coordinates.get("artifactId")) && version.equals(coordinates.get("version")))
      {
        for (ApiLicenseThreatDTOV2 effectiveLicense : candidate.licenseData.effectiveLicenseThreats) {
          if (threatGroup.equals(effectiveLicense.licenseThreatGroupName)) {
            assertThat(effectiveLicense.licenseThreatGroupLevel).isEqualTo(threatLevel);

            if (threatLevel > 7) {
              assertThat(effectiveLicense.licenseThreatGroupCategory).isEqualTo("critical");
            }
            else if (threatLevel > 3) {
              assertThat(effectiveLicense.licenseThreatGroupCategory).isEqualTo("severe");
            }
            else if (threatLevel > 0) {
              assertThat(effectiveLicense.licenseThreatGroupCategory).isEqualTo("moderate");
            }
            else {
              assertThat(effectiveLicense.licenseThreatGroupCategory).isEqualTo("no-threat");
            }
            return;
          }
        }
        fail("Failed to find LTG");
      }
    }
    fail("Failed to find component");
  }

  private ComponentDetails findDetailsForComponent(ComponentDetailsList list, ComponentIdentifier componentIdentifier) {
    for (ComponentDetails details : list.getList()) {
      if (componentIdentifier.equals(details.getComponentIdentifier())) {
        return details;
      }
    }
    return null;
  }

  private void testDataJsonApplyChanges(String json) throws IOException {
    final ContainerNode<?> data = JsonUtils.parse(json);

    assertThat(data.get("weakcopyleftLicenseCount").asInt()).isEqualTo(2);
    assertThat(data.get("nonStandardLicenseCount").asInt()).isEqualTo(2);
    assertThat(data.get("copyleftLicenseCount").asInt()).isEqualTo(3);
    assertThat(data.get("liberalLicenseCount").asInt()).isEqualTo(21);
    assertThat(data.get("notProvidedLicenseCount").asInt()).isEqualTo(1);
    assertThat(data.get("effectiveLicenseCounts").toString()).isEqualTo("[11,0,1,0,0,11,2,0,0,4,0]");

    assertThat(data.get("insecureArtifactCount").asInt()).isEqualTo(8);
    assertThat(data.get("securityCounts").toString()).isEqualTo("[0,4,0,0,2,13,15,2,0,1]");

    assertThat(data.get("policyCounts").toString()).isEqualTo("[2,0,0,0,0,0,0,0,0,0,0]");
    assertThat(data.get("policyComponentCount").asInt()).isEqualTo(0);
    assertThat(data.get("grandfatheredPolicyViolationCount").asInt()).isEqualTo(2);

    assertThat(data.get("securityPunchCard").toString()).isEqualTo("[[4,11,3],[0,18,0],[0,12,0],[0,6,0],[0,6,0]]");
    assertThat(data.get("licensePunchCard").toString()).isEqualTo("[[2,7,1],[2,6,0],[1,3,0],[0,1,0],[0,1,0]]");

    assertThat(data.get("exactlyMatchedComponentCount").asInt()).isEqualTo(27);
    assertThat(data.get("knownArtifactCount").asInt()).isEqualTo(29);
    assertThat(data.get("partiallyMatchedComponentCount").asInt()).isEqualTo(2);
  }

  private void testLicensesJsonApplyChanges(String json) throws IOException {
    final ContainerNode<?> licenses = JsonUtils.parse(json);
    final JsonNode aaData = licenses.get("aaData");
    int countNotZero = 0;
    for (JsonNode license : aaData) {
      JsonNode effectiveLicenseThreat = license.get("effectiveLicenseThreat");
      assertThat(effectiveLicenseThreat).isNotNull();
      int threat = effectiveLicenseThreat.asInt();
      assertThat(threat).isBetween(0, 10);
      if (threat > 0) {
        countNotZero++;
      }
    }
    assertThat(countNotZero).isPositive();
  }

  private void testJsonApplyComponentChanges(String json) throws IOException {
    final ContainerNode<?> components = JsonUtils.parse(json);
    final ArrayNode aaData = (ArrayNode) components.get("aaData");

    for (int i = 0; i < aaData.size(); i++) {
      testJsonApplyDisplayNameChanges(aaData.get(i));
    }
  }

  private void testLicenseThreatsJsonApplyChanges(String json) throws IOException {
    final ContainerNode<?> licenseThreats = JsonUtils.parse(json);
    final JsonNode aaData = licenseThreats.get("aaData");
    int countNotZero = testLicenseThreatsApplyChanges(aaData);
    assertThat(countNotZero).isPositive();
  }

  private void testPartialMatchedJsonApplyChanges(String json) throws IOException {
    final ContainerNode<?> partialMatched = JsonUtils.parse(json);
    final JsonNode aaNode = partialMatched.get("aaData");
    for (JsonNode license : aaNode) {
      final JsonNode matchedComponentNodes = license.get("matchDetails");
      assertThat(matchedComponentNodes).isNotEmpty();
      testLicenseThreatsApplyChanges(matchedComponentNodes);

      for (JsonNode matchDetail : matchedComponentNodes) {
        testJsonApplyDisplayNameChanges(matchDetail);
      }
    }
  }

  private void testAllPathNamesEntriesAreEmpty(String json) throws IOException {
    final ContainerNode<?> partialMatched = JsonUtils.parse(json);
    final JsonNode aaNode = partialMatched.get("aaData");
    for (JsonNode license : aaNode) {
      final ArrayNode pathnames = (ArrayNode) license.get("pathnames");
      assertThat(pathnames).isEmpty();
    }
  }

  private void testJsonApplyDisplayNameChanges(JsonNode jsonNode) throws IOException {
    ComponentIdentifier componentIdentifier = JsonUtils.asPojo(jsonNode.get("componentIdentifier"),
        ComponentIdentifier.class);
    ArrayNode displayNameNode = (ArrayNode) jsonNode.get("displayName").get("parts");
    assertThat(displayNameNode).isNotNull();
    switch (componentIdentifier.getFormat()) {
      case ComponentIdentifier.FORMAT_MAVEN:
        assertThat(displayNameNode).hasSize(5);
        assertThat(displayNameNode.get(0).get("field").textValue()).isEqualTo("Group");
        assertThat(displayNameNode.get(0).get("value").textValue()).isEqualTo(jsonNode.get("groupId").textValue());
        assertThat(displayNameNode.get(1).get("field")).isNull();
        assertThat(displayNameNode.get(1).get("value").textValue()).isEqualTo(" : ");
        assertThat(displayNameNode.get(2).get("field").textValue()).isEqualTo("Artifact");
        assertThat(displayNameNode.get(2).get("value").textValue()).isEqualTo(jsonNode.get("artifactId").textValue());
        assertThat(displayNameNode.get(3).get("field")).isNull();
        assertThat(displayNameNode.get(3).get("value").textValue()).isEqualTo(" : ");
        assertThat(displayNameNode.get(4).get("field").textValue()).isEqualTo("Version");
        assertThat(displayNameNode.get(4).get("value").textValue()).isEqualTo(jsonNode.get("version").textValue());
        break;
      case ComponentIdentifier.FORMAT_ANAME:
        assertThat(displayNameNode).hasSize(3);
        assertThat(displayNameNode.get(0).get("field").textValue()).isEqualTo("Name");
        assertThat(displayNameNode.get(0).get("value").textValue())
            .isEqualTo(componentIdentifier.get(ComponentIdentifier.ANAME_NAME));
        assertThat(displayNameNode.get(1).get("field")).isNull();
        assertThat(displayNameNode.get(1).get("value").textValue()).isEqualTo(" ");
        assertThat(displayNameNode.get(2).get("field").textValue()).isEqualTo("Version");
        assertThat(displayNameNode.get(2).get("value").textValue()).isEqualTo(jsonNode.get("version").textValue());
        break;
      case ComponentIdentifier.FORMAT_PYPI:
        assertThat(displayNameNode).hasSize(6);
        assertThat(displayNameNode.get(0).get("field").textValue()).isEqualTo("Name");
        assertThat(displayNameNode.get(0).get("value").textValue())
            .isEqualTo(componentIdentifier.get(ComponentIdentifier.PYPI_NAME));
        assertThat(displayNameNode.get(1).get("field")).isNull();
        assertThat(displayNameNode.get(1).get("value").textValue()).isEqualTo(" ");
        assertThat(displayNameNode.get(2).get("field").textValue()).isEqualTo("Version");
        assertThat(displayNameNode.get(2).get("value").textValue()).isEqualTo(jsonNode.get("version").textValue());
        assertThat(displayNameNode.get(3).get("field")).isNull();
        assertThat(displayNameNode.get(3).get("value").textValue()).isEqualTo(" (.");
        assertThat(displayNameNode.get(4).get("field").textValue()).isEqualTo("Extension");
        assertThat(displayNameNode.get(4).get("value").textValue()).isEqualTo(jsonNode.get("extension").textValue());
        assertThat(displayNameNode.get(5).get("field")).isNull();
        assertThat(displayNameNode.get(5).get("value").textValue()).isEqualTo(")");
        break;
      default:
        fail("Unexpected format " + componentIdentifier.getFormat());
    }
  }

  private int testLicenseThreatsApplyChanges(JsonNode licenses) {
    int countNotZero = 0;
    for (JsonNode licenseThreat : licenses) {
      int threat = licenseThreat.asInt();
      assertThat(threat).isBetween(0, 10);
      if (threat > 0) {
        countNotZero++;
      }
    }
    return countNotZero;
  }

  private void assertSecurityReport(String dirName, int vulnerabilitiesSize) throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    createReportFile(app.getId(), scanId, "/ReportResourceTest/" + dirName);
    HttpResponse response =
        restRequest(app.getPublicId(), scanId).path(BROWSE_PATH, SECURITY_JSON.getName()).get();
    assertResponseStatus(200, response);

    File temp = tempDir.newFile();
    Files.write(temp.toPath(), response.getBodyBytes());
    InputStream targetStream = Files.newInputStream(temp.toPath());
    JsonNode jsonNode = JsonUtils.parse(targetStream, JsonNode.class);
    assertThat(jsonNode.withArray("aaData").size()).isEqualTo(vulnerabilitiesSize);
  }

  @Test
  public void testAuditLog_includesOrgScopedEntries() throws Exception {
    // Create an org-scoped license override audit entry directly in the org's audit directory
    String orgId = app.getOrganizationId();
    InsightWork work = getCLMServer().getInstance(InsightWork.class);
    File orgAuditDir = work.getAuditDir(orgId);
    boolean createdDir = orgAuditDir.mkdirs();

    ArrayNode logArray = JsonUtils.arrayNode(null);
    ObjectNode auditEntry = logArray.addObject();
    auditEntry.put("time", System.currentTimeMillis());
    auditEntry.put("user", "admin");
    auditEntry.put("ip", "127.0.0.1");
    auditEntry.putNull("where");
    ArrayNode dataArray = auditEntry.putArray("data");
    ObjectNode dataEntry = dataArray.addObject();
    ObjectNode ci = dataEntry.putObject("componentIdentifier");
    ci.put("format", "maven");
    ObjectNode coords = ci.putObject("coordinates");
    coords.put("groupId", "org.test");
    coords.put("artifactId", "test-artifact");
    coords.put("version", "1.0.0");
    coords.put("classifier", "");
    coords.put("extension", "jar");
    dataEntry.put("status", "Overridden");
    dataEntry.put("comment", "org-scoped override");

    File licenseFile = new File(orgAuditDir, "licenses.json");
    JsonUtils.write(licenseFile, logArray);

    try {
      // Call the audit log endpoint for the application
      HttpResponse response = restRequest()
          .path(ReportResource.RESOURCE_PATH)
          .path("{scanId}/auditLog/{path}")
          .parameter(app.getPublicId(), SCAN_ID, "licenses.json")
          .get();
      assertResponseStatus(200, response);

      // Verify the org-scoped entry is included in the response
      JsonNode body = response.getBody(JsonNode.class);
      assertThat(body).isNotNull();
      ArrayNode entries = (ArrayNode) body.get("aaData");
      assertThat(entries).isNotNull();

      boolean foundOrgEntry = false;
      for (JsonNode entry : entries) {
        if ("org-scoped override".equals(entry.path("comment").asText())) {
          foundOrgEntry = true;
          break;
        }
      }
      assertThat(foundOrgEntry).as("Org-scoped audit entry should appear in app audit log").isTrue();
    }
    finally {
      licenseFile.delete();
      if (createdDir) {
        orgAuditDir.delete();
      }
    }
  }
}

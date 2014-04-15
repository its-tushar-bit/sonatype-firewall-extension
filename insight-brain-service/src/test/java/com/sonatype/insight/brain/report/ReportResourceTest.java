/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.mail.Message;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.ComponentDetailsList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.component.HashGAVResource;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.license.LicenseOverrideResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.RestAccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReportResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testManuallyIdentifiedComponent() throws Exception {
    // The hash of commons-httpclient-3.1.SONATYPE.jar, similar match of commons-httpclient:commons-httpclient:3.1
    String hash = "f0776db1593e215146d2";
    String groupId = "testClaimedComponent_G";
    String artifactId = "testClaimedComponent_A";
    String version = "testClaimedComponent_V";
    String extension = "testClaimedComponent_E";
    String classifier = "testClaimedComponent_C";
    Date createTime = new Date();
    HashGAV hashGAV = new HashGAV(hash, groupId, artifactId, version, extension, classifier);
    hashGAV.setCreateTime(createTime);
    HashGAVDAO hashGAVDAO = new HashGAVDAO();
    hashGAVDAO.insert(hashGAV);

    String applicationPublicId = "testClaimedComponent_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    String scanId = "testClaimedComponent_ScanId";
    String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    assertResponseStatus(200,
        AuthedRestAccess.get(getRestBaseUrl() + ReportResource.getReportPath(applicationPublicId, scanId)));

    String resourcePrefix = getServiceURL(applicationPublicId, scanId);
    Response response = AuthedRestAccess.get(resourcePrefix + "/browseReport/bom.json");
    assertResponseStatus(200, response);
    boolean foundClaimedComponent = false;
    String bomJsonData = response.getResponseBody();
    for (JsonNode bomJsonNode : JsonUtils.parse(bomJsonData).get("aaData")) {
      String bomJsonHash = bomJsonNode.get("hash").asText();
      JsonNode identificationSource = bomJsonNode.get("identificationSource");
      if (hash.equals(bomJsonHash)) {
        assertEquals(IdentificationSource.MANUAL.getId(), identificationSource.asText());
        assertEquals(groupId, bomJsonNode.get("groupId").asText());
        assertEquals(artifactId, bomJsonNode.get("artifactId").asText());
        assertEquals(version, bomJsonNode.get("version").asText());
        assertEquals(extension, bomJsonNode.get("extension").asText());
        assertEquals(classifier, bomJsonNode.get("classifier").asText());
        assertEquals(MatchState.EXACT.getId(), bomJsonNode.get("matchState").asText());
        assertEquals(createTime.getTime(), bomJsonNode.get("createTime").asLong());
        assertEquals(0F, bomJsonNode.get("relativePopularity").asDouble(), 0F);
        foundClaimedComponent = true;
      }
      else {
        assertNull(identificationSource);
      }
    }
    assertTrue(foundClaimedComponent);

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/licenses.json");
    assertResponseStatus(200, response);
    String licensesJsonData = response.getResponseBody();
    assertNotNull(licensesJsonData);
    assertFalse(StringUtils.isEmpty(licensesJsonData));
    assertFalse(licensesJsonData.contains(hash));
    assertFalse(licensesJsonData.contains("commons-httpclient"));

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    String securityJsonData = response.getResponseBody();
    assertNotNull(securityJsonData);
    assertFalse(StringUtils.isEmpty(securityJsonData));
    assertFalse(securityJsonData.contains(hash));
    assertFalse(securityJsonData.contains("commons-httpclient"));

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/partialmatched.json");
    assertResponseStatus(200, response);
    String partialmatched = response.getResponseBody();
    assertNotNull(partialmatched);
    assertFalse(StringUtils.isEmpty(partialmatched));
    assertFalse(partialmatched.contains(hash));
    assertFalse(partialmatched.contains("commons-httpclient"));
    assertTrue(partialmatched.contains("c32df577f739535648b0"));
    assertTrue(partialmatched.contains("org.slf4j.api_1.6.1.v20100831-0715.jar"));

    hashGAVDAO.delete(hashGAV);
  }

  @Test
  public void testManuallyIdentifiedComponent_LicenseOverrides() throws Exception {
    // The hash of commons-httpclient-3.1.SONATYPE.jar, similar match of commons-httpclient:commons-httpclient:3.1
    String hash = "f0776db1593e215146d2";
    String groupId = "testClaimedComponent_G";
    String artifactId = "testClaimedComponent_A";
    String version = "testClaimedComponent_V";
    String extension = "testClaimedComponent_E";
    String classifier = "testClaimedComponent_C";
    Date createTime = new Date();
    HashGAV hashGAV = new HashGAV(hash, groupId, artifactId, version, extension, classifier);
    hashGAV.setCreateTime(createTime);
    HashGAVDAO hashGAVDAO = new HashGAVDAO();
    hashGAVDAO.insert(hashGAV);

    String applicationPublicId = "testClaimedComponent_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    String licenseId = new LicenseDAO().getByIdNotNull("GPL-3.0").getId(); // db lookup to make sure licenseId is valid
    LicenseOverride licenseOverride = new LicenseOverride(application.getId(), groupId, artifactId, version,
        LicenseOverrideStatus.OVERRIDDEN, licenseId, "manual override");
    new LicenseOverrideDAO().insert(licenseOverride);

    String scanId = "testClaimedComponent_ScanId";
    String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    assertResponseStatus(200,
        AuthedRestAccess.get(getRestBaseUrl() + ReportResource.getReportPath(applicationPublicId, scanId)));

    String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    Response response = AuthedRestAccess.get(resourcePrefix + "/browseReport/licenses.json");
    assertResponseStatus(200, response);
    String licensesJsonData = response.getResponseBody();
    assertNotNull(licensesJsonData);
    assertFalse(StringUtils.isEmpty(licensesJsonData));
    assertTrue(licensesJsonData.contains(hash));
    assertTrue(licensesJsonData.contains(artifactId));
    assertFalse(licensesJsonData.contains("commons-httpclient"));

    hashGAVDAO.delete(hashGAV);
  }

  @Test
  public void testManuallyIdentifiedComponentInvalidatesCachedReportData() throws Exception {
    String applicationPublicId = "testClaimedComponent_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    String scanId = "testClaimedComponent_ScanId";
    String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    // populate JSON data cache before claiming the component
    String resourcePrefix = getServiceURL(applicationPublicId, scanId);
    Response response = AuthedRestAccess.get(resourcePrefix + "/browseReport/bom.json");
    assertResponseStatus(200, response);

    // The hash of commons-httpclient-3.1.SONATYPE.jar, similar match of commons-httpclient:commons-httpclient:3.1
    String hash = "f0776db1593e215146d2";
    String groupId = "testClaimedComponent_G";
    String artifactId = "testClaimedComponent_A";
    String version = "testClaimedComponent_V";
    String extension = "testClaimedComponent_E";
    String classifier = "testClaimedComponent_C";
    HashGAV hashGAV = new HashGAV(hash, groupId, artifactId, version, extension, classifier);
    setSaasResponseForURI("rest/ide/component?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version
        + "&extension=" + extension + "&classifier=" + classifier, JsonHelpers.asJson(ComponentSummary.create(false)),
        200);
    response = AuthedRestAccess.post(getRestBaseUrl() + HashGAVResource.SERVICE_PATH, JsonHelpers.asJson(hashGAV));
    assertResponseStatus(200, response);

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/bom.json");
    assertResponseStatus(200, response);
    boolean foundClaimedComponent = false;
    String bomJsonData = response.getResponseBody();
    for (JsonNode bomJsonNode : JsonUtils.parse(bomJsonData).get("aaData")) {
      String bomJsonHash = bomJsonNode.get("hash").asText();
      JsonNode identificationSource = bomJsonNode.get("identificationSource");
      if (hash.equals(bomJsonHash)) {
        assertEquals(IdentificationSource.MANUAL.getId(), identificationSource.asText());
        assertEquals(groupId, bomJsonNode.get("groupId").asText());
        assertEquals(artifactId, bomJsonNode.get("artifactId").asText());
        assertEquals(version, bomJsonNode.get("version").asText());
        assertEquals(extension, bomJsonNode.get("extension").asText());
        assertEquals(classifier, bomJsonNode.get("classifier").asText());
        assertEquals(MatchState.EXACT.getId(), bomJsonNode.get("matchState").asText());
        foundClaimedComponent = true;
      }
      else {
        assertNull(identificationSource);
      }
    }
    assertTrue(foundClaimedComponent);

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/licenses.json");
    assertResponseStatus(200, response);
    String licensesJsonData = response.getResponseBody();
    assertNotNull(licensesJsonData);
    assertFalse(StringUtils.isEmpty(licensesJsonData));
    assertFalse(licensesJsonData.contains(hash));
    assertFalse(licensesJsonData.contains("commons-httpclient"));

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    String securityJsonData = response.getResponseBody();
    assertNotNull(securityJsonData);
    assertFalse(StringUtils.isEmpty(securityJsonData));
    assertFalse(securityJsonData.contains(hash));
    assertFalse(securityJsonData.contains("commons-httpclient"));

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/partialmatched.json");
    assertResponseStatus(200, response);
    String partialmatched = response.getResponseBody();
    assertNotNull(partialmatched);
    assertFalse(StringUtils.isEmpty(partialmatched));
    assertFalse(partialmatched.contains(hash));
    assertFalse(partialmatched.contains("commons-httpclient"));

    HashGAVDAO hashGAVDAO = new HashGAVDAO();
    hashGAV = hashGAVDAO.getByHash(hashGAV.getHash());
    hashGAVDAO.delete(hashGAV);
  }

  @Test
  public void testBrowseReportEntryExpirationDate() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    final Calendar calendar = Calendar.getInstance();
    final SimpleDateFormat expirationHeaderFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm", Locale.ENGLISH);
    expirationHeaderFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    calendar.setTime(new Date());
    calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
    Response response = AuthedRestAccess.get(resourcePrefix + "/browseReport/index.html");
    assertResponseStatus(200, response);
    String expiresHeader = response.getHeader("Expires");
    assertNotNull(expiresHeader);
    Date expires = expirationHeaderFormat.parse(expiresHeader);
    assertTrue("index.html expires in one year: " + expires + " vs " + calendar.getTime(),
        Math.abs(calendar.getTimeInMillis() - expires.getTime()) <= 2 * 60 * 1000);

    calendar.setTime(new Date());
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/data.json");
    assertResponseStatus(200, response);
    expiresHeader = response.getHeader("Expires");
    expires = expirationHeaderFormat.parse(expiresHeader);
    assertTrue("data.json expires immediately: " + expires + " vs " + calendar.getTime(),
        Math.abs(calendar.getTimeInMillis() - expires.getTime()) <= 2 * 60 * 1000);

    Map<String, String> ifModifiedSinceHeader = new HashMap<String, String>();
    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
    ifModifiedSinceHeader.put("If-Modified-Since",
        new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).format(calendar.getTime()));
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/data.json", ifModifiedSinceHeader);
    assertResponseStatus(304, response);
  }

  @Test
  public void testBrowseReport() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    final ZipFile zipFile = new ZipFile(saasReportFile);
    final Enumeration<? extends ZipEntry> e = zipFile.entries();
    while (e.hasMoreElements()) {
      final ZipEntry entry = e.nextElement();
      final Response response = AuthedRestAccess.get(resourcePrefix + "/browseReport/" + entry.getName());
      final String contentType = response.getContentType();
      assertResponseStatus(200, response);

      if ("data.json".equals(entry.getName())) {
        String actual = response.getResponseBody();
        testDataJsonApplyChanges(actual);
      }
      else if ("badges.json".equals(entry.getName())) {
        assertThat(JsonUtils.parse(response.getResponseBodyAsBytes(), int[].class), equalTo(new int[] { 36, 8, 36 }));
      }
      else if ("licenses.json".equals(entry.getName())) {
        String expected = IOUtil.toString(zipFile.getInputStream(entry), "UTF-8");
        String actual = response.getResponseBody();

        testLicensesJsonApplyChanges(actual);

        // embedded report processor modifies the effectiveLicenseThreat property type
        String alteredExpected = expected.replaceAll(",\\s*\"effectiveLicenseThreat\" : \"[^\"]+\"", "");
        String alteredActual = actual.replaceAll(",\\s*\"effectiveLicenseThreat\" : [^,]+", "");

        assertThat(alteredActual, equalToIgnoringWhiteSpace(alteredExpected));
      }
      else if ("licensethreats.json".equals(entry.getName())) {
        String actual = response.getResponseBody();

        testLicenseThreatsJsonApplyChanges(actual);
      }
      else if ("partialmatched.json".equals(entry.getName())) {
        String actual = response.getResponseBody();

        testPartialMatchedJsonApplyChanges(actual);
      }
      else if ("index.html".equals(entry.getName())) {
        String actual = response.getResponseBody();
        assertTrue("The app public id was not included in the report",
            actual.contains("applicationId = '" + applicationPublicId + "'"));
      }
      else if (contentType.startsWith("text") || contentType.endsWith("json")) {
        assertThat(response.getResponseBody(),
            equalToIgnoringWhiteSpace(IOUtil.toString(zipFile.getInputStream(entry), "UTF-8")));
      }
      else {
        assertThat(IOUtil.toByteArray(response.getResponseBodyAsStream()),
            equalTo(IOUtil.toByteArray(zipFile.getInputStream(entry))));
      }
    }

    zipFile.close();

    assertResponseStatus(200,
        AuthedRestAccess.get(getRestBaseUrl() + ReportResource.getReportPath(applicationPublicId, scanId)));
  }

  @Test
  public void testBrowseReport_NoDirectoryTraversal() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    final String appId = tempEntity.newApplicationWithParent(applicationPublicId).getId();
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyURLToFile(testReportResultUrl, saasReportFile);
    File reportDir = brain.getReportDir(appId, scanId);
    reportDir.mkdirs();
    new File(reportDir, "restricted.txt").createNewFile();

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    Response response = AuthedRestAccess.get(resourcePrefix + "/browseReport/../restricted.txt");
    assertResponseStatus(404, response);

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/%2E%2E/restricted.txt");
    assertResponseStatus(404, response);

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/%2E%2E%5Crestricted.txt");
    assertResponseStatus(404, response);
  }

  @Test
  public void testEmbedReport() throws Exception {
    String scanId = "abcdefg12345";
    String appPublicId = "bom1-12345678";

    Response response = RestAccess.get(getRestUrl(ReportResource.SERVICE_PATH + "/embedReport/index.html", appPublicId,
        scanId));
    assertResponseStatus(200, response);

    String content = response.getResponseBody();
    assertTrue(content.contains(getRestUrl(UserInterfaceLinksResource.SERVICE_PATH + "/" + UserInterfaceLinksResource.REPORT_PATH, appPublicId, scanId)));
    assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", response.getHeader("Expires"));
  }

  @Test
  public void testEmbedReport_Json() throws Exception {
    String scanId = "abcdefg12345";
    String appPublicId = "bom1-12345678";

    Response response = RestAccess.get(getRestUrl(ReportResource.SERVICE_PATH + "/embedReport/"
        + PolicyEvaluationUtils.POLICY_ALERTS_FILENAME, appPublicId, scanId));
    assertResponseStatus(404, response);
    assertEquals("Reports have been moved.  Clear cache and reload.", response.getResponseBody());
  }

  @Test
  public void testPrintReport() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    final Response response;
    try {
      response = AuthedRestAccess.get(resourcePrefix + "/printReport?projectName=Test%20Project&buildNumber=8");
      assertResponseStatus(200, response);
      assertThat(response.getHeader("Content-Disposition"),
          stringContainsInOrder(Arrays.asList("attachment; filename=", "Test%20Project-8-", ".pdf")));
    }
    finally {
      Pdf.destroy();
    }

    // validate content type and check the actual content is really a PDF
    assertThat(response.getContentType(), equalTo("application/pdf"));
    assertThat(response.getResponseBodyExcerpt(1024, "US-ASCII"), containsString("%PDF-"));
  }

  @Test
  public void testPrintReport_AfterPreviousGenerationFailure() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    final String appId = tempEntity.newApplicationWithParent(applicationPublicId).getId();
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    Response response;
    try {
      response = AuthedRestAccess.get(resourcePrefix + "/printReport?projectName=Test%20Project&buildNumber=8");
      assertResponseStatus(200, response);

      // pretend the print attempt crashed with OOME, which usually leaves an empty PDF file around
      File pdfFile = new File(new File(new File(brain.getWorkDir(), "report/" + appId), scanId), "report.pdf");
      assertTrue(pdfFile.getPath(), pdfFile.isFile());
      new FileOutputStream(pdfFile).close();

      // printing again after fixing the mem setting should produce a proper PDF
      response = AuthedRestAccess.get(resourcePrefix + "/printReport?projectName=Test%20Project&buildNumber=8");
      assertResponseStatus(200, response);
      assertThat(response.getHeader("Content-Disposition"),
          stringContainsInOrder(Arrays.asList("attachment; filename=", "Test%20Project-8-", ".pdf")));
      assertThat(Long.parseLong(response.getHeader("Content-Length")), greaterThan(0L));
    }
    finally {
      Pdf.destroy();
    }

    // validate content type and check the actual content is really a PDF
    assertThat(response.getContentType(), equalTo("application/pdf"));
    assertThat(response.getResponseBodyExcerpt(1024, "US-ASCII"), containsString("%PDF-"));
  }

  @Test
  public void testReevaluateReport() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    final Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyEvaluation policyEvaluation = policyEvaluationDAO
        .getLastByApplicationIdAndScanId(application.getId(), scanId);
    Assert.assertNull(policyEvaluation);

    final Constraint constraint = new Constraint("C1", "testReevaluateReport constraint 1", LogicalOperator.AND);
    final Condition condition = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint.addCondition(condition);
    final Policy policy = new Policy("P1", "testReevaluateReport policy1");
    policy.setOwnerId(application.getId());
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    final Action notifyAction = new Action(NotifyActionType.ID);
    notifyAction.setTarget("manager@test.corp");
    policy.addAction(BuildStageType.ID, notifyAction);
    PolicyDAO policyDAO = new PolicyDAO();
    policyDAO.insert(policy);
    final Stage stage = new Stage(BuildStageType.ID);

    List<Message> notifications = Mailbox.get("manager@test.corp");

    // Evaluate policy
    Response response = AuthedRestAccess.post(
        getRestBaseUrl() + PolicyEvaluateResource.SERVICE_PATH.replace("{applicationPublicId}", applicationPublicId)
            + "?scanId=" + scanId, JsonHelpers.asJson(stage));
    assertResponseStatus(200, response);

    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(), scanId);
    Assert.assertNotNull(policyEvaluation);
    Assert.assertEquals(scanId, policyEvaluation.getScanId());
    Assert.assertEquals(BuildStageType.ID, policyEvaluation.getStageTypeId());
    assertTrue(System.currentTimeMillis() - policyEvaluation.getTime().getTime() < 60 * 1000);
    Assert.assertFalse(policyEvaluation.isReevaluation());

    Assert.assertEquals(1, notifications.size());
    notifications.clear();

    Thread.sleep(1);

    // ReEvaluate
    policy.setName(policy.getName() + " Updated");
    policyDAO.update(policy);
    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);
    response = AuthedRestAccess.get(resourcePrefix + "/reevaluatePolicy");
    assertResponseStatus(200, response);

    PolicyEvaluation policyReEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(),
        scanId);
    Assert.assertNotNull(policyReEvaluation);
    Assert.assertEquals(scanId, policyReEvaluation.getScanId());
    Assert.assertEquals(BuildStageType.ID, policyReEvaluation.getStageTypeId());
    assertTrue(policyReEvaluation.getTime().getTime() > policyEvaluation.getTime().getTime());
    assertTrue(policyReEvaluation.isReevaluation());

    Assert.assertEquals(0, notifications.size());

    // Evaluate the policy for a new scan for the same app. It should send notifications since this is not a
    // reevaluation.
    scanId = "ReportResourceTest_ScanId1";
    saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);
    response = AuthedRestAccess.post(
        getRestBaseUrl() + PolicyEvaluateResource.SERVICE_PATH.replace("{applicationPublicId}", applicationPublicId)
            + "?scanId=" + scanId, JsonHelpers.asJson(stage));
    assertResponseStatus(200, response);

    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(), scanId);
    Assert.assertNotNull(policyEvaluation);
    Assert.assertEquals(scanId, policyEvaluation.getScanId());
    Assert.assertEquals(BuildStageType.ID, policyEvaluation.getStageTypeId());
    assertTrue(System.currentTimeMillis() - policyEvaluation.getTime().getTime() < 60 * 1000);
    Assert.assertFalse(policyEvaluation.isReevaluation());

    Assert.assertEquals(1, notifications.size());
  }

  @Test
  public void testAugmentDataAndAuditLog() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    final String query = "security.json?user=test&where=ReportResourceTest";

    // attempt a bad edit (no augmented data)
    Response response = AuthedRestAccess.post(resourcePrefix + "/augmentData/" + query, "");
    assertResponseStatus(400, response); // bad request; no changes

    // verify nothing has changed
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), not(containsString("\"state\" : \"accepted\"")));

    // edit the state
    String edit = "{ \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\" }";
    response = AuthedRestAccess.post(resourcePrefix + "/augmentData/" + query, edit);
    assertResponseStatus(200, response);

    // verify the state has changed
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), containsString("\"state\" : \"accepted\""));

    // check the audit log reflects this change
    response = AuthedRestAccess.get(resourcePrefix + "/auditLog/security.json?key="
        + UrlUtils.encodeUrlComponent("{\"hash\":\"1249e25aebb15358bedd\"}"));
    assertResponseStatus(200, response);

    String feed = "{ \"aaData\" : [ { \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\", \"user\" : \"admin\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" } ] }";

    assertThat(response.getResponseBody().replaceFirst("\"time\" : [0-9]+,", ""), equalToIgnoringWhiteSpace(feed));

    // edit the state again
    edit = "{ \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"confirmed\" }";
    response = AuthedRestAccess.post(resourcePrefix + "/augmentData/" + query, edit);
    assertResponseStatus(200, response);

    // verify the state has changed again
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), containsString("\"state\" : \"confirmed\""));

    // check the audit log reflects this change
    response = AuthedRestAccess.get(resourcePrefix + "/auditLog/security.json?key="
        + UrlUtils.encodeUrlComponent("{\"hash\":\"1249e25aebb15358bedd\"}"));
    assertResponseStatus(200, response);

    feed = "{ \"aaData\" : [ { \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"confirmed\", \"user\" : \"admin\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" }, "
        + "{ \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\", \"user\" : \"admin\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" } ] }";

    assertThat(response.getResponseBody().replaceAll("\"time\" : [0-9]+,", ""), equalToIgnoringWhiteSpace(feed));

    // edit the BoM
    final String bomEdit = "[{\"groupId\":\"commons-pool\",\"artifactId\":\"commons-pool\",\"version\":\"1.4\",\"modified\":\"true\"}]:";
    final String bomQuery = "bom.json?user=test&where=ReportResourceTest";

    response = AuthedRestAccess.post(resourcePrefix + "/augmentData/" + bomQuery, bomEdit);
    assertResponseStatus(200, response);

    // verify the BoM change has been applied
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/bom.json");
    assertResponseStatus(200, response);
    boolean found = false;
    final String bomJsonString = response.getResponseBody();
    final JsonNode bomJsonData = JsonUtils.parse(bomJsonString).get("aaData");
    for (JsonNode bomJsonNode : bomJsonData) {
      if ("commons-pool".equals(bomJsonNode.get("groupId").asText())
          && "commons-pool".equals(bomJsonNode.get("artifactId").asText())
          && "1.4".equals(bomJsonNode.get("version").asText())) {
        found = true;
        Assert.assertEquals("true", bomJsonNode.path("modified").asText());
        break;
      }
    }
    Assert.assertTrue("Did not find augmented record in BoM", found);
  }

  @Test
  public void testAugmentDataAndAuditLog_LicenseOverrides() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    LicenseOverride licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(application.getId(), "tomcat",
        "tomcat-util", "5.5.23");
    assertNull(licenseOverride);
    licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(application.getId(), "commons-pool", "commons-pool", "1.4");
    assertNull(licenseOverride);

    // edit the license
    String licenseEdit = "["
        + "{\"groupId\":\"tomcat\",\"artifactId\":\"tomcat-util\",\"version\":\"5.5.23\",\"status\":\"Overridden\",\"overriddenLicenses\":[\"GPL-3.0\"],\"overriddenLicenseThreat\":10,\"comment\":\"My comment\"},"
        + "{\"groupId\":\"commons-pool\",\"artifactId\":\"commons-pool\",\"version\":\"1.4\",\"status\":\"Overridden\",\"overriddenLicenses\":[\"GPL-3.0\"],\"overriddenLicenseThreat\":10,\"comment\":\"My comment\"}"
        + "]:";
    final String licenseQuery = "licenses.json?user=test&where=ReportResourceTest";
    Response response = AuthedRestAccess.post(resourcePrefix + "/augmentData/" + licenseQuery, licenseEdit);
    assertResponseStatus(200, response);

    // verify that the license overrides were saved in the database
    licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(application.getId(), "tomcat", "tomcat-util", "5.5.23");
    assertNotNull(licenseOverride);
    assertEquals(LicenseOverrideStatus.OVERRIDDEN, licenseOverride.getStatus());
    assertEquals("GPL-3.0", licenseOverride.getLicenseId());
    assertEquals("My comment", licenseOverride.getComment());
    licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(application.getId(), "commons-pool", "commons-pool", "1.4");
    assertNotNull(licenseOverride);
    assertEquals(LicenseOverrideStatus.OVERRIDDEN, licenseOverride.getStatus());
    assertEquals("GPL-3.0", licenseOverride.getLicenseId());
    assertEquals("My comment", licenseOverride.getComment());

    // verify the license overrides were applied to the license.json file
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/licenses.json");
    assertResponseStatus(200, response);
    int found = 0;
    String licenseJsonString = response.getResponseBody();
    JsonNode licenseJsonData = JsonUtils.parse(licenseJsonString).get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonData) {
      String groupId = licenseJsonNode.get("groupId").asText();
      String artifactId = licenseJsonNode.get("artifactId").asText();
      String version = licenseJsonNode.get("version").asText();
      if (("tomcat".equals(groupId) && "tomcat-util".equals(artifactId) && "5.5.23".equals(version))
          || ("commons-pool".equals(groupId) && "commons-pool".equals(artifactId) && "1.4".equals(version))) {
        String overridenLicenseNamesStr = licenseJsonNode.get("overriddenLicenses").toString();
        Assert.assertEquals("[\"GPL-3.0\"]", overridenLicenseNamesStr);
        int effectiveLicenseThreat = licenseJsonNode.get("effectiveLicenseThreat").asInt();
        Assert.assertEquals(9, effectiveLicenseThreat);
        int overriddenLicenseThreat = licenseJsonNode.get("overriddenLicenseThreat").asInt();
        Assert.assertEquals(9, overriddenLicenseThreat);
        String status = licenseJsonNode.get("status").asText();
        Assert.assertEquals(LicenseOverrideStatus.OVERRIDDEN.getName(), status);
        String comment = licenseJsonNode.get("comment").asText();
        Assert.assertEquals("My comment", comment);
        found++;
      }
    }
    Assert.assertEquals("Did not find expected overridden license", 2, found);

    // edit the license again
    licenseEdit = "["
        + "{\"groupId\":\"tomcat\",\"artifactId\":\"tomcat-util\",\"version\":\"5.5.23\",\"status\":\"Overridden\",\"overriddenLicenses\":[\"Apache-2.0\"],\"overriddenLicenseThreat\":1,\"comment\":\"My comment1\"},"
        + "{\"groupId\":\"commons-pool\",\"artifactId\":\"commons-pool\",\"version\":\"1.4\",\"status\":\"Overridden\",\"overriddenLicenses\":[\"Apache-2.0\"],\"overriddenLicenseThreat\":1,\"comment\":\"My comment1\"}"
        + "]:";
    response = AuthedRestAccess.post(resourcePrefix + "/augmentData/" + licenseQuery, licenseEdit);
    assertResponseStatus(200, response);

    // verify that the license overrides were saved in the database
    licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(application.getId(), "tomcat", "tomcat-util", "5.5.23");
    assertNotNull(licenseOverride);
    assertEquals(LicenseOverrideStatus.OVERRIDDEN, licenseOverride.getStatus());
    assertEquals("Apache-2.0", licenseOverride.getLicenseId());
    assertEquals("My comment1", licenseOverride.getComment());
    licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(application.getId(), "commons-pool", "commons-pool", "1.4");
    assertNotNull(licenseOverride);
    assertEquals(LicenseOverrideStatus.OVERRIDDEN, licenseOverride.getStatus());
    assertEquals("Apache-2.0", licenseOverride.getLicenseId());
    assertEquals("My comment1", licenseOverride.getComment());

    // verify the license overrides were applied to the license.json file
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/licenses.json");
    assertResponseStatus(200, response);
    found = 0;
    licenseJsonString = response.getResponseBody();
    licenseJsonData = JsonUtils.parse(licenseJsonString).get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonData) {
      String groupId = licenseJsonNode.get("groupId").asText();
      String artifactId = licenseJsonNode.get("artifactId").asText();
      String version = licenseJsonNode.get("version").asText();
      if (("tomcat".equals(groupId) && "tomcat-util".equals(artifactId) && "5.5.23".equals(version))
          || ("commons-pool".equals(groupId) && "commons-pool".equals(artifactId) && "1.4".equals(version))) {
        String overridenLicenseNamesStr = licenseJsonNode.get("overriddenLicenses").toString();
        Assert.assertEquals("[\"Apache-2.0\"]", overridenLicenseNamesStr);
        int effectiveLicenseThreat = licenseJsonNode.get("effectiveLicenseThreat").asInt();
        Assert.assertEquals(0, effectiveLicenseThreat);
        int overriddenLicenseThreat = licenseJsonNode.get("overriddenLicenseThreat").asInt();
        Assert.assertEquals(0, overriddenLicenseThreat);
        String status = licenseJsonNode.get("status").asText();
        Assert.assertEquals(LicenseOverrideStatus.OVERRIDDEN.getName(), status);
        String comment = licenseJsonNode.get("comment").asText();
        Assert.assertEquals("My comment1", comment);
        found++;
      }
    }
    Assert.assertEquals("Did not find expected overridden license", 2, found);
  }

  @Test
  public void test_LicenseOverrides_Organization() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    // Verify before any license overrides are added
    Response response = AuthedRestAccess.get(resourcePrefix + "/browseReport/licenses.json");
    assertResponseStatus(200, response);
    int found = 0;
    String licenseJsonString = response.getResponseBody();
    JsonNode licenseJsonData = JsonUtils.parse(licenseJsonString).get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonData) {
      String groupId = licenseJsonNode.get("groupId").asText();
      String artifactId = licenseJsonNode.get("artifactId").asText();
      String version = licenseJsonNode.get("version").asText();
      if ("commons-pool".equals(groupId) && "commons-pool".equals(artifactId) && "1.4".equals(version)) {
        assertNull(licenseJsonNode.get("overriddenLicenses"));
        found++;
      }
    }
    Assert.assertEquals("Did not find expected license", 1, found);

    // Override the license at organization level
    LicenseOverride orgLicenseOverride = new LicenseOverride(application.getOrganizationId(), "commons-pool",
        "commons-pool", "1.4", LicenseOverrideStatus.OVERRIDDEN, "GPL-3.0", "My org license override");
    response = AuthedRestAccess.post(
        getLicenseOverrideServiceURL(IdUtils.TYPE_ORGANIZATION, application.getOrganizationId()),
        JsonHelpers.asJson(orgLicenseOverride));
    assertResponseStatus(200, response);
    orgLicenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/licenses.json");
    assertResponseStatus(200, response);
    found = 0;
    licenseJsonString = response.getResponseBody();
    licenseJsonData = JsonUtils.parse(licenseJsonString).get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonData) {
      String groupId = licenseJsonNode.get("groupId").asText();
      String artifactId = licenseJsonNode.get("artifactId").asText();
      String version = licenseJsonNode.get("version").asText();
      if ("commons-pool".equals(groupId) && "commons-pool".equals(artifactId) && "1.4".equals(version)) {
        String overridenLicenseNamesStr = licenseJsonNode.get("overriddenLicenses").toString();
        Assert.assertEquals("[\"GPL-3.0\"]", overridenLicenseNamesStr);
        int effectiveLicenseThreat = licenseJsonNode.get("effectiveLicenseThreat").asInt();
        Assert.assertEquals(9, effectiveLicenseThreat);
        int overriddenLicenseThreat = licenseJsonNode.get("overriddenLicenseThreat").asInt();
        Assert.assertEquals(9, overriddenLicenseThreat);
        String status = licenseJsonNode.get("status").asText();
        Assert.assertEquals(LicenseOverrideStatus.OVERRIDDEN.getName(), status);
        String comment = licenseJsonNode.get("comment").asText();
        Assert.assertEquals("My org license override", comment);
        found++;
      }
    }
    Assert.assertEquals("Did not find expected overridden license", 1, found);

    // Override the license at application level
    LicenseOverride appLicenseOverride = new LicenseOverride(application.getId(), "commons-pool", "commons-pool",
        "1.4", LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My app license override");
    response = AuthedRestAccess.post(getLicenseOverrideServiceURL(IdUtils.TYPE_APPLICATION, application.getPublicId()),
        JsonHelpers.asJson(appLicenseOverride));
    assertResponseStatus(200, response);
    appLicenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);

    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/licenses.json");
    assertResponseStatus(200, response);
    found = 0;
    licenseJsonString = response.getResponseBody();
    licenseJsonData = JsonUtils.parse(licenseJsonString).get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonData) {
      String groupId = licenseJsonNode.get("groupId").asText();
      String artifactId = licenseJsonNode.get("artifactId").asText();
      String version = licenseJsonNode.get("version").asText();
      if ("commons-pool".equals(groupId) && "commons-pool".equals(artifactId) && "1.4".equals(version)) {
        String overridenLicenseNamesStr = licenseJsonNode.get("overriddenLicenses").toString();
        Assert.assertEquals("[\"GPL-2.0\"]", overridenLicenseNamesStr);
        int effectiveLicenseThreat = licenseJsonNode.get("effectiveLicenseThreat").asInt();
        Assert.assertEquals(9, effectiveLicenseThreat);
        int overriddenLicenseThreat = licenseJsonNode.get("overriddenLicenseThreat").asInt();
        Assert.assertEquals(9, overriddenLicenseThreat);
        String status = licenseJsonNode.get("status").asText();
        Assert.assertEquals(LicenseOverrideStatus.OVERRIDDEN.getName(), status);
        String comment = licenseJsonNode.get("comment").asText();
        Assert.assertEquals("My app license override", comment);
        found++;
      }
    }
    Assert.assertEquals("Did not find expected overridden license", 1, found);
  }

  @Test
  public void testRefreshOnlyOnChange() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    String appId = application.getId();
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    final String query = "security.json?user=test&where=ReportResourceTest";

    // verify nothing has changed
    Response response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), not(containsString("\"state\" : \"accepted\"")));

    // edit the state
    final String edit = "{ \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\" }";
    response = AuthedRestAccess.post(resourcePrefix + "/augmentData/" + query, edit);
    assertResponseStatus(200, response);

    // check the audit log reflects this change
    response = AuthedRestAccess.get(resourcePrefix + "/auditLog/security.json?key="
        + UrlUtils.encodeUrlComponent("{\"hash\":\"1249e25aebb15358bedd\"}"));
    assertResponseStatus(200, response);

    final String feed = "{ \"aaData\" : [ { \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\", \"user\" : \"admin\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" } ] }";

    assertThat(response.getResponseBody().replaceFirst("\"time\" : [0-9]+,", ""), equalToIgnoringWhiteSpace(feed));

    // force the internal modification count to make it look like we're already up-to-date
    int oldModCount = ReportResource.MODIFICATION_COUNTS.put(appId + '-' + scanId, 888);

    // verify nothing has changed
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), not(containsString("\"state\" : \"accepted\"")));

    // put back the accurate modification count, which should lead to a refresh
    ReportResource.MODIFICATION_COUNTS.put(appId + '-' + scanId, oldModCount);

    // verify the state has changed
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), containsString("\"state\" : \"accepted\""));
  }

  @Test
  public void testCanAuditNonReportData() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    final String query = "extra.json?user=test&where=ReportResourceTest";

    // audit non-report data
    final String extra = "{ \"policy\" : \"TEST\", \"result\" : \"OK\" }";
    Response response = AuthedRestAccess.post(resourcePrefix + "/augmentData/" + query, extra);
    assertResponseStatus(200, response);

    // verify can still access report
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), not(containsString("\"state\" : \"accepted\"")));

    // check the audit log reflects this change
    response = AuthedRestAccess.get(resourcePrefix + "/auditLog/extra.json");
    assertResponseStatus(200, response);

    final String feed = "{ \"aaData\" : [ { \"policy\" : \"TEST\", \"result\" : \"OK\", \"user\" : \"admin\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"extra.json\" } ] }";

    assertThat(response.getResponseBody().replaceFirst("\"time\" : [0-9]+,", ""), equalToIgnoringWhiteSpace(feed));
  }

  @Test
  public void testRedirection() throws Exception {
    String path = "index.html?x=y&a=b";
    String url = getServiceURL("appId", "scanId");
    Response response = AuthedRestAccess.get(url + "/brain/" + path);
    assertResponseStatus(307, response);
    Assert.assertEquals(getRestBaseUrl() + path, response.getHeader("Location"));
  }

  @Test
  public void testDownloadBundle() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    String appId = tempEntity.newApplicationWithParent(applicationPublicId).getId();
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    final URL testReportResultUrl = getClass().getResource("/ReportResourceTest/fortify.zip");
    FileUtils.copyURLToFile(testReportResultUrl, saasReportFile);

    HashGAV claimedCompenent = tempEntity.newClaimedComponent("f0776db1593e215146d2", "commons-httpclient",
        "commons-httpclient", "3.1.SONATYPE");
    LicenseOverride licenseOverride = tempEntity
        .newLicenseOverride(appId, claimedCompenent.getGroupId(), claimedCompenent.getArtifactId(),
            claimedCompenent.getVersion(), LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0");
    LicenseOverride licenseOverride2 = tempEntity.newLicenseOverride(appId, "tomcat", "tomcat-util", "5.5.23",
        LicenseOverrideStatus.OVERRIDDEN, "EPL-1.0");
    Policy policy = tempEntity.newPolicy(appId, testName.getMethodName());

    Response response = AuthedRestAccess.post(getRestUrl(PolicyEvaluateResource.SERVICE_PATH, applicationPublicId)
        + "?scanId=" + scanId, toJson(new Stage(Stage.ID_BUILD)));
    assertResponseStatus(200, response);

    String url = getRestUrl(ReportResource.SERVICE_PATH + '/' + ReportResource.DOWNLOAD_BUNDLE_PATH,
        applicationPublicId, scanId);
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), is("application/zip"));
    assertThat(response.getHeader("Content-Disposition"), containsString("filename="));
    try (InputStream actual = response.getResponseBodyAsStream()) {
      File temp = File.createTempFile("report", "zip");
      FileUtils.copyStreamToFile(new RawInputStreamFacade(actual), temp);
      try (ZipFile zip = new ZipFile(temp)) {
        assertNotNull(zip.getEntry("data/report.pdf"));
        assertNull(zip.getEntry("detail.rptdesign"));
        assertNull(zip.getEntry("data/index.html"));
        assertNotNull(zip.getEntry("data/components.json"));
        assertNotNull(zip.getEntry("data/release-graph/tomcat/tomcat-util/5.5.23.png"));
        assertNotNull(zip.getEntry("data/" + PolicyEvaluationUtils.POLICY_THREATS_FILENAME));

        assertNull(zip.getEntry("cip/details/f0776db1593e215146d2.json"));
        ComponentDetails details = JsonUtils.parse(
            zip.getInputStream(zip.getEntry("data/cip/details/f0776db1593e215146d2.json")), ComponentDetails.class);
        assertThat(details.getMatchState(), is("exact"));
        assertThat(details.getGroupId(), is(claimedCompenent.getGroupId()));
        assertThat(details.getArtifactId(), is(claimedCompenent.getArtifactId()));
        assertThat(details.getVersion(), is(claimedCompenent.getVersion()));
        assertThat(details.getCatalogDate(), is(claimedCompenent.getCreateTimeLong()));
        assertThat(details.getOverriddenLicenses(), hasSize(1));
        assertThat(details.getOverriddenLicenses().iterator().next().getLicenseId(), is(licenseOverride.getLicenseId()));
        assertThat(details.getLicenseThreatGroupNames(), containsInAnyOrder("Copyleft"));
        assertThat(details.getLicenseThreatLevel(), is(9));
        assertThat(details.getIdentificationSource(), is(IdentificationSource.MANUAL.getId()));
        assertThat(details.getIdentificationSourceComment(), is(claimedCompenent.getComment()));
        ComponentDetailsList list = JsonUtils.parse(
            zip.getInputStream(zip.getEntry("data/cip/list/" + claimedCompenent.getGroupId() + "/"
                + claimedCompenent.getArtifactId() + "/" + claimedCompenent.getVersion() + ".json")),
            ComponentDetailsList.class);
        assertThat(list.getList(), hasSize(0));

        details = JsonUtils.parse(zip.getInputStream(zip.getEntry("data/cip/details/1249e25aebb15358bedd.json")),
            ComponentDetails.class);
        assertThat(details.getMatchState(), is("exact"));
        assertThat(details.getIdentificationSource(), is(IdentificationSource.SONATYPE.getId()));
        assertThat(details.getIdentificationSourceComment(), is(nullValue()));
        assertThat(details.getPolicyAlerts(), hasSize(1));
        assertThat(details.getPolicyAlerts().get(0).getTrigger().getPolicyId(), is(policy.getId()));
        assertThat(details.getPolicyAlerts().get(0).getTrigger().getComponentFacts(), hasSize(1));

        list = JsonUtils.parse(zip.getInputStream(zip.getEntry("data/cip/list/tomcat/tomcat-util/5.5.23.json")),
            ComponentDetailsList.class);
        details = findGAV(list, "tomcat", "tomcat-util", "5.5.23");
        assertThat(details, is(notNullValue()));
        assertThat(details.getOverriddenLicenses(), hasSize(1));
        assertThat(details.getOverriddenLicenses().iterator().next().getLicenseId(),
            is(licenseOverride2.getLicenseId()));
        assertThat(details.getLicenseThreatGroupNames(), containsInAnyOrder("Weak Copyleft"));
        assertThat(details.getLicenseThreatLevel(), is(2));
      }
    }
  }

  private ComponentDetails findGAV(ComponentDetailsList list, String g, String a, String v) {
    for (ComponentDetails details : list.getList()) {
      if (g.equals(details.getGroupId()) && a.equals(details.getArtifactId()) && v.equals(details.getVersion())) {
        return details;
      }
    }
    return null;
  }

  private void testDataJsonApplyChanges(String json) throws IOException {
    final ContainerNode<?> data = JsonUtils.parse(json);

    // keyFindings must not have duplicates
    JsonNode keyFindings = data.get("keyFindings");
    Assert.assertNotNull(keyFindings);
    Assert.assertTrue(keyFindings.size() > 0);
    Set<String> uniqueKeyFindings = new LinkedHashSet<String>();
    for (int i = 0; i < keyFindings.size(); i++) {
      String keyFinding = keyFindings.get(i).get("text").asText();
      uniqueKeyFindings.add(keyFinding);
    }
    Assert.assertEquals(keyFindings.toString(), uniqueKeyFindings.size(), keyFindings.size());

    Assert.assertEquals(2, data.get("weakcopyleftLicenseCount").asInt());
    Assert.assertEquals(2, data.get("nonStandardLicenseCount").asInt());
    Assert.assertEquals(3, data.get("copyleftLicenseCount").asInt());
    Assert.assertEquals(20, data.get("liberalLicenseCount").asInt());
    Assert.assertEquals(1, data.get("notProvidedLicenseCount").asInt());
    Assert.assertEquals("[19,0,2,0,0,0,2,0,0,4,0]", data.get("effectiveLicenseCounts").toString());

    Assert.assertEquals(7, data.get("insecureArtifactCount").asInt());
    Assert.assertEquals("[0,4,0,0,2,12,15,2,0,1]", data.get("securityCounts").toString());

    Assert.assertEquals("[0,0,0,0,0,0,0,0,0,0,0]", data.get("policyCounts").toString());
    Assert.assertEquals(0, data.get("policyComponentCount").asInt());

    Assert.assertEquals("[[4,11,3],[0,18,0],[0,12,0],[0,6,0],[0,6,0]]", data.get("securityPunchCard").toString());
    Assert.assertEquals("[[2,1,2],[2,1,0],[1,0,0],[0,1,0],[0,1,0]]", data.get("licensePunchCard").toString());
  }

  private void testLicensesJsonApplyChanges(String json) throws IOException {
    final ContainerNode<?> licenses = JsonUtils.parse(json);
    final JsonNode aaData = licenses.get("aaData");
    int countNotZero = 0;
    for (JsonNode license : aaData) {
      JsonNode effectiveLicenseThreat = license.get("effectiveLicenseThreat");
      Assert.assertNotNull(effectiveLicenseThreat);
      Integer threat = effectiveLicenseThreat.asInt();
      Assert.assertTrue("Effective license threat between null and 10.", threat == null
          || (threat >= 0 && threat <= 10));
      if (threat != null && threat > 0) {
        countNotZero++;
      }
    }
    Assert.assertTrue(countNotZero > 0);
  }

  private void testLicenseThreatsJsonApplyChanges(String json) throws IOException {
    final ContainerNode<?> licenseThreats = JsonUtils.parse(json);
    final JsonNode aaData = licenseThreats.get("aaData");
    int countNotZero = testLicenseThreatsApplyChanges(aaData);
    Assert.assertTrue(countNotZero > 0);
  }

  private void testPartialMatchedJsonApplyChanges(String json) throws IOException {
    final ContainerNode<?> partialMatched = JsonUtils.parse(json);
    final JsonNode aaNode = partialMatched.get("aaData");
    for (JsonNode license : aaNode) {
      final JsonNode matchedComponentNodes = license.get("matchDetails");
      Assert.assertTrue(matchedComponentNodes.size() > 0);
      testLicenseThreatsApplyChanges(matchedComponentNodes);
    }
  }

  private int testLicenseThreatsApplyChanges(JsonNode licenses) {
    int countNotZero = 0;
    for (JsonNode licenseThreat : licenses) {
      Integer threat = licenseThreat.asInt();
      Assert.assertTrue("Effective license threat between null and 10.", threat == null
          || (threat >= 0 && threat <= 10));
      if (threat != null && threat > 0) {
        countNotZero++;
      }
    }
    return countNotZero;
  }

  private String getServiceURL(final String appId, final String scanId) {
    return getRestBaseUrl()
        + ReportResource.SERVICE_PATH.replace("{applicationPublicId}", appId).replace("{scanId}", scanId);
  }

  private String getLicenseOverrideServiceURL(final String ownerType, final String ownerId) {
    return getRestBaseUrl() + LicenseOverrideResource.SERVICE_BASEPATH + ownerType + "/" + ownerId;
  }
}

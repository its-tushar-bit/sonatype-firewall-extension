/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.mail.Message;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportDataDTOV2;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.component.HashComponentIdentifierResource;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.license.LicenseOverrideResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
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
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.saas.TestNamedComponentDetails;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.RestAccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ning.http.client.Response;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ReportResourceTest
    extends AbstractResourceTest
{

  private static final ComponentIdentifier COMMONS_POOL_ID = ComponentIdentifier.createMavenCoordinates("commons-pool",
      "commons-pool", "1.4");

  @Test
  public void testManuallyIdentifiedComponent() throws Exception {
    // The hash of commons-httpclient-3.1.SONATYPE.jar, similar match of commons-httpclient:commons-httpclient:3.1
    String hash = "f0776db1593e215146d2";
    String groupId = "testClaimedComponent_G";
    String artifactId = "testClaimedComponent_A";
    String version = "testClaimedComponent_V";
    String extension = "testClaimedComponent_E";
    String classifier = "testClaimedComponent_C";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version,
        classifier, extension);
    Date createTime = new Date();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash, componentIdentifier);
    hashComponentIdentifier.setCreateTime(createTime);
    HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    String applicationPublicId = "testClaimedComponent_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    String scanId = "testClaimedComponent_ScanId";
    String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    mockReport(scanId, "/ReportResourceTest/report.zip");

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
        assertEquals(componentIdentifier, ComponentIdentifierAdapter.getComponentIdentifier(bomJsonNode));
        assertEquals(MatchState.EXACT.getId(), bomJsonNode.get("matchState").asText());
        assertEquals(createTime.getTime(), bomJsonNode.get("createTime").asLong());
        assertEquals(0F, bomJsonNode.get("relativePopularity").asDouble(), 0F);
        assertEquals("testClaimedComponent_G : testClaimedComponent_A : testClaimedComponent_E : " +
                "testClaimedComponent_C : testClaimedComponent_V",
            JsonUtils.asPojo(bomJsonNode.get("displayName"), ComponentDisplayName.class).toString());
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

    hashComponentIdentifierDAO.delete(hashComponentIdentifier);
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
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version,
        classifier, extension);
    Date createTime = new Date();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash, componentIdentifier);
    hashComponentIdentifier.setCreateTime(createTime);
    HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    String applicationPublicId = "testClaimedComponent_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    String licenseId = new LicenseDAO().getByIdNotNull("GPL-3.0").getId(); // db lookup to make sure licenseId is valid
    tempEntity.newLicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, licenseId,
      "manual override");

    String scanId = "testClaimedComponent_ScanId";
    String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    mockReport(scanId, "/ReportResourceTest/report.zip");

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

    hashComponentIdentifierDAO.delete(hashComponentIdentifier);
  }

  @Test
  public void testManuallyIdentifiedComponentInvalidatesCachedReportData() throws Exception {
    String applicationPublicId = "testClaimedComponent_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    String scanId = "testClaimedComponent_ScanId";
    String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    mockReport(scanId, "/ReportResourceTest/report.zip");

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
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version,
        classifier, extension);
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash, componentIdentifier);
    mockComponentSummary(componentIdentifier, ComponentSummary.create(false));
    response = AuthedRestAccess.post(getRestBaseUrl() + HashComponentIdentifierResource.SERVICE_PATH,
        toJson(hashComponentIdentifier));
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
        assertEquals(componentIdentifier, ComponentIdentifierAdapter.getComponentIdentifier(bomJsonNode));
        assertEquals("testClaimedComponent_G : testClaimedComponent_A : testClaimedComponent_E : " +
                "testClaimedComponent_C : testClaimedComponent_V",
            JsonUtils.asPojo(bomJsonNode.get("displayName"), ComponentDisplayName.class).toString());
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

    HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();
    hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(hashComponentIdentifier.getHash());
    hashComponentIdentifierDAO.delete(hashComponentIdentifier);
  }

  @Test
  public void testBrowseReportEntryExpirationDate() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    mockReport(scanId, "/ReportResourceTest/report.zip");

    TimeZone gmt = TimeZone.getTimeZone("GMT");
    final Calendar calendar = Calendar.getInstance(gmt);
    final SimpleDateFormat expirationHeaderFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm", Locale.ENGLISH);
    expirationHeaderFormat.setTimeZone(gmt);

    calendar.setTime(new Date());
    calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 365);
    Response response = AuthedRestAccess.get(resourcePrefix + "/browseReport/insight.js");
    assertResponseStatus(200, response);
    String expiresHeader = response.getHeader("Expires");
    assertNotNull(expiresHeader);
    Date expires = expirationHeaderFormat.parse(expiresHeader);
    assertTrue("insight.js expires in 365 days: " + expires + " vs " + calendar.getTime(),
        Math.abs(calendar.getTimeInMillis() - expires.getTime()) <= 2 * 60 * 1000);

    calendar.setTime(new Date());
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/data.json");
    assertResponseStatus(200, response);
    expiresHeader = response.getHeader("Expires");
    expires = expirationHeaderFormat.parse(expiresHeader);
    assertTrue("data.json expires immediately: " + expires + " vs " + calendar.getTime(),
        Math.abs(calendar.getTimeInMillis() - expires.getTime()) <= 2 * 60 * 1000);

    calendar.setTime(new Date());
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/index.html");
    assertResponseStatus(200, response);
    expiresHeader = response.getHeader("Expires");
    expires = expirationHeaderFormat.parse(expiresHeader);
    assertTrue("index.html expires immediately: " + expires + " vs " + calendar.getTime(),
        Math.abs(calendar.getTimeInMillis() - expires.getTime()) <= 2 * 60 * 1000);

    Map<String, String> ifModifiedSinceHeader = new HashMap<>();
    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
    ifModifiedSinceHeader.put("If-Modified-Since",
        new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).format(calendar.getTime()));
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/data.json", ifModifiedSinceHeader);
    assertResponseStatus(304, response);

    //make sure index.html always returns 200, no 304s here
    ifModifiedSinceHeader = new HashMap<>();
    ifModifiedSinceHeader.put("If-Modified-Since",
        new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).format(calendar.getTime()));
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/index.html", ifModifiedSinceHeader);
    assertResponseStatus(200, response);
  }

  @Test
  public void testBrowseReport() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    String reportResource = "/ReportResourceTest/report.zip";
    mockReport(scanId, reportResource);

    try (ZipInputStream zipStream = new ZipInputStream(getClass().getResourceAsStream(reportResource))) {
      for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream.getNextEntry()) {
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
          String actual = response.getResponseBody();

          testLicensesJsonApplyChanges(actual);
          testJsonApplyComponentChanges(actual);
        }
        else if ("licensethreats.json".equals(entry.getName())) {
          String actual = response.getResponseBody();

          testLicenseThreatsJsonApplyChanges(actual);
        }
        else if ("partialmatched.json".equals(entry.getName())) {
          String actual = response.getResponseBody();

          testPartialMatchedJsonApplyChanges(actual);
        }
        else if ("security.json".equals(entry.getName())) {
          JsonNode actual = JsonUtils.parse(response.getResponseBody());
          JsonNode expected = JsonUtils.parse(IOUtil.toString(zipStream));
          for (JsonNode node : expected.get("aaData")) {
            ComponentIdentifierAdapter.injectComponentIdentifier((ObjectNode) node);
            ComponentDisplayNameUtil.injectDisplayName((ObjectNode) node);
          }
          assertThat(actual, is(expected));
        }
        else if ("index.html".equals(entry.getName())) {
          String actual = response.getResponseBody();
          assertTrue("The app public id was not included in the report",
              actual.contains("applicationId = '" + applicationPublicId + "'"));
        }
        else if ("bom.json".equals(entry.getName())) {
          String actual = response.getResponseBody();
          testJsonApplyComponentChanges(actual);
        }
        else if (contentType.startsWith("text") || contentType.endsWith("json")) {
          assertThat(response.getResponseBody(),
              equalToIgnoringWhiteSpace(IOUtil.toString(zipStream, "UTF-8")));
        }
        else {
          assertThat(IOUtil.toByteArray(response.getResponseBodyAsStream()),
              equalTo(IOUtil.toByteArray(zipStream)));
        }
      }
    }

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

    mockReport(scanId, "/ReportResourceTest/report.zip");
    File reportDir = getCLMServer().getReportDir(appId, scanId);
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
    final String appId = tempEntity.newApplicationWithParent(applicationPublicId, "Test Project").getId();
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    tempEntity.newPolicyEvaluation(appId, StageTypes.BUILD.getId(), scanId);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    mockReport(scanId, "/ReportResourceTest/report.zip");

    final Response response;
    try {
      response = AuthedRestAccess.get(resourcePrefix + "/printReport");
      assertResponseStatus(200, response);
      assertThat(response.getHeader("Content-Disposition"),
          stringContainsInOrder(Arrays.asList("attachment; filename=\"Test Project-Build-", ".pdf\"")));
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
    final String appId = tempEntity.newApplicationWithParent(applicationPublicId, "Test Project").getId();
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    tempEntity.newPolicyEvaluation(appId, StageTypes.BUILD.getId(), scanId);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    mockReport(scanId, "/ReportResourceTest/report.zip");

    Response response;
    try {
      response = AuthedRestAccess.get(resourcePrefix + "/printReport");
      assertResponseStatus(200, response);

      // pretend the print attempt crashed with OOME, which usually leaves an empty PDF file around
      File pdfFile = new File(new File(new File(getCLMServer().getWorkDir(), "report/" + appId), scanId), "report.pdf");
      assertTrue(pdfFile.getPath(), pdfFile.isFile());
      new FileOutputStream(pdfFile).close();

      // printing again after fixing the mem setting should produce a proper PDF
      response = AuthedRestAccess.get(resourcePrefix + "/printReport");
      assertResponseStatus(200, response);
      assertThat(response.getHeader("Content-Disposition"),
          stringContainsInOrder(Arrays.asList("attachment; filename=\"Test Project-Build-", ".pdf\"")));
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

    mockReport(scanId, "/ReportResourceTest/report.zip");

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
            + "?scanId=" + scanId, toJson(stage));
    assertResponseStatus(200, response);

    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(), scanId);
    Assert.assertNotNull(policyEvaluation);
    Assert.assertEquals(scanId, policyEvaluation.getScanId());
    Assert.assertEquals(BuildStageType.ID, policyEvaluation.getStageTypeId());
    assertTrue(System.currentTimeMillis() - policyEvaluation.getTime().getTime() < 60 * 1000);
    Assert.assertFalse(policyEvaluation.isReevaluation());

    assertNotifications(notifications, 1, 5000);
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

    assertNotifications(notifications, 0, 5000);

    // Evaluate the policy for a new scan for the same app. It should send notifications since this is not a
    // reevaluation.
    scanId = "ReportResourceTest_ScanId1";
    mockReport(scanId, "/ReportResourceTest/report.zip");
    response = AuthedRestAccess.post(
        getRestBaseUrl() + PolicyEvaluateResource.SERVICE_PATH.replace("{applicationPublicId}", applicationPublicId)
            + "?scanId=" + scanId, toJson(stage));
    assertResponseStatus(200, response);

    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(), scanId);
    Assert.assertNotNull(policyEvaluation);
    Assert.assertEquals(scanId, policyEvaluation.getScanId());
    Assert.assertEquals(BuildStageType.ID, policyEvaluation.getStageTypeId());
    assertTrue(System.currentTimeMillis() - policyEvaluation.getTime().getTime() < 60 * 1000);
    Assert.assertFalse(policyEvaluation.isReevaluation());

    assertNotifications(notifications, 1, 5000);
  }

  @Test
  public void testAugmentDataAndAuditLog() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    mockReport(scanId, "/ReportResourceTest/report.zip");

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
  public void test_LicenseOverrides_Organization() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final String resourcePrefix = getServiceURL(applicationPublicId, scanId);

    mockReport(scanId, "/ReportResourceTest/report.zip");

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
    LicenseOverride orgLicenseOverride = new LicenseOverride(application.getOrganizationId(), COMMONS_POOL_ID,
      LicenseOverrideStatus.OVERRIDDEN, "GPL-3.0", "My org license override");
    response = AuthedRestAccess.post(
        getLicenseOverrideServiceURL(IdUtils.TYPE_ORGANIZATION, application.getOrganizationId()),
        toJson(orgLicenseOverride));
    assertResponseStatus(200, response);
    orgLicenseOverride = fromJson(response, LicenseOverride.class);

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
    LicenseOverride appLicenseOverride = new LicenseOverride(application.getId(),
      COMMONS_POOL_ID,
      LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My app license override");
    response = AuthedRestAccess.post(getLicenseOverrideServiceURL(IdUtils.TYPE_APPLICATION, application.getPublicId()),
        toJson(appLicenseOverride));
    assertResponseStatus(200, response);
    appLicenseOverride = fromJson(response, LicenseOverride.class);

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

    mockReport(scanId, "/ReportResourceTest/report.zip");

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
    int oldModCount = ReportService.MODIFICATION_COUNTS.put(appId + '-' + scanId, 888);

    // verify nothing has changed
    response = AuthedRestAccess.get(resourcePrefix + "/browseReport/security.json");
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), not(containsString("\"state\" : \"accepted\"")));

    // put back the accurate modification count, which should lead to a refresh
    ReportService.MODIFICATION_COUNTS.put(appId + '-' + scanId, oldModCount);

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

    mockReport(scanId, "/ReportResourceTest/report.zip");

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
  public void testDownloadBundle_LegacyFormat() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    String appId = tempEntity.newApplicationWithParent(applicationPublicId).getId();
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    mockReport(scanId, "/ReportResourceTest/standalone-legacy.zip");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("commons-httpclient",
        "commons-httpclient", "3.1.SONATYPE");
    HashComponentIdentifier claimedComponent = tempEntity.newClaimedComponent("f0776db1593e215146d2",
        componentIdentifier);
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(appId, claimedComponent.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0");
    LicenseOverride licenseOverride2 = tempEntity.newLicenseOverride(appId,
      ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"), LicenseOverrideStatus.OVERRIDDEN,
      "EPL-1.0");
    Policy policy = tempEntity.newPolicy(appId, testName.getMethodName().replaceAll("[_]", ""));

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
        assertComponentIdentifier(details, claimedComponent.getComponentIdentifier());
        assertThat(details.getComponentIdentifier(), is(claimedComponent.getComponentIdentifier()));
        assertThat(details.getCatalogDate(), is(claimedComponent.getCreateTimeLong()));
        assertThat(details.getOverriddenLicenses(), hasSize(1));
        assertThat(details.getOverriddenLicenses().iterator().next().getLicenseId(), is(licenseOverride.getLicenseIds().iterator().next()));
        assertThat(details.getLicenseThreatGroupNames(), containsInAnyOrder("Copyleft"));
        assertThat(details.getLicenseThreatLevel(), is(9));
        assertThat(details.getIdentificationSource(), is(IdentificationSource.MANUAL.getId()));
        assertThat(details.getIdentificationSourceComment(), is(claimedComponent.getComment()));
        ComponentDetailsList list = JsonUtils.parse(
            zip.getInputStream(zip.getEntry("data/cip/list/"
                + componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID) + "/"
                + componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) + "/"
                + componentIdentifier.get(ComponentIdentifier.VERSION) + ".json")), ComponentDetailsList.class);
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
        details = findDetailsForComponent(list,
          ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"));
        assertThat(details, is(notNullValue()));
        assertThat(details.getOverriddenLicenses(), hasSize(1));
        assertThat(details.getOverriddenLicenses().iterator().next().getLicenseId(),
            is(licenseOverride2.getLicenseIds().iterator().next()));
        assertThat(details.getLicenseThreatGroupNames(), containsInAnyOrder("Weak Copyleft"));
        assertThat(details.getLicenseThreatLevel(), is(2));
      }
    }
  }

  @SuppressWarnings("deprecation")
  private void assertComponentIdentifier(ComponentDetails actual, ComponentIdentifier expected) {
    assertThat(actual.getComponentIdentifier(), is(expected));
    assertThat(actual.getGroupId(), is(expected.get(ComponentIdentifier.MAVEN_GROUP_ID)));
    assertThat(actual.getArtifactId(), is(expected.get(ComponentIdentifier.MAVEN_ARTIFACT_ID)));
    assertThat(actual.getVersion(), is(expected.get(ComponentIdentifier.VERSION)));
  }

  @Test
  public void testDownloadBundle() throws Exception {
    final String applicationPublicId = "ReportResourceTest_AppId";
    String appId = tempEntity.newApplicationWithParent(applicationPublicId).getId();
    final String scanId = "ReportResourceTest_ScanId";
    final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    mockReport(scanId, "/ReportResourceTest/standalone.zip");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("commons-httpclient",
        "commons-httpclient", "3.1.SONATYPE", "", "jar");
    HashComponentIdentifier claimedComponent = tempEntity.newClaimedComponent("f0776db1593e215146d2",
        componentIdentifier);
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(appId, claimedComponent.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0");
    LicenseOverride licenseOverride2 = tempEntity.newLicenseOverride(appId,
      ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"), LicenseOverrideStatus.OVERRIDDEN,
      "EPL-1.0");
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

        ZipEntry componentEntry = zip.getEntry("data/components.json");
        assertNotNull(componentEntry);
        ApiReportDataDTOV2 components = JsonUtils.parse(zip.getInputStream(componentEntry), ApiReportDataDTOV2.class);

        assertEquals(5, components.matchSummary.knownComponentCount);
        assertEquals(29, components.matchSummary.totalComponentCount);
        assertComponent("tomcat", "tomcat-util", "5.5.23", "Weak Copyleft", 2, components.components);

        assertNotNull(zip.getEntry("data/release-graph/maven/"
            + "artifactId=tomcat-util/classifier=/extension=jar/groupId=tomcat/version=5.5.23/releases.png"));
        assertNotNull(zip.getEntry("data/" + PolicyEvaluationUtils.POLICY_THREATS_FILENAME));

        assertNull(zip.getEntry("cip/details/f0776db1593e215146d2.json"));
        TestNamedComponentDetails details = JsonUtils.parse(
            zip.getInputStream(zip.getEntry("data/cip/details/f0776db1593e215146d2.json")), TestNamedComponentDetails.class);
        assertThat(details.getMatchState(), is("exact"));
        assertComponentIdentifier(details, claimedComponent.getComponentIdentifier());
        assertThat(details.getDisplayName().toString(), is("commons-httpclient : commons-httpclient : 3.1.SONATYPE"));
        assertThat(details.getCatalogDate(), is(claimedComponent.getCreateTimeLong()));
        assertThat(details.getOverriddenLicenses(), hasSize(1));
        assertThat(details.getOverriddenLicenses().iterator().next().getLicenseId(), is(licenseOverride.getLicenseIds().iterator().next()));
        assertThat(details.getLicenseThreatGroupNames(), containsInAnyOrder("Copyleft"));
        assertThat(details.getLicenseThreatLevel(), is(9));
        assertThat(details.getIdentificationSource(), is(IdentificationSource.MANUAL.getId()));
        assertThat(details.getIdentificationSourceComment(), is(claimedComponent.getComment()));
        ComponentDetailsList list = JsonUtils.parse(
            zip.getInputStream(zip.getEntry("data/cip/list/maven/artifactId="
                + componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) + "/classifier="
                + componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER) + "/extension="
                + componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION) + "/groupId="
                + componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID) + "/version="
                + componentIdentifier.get(ComponentIdentifier.VERSION) + "/list.json")), ComponentDetailsList.class);
        assertThat(list.getList(), hasSize(0));

        details = JsonUtils.parse(zip.getInputStream(zip.getEntry("data/cip/details/1249e25aebb15358bedd.json")),
            TestNamedComponentDetails.class);
        assertThat(details.getMatchState(), is("exact"));
        assertThat(details.getIdentificationSource(), is(IdentificationSource.SONATYPE.getId()));
        assertThat(details.getIdentificationSourceComment(), is(nullValue()));
        assertThat(details.getPolicyAlerts(), hasSize(1));
        assertThat(details.getPolicyAlerts().get(0).getTrigger().getPolicyId(), is(policy.getId()));
        assertThat(details.getPolicyAlerts().get(0).getTrigger().getComponentFacts(), hasSize(1));

        list = JsonUtils.parse(zip.getInputStream(zip.getEntry("data/cip/list/maven/"
            + "artifactId=tomcat-util/classifier=/extension=jar/groupId=tomcat/version=5.5.23/list.json")),
            ComponentDetailsList.class);
        ComponentDetails detailsFromList = findDetailsForComponent(list,
          ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"));
        assertThat(detailsFromList, is(notNullValue()));
        assertThat(detailsFromList.getOverriddenLicenses(), hasSize(1));
        assertThat(detailsFromList.getOverriddenLicenses().iterator().next().getLicenseId(),
            is(licenseOverride2.getLicenseIds().iterator().next()));
        assertThat(detailsFromList.getLicenseThreatGroupNames(), containsInAnyOrder("Weak Copyleft"));
        assertThat(detailsFromList.getLicenseThreatLevel(), is(2));
      }
    }
  }

  private static void assertComponent(String groupId, String artifactId, String version, String threatGroup,
      int threatLevel, List<ApiReportComponentDTOV2> components)
  {
    for (ApiReportComponentDTOV2 candidate : components) {
      Map<String, String> coordinates = candidate.componentIdentifier == null ? null
          : candidate.componentIdentifier.getCoordinates();

      if (coordinates != null && groupId.equals(coordinates.get("groupId"))
          && artifactId.equals(coordinates.get("artifactId"))
          && version.equals(coordinates.get("version"))) {
        for (ApiLicenseThreatDTOV2 effectiveLicense : candidate.licenseData.effectiveLicenseThreats) {
          if (threatGroup.equals(effectiveLicense.licenseThreatGroupName)) {
            assertThat(effectiveLicense.licenseThreatGroupLevel, is(threatLevel));

            if (threatLevel > 7) {
              assertThat(effectiveLicense.licenseThreatGroupCategory, is("critical"));
            }
            else if (threatLevel > 3) {
              assertThat(effectiveLicense.licenseThreatGroupCategory, is("severe"));
            }
            else if (threatLevel > 0) {
              assertThat(effectiveLicense.licenseThreatGroupCategory, is("moderate"));
            }
            else {
              assertThat(effectiveLicense.licenseThreatGroupCategory, is("no-threat"));
            }
            return;
          }
        }
        Assert.fail("Failed to find LTG");
      }
    }
    Assert.fail("Failed to find component");
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

    // keyFindings must not have duplicates
    JsonNode keyFindings = data.get("keyFindings");
    Assert.assertNotNull(keyFindings);
    Assert.assertTrue(keyFindings.size() > 0);
    Set<String> uniqueKeyFindings = new LinkedHashSet<>();
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
    Assert.assertTrue(countNotZero > 0);
  }

  private void testPartialMatchedJsonApplyChanges(String json) throws IOException {
    final ContainerNode<?> partialMatched = JsonUtils.parse(json);
    final JsonNode aaNode = partialMatched.get("aaData");
    for (JsonNode license : aaNode) {
      final JsonNode matchedComponentNodes = license.get("matchDetails");
      Assert.assertTrue(matchedComponentNodes.size() > 0);
      testLicenseThreatsApplyChanges(matchedComponentNodes);

      for (JsonNode matchDetail : matchedComponentNodes) {
        testJsonApplyDisplayNameChanges(matchDetail);
      }
    }
  }

  private void testJsonApplyDisplayNameChanges(JsonNode jsonNode) {
    ArrayNode displayNameNode = (ArrayNode) jsonNode.get("displayName").get("parts");
    assertThat(displayNameNode, is(notNullValue()));
    assertThat(displayNameNode.size(), is(5));
    Assert.assertThat(displayNameNode.get(0).get("field").textValue(), is("Group"));
    Assert.assertThat(displayNameNode.get(0).get("value").textValue(), is(jsonNode.get("groupId").textValue()));
    Assert.assertThat(displayNameNode.get(1).get("field"), is(nullValue()));
    Assert.assertThat(displayNameNode.get(1).get("value").textValue(), is(" : "));
    Assert.assertThat(displayNameNode.get(2).get("field").textValue(), is("Artifact"));
    Assert.assertThat(displayNameNode.get(2).get("value").textValue(), is(jsonNode.get("artifactId").textValue()));
    Assert.assertThat(displayNameNode.get(3).get("field"), is(nullValue()));
    Assert.assertThat(displayNameNode.get(3).get("value").textValue(), is(" : "));
    Assert.assertThat(displayNameNode.get(4).get("field").textValue(), is("Version"));
    Assert.assertThat(displayNameNode.get(4).get("value").textValue(), is(jsonNode.get("version").textValue()));
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

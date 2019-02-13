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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.mail.Message;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportDataDTOV2;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.component.HashComponentIdentifierDTO;
import com.sonatype.insight.brain.component.HashComponentIdentifierResource;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.hds.TestNamedComponentDetails;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.license.LicenseOverrideResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
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
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.vulnerability.SecurityVulnerabilityOverrideResource;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ReportResourceTest
    extends AbstractResourceTest
{
  private Application app;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent("ReportResourceTest_AppId");
  }

  private HttpRequest restRequest(String appId, String scanId) {
    return restRequest().path(ReportResource.RESOURCE_PATH).parameter(appId, scanId);
  }

  @Test
  public void testManuallyIdentifiedSimilarComponent() throws Exception {
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
    tempEntity.newClaimedComponent(hashComponentIdentifier);

    String scanId = "testClaimedComponent_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");

    assertResponseStatus(200, restRequest().path(ReportResource.getReportPath(app.getPublicId(), scanId)).get());

    HttpRequest request = restRequest(app.getPublicId(), scanId).path("browseReport");
    HttpResponse response = request.subpath("bom.json").get();
    assertResponseStatus(200, response);
    boolean foundClaimedComponent = false;
    String bomJsonData = response.getBodyText();
    for (JsonNode bomJsonNode : JsonUtils.parse(bomJsonData).get("aaData")) {
      String bomJsonHash = bomJsonNode.get("hash").asText();
      JsonNode identificationSource = bomJsonNode.get("identificationSource");
      if (hash.equals(bomJsonHash)) {
        assertThat(identificationSource.asText()).isEqualTo(IdentificationSource.MANUAL.getId());
        assertThat(bomJsonNode.get("groupId").asText()).isEqualTo(groupId);
        assertThat(bomJsonNode.get("artifactId").asText()).isEqualTo(artifactId);
        assertThat(bomJsonNode.get("version").asText()).isEqualTo(version);
        assertThat(bomJsonNode.get("extension").asText()).isEqualTo(extension);
        assertThat(bomJsonNode.get("classifier").asText()).isEqualTo(classifier);
        assertThat(ComponentIdentifierAdapter.getComponentIdentifier(bomJsonNode)).isEqualTo(componentIdentifier);
        assertThat(bomJsonNode.get("matchState").asText()).isEqualTo(MatchState.EXACT.getId());
        assertThat(bomJsonNode.get("createTime").asLong()).isEqualTo(createTime.getTime());
        assertThat(bomJsonNode.get("relativePopularity").asDouble()).isEqualTo(0F);
        assertThat(JsonUtils.asPojo(bomJsonNode.get("displayName"), ComponentDisplayName.class).toString())
            .isEqualTo("testClaimedComponent_G : testClaimedComponent_A : testClaimedComponent_E : "
                + "testClaimedComponent_C : testClaimedComponent_V");
        foundClaimedComponent = true;
      }
      else {
        assertThat(identificationSource).isNull();
      }
    }
    assertThat(foundClaimedComponent).isTrue();

    response = request.subpath("licenses.json").get();
    assertResponseStatus(200, response);
    String licensesJsonData = response.getBodyText();
    assertThat(licensesJsonData).isNotBlank().doesNotContain(hash, "commons-httpclient");

    response = request.subpath("security.json").get();
    assertResponseStatus(200, response);
    String securityJsonData = response.getBodyText();
    assertThat(securityJsonData).isNotBlank().doesNotContain(hash, "commons-httpclient");

    response = request.subpath("partialmatched.json").get();
    assertResponseStatus(200, response);
    String partialmatched = response.getBodyText();
    assertThat(partialmatched).isNotBlank().doesNotContain(hash, "commons-httpclient").contains("c32df577f739535648b0",
        "org.slf4j.api_1.6.1.v20100831-0715.jar");

    response = request.subpath("data.json").get();
    assertResponseStatus(200, response);
    String jsonData = response.getBodyText();
    JsonNode actual = JsonUtils.parse(jsonData);
    assertThat(actual.get("partiallyMatchedComponentCount").asInt()).isEqualTo(1);
    assertThat(actual.get("exactlyMatchedComponentCount").asInt()).isEqualTo(27);
    assertThat(actual.get("knownArtifactCount").asInt()).isEqualTo(28);
  }

  @Test
  public void testManuallyIdentifiedUnknownComponent() throws Exception {
    String hash = "c32df577f739535648b0";
    String groupId = "testClaimedComponent_G";
    String artifactId = "testClaimedComponent_A";
    String version = "testClaimedComponent_V";
    String extension = "testClaimedComponent_E";
    String classifier = "testClaimedComponent_C";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version,
        classifier, extension);
    Date createTime = new Date();
    // The hash of org.slf4j.api_1.6.1.v20100831-0715.jar which is marked as unknown
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash, componentIdentifier);
    hashComponentIdentifier.setCreateTime(createTime);
    tempEntity.newClaimedComponent(hashComponentIdentifier);

    String scanId = "testClaimedComponent_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");

    assertResponseStatus(200, restRequest().path(ReportResource.getReportPath(app.getPublicId(), scanId)).get());

    HttpRequest request = restRequest(app.getPublicId(), scanId).path("browseReport");
    HttpResponse response = request.subpath("data.json").get();

    assertResponseStatus(200, response);
    String jsonData = response.getBodyText();
    JsonNode actual = JsonUtils.parse(jsonData);
    assertThat(actual.get("partiallyMatchedComponentCount").asInt()).isEqualTo(2);
    assertThat(actual.get("exactlyMatchedComponentCount").asInt()).isEqualTo(27);
    assertThat(actual.get("knownArtifactCount").asInt()).isEqualTo(29);

    response = request.subpath("summary.json").get();
    String summaryData = response.getBodyText();
    actual = JsonUtils.parse(summaryData);
    assertThat(actual.get("knownArtifactCount").asInt()).isEqualTo(29);
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
    tempEntity.newClaimedComponent(hashComponentIdentifier);

    String licenseId = new LicenseDAO().getByIdNotNull("GPL-3.0").getId(); // db lookup to make sure licenseId is valid
    tempEntity.newLicenseOverride(app.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, licenseId,
        "manual override");

    String scanId = "testClaimedComponent_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");

    assertResponseStatus(200, restRequest().path(ReportResource.getReportPath(app.getPublicId(), scanId)).get());

    HttpRequest request = restRequest(app.getPublicId(), scanId).path("browseReport");
    HttpResponse response = request.subpath("licenses.json").get();
    assertResponseStatus(200, response);
    String licensesJsonData = response.getBodyText();
    assertThat(licensesJsonData).isNotBlank().contains(hash, artifactId).doesNotContain("commons-httpclient");
  }

  @Test
  public void testManuallyIdentifiedComponentInvalidatesCachedReportData() throws Exception {
    String scanId = "testClaimedComponent_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");

    // populate JSON data cache before claiming the component
    HttpRequest request = restRequest(app.getPublicId(), scanId).path("browseReport");
    HttpResponse response = request.subpath("bom.json").get();
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
    response = restRequest().path(HashComponentIdentifierResource.RESOURCE_PATH).body(hashComponentIdentifier).post();
    assertResponseStatus(200, response);
    HashComponentIdentifierDTO hashComponentIdentifierDTO = response.getBody(HashComponentIdentifierDTO.class);
    tempEntity.register(new HashComponentIdentifierDAO().getById(hashComponentIdentifierDTO.id));

    response = request.subpath("bom.json").get();
    assertResponseStatus(200, response);
    boolean foundClaimedComponent = false;
    String bomJsonData = response.getBodyText();
    for (JsonNode bomJsonNode : JsonUtils.parse(bomJsonData).get("aaData")) {
      String bomJsonHash = bomJsonNode.get("hash").asText();
      JsonNode identificationSource = bomJsonNode.get("identificationSource");
      if (hash.equals(bomJsonHash)) {
        assertThat(identificationSource.asText()).isEqualTo(IdentificationSource.MANUAL.getId());
        assertThat(bomJsonNode.get("groupId").asText()).isEqualTo(groupId);
        assertThat(bomJsonNode.get("artifactId").asText()).isEqualTo(artifactId);
        assertThat(bomJsonNode.get("version").asText()).isEqualTo(version);
        assertThat(bomJsonNode.get("extension").asText()).isEqualTo(extension);
        assertThat(bomJsonNode.get("classifier").asText()).isEqualTo(classifier);
        assertThat(ComponentIdentifierAdapter.getComponentIdentifier(bomJsonNode)).isEqualTo(componentIdentifier);
        assertThat(JsonUtils.asPojo(bomJsonNode.get("displayName"), ComponentDisplayName.class).toString())
            .isEqualTo("testClaimedComponent_G : testClaimedComponent_A : testClaimedComponent_E : "
                + "testClaimedComponent_C : testClaimedComponent_V");
        assertThat(bomJsonNode.get("matchState").asText()).isEqualTo(MatchState.EXACT.getId());
        foundClaimedComponent = true;
      }
      else {
        assertThat(identificationSource).isNull();
      }
    }
    assertThat(foundClaimedComponent).isTrue();

    response = request.subpath("licenses.json").get();
    assertResponseStatus(200, response);
    String licensesJsonData = response.getBodyText();
    assertThat(licensesJsonData).isNotBlank().doesNotContain(hash, "commons-httpclient");

    response = request.subpath("security.json").get();
    assertResponseStatus(200, response);
    String securityJsonData = response.getBodyText();
    assertThat(securityJsonData).isNotBlank().doesNotContain(hash, "commons-httpclient");

    response = request.subpath("partialmatched.json").get();
    assertResponseStatus(200, response);
    String partialmatched = response.getBodyText();
    assertThat(partialmatched).isNotBlank().doesNotContain(hash, "commons-httpclient");
  }

  @Test
  public void testBrowseReportEntryExpirationDate() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    HttpRequest request = restRequest(app.getPublicId(), scanId).path("browseReport");

    mockReport(scanId, "/ReportResourceTest/report");

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
        .as("insight.js expires in 365 days: " + expires + " vs " + calendar.getTime()).isLessThan(2 * 60 * 1000);

    calendar.setTime(new Date());
    response = request.subpath("data.json").get();
    assertResponseStatus(200, response);
    expiresHeader = response.getHeader("Expires");
    expires = expirationHeaderFormat.parse(expiresHeader);
    assertThat(Math.abs(calendar.getTimeInMillis() - expires.getTime()))
        .as("data.json expires immediately: " + expires + " vs " + calendar.getTime()).isLessThan(2 * 60 * 1000);

    calendar.setTime(new Date());
    response = request.subpath("index.html").get();
    assertResponseStatus(200, response);
    expiresHeader = response.getHeader("Expires");
    expires = expirationHeaderFormat.parse(expiresHeader);
    assertThat(Math.abs(calendar.getTimeInMillis() - expires.getTime()))
        .as("index.html expires immediately: " + expires + " vs " + calendar.getTime()).isLessThan(2 * 60 * 1000);

    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
    String ifModifiedSinceHeader = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).format(calendar
        .getTime());
    response = request.subpath("data.json").header("If-Modified-Since", ifModifiedSinceHeader).get();
    assertResponseStatus(304, response);

    // make sure index.html always returns 200, no 304s here
    response = request.subpath("index.html").header("If-Modified-Since", ifModifiedSinceHeader).get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testBrowseReport() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    HttpRequest request = restRequest(app.getPublicId(), scanId).path("browseReport");

    String reportResource = "/ReportResourceTest/report";
    mockReport(scanId, reportResource);

    //This will trigger two grandfathered policy violations upon evaluation.
    app.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(app);
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
    policy.setPolicyViolationGrandfatheringAllowed(true);
    tempEntity.newPolicy(policy);

    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).query("scanId", scanId)
        .parameter(app.getPublicId()).body(new Stage(Stage.ID_BUILD)).post();
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

      if ("data.json".equals(entry)) {
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
        JsonNode expected = JsonUtils.parse(FileUtils.fileRead(file, "UTF-8"));
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
        assertThat(response.getBodyText()).isEqualToIgnoringWhitespace(FileUtils.fileRead(file, "UTF-8"));
      }
      else {
        assertThat(response.getBodyBytes()).as("Unexpected content for " + entry)
            .isEqualTo(org.apache.commons.io.FileUtils.readFileToByteArray(file));
      }
    }
    assertThat(verifiedFileCount).isEqualTo(110);

    assertResponseStatus(200, restRequest().path(ReportResource.getReportPath(app.getPublicId(), scanId)).get());
  }

  @Test
  public void testBrowseReport_NoDirectoryTraversal() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");
    File reportDir = getCLMServer().getReportDir(app.getId(), scanId);
    reportDir.mkdirs();
    new File(reportDir, "restricted.txt").createNewFile();

    HttpRequest request = restRequest(app.getPublicId(), scanId).path("browseReport");

    HttpResponse response = request.subpath("../restricted.txt").get();
    assertResponseStatus(404, response);

    response = request.subpath("%2E%2E/restricted.txt").get();
    assertResponseStatus(404, response);

    response = request.subpath("%2E%2E%5Crestricted.txt").get();
    assertResponseStatus(404, response);
  }

  @Test
  public void testEmbedReport() throws Exception {
    String scanId = "abcdefg12345";
    HttpResponse response = restRequest(app.getPublicId(), scanId).path("embedReport/index.html").get();
    assertResponseStatus(200, response);

    String content = response.getBodyText();
    assertThat(content)
        .contains(restRequest().path(UserInterfaceLinksResource.RESOURCE_PATH, UserInterfaceLinksResource.REPORT_PATH)
            .parameter(app.getPublicId(), scanId).getUrl());
    assertThat(response.getHeader("Expires")).isEqualTo("Thu, 01 Jan 1970 00:00:00 GMT");
    assertThat(response.getContentType().replace(" ", "")).isEqualToIgnoringCase("text/html;charset=UTF-8");
  }

  @Test
  public void testEmbedReport_AnonymousNotAllowed() throws Exception {
    String scanId = "abcdefg12345";
    String appPublicId = "bom1-12345678";

    HttpResponse response = restRequest(appPublicId, scanId).path("embedReport/index.html").anon().get();
    assertResponseStatus(401, response);
  }

  @Test
  @ManualServerInit
  public void testEmbedReport_AnonymousAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    String scanId = "abcdefg12345";
    String appPublicId = "bom1-12345678";

    HttpResponse response = restRequest(appPublicId, scanId).path("embedReport/index.html").anon().get();
    assertResponseStatus(200, response);

    String content = response.getBodyText();
    assertThat(content)
        .contains(restRequest().path(UserInterfaceLinksResource.RESOURCE_PATH, UserInterfaceLinksResource.REPORT_PATH)
            .parameter(appPublicId, scanId).getUrl());
    assertThat(response.getHeader("Expires")).isEqualTo("Thu, 01 Jan 1970 00:00:00 GMT");
  }

  @Test
  public void testEmbedReport_Json() throws Exception {
    String scanId = "abcdefg12345";
    HttpResponse response = restRequest(app.getPublicId(), scanId)
        .path("embedReport", ScanPolicyEvaluator.POLICY_ALERTS_FILENAME).get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Reports have been moved.  Clear cache and reload.");
  }

  @Test
  public void testEmbedReport_Json_AnonymousNotAllowed() throws Exception {
    String scanId = "abcdefg12345";
    String appPublicId = "bom1-12345678";

    HttpResponse response = restRequest(appPublicId, scanId)
        .path("embedReport", ScanPolicyEvaluator.POLICY_ALERTS_FILENAME).anon().get();
    assertResponseStatus(401, response);
  }

  @Test
  @ManualServerInit
  public void testEmbedReport_Json_AnonymousAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    String scanId = "abcdefg12345";
    String appPublicId = "bom1-12345678";

    HttpResponse response = restRequest(appPublicId, scanId)
        .path("embedReport", ScanPolicyEvaluator.POLICY_ALERTS_FILENAME).anon().get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Reports have been moved.  Clear cache and reload.");
  }

  @Test
  public void testPrintReport() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    createReportFile(app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    final HttpResponse response;
    try {
      response = restRequest(app.getPublicId(), scanId).path("printReport").get();
      assertResponseStatus(200, response);
      assertThat(response.getHeader("Content-Disposition"))
          .containsSubsequence("attachment; filename=\"" + app.getName() + "-Build-", ".pdf\"");
    }
    finally {
      Pdf.destroy();
    }

    // validate content type and check the actual content is really a PDF
    assertThat(response.getContentType()).isEqualTo("application/pdf");
    assertThat(new String(response.getBodyBytes(), 0, 1024, "US-ASCII")).contains("%PDF-");
  }

  @Test
  public void testPrintReport_AfterPreviousGenerationFailure() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    createReportFile(app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);

    HttpRequest request = restRequest(app.getPublicId(), scanId).path("printReport");
    HttpResponse response;
    try {
      response = request.get();
      assertResponseStatus(200, response);

      // pretend the print attempt crashed with OOME, which usually leaves an empty PDF file around
      File pdfFile = new File(getCLMServer().getReportDir(app.getId(), scanId), "report.pdf");
      assertThat(pdfFile).isFile();
      new FileOutputStream(pdfFile).close();

      // printing again after fixing the mem setting should produce a proper PDF
      response = request.get();
      assertResponseStatus(200, response);
      assertThat(response.getHeader("Content-Disposition"))
          .containsSubsequence("attachment; filename=\"" + app.getName() + "-Build-", ".pdf\"");
      assertThat(Long.parseLong(response.getHeader("Content-Length"))).isGreaterThan(0);
    }
    finally {
      Pdf.destroy();
    }

    // validate content type and check the actual content is really a PDF
    assertThat(response.getContentType()).isEqualTo("application/pdf");
    assertThat(new String(response.getBodyBytes(), 0, 1024, "US-ASCII")).contains("%PDF-");
  }

  @Test
  public void testPrintReport_BirtRenderingErrorsLeaveNoInvalidPdfBehind() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    File reportFile = createReportFile(app.getId(), scanId, "/ReportResourceTest/report-pdf");
    File pdfFile = Pdf.getPdfFile(reportFile);
    File cacheDir = Report.getCacheDir(reportFile);
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    HttpResponse response;
    try {
      response = restRequest(app.getPublicId(), scanId).path("printReport").get();
      assertResponseStatus(500, response);
      assertThat(pdfFile).doesNotExist();

      // until the missing JSON file gets fixed, the PDF should remain unprintable
      response = restRequest(app.getPublicId(), scanId).path("printReport").get();
      assertResponseStatus(500, response);
      assertThat(pdfFile).doesNotExist();

      FileUtils.fileWrite(new File(cacheDir, "policyalerts.json"), "UTF-8", "{\"aaData\":[]}");
      response = restRequest(app.getPublicId(), scanId).path("printReport").get();
      assertResponseStatus(200, response);
      assertThat(pdfFile).isFile();
    }
    finally {
      Pdf.destroy();
    }
  }

  @Test
  public void testReevaluateReport() throws Exception {
    String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/report");

    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
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
    PolicyDAO policyDAO = new PolicyDAO();
    policyDAO.insert(policy);
    final Stage stage = new Stage(Stage.ID_BUILD);

    List<Message> notifications = Mailbox.get("manager@test.corp");

    // Evaluate policy
    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).parameter(app.getPublicId())
        .query("scanId", scanId).body(stage).post();
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
    response = restRequest(app.getPublicId(), scanId).path("reevaluatePolicy").post();
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
    response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).parameter(app.getPublicId())
        .query("scanId", scanId).body(stage).post();
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
  public void test_LicenseOverrides_Organization() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    HttpRequest request = restRequest(app.getPublicId(), scanId).subpath("browseReport", "licenses.json");

    mockReport(scanId, "/ReportResourceTest/report");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("commons-pool", "commons-pool",
        "1.4", "", "jar");

    // Verify before any license overrides are added
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    int found = 0;
    String licenseJsonString = response.getBodyText();
    JsonNode licenseJsonData = JsonUtils.parse(licenseJsonString).get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonData) {
      if (componentIdentifier
          .equals(JsonUtils.asPojo(licenseJsonNode.get("componentIdentifier"), ComponentIdentifier.class))) {
        assertThat(licenseJsonNode.get("overriddenLicenses")).isNull();
        found++;
      }
    }
    assertThat(found).as("Did not find expected license").isEqualTo(1);

    // Override the license at organization level
    LicenseOverride orgLicenseOverride = new LicenseOverride(app.getOrganizationId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GPL-3.0", "My org license override");
    response = restRequest().path(LicenseOverrideResource.RESOURCE_PATH)
        .parameter(OwnerType.ORGANIZATION, app.getOrganizationId()).body(orgLicenseOverride).post();
    assertResponseStatus(200, response);
    orgLicenseOverride = response.getBody(LicenseOverride.class);

    response = request.get();
    assertResponseStatus(200, response);
    found = 0;
    licenseJsonString = response.getBodyText();
    licenseJsonData = JsonUtils.parse(licenseJsonString).get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonData) {
      if (componentIdentifier
          .equals(JsonUtils.asPojo(licenseJsonNode.get("componentIdentifier"), ComponentIdentifier.class))) {
        String overridenLicenseNamesStr = licenseJsonNode.get("overriddenLicenses").toString();
        assertThat(overridenLicenseNamesStr).isEqualTo("[\"GPL-3.0\"]");
        int effectiveLicenseThreat = licenseJsonNode.get("effectiveLicenseThreat").asInt();
        assertThat(effectiveLicenseThreat).isEqualTo(9);
        int overriddenLicenseThreat = licenseJsonNode.get("overriddenLicenseThreat").asInt();
        assertThat(overriddenLicenseThreat).isEqualTo(9);
        String status = licenseJsonNode.get("status").asText();
        assertThat(status).isEqualTo(LicenseOverrideStatus.OVERRIDDEN.getName());
        String comment = licenseJsonNode.get("comment").asText();
        assertThat(comment).isEqualTo("My org license override");
        found++;
      }
    }
    assertThat(found).as("Did not find expected overridden license").isEqualTo(1);

    // Override the license at application level
    LicenseOverride appLicenseOverride = new LicenseOverride(app.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My app license override");
    response = restRequest().path(LicenseOverrideResource.RESOURCE_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId()).body(appLicenseOverride).post();
    assertResponseStatus(200, response);
    appLicenseOverride = response.getBody(LicenseOverride.class);

    response = request.get();
    assertResponseStatus(200, response);
    found = 0;
    licenseJsonString = response.getBodyText();
    licenseJsonData = JsonUtils.parse(licenseJsonString).get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonData) {
      if (componentIdentifier
          .equals(JsonUtils.asPojo(licenseJsonNode.get("componentIdentifier"), ComponentIdentifier.class))) {
        String overridenLicenseNamesStr = licenseJsonNode.get("overriddenLicenses").toString();
        assertThat(overridenLicenseNamesStr).isEqualTo("[\"GPL-2.0\"]");
        int effectiveLicenseThreat = licenseJsonNode.get("effectiveLicenseThreat").asInt();
        assertThat(effectiveLicenseThreat).isEqualTo(9);
        int overriddenLicenseThreat = licenseJsonNode.get("overriddenLicenseThreat").asInt();
        assertThat(overriddenLicenseThreat).isEqualTo(9);
        String status = licenseJsonNode.get("status").asText();
        assertThat(status).isEqualTo(LicenseOverrideStatus.OVERRIDDEN.getName());
        String comment = licenseJsonNode.get("comment").asText();
        assertThat(comment).isEqualTo("My app license override");
        found++;
      }
    }
    assertThat(found).as("Did not find expected overridden license").isEqualTo(1);
  }

  @Test
  public void test_SecurityVulnerabilityOverrides() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    HttpRequest request = restRequest(app.getPublicId(), scanId).subpath("browseReport", "security.json");

    mockReport(scanId, "/ReportResourceTest/report");

    // Verify before any overrides are added
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    int found = 0;
    String svJsonString = response.getBodyText();
    JsonNode svJsonData = JsonUtils.parse(svJsonString).get("aaData");
    assertThat(svJsonData.size()).isGreaterThan(0);
    for (JsonNode svJsonNode : svJsonData) {
      assertThat(svJsonNode.get("status")).isNull();
      assertThat(svJsonNode.get("comment")).isNull();
    }

    // Override a security vulnerability
    String hash = "494308fc2d433720c778";
    String source = "cve";
    String referenceId = "CVE-2009-1524";
    String comment = "My comment";
    SecurityVulnerabilityOverride override = new SecurityVulnerabilityOverride(app.getId(), hash, source,
        referenceId, SecurityVulnerabilityOverrideStatus.CONFIRMED, comment);
    response = restRequest().path(SecurityVulnerabilityOverrideResource.RESOURCE_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId()).body(override).put();
    assertResponseStatus(200, response);
    override = response.getBody(SecurityVulnerabilityOverride.class);

    response = request.get();
    assertResponseStatus(200, response);
    found = 0;
    svJsonString = response.getBodyText();
    svJsonData = JsonUtils.parse(svJsonString).get("aaData");
    for (JsonNode svJsonNode : svJsonData) {
      if (hash.equals(svJsonNode.get("hash").asText()) && source.equals(svJsonNode.get("source").asText())
          && referenceId.equals(svJsonNode.get("reference").asText())) {
        assertThat(svJsonNode.get("status").asText())
            .isEqualTo(SecurityVulnerabilityOverrideStatus.CONFIRMED.getName());
        assertThat(svJsonNode.get("comment").asText()).isEqualTo(comment);
        found++;
      }
    }
    assertThat(found).as("Did not find expected overridden security vulnerability").isEqualTo(1);
  }

  @Test
  public void testRedirection() throws Exception {
    String path = "index.html?x=y&a=b";
    HttpResponse response = restRequest("appId", "scanId").path("brain", "index.html").query("x=y&a=b").get();
    assertResponseStatus(307, response);
    assertThat(response.getHeader("Location")).isEqualTo(getRestBaseUrl() + path);
  }

  @Test
  public void testDownloadBundle_LegacyFormat() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";

    mockReport(scanId, "/ReportResourceTest/standalone-legacy");

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

    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).parameter(app.getPublicId())
        .query("scanId", scanId).body(new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    response = restRequest(app.getPublicId(), scanId).path(ReportResource.DOWNLOAD_BUNDLE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("application/zip");
    assertThat(response.getHeader("Content-Disposition")).contains("filename=");
    try (InputStream actual = response.getBodyStream()) {
      File temp = tempDir.newFile();
      FileUtils.copyStreamToFile(new RawInputStreamFacade(actual), temp);
      try (ZipFile zip = new ZipFile(temp)) {
        assertThat(zip.getEntry("data/report.pdf")).isNotNull();
        assertThat(zip.getEntry("detail.rptdesign")).isNull();
        assertThat(zip.getEntry("data/index.html")).isNull();
        assertThat(zip.getEntry("data/components.json")).isNotNull();
        assertThat(zip.getEntry("data/release-graph/tomcat/tomcat-util/5.5.23.png")).isNotNull();
        assertThat(zip.getEntry("data/" + ScanPolicyEvaluator.POLICY_THREATS_FILENAME)).isNotNull();

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
                + componentIdentifier.get(ComponentIdentifier.VERSION) + ".json")), ComponentDetailsList.class);
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
            .getLicenseIds().iterator().next());
        assertThat(details.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Weak Copyleft");
        assertThat(details.getLicenseThreatLevel()).isEqualTo(2);
      }
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

    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).parameter(app.getPublicId())
        .query("scanId", scanId).body(new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    response = restRequest(app.getPublicId(), scanId).path(ReportResource.DOWNLOAD_BUNDLE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("application/zip");
    assertThat(response.getHeader("Content-Disposition")).contains("filename=");
    try (InputStream actual = response.getBodyStream()) {
      File temp = tempDir.newFile();
      FileUtils.copyStreamToFile(new RawInputStreamFacade(actual), temp);
      try (ZipFile zip = new ZipFile(temp)) {
        assertThat(zip.getEntry("data/report.pdf")).isNotNull();
        assertThat(zip.getEntry("detail.rptdesign")).isNull();
        assertThat(zip.getEntry("data/index.html")).isNull();

        ZipEntry componentEntry = zip.getEntry("data/components.json");
        assertThat(componentEntry).isNotNull();
        ApiReportDataDTOV2 components = JsonUtils.parse(zip.getInputStream(componentEntry), ApiReportDataDTOV2.class);

        assertThat(components.matchSummary.knownComponentCount).isEqualTo(5);
        assertThat(components.matchSummary.totalComponentCount).isEqualTo(29);
        assertComponent("tomcat", "tomcat-util", "5.5.23", "Weak Copyleft", 2, components.components);

        assertThat(zip.getEntry("data/release-graph/maven/"
            + "artifactId=tomcat-util/classifier=/extension=jar/groupId=tomcat/version=5.5.23/releases.png"))
                .isNotNull();
        assertThat(zip.getEntry("data/" + ScanPolicyEvaluator.POLICY_THREATS_FILENAME)).isNotNull();

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
                + componentIdentifier.get(ComponentIdentifier.VERSION) + "/list.json")), ComponentDetailsList.class);
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
            .getLicenseIds().iterator().next());
        assertThat(detailsFromList.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Weak Copyleft");
        assertThat(detailsFromList.getLicenseThreatLevel()).isEqualTo(2);
      }
    }
  }

  @Test
  public void testDownloadBundle_v3() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    mockReport(scanId, "/ReportResourceTest/standalone-v3/");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("org.webjars.npm",
        "reactivex:rxjs", "5.0.0-alpha.7", "", "jar");
    Policy policy = tempEntity.newPolicy(app);

    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).parameter(app.getPublicId())
        .query("scanId", scanId).body(new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    response = restRequest(app.getPublicId(), scanId).path(ReportResource.DOWNLOAD_BUNDLE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("application/zip");
    assertThat(response.getHeader("Content-Disposition")).contains("filename=");
    try (InputStream actual = response.getBodyStream()) {
      File temp = tempDir.newFile();
      FileUtils.copyStreamToFile(new RawInputStreamFacade(actual), temp);
      try (ZipFile zip = new ZipFile(temp)) {
        assertThat(zip.getEntry("data/report.pdf")).isNotNull();
        assertThat(zip.getEntry("detail.rptdesign")).isNull();
        assertThat(zip.getEntry("data/index.html")).isNull();

        ZipEntry componentEntry = zip.getEntry("data/components.json");
        assertThat(componentEntry).isNotNull();
        ApiReportDataDTOV2 components = JsonUtils.parse(zip.getInputStream(componentEntry), ApiReportDataDTOV2.class);

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
        assertThat(zip.getEntry("data/" + ScanPolicyEvaluator.POLICY_THREATS_FILENAME)).isNotNull();

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

    // ReportResource.fetchReport requires a report.zip to exist when evaluations exist
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
    assertThat(response.getBodyText()).isEqualTo("Could not download the report for scan ID 12345678");
  }

  @Test
  public void testGetReportMetadata_expandedCoverage() throws Exception {
    final String scanId = "ScanId";
    mockReport(scanId, "/ReportResourceTest/report-expanded_coverage");

    // Verify Response for scan
    HttpResponse response = restRequest(app.getPublicId(), scanId).path(ReportResource.METADATA_PATH).get();
    assertResponseStatus(200, response);
    ReportMetadataDTO metadata = response.getBody(ReportMetadataDTO.class);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getReportTitle()).isEqualTo("Expanded Coverage Report");
    assertThat(metadata.getReportTime().getTime()).isEqualTo(1503511338632L);
  }

  private static ComponentIdentifier identifier(Character c) {
    return new ComponentIdentifier("bb", Collections.singletonMap("x", String.valueOf(c)));
  }

  private static void assertComponent(String groupId,
                                      String artifactId,
                                      String version,
                                      String threatGroup,
                                      int threatLevel,
                                      List<ApiReportComponentDTOV2> components)
  {
    for (ApiReportComponentDTOV2 candidate : components) {
      Map<String, String> coordinates = candidate.componentIdentifier == null ? null : candidate.componentIdentifier
          .getCoordinates();

      if (coordinates != null && groupId.equals(coordinates.get("groupId"))
          && artifactId.equals(coordinates.get("artifactId")) && version.equals(coordinates.get("version"))) {
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

    assertThat(data.get("policyCounts").toString()).isEqualTo("[0,0,0,0,0,2,0,0,0,0,0]");
    assertThat(data.get("policyComponentCount").asInt()).isEqualTo(2);
    assertThat(data.get("grandfatheredPolicyViolationCount").asInt()).isEqualTo(2);

    assertThat(data.get("securityPunchCard").toString()).isEqualTo("[[4,11,3],[0,18,0],[0,12,0],[0,6,0],[0,6,0]]");
    assertThat(data.get("licensePunchCard").toString()).isEqualTo("[[2,7,1],[2,6,0],[1,3,0],[0,1,0],[0,1,0]]");

    assertThat(data.get("exactlyMatchedComponentCount").asInt()).isEqualTo(26);
    assertThat(data.get("knownArtifactCount").asInt()).isEqualTo(28);
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
      assertThat(threat).isBetween(0,  10);
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
      default:
        fail("Unexpected format " + componentIdentifier.getFormat());
    }
  }

  @Test
  public void testPrepareExpandedCoverageReport() throws Exception {
    String scanId = "ScanId";
    mockReport(scanId, "/ReportResourceTest/report-expanded_coverage");

    File reportFile = new InsightWork(getCLMServer().getConfiguration()).getReportFile(app.getId(), scanId);
    assertThat(reportFile).doesNotExist();

    HttpResponse response = restRequest(app.getPublicId(), scanId).path(ReportResource.PREPARE_PATH).post();
    assertResponseStatus(204, response);
    assertThat(reportFile).isFile();
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

  private File createReportFile(String appId, String scanId) throws IOException {
    File reportFile = new InsightWork(getCLMServer().getConfiguration()).getReportFile(appId, scanId);
    FileUtils.copyURLToFile(getClass().getResource("/ReportResourceTest/sample-report.zip"), reportFile);
    return reportFile;
  }

  private File createReportFile(String appId, String scanId, String sourceReportDir) throws IOException {
    File reportFile = new InsightWork(getCLMServer().getConfiguration()).getReportFile(appId, scanId);
    FileUtils.copyFile(zipResourceDir(sourceReportDir), reportFile);
    return reportFile;
  }
}

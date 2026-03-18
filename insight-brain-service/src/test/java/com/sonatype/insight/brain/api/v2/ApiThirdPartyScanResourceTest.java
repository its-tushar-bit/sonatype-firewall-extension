/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiEvaluationResultCounterDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.IdeUsersOverviewDTO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiThirdPartyScanResource.SINCE_UTC_TIMESTAMP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ApiThirdPartyScanResourceTest
    extends AbstractResourceTest
{
  private HttpRequest scanBomRequest(String applicationId, String source, String stageId) {
    return restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyScanResource.SCAN_COMPONENTS)
        .parameter(applicationId, source)
        .query("stageId", stageId);
  }

  @Test
  public void testScanComponentAndGetScanStatus_Cyclone_Xml_v1_1() throws Exception {
    testScanComponentAndGetScanStatus("valid_bom.xml", MediaType.APPLICATION_XML);
  }

  @Test
  public void testScanComponentAndGetScanStatus_Cyclone_Xml_v1_2() throws Exception {
    testScanComponentAndGetScanStatus("valid_bom_1_2.xml", MediaType.APPLICATION_XML);
  }

  @Test
  public void testScanComponentAndGetScanStatus_Cyclone_Xml_v1_3() throws Exception {
    testScanComponentAndGetScanStatus("valid_bom_1_3.xml", MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetIdeUsersOverview_RetrieveCorrectUserCountFromDbNoRange() throws Exception {
    tempEntity.newUserIdePolicyEvaluation("Jan");
    tempEntity.newUserIdePolicyEvaluation("Feb");
    tempEntity.newUserIdePolicyEvaluation("Jan");

    checkCountAtTimestamp(restRequest(), "IDE user count of all time", 2);
  }

  @Test
  public void testGetIdeUsersOverview_RetrieveCorrectUserCountFromDbWithRanges() throws Exception {
    final Date BEFORE_USERS = new Date();
    newUserIdePolicyEvaluationWithASmallDelay("Jan");
    final Date BETWEEN_USERS = new Date();
    newUserIdePolicyEvaluationWithASmallDelay("Feb");
    final Date AFTER_USERS = new Date();

    checkCountAtTimestamp(restRequest()
        .query(SINCE_UTC_TIMESTAMP, BEFORE_USERS.getTime()), "IDE user count since before both users recorded",
        2);
    checkCountAtTimestamp(restRequest()
        .query(SINCE_UTC_TIMESTAMP, BETWEEN_USERS.getTime()), "IDE user count between two users recorded",
        1);
    checkCountAtTimestamp(restRequest()
        .query(SINCE_UTC_TIMESTAMP, AFTER_USERS.getTime()), "IDE user count after both users recorded",
        0);
  }

  private void newUserIdePolicyEvaluationWithASmallDelay(String username) throws InterruptedException {
    // This is to prevent test flakiness in the case that the timestamps being the same
    TimeUnit.MILLISECONDS.sleep(5L);
    tempEntity.newUserIdePolicyEvaluation(username);
    TimeUnit.MILLISECONDS.sleep(5L);
  }

  private void checkCountAtTimestamp(
      final HttpRequest restRequest,
      final String description,
      final int expectedCount) throws Exception
  {
    HttpRequest request = restRequest
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyScanResource.IDE_USER_OVERVIEW)
        .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, "testClientUserAgent");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    assertThat(response.getBody(IdeUsersOverviewDTO.class).userCount)
        .as(description)
        .isEqualTo(expectedCount);
  }

  public void testScanComponentAndGetScanStatus(String fileName, String mediaType) throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    mockScanReceipt(scanReceipt);

    String bom = getBomFile(fileName);
    String testClientUserAgent = "testClientUserAgent";
    HttpRequest scanBomRequest = scanBomRequest(app.getId(), "clair", Stage.ID_BUILD);
    scanBomRequest.body(bom, mediaType);
    HttpResponse response = scanBomRequest //
        .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
        .post();
    assertResponseStatus(202, response);

    ApiThirdPartyScanTicketDTO ticketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(ticketDTO).isNotNull();
    assertThat(ticketDTO.statusUrl).isNotNull();
    assertThat(URI.create(ticketDTO.statusUrl).isAbsolute()).isFalse();

    ApiThirdPartyScanResultDTO resultDTO = getApiThirdPartyTicketResultDTO(ticketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();

    String reportUrl = "ui/links/application/" + app.getPublicId() + "/report/" + scanId;
    assertThat(resultDTO.reportHtmlUrl).isEqualTo(reportUrl);
    assertThat(resultDTO.reportPdfUrl).isEqualTo(reportUrl + "/pdf");
    assertThat(resultDTO.embeddableReportHtmlUrl).isEqualTo(reportUrl + "/embeddable");
    assertThat(resultDTO.reportDataUrl)
        .isEqualTo("api/v2/applications/" + app.getPublicId() + "/reports/" + scanId + "/raw");

    assertThat(resultDTO.policyAction).isEqualTo("None");
    assertEvaluationResultCounter(resultDTO.componentsAffected);
    assertEvaluationResultCounter(resultDTO.openPolicyViolations);
    assertThat(resultDTO.grandfatheredPolicyViolations).isEqualTo(0);
    assertThat(resultDTO.legacyViolations).isEqualTo(0);

    assertThat(getHdsServer().getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)
        .get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
  }

  private ApiThirdPartyScanResultDTO getApiThirdPartyTicketResultDTO(String statusUrl) {
    HttpResponse response = await().atMost(10, TimeUnit.SECONDS)
        .until(() -> restRequest().path(statusUrl).get(),
            resp -> resp.getStatusCode() == 200);
    return response.getBody(ApiThirdPartyScanResultDTO.class);
  }

  private String getBomFile(String path) throws Exception {
    byte[] bytes =
        Files.readAllBytes(Paths.get(getClass().getResource("/" + getClass().getSimpleName() + "/" + path).toURI()));
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private void assertEvaluationResultCounter(ApiEvaluationResultCounterDTO counter) {
    assertThat(counter).isNotNull();
    assertThat(counter.critical).isEqualTo(0);
    assertThat(counter.moderate).isEqualTo(0);
    assertThat(counter.severe).isEqualTo(0);
  }
}

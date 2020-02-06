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

import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiEvaluationResultCounterDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.75
 */
public class ApiThirdPartyResourceTest
    extends AbstractResourceTest
{
  private Application app;

  private HttpRequest scanBomRequest(String applicationId, String source, String stageId, String bom) {
    return restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(applicationId, source)
        .query("stageId", stageId)
        .body(bom, MediaType.APPLICATION_XML);
  }

  @Before
  public void setupApplication() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testEvaluateComponents_thirdPartyScan() throws Exception {
    String bom = getBomFile("/ApiThirdPartyResourceTest/valid_bom.xml");

    HttpResponse response = scanBomRequest(app.getId(), "clair", Stage.ID_BUILD, bom).post();
    assertResponseStatus(202, response);

    ApiThirdPartyScanTicketDTO ticketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(ticketDTO).isNotNull();
    assertThat(ticketDTO.statusUrl).isNotNull();
    assertThat(new URI(ticketDTO.statusUrl)).isNotNull();
  }

  @Test
  public void testEvaluateComponents_nullBom() throws Exception {
    HttpResponse response = scanBomRequest(app.getId(), "clair", Stage.ID_BUILD, null).post();
    assertResponseStatus(400, response);
  }
  
  @Test
  public void testEvaluateComponents_invalidStage() throws Exception {
    HttpResponse response = scanBomRequest(app.getId(), "clair", "invalidStage", null).post();
    assertResponseStatus(400, response);
  }

  @Test
  public void testEvaluateComponents_invalidBom() throws Exception {
    String bom = getBomFile("/ApiThirdPartyResourceTest/invalid_bom.xml");

    HttpResponse response = scanBomRequest(app.getId(), "clair", Stage.ID_BUILD, bom).post();
    String error = response.getBodyText();
    assertThat(error).matches(
        ".*Error on line number: 14, column number: 21 message: " +
            "cvc-complex-type.4:.*['\"]name['\"].*['\"]v:source['\"].*");
  }

  @Test
  public void testGetScanStatus_Completed() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    mockScanReceipt(scanReceipt);

    String bom = getBomFile("/" + getClass().getSimpleName() + "/valid_bom.xml");
    HttpResponse response = scanBomRequest(app.getId(), "clair", Stage.ID_BUILD, bom).post();
    assertResponseStatus(202, response);

    ApiThirdPartyScanTicketDTO ticketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(ticketDTO).isNotNull();
    assertThat(ticketDTO.statusUrl).isNotNull();
    assertThat(new URI(ticketDTO.statusUrl)).isNotNull();

    ApiThirdPartyScanResultDTO resultDTO = getApiThirdPartyTicketResultDTO(ticketDTO.statusUrl);
    assertThat(resultDTO.reportHtmlUrl).contains("/ui/links/application/" + app.getPublicId() + "/report/" + scanId);
    assertThat(resultDTO.policyAction).isEqualTo("None");
    assertThat(resultDTO.isError).isFalse();
    assertThat(resultDTO.errorMessage).isNull();
    assertEvaluationResultCounter(resultDTO.componentsAffected);
    assertEvaluationResultCounter(resultDTO.openPolicyViolations);
    assertThat(resultDTO.grandfatheredPolicyViolations).isEqualTo(0);
  }

  private ApiThirdPartyScanResultDTO getApiThirdPartyTicketResultDTO(String statusUrl) throws Exception {
    long endTime = System.currentTimeMillis() + 10000;
    while (System.currentTimeMillis() < endTime) {
      HttpResponse response = restRequest().path(statusUrl).get();
      if (response.getStatusCode() == 200) {
        return response.getBody(ApiThirdPartyScanResultDTO.class);
      }
      Thread.sleep(500);
    }
    throw new RuntimeException(
        "Retrieving scan status did not complete within the expected 10 seconds to get the scan result");
  }

  private String getBomFile(String path) throws Exception {
    byte[] bytes =
        Files.readAllBytes(Paths.get(getClass().getResource(path).toURI()));
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private void assertEvaluationResultCounter(ApiEvaluationResultCounterDTO counter) {
    assertThat(counter).isNotNull();
    assertThat(counter.critical).isEqualTo(0);
    assertThat(counter.moderate).isEqualTo(0);
    assertThat(counter.severe).isEqualTo(0);
  }
}

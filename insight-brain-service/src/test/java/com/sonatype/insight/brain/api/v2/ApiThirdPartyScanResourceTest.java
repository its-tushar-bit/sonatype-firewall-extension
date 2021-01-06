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
import java.util.concurrent.TimeUnit;

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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ApiThirdPartyScanResourceTest
    extends AbstractResourceTest
{
  private HttpRequest scanBomRequest(String applicationId, String source, String stageId, String bom) {
    return restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyScanResource.SCAN_COMPONENTS)
        .parameter(applicationId, source)
        .query("stageId", stageId)
        .body(bom, MediaType.APPLICATION_XML);
  }

  @Test
  public void testScanComponentAndGetScanStatus() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    mockScanReceipt(scanReceipt);

    String bom = getBomFile("valid_bom.xml");
    HttpResponse response = scanBomRequest(app.getId(), "clair", Stage.ID_BUILD, bom).post();
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
  }

  private ApiThirdPartyScanResultDTO getApiThirdPartyTicketResultDTO(String statusUrl) throws Exception {
    HttpResponse response = await().atMost(10, TimeUnit.SECONDS).until(() -> restRequest().path(statusUrl).get(),
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

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Kept in the {@code com.sonatype.insight.brain.scan} package with the original simple name because
 * {@link #uploadRequest} resolves its fixture via {@code getClass().getSimpleName()}.
 */
@IqH2Test
class ScanResourceTest
{
  private IqTestContext ctx;

  private Application app;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ScanResource.RESOURCE_PATH);
  }

  private HttpRequest uploadRequest(String appPublicId, String stageId, String resource) {
    return restRequest().query("stageId", stageId)
        .parameter(appPublicId)
        .part("file", resource, getClass().getResource("/" + getClass().getSimpleName() + "/" + resource))
        .part("filename", resource);
  }

  @BeforeEach
  void init() {
    app = ctx.tempEntity()
        .newApplication("ScanResourceTest", "ScanResourceTest", ctx.tempEntity().newOrganization().getId());
  }

  @Test
  void testUploadBinary() throws Exception {
    HttpResponse response = uploadRequest(app.getPublicId(), Stage.ID_BUILD, "app01.zip").post();
    ctx.assertResponseStatus(200, response);
    ScanTicket result = response.getBody(ScanTicket.class);
    assertThat(result).isNotNull();
    assertThat(result.ticketId).isNotNull();
    waitForScanTaskToBeProcessed(app.getPublicId(), result.ticketId);
  }

  @Test
  void testUploadBinary_IeErrorHandling() throws Exception {
    HttpResponse response = uploadRequest("bad-app-id", Stage.ID_BUILD, "app01.zip").query("noFormData", "true").post();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getContentType()).startsWith("text/plain");
    assertThat(response.getBodyText()).isEqualTo("Could not find an application with public ID bad-app-id.");
  }

  @Test
  void testUploadBinary_ValidateCsrfToken() throws Exception {
    HttpRequest request = uploadRequest(app.getPublicId(), Stage.ID_BUILD, "app01.zip");

    HttpResponse response = request.csrfToken("nonce", null, "nonce").post();
    ctx.assertResponseStatus(200, response);
    ScanTicket result = response.getBody(ScanTicket.class);
    assertThat(result).isNotNull();
    assertThat(result.ticketId).isNotNull();

    try {
      response = request.query("noFormData", "true").noCsrfToken().post();
      ctx.assertResponseStatus(200, response);
      assertThat(response.getBodyText()).isEqualTo("Invalid cross-site request forgery token");
    }
    finally {
      // let processing for this test complete before we head into the next test
      waitForScanTaskToBeProcessed(app.getPublicId(), result.ticketId);
    }
  }

  private void waitForScanTaskToBeProcessed(String appPublicId, String scanTicketId) {
    HttpRequest request = restRequest().path("{ticketId}").parameter(appPublicId, scanTicketId);
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      HttpResponse response = request.get();
      ctx.assertResponseStatus(200, response);
      ScanTicket scanTicket = response.getBody(ScanTicket.class);
      assertThat(scanTicket.currentStep).as("Scan task %s for application %s", scanTicketId, appPublicId)
          .isGreaterThanOrEqualTo(scanTicket.totalSteps);
    });
  }
}

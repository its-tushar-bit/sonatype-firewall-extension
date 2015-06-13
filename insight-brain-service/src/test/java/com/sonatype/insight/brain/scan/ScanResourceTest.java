/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ScanResourceTest
    extends AbstractResourceTest
{
  private Application app;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ScanResource.SERVICE_PATH);
  }

  private HttpRequest uploadRequest(String appPublicId, String stageId, String resource) {
    return restRequest().query("stageId", "{stageId}").parameter(appPublicId, stageId)
        .part("file", resource, getClass().getResource("/" + getClass().getSimpleName() + "/" + resource));
  }

  @Before
  public void init() {
    app = tempEntity.newApplication("ScanResourceTest", "ScanResourceTest", tempEntity.newOrganization().getId());
  }

  @Test
  public void testUploadBinary() throws Exception {
    HttpResponse response = uploadRequest(app.getPublicId(), Stage.ID_BUILD, "app01.zip").post();
    assertResponseStatus(200, response);
    ScanTicket result = fromJson(response, ScanTicket.class);
    assertThat(result, is(notNullValue()));
    assertThat(result.ticketId, is(notNullValue()));
    waitForScanTaskToBeProcessed(app.getPublicId(), result.ticketId);
  }

  @Test
  public void testUploadBinary_IeErrorHandling() throws Exception {
    HttpResponse response = uploadRequest("bad-app-id", Stage.ID_BUILD, "app01.zip").query("noFormData", "true").post();
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), startsWith("text/plain"));
    assertThat(response.getResponseBody(), is("Could not find an application with public ID bad-app-id."));
  }

  private void waitForScanTaskToBeProcessed(String appPublicId, String scanTicketId) throws Exception {
    // Allow 10 seconds for the scan task to be processed
    HttpRequest request = restRequest().path("{ticketId}").parameter(appPublicId, scanTicketId);
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start <= 10000) {
      HttpResponse response = request.get();
      assertResponseStatus(200, response);
      ScanTicket scanTicket = fromJson(response, ScanTicket.class);
      if (scanTicket.currentStep >= scanTicket.totalSteps) {
        System.out.println("Scan task " + scanTicketId + " for appPublicId " + appPublicId + " was finished after "
            + (System.currentTimeMillis() - start) + " ms");
        return;
      }
      Thread.sleep(10);
    }
    fail("Scan task " + scanTicketId + " for appPublicId " + appPublicId + " was not finished after "
        + (System.currentTimeMillis() - start) + " ms");
  }
}

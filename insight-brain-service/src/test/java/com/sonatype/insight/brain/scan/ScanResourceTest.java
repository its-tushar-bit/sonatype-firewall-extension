/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.InputStream;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ScanResourceTest
    extends AbstractResourceTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private Application app;

  private String getUploadUrl(String appPublicId, String stageId) {
    return getRestUrl(ScanResource.SERVICE_PATH, appPublicId) + "?stageId=" + stageId;
  }

  private Response upload(String resource, String url) throws Exception {
    InputStream resourceInputStream = getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/" + resource);
    try {
      AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(url);
      builder.addBodyPart(new FilePart("file", new ByteArrayPartSource(resource, IOUtil
          .toByteArray(resourceInputStream))));
      return AuthedRestAccess.execute(builder);
    }
    finally {
      IOUtil.close(resourceInputStream);
    }
  }

  @Before
  public void init() {
    app = tempEntity.newApplication("ScanResourceTest", "ScanResourceTest", tempEntity.newOrganization().getId());
  }

  @Test
  public void testUploadBinary() throws Exception {
    Response response = upload("app01.zip", getUploadUrl(app.getPublicId(), Stage.ID_BUILD));
    assertResponseStatus(200, response);
    ScanTicket result = fromJson(response, ScanTicket.class);
    assertThat(result, is(notNullValue()));
    assertThat(result.ticketId, is(notNullValue()));
    waitForScanTaskToBeProcessed(app.getPublicId(), result.ticketId);
  }

  @Test
  public void testUploadBinary_IeErrorHandling() throws Exception {
    Response response = upload("app01.zip", getUploadUrl("bad-app-id", Stage.ID_BUILD) + "&noFormData=true");
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), startsWith("text/plain"));
    assertThat(response.getResponseBody(), is("Could not find an application with public id bad-app-id."));
  }

  private void waitForScanTaskToBeProcessed(String appPublicId, String scanTicketId) throws Exception {
    // Allow 10 seconds for the scan task to be processed
    String url = getRestUrl(ScanResource.SERVICE_PATH, appPublicId) + "/" + scanTicketId;
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start <= 10000) {
      Response response = AuthedRestAccess.get(url);
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

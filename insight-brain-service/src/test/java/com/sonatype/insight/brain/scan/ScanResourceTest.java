/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.InputStream;

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

public class ScanResourceTest
    extends AbstractResourceTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private Application app;

  private String getUploadUrl(String appPublicId) {
    return getRestUrl(ScanResource.SERVICE_PATH, appPublicId);
  }

  private Response upload(String resource, String url) throws Exception {
    InputStream license = getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/" + resource);
    try {
      AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(url);
      builder.addBodyPart(new FilePart("file", new ByteArrayPartSource(null, IOUtil.toByteArray(license))));
      return AuthedRestAccess.execute(builder);
    }
    finally {
      IOUtil.close(license);
    }
  }

  @Before
  public void init() {
    app = tempEntity.newApplication(tempEntity.newOrganization().getId());
  }

  @Test
  public void testUploadBinary() throws Exception {
    Response response = upload("app01.zip", getUploadUrl(app.getPublicId()));
    assertResponseStatus(200, response);
    ScanTicket result = fromJson(response, ScanTicket.class);
    assertThat(result, is(notNullValue()));
    assertThat(result.ticketId, is(notNullValue()));
  }

  @Test
  public void testUploadBinary_IeErrorHandling() throws Exception {
    Response response = upload("app01.zip", getUploadUrl("bad-app-id") + "?forceSuccess=true");
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), startsWith("text/plain"));
    assertThat(response.getResponseBody(), is("Could not find an application with public id bad-app-id."));
  }
}

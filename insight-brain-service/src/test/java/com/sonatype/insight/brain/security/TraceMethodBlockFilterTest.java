/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.version.VersionResource;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;

public class TraceMethodBlockFilterTest
    extends AbstractBrainServiceTest
{
  private static class AsyncHttpClient
      extends com.ning.http.client.AsyncHttpClient
  {
    public BoundRequestBuilder prepareTrace(String url) {
      return requestBuilder("TRACE", url);
    }

    public BoundRequestBuilder prepareTrack(String url) {
      return requestBuilder("TRACK", url);
    }
  }

  @Test
  public void testTraceMethodBlocked() throws Exception {
    BoundRequestBuilder requestBuilder = new AsyncHttpClient()
        .prepareTrace(getRestUrl(InsightBrainService.BRAIN_ASSET_PATH.substring(1) + "/index.html"));
    Response response = AuthedRestAccess.execute(requestBuilder);
    assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);

    requestBuilder = new AsyncHttpClient().prepareTrace(getRestUrl(VersionResource.SERVICE_PATH));
    response = AuthedRestAccess.execute(requestBuilder);
    assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);
  }

  @Test
  public void testTrackMethodBlocked() throws Exception {
    BoundRequestBuilder requestBuilder = new AsyncHttpClient()
        .prepareTrack(getRestUrl(InsightBrainService.BRAIN_ASSET_PATH.substring(1) + "/index.html"));
    Response response = AuthedRestAccess.execute(requestBuilder);
    assertResponseStatus(HttpStatus.NOT_IMPLEMENTED_501, response);

    requestBuilder = new AsyncHttpClient().prepareTrack(getRestUrl(VersionResource.SERVICE_PATH));
    response = AuthedRestAccess.execute(requestBuilder);
    assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);
  }
}

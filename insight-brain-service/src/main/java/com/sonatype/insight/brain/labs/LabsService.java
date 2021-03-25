/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.labs;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.hds.DefaultHdsClient;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.HttpResponse;

@Named
public class LabsService
{
  private final DefaultHdsClient hdsClient;

  @Inject
  public LabsService(DefaultHdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  protected Response getLabsResponse(@Context final HttpServletRequest httpRequest, Map<String, String> params)
      throws IOException
  {
    HttpResponse httpResponseBack = hdsClient.forwardingProxy(httpRequest, params);
    return convertResponse(httpResponseBack);
  }

  @VisibleForTesting
  protected Response convertResponse(final HttpResponse httpResponseBack) throws IOException {
    return Response
        .status(Status.fromStatusCode(httpResponseBack.getStatusLine().getStatusCode()))
        .type(httpResponseBack.getEntity().getContentType().getValue())
        .entity(httpResponseBack.getEntity().getContent())
        .build();
  }
}

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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.hds.DefaultHdsClient;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
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
    HttpEntity entity = httpResponseBack.getEntity();
    ResponseBuilder responseBuilder = Response
        .status(Status.fromStatusCode(httpResponseBack.getStatusLine().getStatusCode()));
    String entityContentTypeValue = null;
    if (entity != null) {
      responseBuilder.entity(httpResponseBack.getEntity().getContent());

      Header contentType = entity.getContentType();
      if (contentType != null) {
        entityContentTypeValue = contentType.getValue();
        responseBuilder.type(httpResponseBack.getEntity().getContentType().getValue());
      }
    }

    Header[] allHeaders = httpResponseBack.getAllHeaders();
    if (allHeaders != null) {
      for (Header header : allHeaders) {
        // Jax-RS will throw exception if Content-Type header is set twice, setting the content type on the entity
        // automatically will set this header
        if (!header.getName().equals(HttpHeaders.CONTENT_TYPE) || entityContentTypeValue == null) {
          responseBuilder.header(header.getName(), header.getValue());
        }
      }
    }
    return responseBuilder.build();
  }
}

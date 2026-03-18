/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.labs;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.hds.HdsClient;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;

@Named
public class LabsService
{
  private final HdsClient hdsClient;

  @Inject
  public LabsService(HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  protected Response getLabsResponse(
      @Context final HttpServletRequest httpRequest,
      Map<String, String> params) throws IOException
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

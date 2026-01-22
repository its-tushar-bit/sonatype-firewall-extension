/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.hds;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HdsRequestLog
    implements RequestLog
{
  private static final Logger log = LoggerFactory.getLogger(HdsRequestLog.class);

  @Override
  public void log(Request request, Response response) {
    String query = request.getHttpURI().getQuery();
    query = query != null ? '?' + query : "";
    long elapsedMs = System.currentTimeMillis() - request.getBeginNanoTime() / 1_000_000;
    String statusMsg = HttpStatus.getMessage(response.getStatus());
    long contentLength = response.getHeaders().getLongField("Content-Length");
    log.info("{} \"{}{}\", {} {}, {} bytes, {} ms, license {}",
        request.getMethod(), request.getHttpURI().getPath(), query,
        response.getStatus(), statusMsg, contentLength, elapsedMs,
        request.getHeaders().get("X-CLM-Token"));
  }
}

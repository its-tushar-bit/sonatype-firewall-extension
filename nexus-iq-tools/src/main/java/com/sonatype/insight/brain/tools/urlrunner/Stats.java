/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.urlrunner;

import org.apache.http.HttpResponse;

public class Stats
{
  private String url;

  private String type;

  private HttpResponse response;

  private long responseTime;

  private String requestPayload;


  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public HttpResponse getResponse() {
    return response;
  }

  public void setResponse(final HttpResponse response) {
    this.response = response;
  }

  public long getResponseTime() {
    return responseTime;
  }

  public void setResponseTime(final long responseTime) {
    this.responseTime = responseTime;
  }

  public String getRequestPayload() {
    return requestPayload;
  }

  public void setRequestPayload(final String requestPayload) {
    this.requestPayload = requestPayload;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.urlrunner;

import com.sonatype.insight.brain.tools.metrics.MetricsReport;

import org.apache.http.StatusLine;

public class Stats
{
  private String url;

  private String type;

  private ResponseBody responseBody;

  private StatusLine statusLine;

  private long responseTime;

  private String requestPayload;

  private MetricsReport metricsReport;

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

  public ResponseBody getResponseBody() {
    return responseBody;
  }

  public void setResponseBody(final ResponseBody responseBody) {
    this.responseBody = responseBody;
  }

  public StatusLine getStatusLine() {
    return statusLine;
  }

  public void setStatusLine(final StatusLine statusLine) {
    this.statusLine = statusLine;
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

  public void setMetricsReport(MetricsReport metricsReport) {
    this.metricsReport = metricsReport;
  }

  public MetricsReport getMetricsReport() {
    return metricsReport;
  }
}

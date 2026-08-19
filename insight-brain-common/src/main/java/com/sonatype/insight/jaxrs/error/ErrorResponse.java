/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-utils
package com.sonatype.insight.jaxrs.error;

public class ErrorResponse
{
  public static final String CONTENT_TYPE = "text/plain;charset=UTF-8";

  private int statusCode;

  private String messageBody;

  private String incidentId;

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public String getMessageBody() {
    return messageBody;
  }

  public void setMessageBody(String messageBody) {
    this.messageBody = messageBody;
  }

  public ErrorResponse(int statusCode, String messageBody) {
    this.statusCode = statusCode;
    this.messageBody = messageBody;
  }

  public String getIncidentId() {
    return incidentId;
  }

  public void setIncidentId(String incidentId) {
    this.incidentId = incidentId;
  }

  @Override
  public String toString() {
    return statusCode + " " + messageBody;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation.actions;

/**
 * @since 1.64
 */
public class ApiRemediationRestActionValueDTO
{
  private String url;

  private String methodType;

  private String payload;

  public ApiRemediationRestActionValueDTO(String url, String methodType, String payload) {
    this.url = url;
    this.methodType = methodType;
    this.payload = payload;
  }

  // for JSON
  public ApiRemediationRestActionValueDTO() {
  }

  public String getUrl() {
    return url;
  }

  public String getMethodType() {
    return methodType;
  }

  public String getPayload() {
    return payload;
  }

  // for JSON
  public void setUrl(final String url) {
    this.url = url;
  }

  // for JSON
  public void setMethodType(final String methodType) {
    this.methodType = methodType;
  }

  // for JSON
  public void setPayload(final String payload) {
    this.payload = payload;
  }
}

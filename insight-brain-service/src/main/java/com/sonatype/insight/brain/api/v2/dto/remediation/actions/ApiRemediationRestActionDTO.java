/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation.actions;

/**
 * @since 1.64
 */
public class ApiRemediationRestActionDTO
{
  private ApiRemediationRestActionValueDTO restActionInternal;

  public ApiRemediationRestActionDTO(String url, String methodType, String payload) {
    this.restActionInternal = new ApiRemediationRestActionValueDTO(url, methodType, payload);
  }

  // for JSON
  public ApiRemediationRestActionDTO() {
  }

  public ApiRemediationRestActionValueDTO getRest() {
    return restActionInternal;
  }

  // for JSON
  public void setRest(ApiRemediationRestActionValueDTO rest) {
    this.restActionInternal = rest;
  }
}

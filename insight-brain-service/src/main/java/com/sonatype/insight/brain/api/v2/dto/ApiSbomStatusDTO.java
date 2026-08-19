/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class ApiSbomStatusDTO
{
  public String downloadUrl;

  public String applicationId;

  public String version;

  public boolean isError;

  public String errorMessage;

  public ApiSbomStatusDTO() {
  }

  public ApiSbomStatusDTO(String errorMessage) {
    this.isError = true;
    this.errorMessage = errorMessage;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class ApiLicensedSolutionDTO
{
  public String id;

  public String url;

  public ApiLicensedSolutionDTO() {

  }

  public ApiLicensedSolutionDTO(String id, String url) {
    this.id = id;
    this.url = url;
  }
}

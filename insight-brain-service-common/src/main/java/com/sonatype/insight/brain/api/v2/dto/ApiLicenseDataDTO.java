/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiLicenseDataDTO
{
  // licenses of component, in no particular order
  public List<ApiLicenseDTO> declaredLicenses = new ArrayList<>();

  public List<ApiLicenseDTO> observedLicenses = new ArrayList<>();

  /**
   * @since 1.88
   */
  public List<ApiLicenseDTO> effectiveLicenses = new ArrayList<>();

  @JsonInclude(Include.NON_NULL)
  public List<ApiLicenseDTO> overriddenLicenses = new ArrayList<>();

  @JsonInclude(Include.NON_NULL)
  public String status;
}

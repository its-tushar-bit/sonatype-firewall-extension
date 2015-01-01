/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.dto;

import java.util.ArrayList;
import java.util.List;

public class ApiLicenseDataDTO
{
  // licenses of component, in no particular order
  public List<ApiLicenseDTO> declaredLicenses = new ArrayList<>();

  public List<ApiLicenseDTO> observedLicenses = new ArrayList<>();

  public List<ApiLicenseDTO> overriddenLicenses = new ArrayList<>();

  public String status;
}

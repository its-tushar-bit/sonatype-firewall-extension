/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v1.dto.ApiLicenseDataDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiSecurityDataDTO;

/**
 *
 * @since 1.13.0
 */
public class ApiReportComponentDTOV2 extends ApiComponentDTOV2
{
  public String matchState;

  // occurrences of component, in no particular order
  public List<String> pathnames = new ArrayList<>();

  public ApiLicenseDataDTO licenseData;

  public ApiSecurityDataDTO securityData;
}

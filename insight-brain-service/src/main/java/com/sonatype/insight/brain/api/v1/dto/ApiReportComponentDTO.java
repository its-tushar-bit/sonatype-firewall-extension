/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.dto;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;

/**
 * @deprecated since 1.13.0, use {@link ApiReportComponentDTOV2}
 */
public class ApiReportComponentDTO
{
  public String hash;

  public ApiMavenCoordinatesDTO mavenCoordinates;

  public String matchState;

  public boolean proprietary;

  // occurrences of component, in no particular order
  public List<String> pathnames = new ArrayList<>();

  public ApiLicenseDataDTO licenseData;

  public ApiSecurityDataDTO securityData;
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.dto;

import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultDTOV2;

/**
 * Describes a single component within an application that matches the search criteria.
 *
 * @deprecated since 1.13.0, use {@link ApiSearchResultDTOV2}
 */
@Deprecated
public class ApiSearchResultDTO
{
  public String applicationId;

  public String applicationName;

  public String reportUrl;

  public String hash;

  public String groupId;

  public String artifactId;

  public String version;

  public Integer threatLevel;
}

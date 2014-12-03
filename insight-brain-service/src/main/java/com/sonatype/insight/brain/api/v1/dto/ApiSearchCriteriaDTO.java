/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.dto;

import com.sonatype.insight.brain.api.v2.dto.ApiSearchCriteriaDTOV2;

/**
 * Describes the search criteria that was processed.
 *
 * @deprecated since 1.13.0, use {@link ApiSearchCriteriaDTOV2}
 */
@Deprecated
public class ApiSearchCriteriaDTO
{
  public String stageId;

  public String hash;

  public String groupId;

  public String artifactId;

  public String version;
}

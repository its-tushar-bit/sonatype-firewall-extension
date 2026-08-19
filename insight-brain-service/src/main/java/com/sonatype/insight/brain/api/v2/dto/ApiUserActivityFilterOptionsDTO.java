/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

/**
 * DTO for user activity filter options response.
 */
public class ApiUserActivityFilterOptionsDTO
{
  public List<String> activityTypes;

  public List<String> domains;

  public List<String> errorTypes;
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.125
 */
public class ApiLicenseLegalComponentDashboardResultDTO
{
  public List<ApiLicenseLegalComponentDashboardDTO> results = new ArrayList<>();

  public int totalResultsCount;
}

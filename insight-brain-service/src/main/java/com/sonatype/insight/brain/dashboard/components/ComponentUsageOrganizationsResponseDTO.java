/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.List;

public class ComponentUsageOrganizationsResponseDTO
{
  public List<ComponentUsageOrganizationRowDTO> organizations = new ArrayList<>();

  public long total;

  public int page;

  public int pageSize;

  public boolean hasNextPage;
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.List;

/**
 * Paged reports where a component hash appears for a single application.
 */
public class ComponentUsageReportsResponseDTO
{
  public List<ComponentUsageReportRowDTO> reports = new ArrayList<>();

  /** Internal application id from the request (echoed for clients that only hold this response). */
  public String applicationId;

  /**
   * Public application id for report deep-links
   * ({@code /applications/{applicationPublicId}/report/{reportId}}). Null when the app cannot be
   * resolved or the caller is denied (RBAC short-circuit).
   */
  public String applicationPublicId;

  public long total;

  public int page;

  public int pageSize;

  public boolean hasNextPage;
}

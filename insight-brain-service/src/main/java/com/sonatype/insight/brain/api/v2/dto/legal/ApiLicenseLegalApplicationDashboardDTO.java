/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.ArrayList;
import java.util.List;

public class ApiLicenseLegalApplicationDashboardDTO
{
  public String applicationId;

  public String applicationName;

  public String applicationPublicId;

  public List<String> applicationTagNames = new ArrayList<>();

  public long lastScanTime;

  public String stageTypeId;

  public String stageTypeName;

  public int componentsReviewedCount;

  public int componentsTotalCount;
}

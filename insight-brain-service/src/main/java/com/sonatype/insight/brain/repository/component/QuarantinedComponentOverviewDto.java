/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.util.Date;

import com.sonatype.insight.json.store.ApiDateFormat;

public class QuarantinedComponentOverviewDto
{
  public String componentDisplayName;

  public boolean isQuarantined;

  public int quarantinedPolicyViolationsCount;

  public String repositoryName;

  @ApiDateFormat
  public Date quarantinedDate;

  @ApiDateFormat
  public Date cataloguedDate;
}

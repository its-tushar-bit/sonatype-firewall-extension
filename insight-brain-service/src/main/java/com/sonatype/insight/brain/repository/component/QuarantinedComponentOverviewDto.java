/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.json.store.ApiDateFormat;

public class QuarantinedComponentOverviewDto
{
  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String componentHash;

  public String matchState;

  public String pathname;

  public String componentDisplayName;

  public String componentVersion;

  public boolean isQuarantined;

  public int quarantinedPolicyViolationsCount;

  public String repositoryId;

  public String repositoryName;

  @ApiDateFormat
  public Date quarantinedDate;

  @ApiDateFormat
  public Date tokenExpiryTime;
}

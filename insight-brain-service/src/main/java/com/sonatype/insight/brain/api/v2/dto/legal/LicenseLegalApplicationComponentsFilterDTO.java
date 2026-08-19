/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Set;

public class LicenseLegalApplicationComponentsFilterDTO
{
  public Set<String> stageTypeIds;

  public Set<LicenseObligationReviewStatus> reviewStatuses;

  public Set<String> licenseThreatGroupNames;
}

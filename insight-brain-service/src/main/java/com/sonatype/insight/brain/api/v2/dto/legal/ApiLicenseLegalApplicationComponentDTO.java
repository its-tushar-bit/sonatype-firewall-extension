/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTOV2;

public class ApiLicenseLegalApplicationComponentDTO
{
  public String hash;

  public String displayName;

  public Set<ApiLicenseDTOV2> licenses = new HashSet<>();

  public int reviewCompletedCount;

  public int reviewTotalCount;

  public LicenseObligationReviewStatus reviewStatus;
}

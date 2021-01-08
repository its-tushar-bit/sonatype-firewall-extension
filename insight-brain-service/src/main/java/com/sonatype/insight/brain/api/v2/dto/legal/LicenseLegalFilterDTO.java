/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Set;

public class LicenseLegalFilterDTO
{
  public Set<String> applicationIds;

  public Set<String> organizationIds;

  public Set<String> stageTypeIds;

  public Set<String> tagIds;

  public Set<String> licenseIds;
}

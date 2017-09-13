/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Set;

/**
 * @since 1.33
 */
public class OwnerFilterDTO
{
  public Set<String> organizationIds;

  public Set<String> applicationIds;
}

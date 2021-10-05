/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * @since 1.106
 */
public class ApiFirewallQuarantineSummaryDTO
{
  public long repositoryCount;

  public long quarantineEnabledRepositoryCount;

  public boolean quarantineEnabled;

  public long totalComponentCount;

  public long quarantinedComponentCount;
}

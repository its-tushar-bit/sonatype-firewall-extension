/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

public class ContainerImageSummaryDTO
{
  public long totalContainerImageCount;

  public long totalContainerImageViolationCount;

  public long criticalViolationCount;

  public long severeViolationCount;

  public long moderateViolationCount;

  public long affectedContainerImageCount;

  public long quarantinedContainerImageCount;
}

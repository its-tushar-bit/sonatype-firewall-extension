/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.List;

/**
 * @since 1.35
 */
public class ComponentCountsDTO
{
  public int componentsPerApplication;

  public List<ComponentCountDTO> componentsInTheMostApplications;

  public List<ComponentCountDTO> componentsWithTheMostViolations;

  public static class ComponentCountDTO
  {
    public String componentDisplayName;

    // componentDisplayName alone isn't enough to key uniqueness off of
    public String hash;

    public int count;
  }
}

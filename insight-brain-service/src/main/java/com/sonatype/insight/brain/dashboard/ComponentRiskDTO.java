/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Carries the data backing the "Highest Risk Component View", i.e. roll-up of violations by component.
 */
public class ComponentRiskDTO
{

  public String hash;

  public int score;

  public int scoreCritical;

  public int scoreSevere;

  public int scoreModerate;

  public int scoreLow;

  public Set<GavDTO> gavs = new LinkedHashSet<>();

  // Insertion order matters, as the first path will be used as the display name throughout the UI for unknown
  // components.
  public Set<String> pathnames = new LinkedHashSet<>();
}

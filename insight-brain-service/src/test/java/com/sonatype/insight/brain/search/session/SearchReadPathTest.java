/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SearchReadPathTest
{
  @Test
  public void forSurface_defaultsToOld() {
    System.clearProperty("nexusOne.search.readPath.applications");
    System.clearProperty("nexusOne.search.readPath.violations");
    assertThat(SearchReadPathFlags.forSurface(SearchReadPathSurface.APPLICATIONS))
        .isEqualTo(SearchReadPath.OLD);
    assertThat(SearchReadPathFlags.forSurface(SearchReadPathSurface.VIOLATIONS))
        .isEqualTo(SearchReadPath.OLD);
  }

  @Test
  public void forSurface_readsNew() {
    System.setProperty("nexusOne.search.readPath.applications", "new");
    System.setProperty("nexusOne.search.readPath.violations", "new");
    try {
      assertThat(SearchReadPathFlags.forSurface(SearchReadPathSurface.APPLICATIONS))
          .isEqualTo(SearchReadPath.NEW);
      assertThat(SearchReadPathFlags.forSurface(SearchReadPathSurface.VIOLATIONS))
          .isEqualTo(SearchReadPath.NEW);
    }
    finally {
      System.clearProperty("nexusOne.search.readPath.applications");
      System.clearProperty("nexusOne.search.readPath.violations");
    }
  }
}

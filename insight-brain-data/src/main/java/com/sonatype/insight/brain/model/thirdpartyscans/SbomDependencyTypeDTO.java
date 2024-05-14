/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

public class SbomDependencyTypeDTO
{
  private long direct;

  private long transitive;

  private long unspecified;

  public SbomDependencyTypeDTO() {
    // for Jackson
  }

  public SbomDependencyTypeDTO(Object[] array) {
    direct = Long.parseLong(String.valueOf(array[0]));
    transitive = Long.parseLong(String.valueOf(array[1]));
    unspecified = Long.parseLong(String.valueOf(array[2]));
  }

  public long getDirect() {
    return direct;
  }

  public long getTransitive() {
    return transitive;
  }

  public long getUnspecified() {
    return unspecified;
  }

  public void setDirect(final long direct) {
    this.direct = direct;
  }

  public void setTransitive(final long transitive) {
    this.transitive = transitive;
  }

  public void setUnspecified(final long unspecified) {
    this.unspecified = unspecified;
  }
}

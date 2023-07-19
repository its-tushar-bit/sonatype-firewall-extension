/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2;

class UnsupportedFeature
{
  private final String featureName;

  private final String replacementFeatureName;

  public UnsupportedFeature(final String featureName, final String replacementFeatureName) {
    this.featureName = featureName;
    this.replacementFeatureName = replacementFeatureName;
  }

  public String getFeatureName() {
    return this.featureName;
  }

  public String getReplacementFeatureName() {
    return this.replacementFeatureName;
  }

  public boolean hasReplacementFeatureName() {
    return this.replacementFeatureName != null;
  }
}

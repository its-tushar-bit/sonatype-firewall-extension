/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render.model;

public class SeverityInfo
{
  private final String refId;

  private final Float cvssScore;

  private final MDImages verificationImage;

  public SeverityInfo(final String refId, final Float cvssScore, final MDImages verificationImage) {
    this.refId = refId;
    this.cvssScore = cvssScore;
    this.verificationImage = verificationImage;
  }

  public String getRefId() {
    return refId;
  }

  public Float getCvssScore() {
    return cvssScore;
  }

  public MDImages getVerificationImage() {
    return verificationImage;
  }
}

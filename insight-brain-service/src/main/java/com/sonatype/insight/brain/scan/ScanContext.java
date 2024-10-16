/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

/**
 * This class is intended to allow us to pass any information along for a scan without having to add extra method
 * parameters everywhere. It is similar to {@link com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext} but is
 * intended to be usable for any scan.
 */
public record ScanContext(String applicationVersion)
{
  /**
   * This builder is intended to make constructing a {@link ScanContext} easier by not having to set all fields.
   */
  public static class Builder
  {
    private String applicationVersion;

    public Builder applicationVersion(final String applicationVersion) {
      this.applicationVersion = applicationVersion;
      return this;
    }

    public ScanContext build() {
      return new ScanContext(applicationVersion);
    }
  }
}

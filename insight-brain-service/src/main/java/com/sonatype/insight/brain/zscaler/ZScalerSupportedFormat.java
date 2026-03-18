/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

public enum ZScalerSupportedFormat
{
  MAVEN,
  NPM,
  PYPI,
  NUGET;

  public static ZScalerSupportedFormat fromString(String format) {
    for (ZScalerSupportedFormat zScalerFormat : ZScalerSupportedFormat.values()) {
      if (zScalerFormat.name().equalsIgnoreCase(format)) {
        return zScalerFormat;
      }
    }
    throw new IllegalArgumentException("Unknown format: " + format);
  }
}

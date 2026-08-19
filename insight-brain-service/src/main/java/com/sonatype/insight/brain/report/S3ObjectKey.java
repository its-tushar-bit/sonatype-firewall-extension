/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import static java.util.Objects.requireNonNull;

public record S3ObjectKey(String format, String appId, String scanId, String objectName, String keyPrefix)
{
  public S3ObjectKey {
    requireNonNull(format);
    requireNonNull(appId);
    requireNonNull(scanId);
    requireNonNull(objectName);
    requireNonNull(keyPrefix);
  }

  @Override
  public String toString() {
    return keyPrefix + String.format(format, appId, scanId, objectName);
  }
}

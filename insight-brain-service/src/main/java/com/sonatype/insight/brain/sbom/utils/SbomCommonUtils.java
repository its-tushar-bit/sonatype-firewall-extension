/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.util.UUID;

public class SbomCommonUtils
{
  public static String newFilteredScanFileName(String scanId) {
    if (scanId == null) {
      scanId = UUID.randomUUID().toString().replace("-", "");
    }
    return "scan-" + scanId + "-filtered.xml.gz";
  }
}

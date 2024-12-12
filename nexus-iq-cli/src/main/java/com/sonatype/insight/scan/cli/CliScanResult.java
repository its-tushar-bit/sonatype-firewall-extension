/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;

import com.sonatype.insight.scan.model.ClientScanResult;

public class CliScanResult
    extends ClientScanResult
{
  private final boolean hasScanningErrors;

  public CliScanResult(final File scanFile, final boolean hasThirdPartyScanContent, final boolean hasScanningErrors) {
    super(scanFile, hasThirdPartyScanContent);
    this.hasScanningErrors = hasScanningErrors;
  }

  public boolean hasScanningErrors() {
    return hasScanningErrors;
  }
}

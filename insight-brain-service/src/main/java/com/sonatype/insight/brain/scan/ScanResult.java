/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;

import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.scan.model.ClientScanResult;

public class ScanResult
    extends ClientScanResult
{
  private ScanEntity scanEntity;

  public ScanResult() {
    // noop
  }

  @Deprecated
  public ScanResult(final File scanFile, final boolean hasThirdPartyScanContent) {
    super(scanFile, hasThirdPartyScanContent);
  }

  public ScanResult(final ScanEntity scanEntity, final boolean hasThirdPartyScanContent) {
    setScanEntity(scanEntity);
    setHasThirdPartyScanContent(hasThirdPartyScanContent);
  }

  public ScanEntity getScanEntity() {
    return scanEntity;
  }

  public void setScanEntity(ScanEntity scanEntity) {
    this.scanEntity = scanEntity;
  }
}

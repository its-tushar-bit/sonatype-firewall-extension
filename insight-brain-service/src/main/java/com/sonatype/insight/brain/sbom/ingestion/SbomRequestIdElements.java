/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;

public class SbomRequestIdElements
{
  private SbomScanType scanType;

  private String filename;

  private SbomFormat sbomFormat;

  private ItemContentType contentType;

  public SbomRequestIdElements(String filename) {
    this.scanType = SbomScanType.BINARY;
    this.filename = filename;
  }

  public SbomRequestIdElements(final String filename, final SbomFormat sbomFormat, final ItemContentType contentType) {
    this.scanType = SbomScanType.SBOM;
    this.filename = filename;
    this.sbomFormat = sbomFormat;
    this.contentType = contentType;
  }

  public SbomScanType getScanType() {
    return scanType;
  }

  public void setScanType(final SbomScanType scanType) {
    this.scanType = scanType;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(final String filename) {
    this.filename = filename;
  }

  public SbomFormat getSbomFormat() {
    return sbomFormat;
  }

  public void setSbomFormat(final SbomFormat sbomFormat) {
    this.sbomFormat = sbomFormat;
  }

  public ItemContentType getContentType() {
    return contentType;
  }

  public void setContentType(final ItemContentType contentType) {
    this.contentType = contentType;
  }
}

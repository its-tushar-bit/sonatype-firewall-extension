/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "scanned_file_mapping")
public class ThirdPartyScannedFileMapping
    implements HasStringId
{
  @Id
  @Column(name = "scanned_file_mapping_id")
  private String id;

  @Column(name = "scanned_file_id")
  private String scannedFileId;

  @Column(name = "scan_id")
  private String scanId;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getScannedFileId() {
    return scannedFileId;
  }

  public void setScannedFileId(String scannedFileId) {
    this.scannedFileId = scannedFileId;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }
}

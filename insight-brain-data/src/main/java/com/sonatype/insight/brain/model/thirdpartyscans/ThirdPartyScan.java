/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "third_party_scan")
public class ThirdPartyScan
    implements HasStringId
{
  public ThirdPartyScan() {
  }

  public ThirdPartyScan(String thirdPartyFileId, String scanRequestId, Date createTime) {
    this.thirdPartyFileId = thirdPartyFileId;
    this.scanRequestId = scanRequestId;
    this.createTime = createTime;
  }

  @Id
  @Column(name = "third_party_scan_id")
  private String id;

  @Column(name = "third_party_file_id")
  private String thirdPartyFileId;

  @Column(name = "scan_request_id")
  private String scanRequestId;

  @Column(name = "scan_id")
  private String scanId;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "filtered_scan_file")
  private String filteredScanFile;

  @Column(name = "previous_scan_id")
  private String previousScanId;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getThirdPartyFileId() {
    return thirdPartyFileId;
  }

  public void setThirdPartyFileId(String thirdPartyFileId) {
    this.thirdPartyFileId = thirdPartyFileId;
  }

  public String getScanRequestId() {
    return scanRequestId;
  }

  public void setScanRequestId(String scanRequestId) {
    this.scanRequestId = scanRequestId;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(final Date createTime) {
    this.createTime = createTime;
  }

  public String getFilteredScanFile() {
    return filteredScanFile;
  }

  public void setFilteredScanFile(final String filteredScanFile) {
    this.filteredScanFile = filteredScanFile;
  }

  public String getPreviousScanId() {
    return previousScanId;
  }

  public void setPreviousScanId(String previousScanId) {
    this.previousScanId = previousScanId;
  }
}

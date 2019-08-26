/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "third_party_scan")
public class ThirdPartyScan
    implements HasStringId
{
  public ThirdPartyScan() {
  }

  public ThirdPartyScan(String thirdPartyFileId, String scanId, Date createTime) {
    this.thirdPartyFileId = thirdPartyFileId;
    this.scanId = scanId;
    this.createTime = createTime;
  }

  @Id
  @Column(name = "third_party_scan_id")
  private String id;

  @Column(name = "third_party_file_id")
  private String thirdPartyFileId;

  @Column(name = "scan_id")
  private String scanId;

  @Column(name = "create_time")
  private Date createTime;

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
}

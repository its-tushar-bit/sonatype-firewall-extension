/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/*
 * @since 1.183
 */
@Entity
@Table(name = "auto_policy_waiver_revocation")
public class AutoPolicyWaiverRevocation
    implements HasStringId
{
  @Id
  @Column(name = "auto_policy_waiver_revocation_id")
  private String id;
  
  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "creator_id")
  private String creatorId;

  @Column(name = "creator_name")
  private String creatorName;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "auto_policy_waiver_id")
  private String autoPolicyWaiverId;
  
  @Column(name = "hash")
  private String hash;

  @Column(name = "associated_package_url")
  private String associatedPackageUrl;
  
  @Column(name = "scan_id")
  private String scanId;
  
  public AutoPolicyWaiverRevocation() {
  }
  
  public AutoPolicyWaiverRevocation(
      String ownerId,
      String creatorId,
      String creatorName,
      Date createTime,
      String autoPolicyWaiverId,
      String hash,
      String associatedPackageUrl,
      String scanId
  )
  {
    this.ownerId = ownerId;
    this.creatorId = creatorId;
    this.creatorName = creatorName;
    this.createTime = createTime;
    this.autoPolicyWaiverId = autoPolicyWaiverId;
    this.hash = hash;
    this.associatedPackageUrl = associatedPackageUrl;
    this.scanId = scanId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String id) {
    this.ownerId = id;
  }

  public String getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(String creatorId) {
    this.creatorId = creatorId;
  }

  public String getCreatorName() {
    return creatorName;
  }

  public void setCreatorName(String creatorName) {
    this.creatorName = creatorName;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }
  
  public void setAutoPolicyWaiverId(String autoPolicyWaiverId) {
    this.autoPolicyWaiverId = autoPolicyWaiverId;
  }
  
  public String getAutoPolicyWaiverId() {
    return autoPolicyWaiverId;
  }
  
  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getAssociatedPackageUrl() {
    return associatedPackageUrl;
  }

  public void setAssociatedPackageUrl(String associatedPackageUrl) {
    this.associatedPackageUrl = associatedPackageUrl;
  }
  
  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }
}
  

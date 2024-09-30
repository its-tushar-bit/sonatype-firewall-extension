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
 * @since 1.177
 */
@Entity
@Table(name = "auto_policy_waiver")
public class AutoPolicyWaiver
    implements HasStringId
{
  @Id
  @Column(name = "auto_policy_waiver_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "threat_level")
  private int threatLevel;

  @Column(name = "reachable")
  private Boolean reachable;

  @Column(name = "path_forward")
  private Boolean pathForward;

  @Column(name = "creator_id")
  private String creatorId;

  @Column(name = "creator_name")
  private String creatorName;

  @Column(name = "create_time")
  private Date createTime;

  public AutoPolicyWaiver() {
  }

  public AutoPolicyWaiver(
      String ownerId,
      int threatLevel,
      boolean reachable,
      boolean pathForward,
      String creatorId,
      String creatorName,
      Date createTime
  )
  {
    this.ownerId = ownerId;
    this.threatLevel = threatLevel;
    this.reachable = reachable;
    this.pathForward = pathForward;
    this.creatorId = creatorId;
    this.creatorName = creatorName;
    this.createTime = createTime;
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

  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(int threatLevel) {
    this.threatLevel = threatLevel;
  }

  public Boolean isReachable() {
    return reachable;
  }

  public void setReachable(Boolean reachable) {
    this.reachable = reachable;
  }

  public Boolean hasPathForward() {
    return pathForward;
  }

  public void setPathForward(Boolean pathForward) {
    this.pathForward = pathForward;
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
}

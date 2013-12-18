/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.7.1
 */
@Entity
@Table(name = "policy_monitoring")
public class PolicyMonitoring
    implements HasStringId
{
  @Id
  @Column(name = "policy_monitoring_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "stage_type_id")
  private String stageTypeId;

  public PolicyMonitoring() {
  }

  public PolicyMonitoring(String ownerId, String stageTypeId) {
    this.ownerId = ownerId;
    setStageTypeId(stageTypeId);
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

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getStageTypeId() {
    return stageTypeId;
  }

  public void setStageTypeId(String stageTypeId) {
    if (!Stage.isValidStageTypeId(stageTypeId)) {
      throw new InvalidStageException("Invalid stage id=" + stageTypeId);
    }
    this.stageTypeId = stageTypeId;
  }
}

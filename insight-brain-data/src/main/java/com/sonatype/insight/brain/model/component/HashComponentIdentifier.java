/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Association of a component hash to a component identifier.
 *
 * @since 1.4.1
 */
@Entity
@Table(name = "hash_component_identifier")
public class HashComponentIdentifier
    extends HasComponentId
    implements HasStringId
{
  @Id
  @Column(name = "hash_component_identifier_id")
  private String id;

  @Column(name = "hash")
  private String hash;

  @Column(name = "comment")
  private String comment;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "claimer_id")
  private String claimerId;

  @Column(name = "claimer_name")
  private String claimerName;

  public HashComponentIdentifier() {
  }

  public HashComponentIdentifier(String hash, ComponentIdentifier componentIdentifier) {
    setHash(hash);
    setComponentIdentifier(componentIdentifier);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = HashHelper.truncateHash(hash);
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public String getClaimerId() {
    return claimerId;
  }

  public void setClaimerId(String claimerId) {
    this.claimerId = claimerId;
  }

  public String getClaimerName() {
    return claimerName;
  }

  public void setClaimerName(String claimerName) {
    this.claimerName = claimerName;
  }

  @JsonIgnore
  public Long getCreateTimeLong() {
    return (createTime != null) ? createTime.getTime() : null;
  }
}

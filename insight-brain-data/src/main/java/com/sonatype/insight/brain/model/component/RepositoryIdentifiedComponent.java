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
import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "repository_identified_component")
public class RepositoryIdentifiedComponent
    extends HasComponentId
    implements HasStringId
{
  @Id
  @Column(name = "hash")
  private String hash;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "last_access_time")
  private Date lastAccessTime;

  public RepositoryIdentifiedComponent() {
  }

  public RepositoryIdentifiedComponent(
      String hash,
      ComponentIdentifier componentIdentifier,
      Date createTime,
      Date lastAccessTime)
  {
    this.hash = hash;
    setComponentIdentifier(componentIdentifier);
    this.createTime = createTime;
    this.lastAccessTime = lastAccessTime;
  }

  @Override
  public String getId() {
    return getHash();
  }

  @Override
  public void setId(String id) {
    setHash(id);
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getLastAccessTime() {
    return lastAccessTime;
  }

  public void setLastAccessTime(Date lastAccessTime) {
    this.lastAccessTime = lastAccessTime;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "perpetual_lock")
public class PerpetualLock
    implements HasStringId
{
  @Id
  @Column(name = "perpetual_lock_id")
  private String id;

  @Column(name = "owner")
  private String owner;

  @Column(name = "expiration_time")
  private Date expirationTime;

  public PerpetualLock() {
  }

  public PerpetualLock(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public Date getExpirationTime() {
    return expirationTime;
  }

  public PerpetualLock setExpirationTime(Date expirationTime) {
    this.expirationTime = expirationTime;
    return this;
  }

  public String getOwner() {
    return owner;
  }

  public PerpetualLock setOwner(String owner) {
    this.owner = owner;
    return this;
  }
}

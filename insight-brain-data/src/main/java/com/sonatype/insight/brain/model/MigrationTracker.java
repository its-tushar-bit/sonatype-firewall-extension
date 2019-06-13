/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Since 1.67
 */
@Entity
@Table(name = "migration_tracker")
public class MigrationTracker implements HasStringId
{
  @Id
  @Column(name = "migration_tracker_id")
  private String id;

  @Column(name = "version")
  private Integer version;

  @Column(name = "configuration")
  private String configuration;

  public MigrationTracker() {
  }

  public MigrationTracker(String id) {
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

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getConfiguration() {
    return configuration;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }
}

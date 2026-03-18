/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Since 1.67
 */
@Entity
@Table(name = "migration_tracker")
public class MigrationTracker
    implements HasStringId
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

  @Override
  public String toString() {
    return "MigrationTracker [id=" + id + ", version=" + version + ", configuration=" + configuration + "]";
  }
}

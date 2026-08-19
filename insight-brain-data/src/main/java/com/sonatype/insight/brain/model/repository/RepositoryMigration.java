/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "repository_migration")
public class RepositoryMigration
    implements HasStringId
{
  @Id
  @Column(name = "repository_migration_id")
  private String id;

  @Column(name = "repository_id")
  private String repositoryId;

  @Column(name = "state")
  private MigrationState state;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public MigrationState getState() {
    return state;
  }

  public void setState(MigrationState state) {
    this.state = state;
  }
}

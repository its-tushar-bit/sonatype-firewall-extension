/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "repository")
public class Repository
    implements HasStringId, Owner
{
  @Id
  @Column(name = "repository_id")
  private String id;

  @Column(name = "repository_manager_id")
  private String repositoryManagerId;

  @Column(name = "public_id")
  private String publicId;

  @Column(name = "enabled")
  private boolean enabled = true;

  @Column(name = "quarantine_enabled")
  private boolean quarantineEnabled = false;

  public Repository() {
  }

  public Repository(String repositoryManagerId, String publicId) {
    this.repositoryManagerId = repositoryManagerId;
    this.publicId = publicId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getRepositoryManagerId() {
    return repositoryManagerId;
  }

  public void setRepositoryManagerId(String repositoryManagerId) {
    this.repositoryManagerId = repositoryManagerId;
  }

  @Override
  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isQuarantineEnabled() {
    return quarantineEnabled;
  }

  public void setQuarantineEnabled(final boolean quarantineEnabled) {
    this.quarantineEnabled = quarantineEnabled;
  }

  @Override
  @JsonIgnore
  public String getName() {
    return getPublicId();
  }

  @Override
  @JsonIgnore
  public String getParentOwnerId() {
    return RepositoryContainer.REPOSITORY_CONTAINER_ID;
  }

  @Override
  @JsonIgnore
  public boolean canHaveChildren() {
    return false;
  }

  @Override
  @JsonIgnore
  public OwnerType getType() {
    return OwnerType.REPOSITORY;
  }
}

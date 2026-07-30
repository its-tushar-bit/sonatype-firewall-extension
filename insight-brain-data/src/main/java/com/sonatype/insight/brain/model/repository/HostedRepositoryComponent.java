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

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

/**
 * One row per artifact in a hosted repository; is-a {@link Owner} and owns scan-based evaluation rows
 * in {@code policy_evaluation} / {@code policy_violation} / {@code owner_component} /
 * {@code last_policy_evaluation} via {@code owner_id}.
 */
@Entity
@Table(name = "hosted_repository_component")
public class HostedRepositoryComponent
    implements Owner, RepositoryComponent
{
  @Id
  @Column(name = "hosted_repository_component_id", nullable = false)
  private String id;

  @Column(name = "repository_id", nullable = false)
  private String repositoryId;

  @Column(name = "pathname", nullable = false)
  private String pathname;

  @Column(name = "hash", nullable = false)
  private String hash;

  @Column(name = "component_id")
  private String componentId;

  @Column(name = "owner_component_id")
  private String ownerComponentId;

  public HostedRepositoryComponent() {
  }

  public HostedRepositoryComponent(String repositoryId, String pathname, String hash) {
    this.repositoryId = repositoryId;
    this.pathname = pathname;
    this.hash = hash;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return pathname;
  }

  @Override
  public String getPublicId() {
    return id;
  }

  @Override
  public String getParentOwnerId() {
    return repositoryId;
  }

  @Override
  public boolean canHaveChildren() {
    return false;
  }

  @Override
  public OwnerType getType() {
    return OwnerType.HOSTED_REPOSITORY_COMPONENT;
  }

  @Override
  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  @Override
  public String getPathname() {
    return pathname;
  }

  public void setPathname(String pathname) {
    this.pathname = pathname;
  }

  @Override
  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  @Override
  public String getComponentId() {
    return componentId;
  }

  public void setComponentId(String componentId) {
    this.componentId = componentId;
  }

  public String getOwnerComponentId() {
    return ownerComponentId;
  }

  public void setOwnerComponentId(String ownerComponentId) {
    this.ownerComponentId = ownerComponentId;
  }

  @Override
  public String toString() {
    return "HostedRepositoryComponent [id=" + id + ", repositoryId=" + repositoryId
        + ", pathname=" + pathname + ", hash=" + hash + ", componentId=" + componentId
        + ", ownerComponentId=" + ownerComponentId + "]";
  }
}

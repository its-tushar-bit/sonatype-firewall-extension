/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.continuousmonitoring;

import java.util.StringJoiner;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Per-flow satellite for the Hosted Repository continuous monitoring flow. Holds the
 * flow-specific identity columns ({@code repository_id}, {@code component_hash}) for a row in
 * the shared {@code continuous_monitoring_queue}. The natural unique key
 * {@code (repository_id, component_hash)} is what producer-side dedup relies on.
 * <p>
 * The primary key is {@code queue_id} — also a foreign key to the parent table's id, with
 * {@code ON DELETE CASCADE}. {@link HasStringId#getId()}/{@link HasStringId#setId(String)} are
 * implemented as aliases for {@code queueId} so this entity can be carried by the standard
 * jOOQ DAO base class.
 */
@Entity
@Table(name = "continuous_monitoring_hosted_repo_item")
public class ContinuousMonitoringHostedRepoItem
    implements HasStringId
{
  @Id
  @Column(name = "queue_id")
  private String queueId;

  @Column(name = "repository_id", nullable = false)
  private String repositoryId;

  @Column(name = "component_hash", nullable = false)
  private String componentHash;

  public ContinuousMonitoringHostedRepoItem() {
  }

  public ContinuousMonitoringHostedRepoItem(
      final String queueId,
      final String repositoryId,
      final String componentHash)
  {
    this.queueId = queueId;
    this.repositoryId = repositoryId;
    this.componentHash = componentHash;
  }

  public String getQueueId() {
    return queueId;
  }

  public void setQueueId(final String queueId) {
    this.queueId = queueId;
  }

  /** Alias of {@link #getQueueId()} required by {@link HasStringId}. */
  @Override
  public String getId() {
    return queueId;
  }

  /** Alias of {@link #setQueueId(String)} required by {@link HasStringId}. */
  @Override
  public void setId(final String id) {
    this.queueId = id;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(final String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getComponentHash() {
    return componentHash;
  }

  public void setComponentHash(final String componentHash) {
    this.componentHash = componentHash;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContinuousMonitoringHostedRepoItem that = (ContinuousMonitoringHostedRepoItem) o;
    return queueId != null ? queueId.equals(that.queueId) : that.queueId == null;
  }

  @Override
  public int hashCode() {
    return queueId != null ? queueId.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ContinuousMonitoringHostedRepoItem.class.getSimpleName() + "[", "]")
        .add("queueId='" + queueId + "'")
        .add("repositoryId='" + repositoryId + "'")
        .add("componentHash='" + componentHash + "'")
        .toString();
  }
}

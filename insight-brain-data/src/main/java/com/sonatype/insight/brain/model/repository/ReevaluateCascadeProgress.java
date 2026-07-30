/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing the progress of a cascade re-evaluation for a specific repository component.
 * Maps to the `reevaluate_cascade_progress` database table.
 *
 * @since 1.196
 */
@Entity
@Table(name = "reevaluate_cascade_progress")
public class ReevaluateCascadeProgress
    implements HasStringId
{
  @Id
  @Column(name = "reevaluate_cascade_progress_id")
  private String id;

  @Column(name = "reevaluate_cascade_request_id", nullable = false)
  private String reevaluateCascadeRequestId;

  @Column(name = "repository_id", nullable = false)
  private String repositoryId;

  @Column(name = "proxy_repository_component_id", nullable = false)
  private String proxyRepositoryComponentId;

  @Column(name = "quarantined")
  private Boolean quarantined;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private ReevaluateCascadeProgressStatus status = ReevaluateCascadeProgressStatus.PENDING;

  public ReevaluateCascadeProgress() {
    // Default constructor for JPA
  }

  public ReevaluateCascadeProgress(
      String reevaluateCascadeRequestId,
      String repositoryId,
      String proxyRepositoryComponentId,
      ReevaluateCascadeProgressStatus status)
  {
    this.reevaluateCascadeRequestId = reevaluateCascadeRequestId;
    this.repositoryId = repositoryId;
    this.proxyRepositoryComponentId = proxyRepositoryComponentId;
    this.status = status;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getReevaluateCascadeRequestId() {
    return reevaluateCascadeRequestId;
  }

  public void setReevaluateCascadeRequestId(String reevaluateCascadeRequestId) {
    this.reevaluateCascadeRequestId = reevaluateCascadeRequestId;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getProxyRepositoryComponentId() {
    return proxyRepositoryComponentId;
  }

  public void setProxyRepositoryComponentId(String proxyRepositoryComponentId) {
    this.proxyRepositoryComponentId = proxyRepositoryComponentId;
  }

  public Boolean isQuarantined() {
    return quarantined;
  }

  public void setQuarantined(Boolean quarantined) {
    this.quarantined = quarantined;
  }

  public ReevaluateCascadeProgressStatus getStatus() {
    return status;
  }

  public void setStatus(ReevaluateCascadeProgressStatus status) {
    this.status = status;
  }

  /**
   * Marks this progress entry as completed successfully.
   */
  public void markCompleted() {
    this.status = ReevaluateCascadeProgressStatus.COMPLETED;
  }

  /**
   * Marks this progress entry as failed.
   */
  public void markFailed() {
    this.status = ReevaluateCascadeProgressStatus.FAILED;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Entity representing a cascade re-evaluation request for components across all accessible repositories.
 * Maps to the `reevaluate_cascade_request` database table.
 *
 * @since 1.196
 */
@Entity
@Table(name = "reevaluate_cascade_request")
public class ReevaluateCascadeRequest
    implements HasStringId
{
  @Id
  @Column(name = "reevaluate_cascade_request_id")
  private String id;

  @Column(name = "component_reference_hash", nullable = false)
  private String componentReferenceHash;

  @Column(name = "created_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdAt;

  @Column(name = "created_by_username", nullable = false)
  private String createdByUsername;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private ReevaluateCascadeRequestStatus status = ReevaluateCascadeRequestStatus.PENDING;

  public ReevaluateCascadeRequest() {
    // Default constructor for JPA
  }

  public ReevaluateCascadeRequest(
      String componentReferenceHash,
      String createdByUsername,
      ReevaluateCascadeRequestStatus status)
  {
    this.componentReferenceHash = componentReferenceHash;
    this.createdByUsername = createdByUsername;
    this.createdAt = new Date();
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

  public String getComponentReferenceHash() {
    return componentReferenceHash;
  }

  public void setComponentReferenceHash(String componentReferenceHash) {
    this.componentReferenceHash = componentReferenceHash;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public String getCreatedByUsername() {
    return createdByUsername;
  }

  public void setCreatedByUsername(String createdByUsername) {
    this.createdByUsername = createdByUsername;
  }

  public ReevaluateCascadeRequestStatus getStatus() {
    return status;
  }

  public void setStatus(ReevaluateCascadeRequestStatus status) {
    this.status = status;
  }

  /**
   * Marks this request as in progress (background task started).
   */
  public void markInProgress() {
    this.status = ReevaluateCascadeRequestStatus.IN_PROGRESS;
  }

  /**
   * Marks this request as completed successfully.
   */
  public void markCompleted() {
    this.status = ReevaluateCascadeRequestStatus.COMPLETED;
  }

  /**
   * Marks this request as having no components found.
   */
  public void markNoComponentsFound() {
    this.status = ReevaluateCascadeRequestStatus.NO_COMPONENTS_FOUND;
  }

  /**
   * Marks this request as failed due to processing error.
   */
  public void markFailed() {
    this.status = ReevaluateCascadeRequestStatus.FAILED;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "version_evaluation_window")
public class VersionEvaluationWindow
    implements HasStringId
{
  @Id
  @Column(name = "version_evaluation_window_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "context_id")
  private String contextId;

  @Column(name = "max_versions")
  private Integer maxVersions;

  @Column(name = "max_age_in_days")
  private Integer maxAgeInDays;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }

  public String getContextId() {
    return contextId;
  }

  public void setContextId(final String contextId) {
    this.contextId = contextId;
  }

  public Integer getMaxVersions() {
    return maxVersions;
  }

  public void setMaxVersions(final Integer maxVersions) {
    this.maxVersions = maxVersions;
  }

  public Integer getMaxAgeInDays() {
    return maxAgeInDays;
  }

  public void setMaxAgeInDays(final Integer maxAgeInDays) {
    this.maxAgeInDays = maxAgeInDays;
  }
}

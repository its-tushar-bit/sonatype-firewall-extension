/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "quarantined_component_access")
public class QuarantinedComponentAccess
    implements HasStringId
{
  @Id
  @Column(name = "quarantined_component_access_id")
  private String id;

  @Column(name = "repository_id")
  private String repositoryId;

  @Column(name = "proxy_repository_component_id")
  private String proxyRepositoryComponentId;

  @Column(name = "generate_time")
  private Date generateTime;

  public QuarantinedComponentAccess() {
  }

  public QuarantinedComponentAccess(
      final String repositoryId,
      final String proxyRepositoryComponentId,
      final Date generateTime)
  {
    this.repositoryId = repositoryId;
    this.generateTime = generateTime;
    this.proxyRepositoryComponentId = proxyRepositoryComponentId;
  }

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

  public void setRepositoryId(final String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getProxyRepositoryComponentId() {
    return proxyRepositoryComponentId;
  }

  public void setProxyRepositoryComponentId(final String proxyRepositoryComponentId) {
    this.proxyRepositoryComponentId = proxyRepositoryComponentId;
  }

  public Date getGenerateTime() {
    return generateTime;
  }

  public void setGenerateTime(final Date generateTime) {
    this.generateTime = generateTime;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "tenant_metadata")
public class TenantMetadata
    implements HasStringId
{
  @Id
  @Column(name = "tenant_metadata_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "application_name")
  private String applicationName;

  @Column(name = "connection_id")
  private String connectionId;

  @Column(name = "connection_name")
  private String connectionName;

  public TenantMetadata() {
  }

  public TenantMetadata(
      final String applicationId,
      final String applicationName,
      final String connectionId,
      final String connectionName)
  {
    this.applicationId = applicationId;
    this.applicationName = applicationName;
    this.connectionId = connectionId;
    this.connectionName = connectionName;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(final String connectionId) {
    this.connectionId = connectionId;
  }

  public String getConnectionName() {
    return connectionName;
  }

  public void setConnectionName(final String connectionName) {
    this.connectionName = connectionName;
  }
}

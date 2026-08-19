/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  @Column(name = "encryption_key_name")
  private String encryptionKeyName;

  @Column(name = "organization_id")
  private String organizationId;

  @Column(name = "organization_name")
  private String organizationName;

  public TenantMetadata() {
  }

  public TenantMetadata(
      final String applicationId,
      final String applicationName,
      final String connectionId,
      final String connectionName,
      final String encryptionKeyName,
      final String organizationId,
      final String organizationName)
  {
    this.applicationId = applicationId;
    this.applicationName = applicationName;
    this.connectionId = connectionId;
    this.connectionName = connectionName;
    this.encryptionKeyName = encryptionKeyName;
    this.organizationId = organizationId;
    this.organizationName = organizationName;
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

  public String getEncryptionKeyName() {
    return encryptionKeyName;
  }

  public void setEncryptionKeyName(final String encryptionKeyName) {
    this.encryptionKeyName = encryptionKeyName;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(final String organizationId) {
    this.organizationId = organizationId;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(final String organizationName) {
    this.organizationName = organizationName;
  }
}

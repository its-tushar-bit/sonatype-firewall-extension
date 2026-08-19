/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.dto;

import com.sonatype.insight.brain.model.security.TenantMetadata;

public class TenantMetadataDTO
{
  private String applicationId;

  private String applicationName;

  private String connectionId;

  private String connectionName;

  private String encryptionKeyName;

  private String organizationId;

  private String organizationName;

  public TenantMetadataDTO() {
    // no-op
  }

  public TenantMetadataDTO(
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

  public static TenantMetadataDTO toDTO(TenantMetadata tenantMetadata) {
    return new TenantMetadataDTO(tenantMetadata.getApplicationId(), tenantMetadata.getApplicationName(),
        tenantMetadata.getConnectionId(), tenantMetadata.getConnectionName(), tenantMetadata.getEncryptionKeyName(),
        tenantMetadata.getOrganizationId(), tenantMetadata.getOrganizationName());
  }

  public static TenantMetadata fromDTO(TenantMetadataDTO tenantMetadataDTO) {
    return new TenantMetadata(tenantMetadataDTO.getApplicationId(), tenantMetadataDTO.getApplicationName(),
        tenantMetadataDTO.getConnectionId(), tenantMetadataDTO.getConnectionName(),
        tenantMetadataDTO.getEncryptionKeyName(), tenantMetadataDTO.getOrganizationId(),
        tenantMetadataDTO.getOrganizationName());
  }
}

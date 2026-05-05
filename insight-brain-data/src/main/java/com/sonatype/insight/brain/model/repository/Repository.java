/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Cacheable
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

  @Column(name = "repository_type")
  @Enumerated(EnumType.STRING)
  private RepositoryType repositoryType = RepositoryType.proxy;

  @Column(name = "audit_enabled")
  private boolean auditEnabled = true;

  @Column(name = "quarantine_enabled")
  private boolean quarantineEnabled = false;

  @Column(name = "policy_compliant_component_selection_enabled")
  private boolean policyCompliantComponentSelectionEnabled = false;

  @Column(name = "namespace_confusion_protection_enabled")
  private boolean namespaceConfusionProtectionEnabled = false;

  @Column(name = "format")
  private String format;

  @Column(name = "last_manual_configure_time")
  private Date lastManualConfigureTime;

  @Column(name = "related_organization_id")
  private String relatedOrganizationId;

  @Column(name = "monitoring_enabled")
  private boolean monitoringEnabled = false;

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

  public boolean isAuditEnabled() {
    return auditEnabled;
  }

  public void setAuditEnabled(boolean auditEnabled) {
    this.auditEnabled = auditEnabled;
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
    return repositoryManagerId;
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

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public boolean isPolicyCompliantComponentSelectionEnabled() {
    return policyCompliantComponentSelectionEnabled;
  }

  public void setPolicyCompliantComponentSelectionEnabled(boolean policyCompliantComponentSelectionEnabled) {
    this.policyCompliantComponentSelectionEnabled = policyCompliantComponentSelectionEnabled;
  }

  public boolean isNamespaceConfusionProtectionEnabled() {
    return namespaceConfusionProtectionEnabled;
  }

  public void setNamespaceConfusionProtectionEnabled(boolean namespaceConfusionProtectionEnabled) {
    this.namespaceConfusionProtectionEnabled = namespaceConfusionProtectionEnabled;
  }

  public RepositoryType getRepositoryType() {
    return repositoryType;
  }

  public void setRepositoryType(RepositoryType repositoryType) {
    this.repositoryType = repositoryType;
  }

  public Date getLastManualConfigureTime() {
    return lastManualConfigureTime;
  }

  public void setLastManualConfigureTime(final Date lastManualConfigureTime) {
    this.lastManualConfigureTime = lastManualConfigureTime;
  }

  public String getRelatedOrganizationId() {
    return relatedOrganizationId;
  }

  public void setRelatedOrganizationId(final String relatedOrganizationId) {
    this.relatedOrganizationId = relatedOrganizationId;
  }

  public boolean isMonitoringEnabled() {
    return monitoringEnabled;
  }

  public void setMonitoringEnabled(final boolean monitoringEnabled) {
    this.monitoringEnabled = monitoringEnabled;
  }

  @Override
  public String toString() {
    return "Repository [id=" + id + ", repositoryManagerId=" + repositoryManagerId + ", publicId=" + publicId
        + ", repositoryType=" + repositoryType + ", format=" + format + ", auditEnabled=" + auditEnabled
        + ", quarantineEnabled=" + quarantineEnabled + ", policyCompliantComponentSelectionEnabled="
        + policyCompliantComponentSelectionEnabled + ", namespaceConfusionProtectionEnabled="
        + namespaceConfusionProtectionEnabled + ", monitoringEnabled=" + monitoringEnabled + "]";
  }
}

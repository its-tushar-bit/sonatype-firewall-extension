/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository.onboarding;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRepositoryType;
import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "firewall_onboarding_repository")
public class FirewallOnboardingRepository
    implements HasStringId
{
  @Id
  @Column(name = "firewall_onboarding_repository_id")
  private String id;

  @Column(name = "firewall_onboarding_repository_manager_id")
  private String repositoryManagerId;

  @Column(name = "name")
  private String name;

  @Column(name = "format")
  private String format;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private FirewallOnboardingRepositoryType type;

  @Column(name = "audit_enabled")
  private boolean auditEnabled = false;

  @Column(name = "quarantine_enabled")
  private boolean quarantineEnabled = false;

  @Column(name = "namespace_confusion_protection_enabled")
  private boolean namespaceConfusionProtectionEnabled = false;

  public FirewallOnboardingRepository() {
  }

  public FirewallOnboardingRepository(
      String repositoryManagerId,
      String name,
      String format,
      FirewallOnboardingRepositoryType type)
  {
    this.repositoryManagerId = repositoryManagerId;
    this.name = name;
    this.format = format;
    this.type = type;
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

  public void setQuarantineEnabled(final boolean quarantineEnabled) {
    this.quarantineEnabled = quarantineEnabled;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FirewallOnboardingRepositoryType getType() {
    return type;
  }

  public void setType(FirewallOnboardingRepositoryType type) {
    this.type = type;
  }

  public boolean isAuditEnabled() {
    return auditEnabled;
  }

  public void setAuditEnabled(boolean auditEnabled) {
    this.auditEnabled = auditEnabled;
  }

  public boolean isNamespaceConfusionProtectionEnabled() {
    return namespaceConfusionProtectionEnabled;
  }

  public void setNamespaceConfusionProtectionEnabled(boolean namespaceConfusionProtectionEnabled) {
    this.namespaceConfusionProtectionEnabled = namespaceConfusionProtectionEnabled;
  }

  public boolean isQuarantineEnabled() {
    return quarantineEnabled;
  }
}

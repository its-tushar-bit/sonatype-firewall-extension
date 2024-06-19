/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.prioritization;

import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "development_prioritization_component_info")
public class DevelopmentPrioritizationComponentInfo
    implements HasStringId
{
  @Id
  @Column(name = "development_prioritization_component_info_id")
  private String id;

  @Column(name = "scan_id")
  private String scanId;

  @Column(name = "development_prioritization_id")
  private String developmentPrioritizationId;

  @Column(name = "component_hash")
  private String componentHash;

  @Column(name = "remediation_type")
  @Enumerated(EnumType.STRING)
  private ApiVersionChangeOptionType remediationType;

  @Column(name = "remediation_version")
  private String remediationVersion;

  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "updated_at")
  private Date updatedAt;

  public DevelopmentPrioritizationComponentInfo() {
  }

  public DevelopmentPrioritizationComponentInfo(
      final String developmentPrioritizationId,
      final String scanId,
      final String componentHash,
      final ApiVersionChangeOptionType remediationType,
      final String remediationVersion)
  {
    this.developmentPrioritizationId = developmentPrioritizationId;
    this.scanId = scanId;
    this.componentHash = componentHash;
    this.remediationType = remediationType;
    this.remediationVersion = remediationVersion;
    this.updatedAt = this.createdAt = new Date();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(final String scanId) {
    this.scanId = scanId;
  }

  public String getComponentHash() {
    return componentHash;
  }

  public void setComponentHash(final String componentHash) {
    this.componentHash = componentHash;
  }

  public ApiVersionChangeOptionType getRemediationType() {
    return remediationType;
  }

  public void setRemediationType(final ApiVersionChangeOptionType remediationType) {
    this.remediationType = remediationType;
  }

  public String getRemediationVersion() {
    return remediationVersion;
  }

  public void setRemediationVersion(final String remediationVersion) {
    this.remediationVersion = remediationVersion;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(final Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getDevelopmentPrioritizationId() {
    return developmentPrioritizationId;
  }

  public void setDevelopmentPrioritizationId(final String developmentPrioritizationId) {
    this.developmentPrioritizationId = developmentPrioritizationId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DevelopmentPrioritizationComponentInfo that = (DevelopmentPrioritizationComponentInfo) o;
    return Objects.equals(id, that.id) && Objects.equals(scanId, that.scanId) &&
        Objects.equals(developmentPrioritizationId, that.developmentPrioritizationId) &&
        Objects.equals(componentHash, that.componentHash) && remediationType == that.remediationType &&
        Objects.equals(remediationVersion, that.remediationVersion) &&
        Objects.equals(createdAt, that.createdAt) && Objects.equals(updatedAt, that.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, scanId, developmentPrioritizationId, componentHash, remediationType, remediationVersion,
        createdAt, updatedAt);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DevelopmentPrioritizationComponentInfo.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("scanId='" + scanId + "'")
        .add("developmentPrioritizationId='" + developmentPrioritizationId + "'")
        .add("componentHash='" + componentHash + "'")
        .add("remediationType=" + remediationType)
        .add("remediationVersion='" + remediationVersion + "'")
        .add("createdAt=" + createdAt)
        .add("updatedAt=" + updatedAt)
        .toString();
  }
}

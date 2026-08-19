/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Cacheable
@Entity
@Table(name = "organization")
public class Organization
    extends Nameable
    implements HasStringId, Owner
{
  public static final String ROOT_ORGANIZATION_ID = "ROOT_ORGANIZATION_ID";

  @Id
  @Column(name = "organization_id")
  private String id;

  @Column(name = "parent_organization_id")
  private String parentOrganizationId;

  /**
   * @since 1.168
   */
  @Column(name = "legacy_violation_enabled")
  private Boolean legacyViolationEnabled;

  /**
   * @since 1.50
   */
  @Column(name = "allow_legacy_violation_override")
  private boolean allowLegacyViolationOverride = true;

  /**
   * @since 1.132
   */
  @Column(name = "repository_connection_enabled")
  private Boolean repositoryConnectionEnabled;

  /**
   * @since 1.132
   */
  @Column(name = "allow_repository_connection_override")
  private boolean allowRepositoryConnectionOverride = true;

  /**
   * @since 1.137
   */
  @Column(name = "artifactory_connection_enabled")
  private Boolean artifactoryConnectionEnabled;

  /**
   * @since 1.137
   */
  @Column(name = "allow_artifactory_connection_override")
  private boolean allowArtifactoryConnectionOverride = true;

  @Column(name = "related_repository_id")
  private String relatedRepositoryId;

  @Column(name = "related_repository_manager_id")
  private String relatedRepositoryManagerId;

  @Column(name = "related_repository_container_id")
  private String relatedRepositoryContainerId;

  public Organization() {
  }

  public Organization(String name) {
    setName(name);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @JsonIgnore
  @Override
  public String getPublicId() {
    // For organization the id is also the publicId
    return id;
  }

  public String getParentOrganizationId() {
    return parentOrganizationId;
  }

  public void setParentOrganizationId(final String parentOrganizationId) {
    this.parentOrganizationId = parentOrganizationId;
  }

  @Override
  @JsonIgnore
  public boolean canHaveChildren() {
    return true;
  }

  @Override
  @JsonIgnore
  public OwnerType getType() {
    return OwnerType.ORGANIZATION;
  }

  @Override
  @JsonIgnore
  public String getParentOwnerId() {
    return parentOrganizationId;
  }

  public Boolean isLegacyViolationEnabled() {
    return legacyViolationEnabled;
  }

  public void setLegacyViolationEnabled(Boolean legacyViolationEnabled) {
    this.legacyViolationEnabled = legacyViolationEnabled;
  }

  public boolean isAllowLegacyViolationOverride() {
    return allowLegacyViolationOverride;
  }

  public void setAllowLegacyViolationOverride(boolean allowLegacyViolationOverride) {
    this.allowLegacyViolationOverride = allowLegacyViolationOverride;
  }

  public Boolean isRepositoryConnectionEnabled() {
    return repositoryConnectionEnabled;
  }

  public void setRepositoryConnectionEnabled(Boolean repositoryConnectionEnabled) {
    this.repositoryConnectionEnabled = repositoryConnectionEnabled;
  }

  public boolean isAllowRepositoryConnectionOverride() {
    return allowRepositoryConnectionOverride;
  }

  public void setAllowRepositoryConnectionOverride(boolean allowRepositoryConnectionOverride) {
    this.allowRepositoryConnectionOverride = allowRepositoryConnectionOverride;
  }

  public Boolean isArtifactoryConnectionEnabled() {
    return artifactoryConnectionEnabled;
  }

  public void setArtifactoryConnectionEnabled(Boolean artifactoryConnectionEnabled) {
    this.artifactoryConnectionEnabled = artifactoryConnectionEnabled;
  }

  public boolean isAllowArtifactoryConnectionOverride() {
    return allowArtifactoryConnectionOverride;
  }

  public void setAllowArtifactoryConnectionOverride(boolean allowArtifactoryConnectionOverride) {
    this.allowArtifactoryConnectionOverride = allowArtifactoryConnectionOverride;
  }

  public String getRelatedRepositoryId() {
    return relatedRepositoryId;
  }

  public void setRelatedRepositoryId(final String relatedRepositoryId) {
    this.relatedRepositoryId = relatedRepositoryId;
  }

  public String getRelatedRepositoryManagerId() {
    return relatedRepositoryManagerId;
  }

  public void setRelatedRepositoryManagerId(final String relatedRepositoryManagerId) {
    this.relatedRepositoryManagerId = relatedRepositoryManagerId;
  }

  public String getRelatedRepositoryContainerId() {
    return relatedRepositoryContainerId;
  }

  public void setRelatedRepositoryContainerId(String relatedRepositoryContainerId) {
    this.relatedRepositoryContainerId = relatedRepositoryContainerId;
  }

  @Override
  public String toString() {
    return "Organization [id=" + id + ", name=" + name + "]";
  }
}

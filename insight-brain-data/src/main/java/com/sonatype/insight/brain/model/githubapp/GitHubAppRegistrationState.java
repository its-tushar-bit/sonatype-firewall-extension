/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.githubapp;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.sonatype.insight.model.HasStringId;

/**
 * State token for GitHub App registration/manifest flow.
 * Stores temporary state data during the manifest registration process.
 * Separate from installation flow for security isolation.
 */
@Entity
@Table(name = "github_app_registration_state")
public class GitHubAppRegistrationState implements HasStringId
{
  @Id
  @Column(name = "github_app_registration_state_id")
  private String id;

  @Column(name = "state_token")
  private String stateToken;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "github_organization_name")
  private String githubOrganizationName;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "expires_at")
  private Date expiresAt;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_at")
  private Date createdAt;

  public GitHubAppRegistrationState() {
    // Default constructor for JPA
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getStateToken() {
    return stateToken;
  }

  public void setStateToken(String stateToken) {
    this.stateToken = stateToken;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getGithubOrganizationName() {
    return githubOrganizationName;
  }

  public void setGithubOrganizationName(String githubOrganizationName) {
    this.githubOrganizationName = githubOrganizationName;
  }

  public Date getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Date expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public boolean isExpired() {
    return new Date().after(expiresAt);
  }
}

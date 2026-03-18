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
 * State token for GitHub App installation flow with OAuth + PKCE.
 * Stores temporary state data during the OAuth authorization process.
 * Separate from registration flow for security isolation.
 */
@Entity
@Table(name = "github_app_installation_state")
public class GitHubAppInstallationState
    implements HasStringId
{
  @Id
  @Column(name = "github_app_installation_state_id")
  private String id;

  @Column(name = "state_token")
  private String stateToken;

  @Column(name = "github_app_id")
  private String githubAppId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "expires_at")
  private Date expiresAt;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_at")
  private Date createdAt;

  public GitHubAppInstallationState() {
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

  public String getGithubAppId() {
    return githubAppId;
  }

  public void setGithubAppId(String githubAppId) {
    this.githubAppId = githubAppId;
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

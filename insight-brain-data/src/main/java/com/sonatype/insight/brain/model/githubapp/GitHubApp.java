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

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.201
 */
@Entity
@Table(name = "github_app")
public class GitHubApp
    implements HasStringId
{
  @Id
  @Column(name = "github_app_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "github_organization_name")
  private String githubOrganizationName;

  @Column(name = "app_id")
  private Integer appId;

  @Column(name = "slug")
  private String slug;

  @Column(name = "client_id")
  private String clientId;

  @RotatableSecret
  @Column(name = "client_secret")
  private String clientSecret;

  @RotatableSecret
  @Column(name = "private_key")
  private String privateKey;

  @Column(name = "installation_id")
  private Long installationId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_updated_at")
  private Date lastUpdatedAt;

  public GitHubApp() {
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

  public Integer getAppId() {
    return appId;
  }

  public void setAppId(Integer appId) {
    this.appId = appId;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public Long getInstallationId() {
    return installationId;
  }

  public void setInstallationId(Long installationId) {
    this.installationId = installationId;
  }

  public Date getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }
}

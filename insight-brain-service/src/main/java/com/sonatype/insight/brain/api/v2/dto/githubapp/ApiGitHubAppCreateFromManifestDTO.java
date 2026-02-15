/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.githubapp;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for GitHub App creation response from manifest conversion.
 *
 * This represents the response from GitHub when exchanging a manifest code for app credentials.
 *
 * @see <a href="https://docs.github.com/en/rest/apps/apps#create-a-github-app-from-a-manifest">GitHub API - Create from Manifest</a>
 * @since 1.195
 */
public class ApiGitHubAppCreateFromManifestDTO
{
  private final Integer id;

  private final String slug;

  private final String clientId;

  private final String clientSecret;

  private final String pem;

  private final Date createdAt;

  private final Date updatedAt;

  @JsonCreator
  public ApiGitHubAppCreateFromManifestDTO(
      @JsonProperty("id") final Integer id,
      @JsonProperty("slug") final String slug,
      @JsonProperty("client_id") final String clientId,
      @JsonProperty("client_secret") final String clientSecret,
      @JsonProperty("pem") final String pem,
      @JsonProperty("created_at") final Date createdAt,
      @JsonProperty("updated_at") final Date updatedAt)
  {
    this.id = id;
    this.slug = slug;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.pem = pem;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Integer getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  @JsonProperty("client_id")
  public String getClientId() {
    return clientId;
  }

  @JsonProperty("client_secret")
  public String getClientSecret() {
    return clientSecret;
  }

  public String getPem() {
    return pem;
  }

  @JsonProperty("created_at")
  public Date getCreatedAt() {
    return createdAt;
  }

  @JsonProperty("updated_at")
  public Date getUpdatedAt() {
    return updatedAt;
  }
}

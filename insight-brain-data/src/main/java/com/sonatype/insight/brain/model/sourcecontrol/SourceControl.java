/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.base.Objects;

/**
 * @since 1.66
 */
@Entity
@Table(name = "source_control")
public class SourceControl
    implements HasStringId
{
  public static final String FAKE_SECRET_KEY = "#~FAKE~SECRET~KEY~#";

  public static final boolean ENABLE_PULL_REQUESTS_BY_DEFAULT = true;

  public static final boolean ENABLE_STATUS_CHECKS_BY_DEFAULT = true;

  @Id
  @Column(name = "source_control_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "repository_url")
  private String repositoryUrl;

  @Column(name = "token")
  private String token;

  @Column(name = "provider")
  @Enumerated(EnumType.STRING)
  private SourceControlProvider provider;

  @Column(name = "base_branch")
  private String baseBranch;

  @Column(name = "enable_pull_requests")
  private Boolean enablePullRequests;

  @Column(name = "enable_status_checks")
  private Boolean enableStatusChecks;

  public SourceControl() {
  }

  public SourceControl(final String ownerId,
                       final String repositoryUrl,
                       final String token,
                       final SourceControlProvider provider,
                       final Boolean enablePullRequests,
                       final Boolean enableStatusChecks,
                       final String baseBranch)
  {
    this.ownerId = ownerId;
    this.repositoryUrl = repositoryUrl;
    this.token = token;
    this.provider = provider;
    this.enablePullRequests = enablePullRequests;
    this.enableStatusChecks = enableStatusChecks;
    this.baseBranch = baseBranch;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public void setRepositoryUrl(final String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
  }

  public String getToken() {
    return token;
  }

  public void setToken(final String token) {
    this.token = token;
  }

  public SourceControlProvider getProvider() {
    return provider;
  }

  public void setProvider(final SourceControlProvider provider) {
    this.provider = provider;
  }

  public String getBaseBranch() {
    return baseBranch;
  }

  public void setBaseBranch(final String baseBranch) {
    this.baseBranch = baseBranch;
  }

  public Boolean getEnablePullRequests() {
    return enablePullRequests;
  }

  public void setEnablePullRequests(final Boolean enablePullRequests) {
    this.enablePullRequests = enablePullRequests;
  }

  public Boolean getEnableStatusChecks() {
    return enableStatusChecks;
  }

  public void setEnableStatusChecks(final Boolean enableStatusChecks) {
    this.enableStatusChecks = enableStatusChecks;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SourceControl that = (SourceControl) o;
    return Objects.equal(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  public static class Builder
  {
    private String ownerId;

    private String repositoryUrl;

    private String token;

    private SourceControlProvider provider;

    private Boolean enablePullRequests;

    private Boolean enableStatusChecks;

    private String baseBranch;

    public Builder setOwnerId(final String ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public Builder setRepositoryUrl(final String repositoryUrl) {
      this.repositoryUrl = repositoryUrl;
      return this;
    }

    public Builder setToken(final String token) {
      this.token = token;
      return this;
    }

    public Builder setProvider(final SourceControlProvider provider) {
      this.provider = provider;
      return this;
    }

    public Builder setEnablePullRequests(final Boolean enablePullRequests) {
      this.enablePullRequests = enablePullRequests;
      return this;
    }

    public Builder setEnableStatusChecks(final Boolean enableStatusChecks) {
      this.enableStatusChecks = enableStatusChecks;
      return this;
    }

    public Builder setBaseBranch(final String baseBranch) {
      this.baseBranch = baseBranch;
      return this;
    }

    public SourceControl build() {
      return new SourceControl(ownerId, repositoryUrl, token, provider, enablePullRequests, enableStatusChecks,
          baseBranch);
    }
  }
}

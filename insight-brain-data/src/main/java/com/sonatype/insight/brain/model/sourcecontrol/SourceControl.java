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

/**
 * @since 1.66
 */
@Entity
@Table(name = "source_control")
public class SourceControl
    implements HasStringId
{
  public static final String FAKE_SECRET_KEY = "#~FAKE~SECRET~KEY~#";

  @Id
  @Column(name = "source_control_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "repository_url")
  private String repositoryUrl;

  @Column(name = "token")
  private String token;

  @Column(name = "provider")
  @Enumerated(EnumType.STRING)
  private SourceControlProvider provider;

  public SourceControl() {
  }

  public SourceControl(final String applicationId,
                       final String repositoryUrl,
                       final String token,
                       final SourceControlProvider provider)
  {
    this.applicationId = applicationId;
    this.repositoryUrl = repositoryUrl;
    this.token = token;
    this.provider = provider;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
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
}

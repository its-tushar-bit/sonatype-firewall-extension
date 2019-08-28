/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;
import com.sonatype.nexus.scm.SourceControlProvider;

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

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "repository_url")
  private String repositoryUrl;

  @Column(name = "token")
  private String token;

  @Column(name = "provider")
  private SourceControlProvider provider;

  public SourceControl() {
  }

  public SourceControl(final String ownerId,
                       final String repositoryUrl,
                       final String token,
                       final SourceControlProvider provider)
  {
    this.ownerId = ownerId;
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
}

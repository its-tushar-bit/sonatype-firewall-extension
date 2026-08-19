/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "repository_connection")
public class RepositoryConnection
    implements HasStringId
{
  @Id
  @Column(name = "repository_connection_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "base_url")
  private String baseUrl;

  @Column(name = "format")
  @Enumerated(EnumType.STRING)
  private RepositoryFormat format;

  @Column(name = "username")
  private String username;

  @RotatableSecret
  @Column(name = "password")
  private char[] password;

  public RepositoryConnection() {
  }

  public RepositoryConnection(
      String ownerId,
      String baseUrl,
      RepositoryFormat format,
      String username,
      char[] password)
  {
    this.ownerId = ownerId;
    this.baseUrl = baseUrl;
    this.format = format;
    this.username = username;
    this.password = password;
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

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public RepositoryFormat getFormat() {
    return format;
  }

  public void setFormat(RepositoryFormat format) {
    this.format = format;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public char[] getPassword() {
    return password;
  }

  public void setPassword(char[] password) {
    this.password = password;
  }
}

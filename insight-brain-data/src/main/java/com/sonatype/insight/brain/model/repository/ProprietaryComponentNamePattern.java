/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "proprietary_component_name_pattern")
public class ProprietaryComponentNamePattern
    implements HasStringId
{
  @Id
  @Column(name = "proprietary_component_name_pattern_id")
  private String id;

  @Column(name = "format")
  private String format;

  @Column(name = "namespace_pattern")
  private String namespacePattern = "";

  @Column(name = "name_pattern")
  private String namePattern = "";

  @Column(name = "repository_id")
  private String repositoryId;

  @Column(name = "enabled")
  private boolean enabled = true;

  public ProprietaryComponentNamePattern() {
  }

  public ProprietaryComponentNamePattern(String repositoryId, String format) {
    this.repositoryId = repositoryId;
    this.format = format;
  }

  public ProprietaryComponentNamePattern(
      String id,
      String format,
      String namespacePattern,
      String namePattern,
      String repositoryId,
      boolean enabled)
  {
    this.id = id;
    this.format = format;
    setNamespacePattern(namespacePattern);
    setNamePattern(namePattern);
    this.repositoryId = repositoryId;
    this.enabled = enabled;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getNamespacePattern() {
    return emptyToNull(namespacePattern);
  }

  public void setNamespacePattern(String namespacePattern) {
    this.namespacePattern = nullToEmpty(namespacePattern);
  }

  public ProprietaryComponentNamePattern withNamespacePattern(String namespacePattern) {
    setNamespacePattern(namespacePattern);
    return this;
  }

  public String getNamePattern() {
    return emptyToNull(namePattern);
  }

  public void setNamePattern(String namePattern) {
    this.namePattern = nullToEmpty(namePattern);
  }

  public ProprietaryComponentNamePattern withNamePattern(String namePattern) {
    setNamePattern(namePattern);
    return this;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String emptyToNull(String value) {
    return "".equals(value) ? null : value;
  }

  @Override
  public String toString() {
    return format + ':' + namespacePattern + '/' + namePattern;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }
}

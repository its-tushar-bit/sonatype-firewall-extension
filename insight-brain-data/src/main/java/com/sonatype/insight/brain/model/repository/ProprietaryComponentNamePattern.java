/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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

  @Column(name = "repository_manager_instance_id")
  private String repositoryManagerInstanceId;

  @Column(name = "repository_public_id")
  private String repositoryPublicId;

  public ProprietaryComponentNamePattern() {
  }

  public ProprietaryComponentNamePattern(String format) {
    setFormat(format);
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

  public String getRepositoryManagerInstanceId() {
    return repositoryManagerInstanceId;
  }

  public void setRepositoryManagerInstanceId(String repositoryManagerInstanceId) {
    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
  }

  public String getRepositoryPublicId() {
    return repositoryPublicId;
  }

  public void setRepositoryPublicId(String repositoryPublicId) {
    this.repositoryPublicId = repositoryPublicId;
  }

  public ProprietaryComponentNamePattern withRepository(String repositoryManagerInstanceId, String repositoryPublicId) {
    setRepositoryManagerInstanceId(repositoryManagerInstanceId);
    setRepositoryPublicId(repositoryPublicId);
    return this;
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
}

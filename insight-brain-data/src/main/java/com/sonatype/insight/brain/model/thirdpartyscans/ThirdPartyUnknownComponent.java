/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "third_party_unknown_component")
public class ThirdPartyUnknownComponent
    implements HasStringId
{
  public ThirdPartyUnknownComponent() {
    // noop
  }

  @Id
  @Column(name = "unknown_component_id")
  private String id;

  @Column(name = "filename")
  private String filename;

  @Column(name = "hash")
  private String hash;

  @Column(name = "dependency_type")
  private String dependencyType;

  @Column(name = "third_party_file_id")
  private String thirdPartyFileId;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String unknownComponentId) {
    this.id = unknownComponentId;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(final String filename) {
    this.filename = filename;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(final String hash) {
    this.hash = hash;
  }

  public String getDependencyType() {
    return dependencyType;
  }

  public void setDependencyType(final String dependencyType) {
    this.dependencyType = dependencyType;
  }

  public String getThirdPartyFileId() {
    return thirdPartyFileId;
  }

  public void setThirdPartyFileId(final String thirdPartyFileId) {
    this.thirdPartyFileId = thirdPartyFileId;
  }
}

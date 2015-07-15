/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.16
 */
@Entity
@Table(name = "schema_info")
public class SchemaInfo
    implements HasStringId
{
  @Id
  @Column(name = "schema_info_id")
  private String id;

  @Column(name = "drools_code_version")
  private int droolsCodeVersion;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public int getDroolsCodeVersion() {
    return droolsCodeVersion;
  }

  public void setDroolsCodeVersion(int droolsCodeVersion) {
    this.droolsCodeVersion = droolsCodeVersion;
  }
}

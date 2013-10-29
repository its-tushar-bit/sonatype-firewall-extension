/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.model.HasStringId;

/**
 * A role a user might have.
 * 
 * @since 1.7
 */
@Entity
@Table(name = "role")
public class Role
    implements HasStringId
{
  /**
   * The id of the Administrator role in the role database table.
   */
  public static final String ADMIN_ROLE_ID = "1b92fae3e55a411793a091fb821c422d";

  @Id
  @Column(name = "role_id")
  private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "name_lowercase_no_whitespace")
  private String nameLowercaseNoWhitespace;

  @Column(name = "description")
  private String description;

  @Column(name = "global")
  private boolean global;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    nameLowercaseNoWhitespace = NameHelper.normalize(name);
    this.name = name;
  }

  @SuppressWarnings("unused")
  private void setNameLowercaseNoWhitespace(String nameLowercaseNoWhitespace) {
    // prevent Jackson from messing this field up during deserialization
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isGlobal() {
    return global;
  }

  public void setGlobal(boolean global) {
    this.global = global;
  }

  @Override
  public String toString() {
    return getName();
  }
}

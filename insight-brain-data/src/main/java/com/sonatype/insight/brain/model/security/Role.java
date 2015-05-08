/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonIgnore;

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
   * The id of the "System Administrator" role in the role database table.
   */
  public static final String SYSTEM_ADMIN_ROLE_ID = "1b92fae3e55a411793a091fb821c422d";

  /**
   * The id of the "CLM Administrator rol"e in the role database table.
   */
  public static final String CLM_ADMIN_ROLE_ID = "b9646757e98e486da7d730025f5245f8";

  @Id
  @Column(name = "role_id")
  private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "sort_order")
  private int sortOrder;

  @JsonIgnore
  @Column(name = "name_lowercase_no_whitespace")
  private String nameLowercaseNoWhitespace;

  @Column(name = "description")
  private String description;

  @Column(name = "global")
  private boolean global;

  @Column(name = "built_in", insertable = false, updatable = false)
  private boolean builtIn;

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

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(final int sortOrder) {
    this.sortOrder = sortOrder;
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

  public boolean isBuiltIn() {
    return builtIn;
  }

  @Override
  public String toString() {
    return getName();
  }
}

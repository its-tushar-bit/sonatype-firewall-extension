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

import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A role a user might have.
 * 
 * @since 1.7
 */
@JsonIgnoreProperties({ "nameLowercaseNoWhitespace"})
@Entity
@Table(name = "role")
public class Role extends Nameable
    implements HasStringId
{
  /**
   * The id of the "System Administrator" role in the role database table.
   */
  public static final String SYSTEM_ADMIN_ROLE_ID = "1b92fae3e55a411793a091fb821c422d";

  /**
   * The id of the "Policy Administrator" role in the role database table.
   */
  public static final String POLICY_ADMIN_ROLE_ID = "b9646757e98e486da7d730025f5245f8";

  /**
   * The id of the "Owner" role in the role database table.
   */
  public static final String OWNER_ROLE_ID = "1cddabf7fdaa47d6833454af10e0a3ef";

  /**
   * The id of the "Developer" role in the role database table.
   */
  public static final String DEVELOPER_ROLE_ID = "1da70fae1fd54d6cb7999871ebdb9a36";

  /**
   * The id of the "Application Evaluator" role in the role database table.
   */
  public static final String APPLICATION_EVALUATOR_ROLE_ID = "2cb71b3468d649789163ea2e212b541e";

  /**
   * The id of the "Component Evaluator" role in the role database table.
   */
  public static final String COMPONENT_EVALUATOR_ROLE_ID = "90c7c98683b4471cb77a916744540bcc";

  /**
   * The id of the "Legal Reviewer" role in the role database table.
   */
  public static final String LEGAL_REVIEWER_ROLE_ID = "0df46317c031440795007f4ce9c7f002";

  /**
   * The id of the "SBOM Exporter" role in the role database table.
   */
  public static final String SBOM_EXPORTER_ROLE_ID = "e5626c1fe4304296b11d209f6cd2f158";

  /**
   * The id of the "SBOM Importer" role in the role database table.
   */
  public static final String SBOM_IMPORTER_ROLE_ID = "602c74a3286942b5a7d7e978e5e2bea8";

  @Id
  @Column(name = "role_id")
  private String id;

  @Column(name = "sort_order")
  private int sortOrder;

  @Column(name = "description")
  private String description;

  @Column(name = "global")
  private boolean global;

  @Column(name = "built_in", insertable = false, updatable = false)
  private boolean builtIn;

  public Role() {
  }

  public Role(String name, String description) {
    setName(name);
    setDescription(description);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(final int sortOrder) {
    this.sortOrder = sortOrder;
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

  public void setBuiltIn(boolean builtIn) {
    this.builtIn = builtIn;
  }

  @Override
  public String toString() {
    return getName();
  }
}

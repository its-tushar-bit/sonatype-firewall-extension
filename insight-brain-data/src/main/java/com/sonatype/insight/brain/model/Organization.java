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

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "organization")
public class Organization
    implements HasStringId, Owner
{
  public static final String ROOT_ORGANIZATION_ID = "ROOT_ORGANIZATION_ID";

  @Id
  @Column(name = "organization_id")
  private String id;

  @Column(name = "parent_organization_id")
  private String parentOrganizationId;

  @Column(name = "name")
  private String name;

  @Column(name = "name_lowercase_no_whitespace")
  private String nameLowercaseNoWhitespace;

  public Organization() {
  }

  public Organization(String name) {
    setName(name);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @JsonIgnore
  @Override
  public String getPublicId() {
    // For organization the id is also the publicId
    return id;
  }

  public String getParentOrganizationId() {
    return parentOrganizationId;
  }

  public void setParentOrganizationId(final String parentOrganizationId) {
    this.parentOrganizationId = parentOrganizationId;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    nameLowercaseNoWhitespace = NameHelper.normalize(name);
    this.name = name;
  }

  public String getNameLowercaseNoWhitespace() {
    return nameLowercaseNoWhitespace;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * nameLowercaseNoWhitespace field. If this method is not defined, jackson will set/access the
   * nameLowercaseNoWhitespace field directly via reflection, possibly setting it to an incorrect value.
   * 
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setNameLowercaseNoWhitespace(String nameLowercaseNoWhitespace) {
  }

  @Override
  @JsonIgnore
  public boolean canHaveChildren() {
    return true;
  }

  @Override
  @JsonIgnore
  public OwnerType getType() {
    return OwnerType.ORGANIZATION;
  }

  @Override
  @JsonIgnore
  public String getParentOwnerId() {
    return parentOrganizationId;
  }

  @Override
  public String toString() {
    return "Organization [id=" + id + ", name=" + name + "]";
  }
}

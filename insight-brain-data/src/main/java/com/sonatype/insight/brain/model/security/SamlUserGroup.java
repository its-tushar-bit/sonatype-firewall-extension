/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "saml_user_group")
public class SamlUserGroup
    implements HasStringId
{
  @Id
  @Column(name = "saml_user_group_id")
  private String id;

  @Column(name = "saml_user_id")
  private String samlUserId;

  @Column(name = "saml_group_id")
  private String samlGroupId;

  public SamlUserGroup() {
  }

  public SamlUserGroup(String samlUserId, String samlGroupId) {
    this.samlUserId = samlUserId;
    this.samlGroupId = samlGroupId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getSamlUserId() {
    return samlUserId;
  }

  public void setSamlUserId(String samlUserId) {
    this.samlUserId = samlUserId;
  }

  public String getSamlGroupId() {
    return samlGroupId;
  }

  public void setSamlGroupId(String samlGroupId) {
    this.samlGroupId = samlGroupId;
  }
}

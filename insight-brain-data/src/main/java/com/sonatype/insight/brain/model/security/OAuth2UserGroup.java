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
@Table(name = "oauth2_user_group")
public class OAuth2UserGroup
    implements HasStringId
{
  @Id
  @Column(name = "oauth2_user_group_id")
  private String id;

  @Column(name = "oauth2_user_id")
  private String oAuth2UserId;

  @Column(name = "oauth2_group_id")
  private String oAuth2GroupId;

  public OAuth2UserGroup() {
  }

  public OAuth2UserGroup(String oAuth2UserId, String oAuth2GroupId) {
    this.oAuth2UserId = oAuth2UserId;
    this.oAuth2GroupId = oAuth2GroupId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOAuth2UserId() {
    return oAuth2UserId;
  }

  public void setOAuth2UserId(String oAuth2UserId) {
    this.oAuth2UserId = oAuth2UserId;
  }

  public String getOAuth2GroupId() {
    return oAuth2GroupId;
  }

  public void setOAuth2GroupId(String oAuth2GroupId) {
    this.oAuth2GroupId = oAuth2GroupId;
  }
}

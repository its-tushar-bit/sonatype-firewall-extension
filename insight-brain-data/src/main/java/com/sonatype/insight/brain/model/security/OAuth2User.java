/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "oauth2_user")
public class OAuth2User
    extends AbstractSsoUser
    implements HasStringId
{
  public static final String OAUTH2_REALM_ID = "OAUTH2";

  @Id
  @Column(name = "oauth2_user_id")
  private String id;

  @Column(name = "groups_json")
  private String groupsJson;

  public OAuth2User() {
  }

  public OAuth2User(String username, String firstName, String lastName, String email, Set<String> groups) {
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    setGroups(groups);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getGroupsJson() {
    return groupsJson;
  }

  @Override
  protected void setGroupsJson(String groupsJson) {
    this.groupsJson = groupsJson;
  }

  @Override
  public Set<String> getGroups() {
    if (StringUtils.isEmpty(getGroupsJson())) {
      return Collections.emptySet();
    }
    try {
      return JsonUtils.parse(getGroupsJson(), new TypeReference<Set<String>>()
      {
      });
    }
    catch (IOException e) {
      return Collections.emptySet();
    }
  }

  @Override
  public String getRealmId() {
    return OAUTH2_REALM_ID;
  }
}

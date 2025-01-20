/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "oauth2_user")
public class OAuth2User
    implements HasStringId
{
  public static final String OAUTH2_REALM_ID = "OAUTH2";

  @Id
  @Column(name = "oauth2_user_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "email")
  private String email;

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

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getGroupsJson() {
    return groupsJson;
  }

  public Set<String> getGroups() {
    if (StringUtils.isEmpty(groupsJson)) {
      return Collections.emptySet();
    }
    try {
      return JsonUtils.parse(groupsJson, new TypeReference<Set<String>>() { });
    }
    catch (IOException e) {
      return Collections.emptySet();
    }
  }

  public void setGroups(Set<String> groups) {
    groupsJson = null;
    if (CollectionUtils.isNotEmpty(groups)) {
      groupsJson = JsonUtils.format(groups);
    }
  }

  public String calculateDisplayName() {
    String displayName = Optional.ofNullable(firstName).orElse("") + " " + Optional.ofNullable(lastName).orElse("");
    displayName = displayName.trim();
    if (displayName.isEmpty()) {
      displayName = username;
    }
    return displayName;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Optional;
import java.util.Set;

import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.apache.commons.collections4.CollectionUtils;

@MappedSuperclass
public abstract class AbstractSsoUser
    implements HasStringId
{
  @Column(name = "username")
  protected String username;

  @Column(name = "first_name")
  protected String firstName;

  @Column(name = "last_name")
  protected String lastName;

  @Column(name = "email")
  protected String email;

  protected AbstractSsoUser() {
  }

  protected AbstractSsoUser(String username, String firstName, String lastName, String email, Set<String> groups) {
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    setGroups(groups);
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

  public abstract Set<String> getGroups();

  public void setGroups(Set<String> groups) {
    if (CollectionUtils.isNotEmpty(groups)) {
      setGroupsJson(JsonUtils.format(groups));
    }
    else {
      setGroupsJson(null);
    }
  }

  public abstract String getGroupsJson();

  protected abstract void setGroupsJson(String groupsJson);

  public String calculateDisplayName() {
    String displayName = Optional.ofNullable(firstName).orElse("") + " " + Optional.ofNullable(lastName).orElse("");
    displayName = displayName.trim();
    if (displayName.isEmpty()) {
      displayName = username;
    }
    return displayName;
  }

  public abstract String getRealmId();
}

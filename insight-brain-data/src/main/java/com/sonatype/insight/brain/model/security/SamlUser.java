/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "saml_user")
public class SamlUser
    implements HasStringId
{
  public static final String SAML_REALM_ID = "SAML";

  private static final String GROUPS_DELIMITER = ",";

  @Id
  @Column(name = "saml_user_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "email")
  private String email;

  @Column(name = "groups")
  private String groupsString;

  public SamlUser() {
  }

  public SamlUser(String username, String firstName, String lastName, String email, Set<String> groups) {
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

  public String getGroupsString() {
    return groupsString;
  }

  @SuppressWarnings("unused")
  /* For JPA's use only */
  private void setGroupsString(String groupsString) {
    this.groupsString = groupsString;
  }

  public Set<String> getGroups() {
    if (StringUtils.isEmpty(groupsString)) {
      return Collections.emptySet();
    }
    return Sets.newLinkedHashSet(Arrays.asList(groupsString.split(GROUPS_DELIMITER)));
  }

  public void setGroups(Set<String> groups) {
    groupsString = null;
    if (CollectionUtils.isNotEmpty(groups)) {
      groupsString = Joiner.on(GROUPS_DELIMITER).join(groups);
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

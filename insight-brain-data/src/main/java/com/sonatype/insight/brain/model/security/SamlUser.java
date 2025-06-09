/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.sonatype.insight.model.HasStringId;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "saml_user")
public class SamlUser
    extends AbstractSsoUser
    implements HasStringId
{
  public static final String SAML_REALM_ID = "SAML";

  // Keeping for backwards compatibility: CLM-22868
  private static final String GROUPS_DELIMITER = ",";

  private static final Gson gson = new Gson();

  @Id
  @Column(name = "saml_user_id")
  private String id;

  @Column(name = "groups")
  private String groupsString;

  public SamlUser() {
  }

  public SamlUser(String username, String firstName, String lastName, String email, Set<String> groups) {
    super(username, firstName, lastName, email, groups);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getGroupsString() {
    return groupsString;
  }

  @SuppressWarnings("unused")
  /* For JPA's use only */
  private void setGroupsString(String groupsString) {
    this.groupsString = groupsString;
  }

  @Override
  public Set<String> getGroups() {
    if (StringUtils.isEmpty(groupsString)) {
      return Collections.emptySet();
    }
    try {
      return Sets.newLinkedHashSet(Arrays.asList(gson.fromJson(groupsString, String[].class)));
    }
    catch (JsonSyntaxException jsonSyntaxException) {
      // This happens if the user is using tokens and their groups were never updated due to not logging in
      // after implementation change.This user needs to refresh their groups by logging in via SAML.
      return Sets.newLinkedHashSet(Arrays.asList(groupsString.split(GROUPS_DELIMITER)));
    }
  }

  @Override
  public String getGroupsJson() {
    return getGroupsString();
  }

  @Override
  protected void setGroupsJson(String groupsJson) {
    setGroupsString(groupsJson);
  }

  @Override
  public String getRealmId() {
    return SAML_REALM_ID;
  }
}

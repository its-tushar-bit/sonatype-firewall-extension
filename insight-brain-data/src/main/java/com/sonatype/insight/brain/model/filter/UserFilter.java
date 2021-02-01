/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.105
 */
@Entity
@Table(name = "user_filter")
public class UserFilter
    implements HasStringId
{
  public static final String ACTIVE_FILTER_NAME = "";

  @Id
  @Column(name = "user_filter_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "username_lowercase")
  private String usernameLowercase;

  @Column(name = "realm_id")
  private String realmId;

  @Column(name = "name")
  private String name;

  @Column(name = "name_lowercase_no_whitespace")
  private String nameLowercaseNoWhitespace;

  @Column(name = "filter_json")
  private String filter;

  @Column(name = "based_on_filter_name")
  private String basedOnFilterName;

  @Column(name = "filter_type")
  @Enumerated(EnumType.STRING)
  private UserFilterType type;

  public UserFilter() {
  }

  public UserFilter(String username, String realmId, String name, UserFilterType type) {
    setUsername(username);
    setRealmId(realmId);
    setName(name);
    setType(type);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    usernameLowercase = User.normalizeUsername(username);
    this.username = username;
  }

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

  public String getFilter() {
    return filter;
  }

  public void setFilter(final String filter) {
    this.filter = filter;
  }

  public String getBasedOnFilterName() {
    return basedOnFilterName;
  }

  public void setBasedOnFilterName(final String basedOnFilterName) {
    this.basedOnFilterName = basedOnFilterName;
  }

  public String getUsernameLowercase() {
    return usernameLowercase;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * usernameLowercase field. If this method is not defined, jackson will set/access the
   * usernameLowercase field directly via reflection, possibly setting it to an incorrect value.
   * 
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setUsernameLowercase(String usernameLowercase) {
  }

  public String getRealmId() {
    return realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  public UserFilterType getType() {
    return type;
  }

  public void setType(UserFilterType type) {
    this.type = type;
  }
}

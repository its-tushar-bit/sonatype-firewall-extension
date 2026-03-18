/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.7
 */
@Entity
@Table(name = "ldap_usermapping")
public class LdapUserMapping
    implements HasStringId, HasLdapServerId
{
  /**
   * Internal id used to identify this LDAP configuration
   *
   * @since 1.7
   */
  @Id
  @Column(name = "ldap_usermapping_id")
  private String id;

  /**
   * LdapServer id
   *
   * @since 1.7
   */
  @Column(name = "ldap_server_id")
  private String serverId;

  // user mapping

  /**
   * @since 1.7
   */
  @Column(name = "user_basedn")
  private String userBaseDN;

  /**
   * @since 1.7
   */
  @Column(name = "user_subtree")
  private boolean userSubtree;

  /**
   * @since 1.7
   */
  @Column(name = "user_object_class")
  private String userObjectClass;

  /**
   * @since 1.7
   */
  @Column(name = "user_filter")
  private String userFilter;

  /**
   * @since 1.7
   */
  @Column(name = "user_id_attribute")
  private String userIDAttribute;

  /**
   * @since 1.7
   */
  @Column(name = "user_realname_attribute")
  private String userRealNameAttribute;

  /**
   * @since 1.7
   */
  @Column(name = "user_email_attribute")
  private String userEmailAttribute;

  /**
   * @since 1.7
   */
  @Column(name = "user_password_attribute")
  private String userPasswordAttribute;

  // group mapping

  /**
   * @since 1.7
   */
  @Column(name = "group_mapping_type")
  @Enumerated(EnumType.STRING)
  private LdapGroupMappingType groupMappingType = LdapGroupMappingType.NONE;

  // static group

  /**
   * @since 1.7
   */
  @Column(name = "group_basedn")
  private String groupBaseDN;

  /**
   * @since 1.7
   */
  @Column(name = "group_subtree")
  private boolean groupSubtree;

  /**
   * @since 1.7
   */
  @Column(name = "group_object_class")
  private String groupObjectClass;

  /**
   * @since 1.7
   */
  @Column(name = "group_id_attribute")
  private String groupIDAttribute;

  /**
   * @since 1.7
   */
  @Column(name = "group_member_attribute")
  private String groupMemberAttribute;

  /**
   * @since 1.7
   */
  @Column(name = "group_member_format")
  private String groupMemberFormat;

  // dynamic group

  /**
   * @since 1.7
   */
  @Column(name = "user_memberofgroup_attribute")
  private String userMemberOfGroupAttribute;

  /**
   * @since 1.11
   */
  @Column(name = "dynamic_group_search_enabled")
  private boolean dynamicGroupSearchEnabled = true;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getServerId() {
    return serverId;
  }

  @Override
  public void setServerId(String serverId) {
    this.serverId = serverId;
  }

  public String getUserBaseDN() {
    return userBaseDN;
  }

  public void setUserBaseDN(String userBaseDN) {
    this.userBaseDN = userBaseDN;
  }

  public boolean isUserSubtree() {
    return userSubtree;
  }

  public void setUserSubtree(boolean userSubtree) {
    this.userSubtree = userSubtree;
  }

  public String getUserObjectClass() {
    return userObjectClass;
  }

  public void setUserObjectClass(String userObjectClass) {
    this.userObjectClass = userObjectClass;
  }

  public String getUserFilter() {
    return userFilter;
  }

  public void setUserFilter(String userFilter) {
    this.userFilter = userFilter;
  }

  public String getUserIDAttribute() {
    return userIDAttribute;
  }

  public void setUserIDAttribute(String userIDAttribute) {
    this.userIDAttribute = userIDAttribute;
  }

  public String getUserRealNameAttribute() {
    return userRealNameAttribute;
  }

  public void setUserRealNameAttribute(String realNameAttribute) {
    this.userRealNameAttribute = realNameAttribute;
  }

  public String getUserEmailAttribute() {
    return userEmailAttribute;
  }

  public void setUserEmailAttribute(String emailAttribute) {
    this.userEmailAttribute = emailAttribute;
  }

  public String getUserPasswordAttribute() {
    return userPasswordAttribute;
  }

  public void setUserPasswordAttribute(String passwordAttribute) {
    this.userPasswordAttribute = passwordAttribute;
  }

  public LdapGroupMappingType getGroupMappingType() {
    return groupMappingType;
  }

  public void setGroupMappingType(LdapGroupMappingType groupMappingType) {
    this.groupMappingType = groupMappingType;
  }

  public String getGroupBaseDN() {
    return groupBaseDN;
  }

  public void setGroupBaseDN(String groupBaseDN) {
    this.groupBaseDN = groupBaseDN;
  }

  public boolean isGroupSubtree() {
    return groupSubtree;
  }

  public void setGroupSubtree(boolean groupSubtree) {
    this.groupSubtree = groupSubtree;
  }

  public String getGroupObjectClass() {
    return groupObjectClass;
  }

  public void setGroupObjectClass(String groupObjectClass) {
    this.groupObjectClass = groupObjectClass;
  }

  public String getGroupIDAttribute() {
    return groupIDAttribute;
  }

  public void setGroupIDAttribute(String groupIDAttribute) {
    this.groupIDAttribute = groupIDAttribute;
  }

  public String getGroupMemberAttribute() {
    return groupMemberAttribute;
  }

  public void setGroupMemberAttribute(String groupMemberAttribute) {
    this.groupMemberAttribute = groupMemberAttribute;
  }

  public String getGroupMemberFormat() {
    return groupMemberFormat;
  }

  public void setGroupMemberFormat(String groupMemberFormat) {
    this.groupMemberFormat = groupMemberFormat;
  }

  public String getUserMemberOfGroupAttribute() {
    return userMemberOfGroupAttribute;
  }

  public void setUserMemberOfGroupAttribute(String userMemberOfGroupAttribute) {
    this.userMemberOfGroupAttribute = userMemberOfGroupAttribute;
  }

  public boolean isDynamicGroupSearchEnabled() {
    return dynamicGroupSearchEnabled;
  }

  public void setDynamicGroupSearchEnabled(boolean dynamicGroupSearchEnabled) {
    this.dynamicGroupSearchEnabled = dynamicGroupSearchEnabled;
  }
}

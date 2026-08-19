/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * An association between a context (global/application/organization), a role and a member (user/group). Each such
 * association denotes that the member possesses the role when acting in the given context.
 *
 * @since 1.7
 */
@Entity
@Table(name = "membership_mapping")
public class MembershipMapping
    implements HasStringId
{
  public static final String GLOBAL_CONTEXT_ID = "global";

  public static final String GLOBAL_CONTEXT_NAME = "Global";

  @Id
  @Column(name = "membership_mapping_id")
  private String id;

  @Column(name = "context_id")
  private String contextId;

  @Column(name = "role_id")
  private String roleId;

  @Column(name = "member_name")
  private String memberName;

  @Column(name = "member_type")
  @Enumerated(EnumType.STRING)
  private MemberType memberType;

  public MembershipMapping() {
  }

  public MembershipMapping(String memberName, MemberType memberType) {
    this(null, null, memberName, memberType);
  }

  public MembershipMapping(String contextId, String roleId, String memberName, MemberType memberType) {
    this.contextId = contextId;
    this.roleId = roleId;
    this.memberName = memberName;
    this.memberType = memberType;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getContextId() {
    return contextId;
  }

  public void setContextId(String contextId) {
    this.contextId = contextId;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getMemberName() {
    return memberName;
  }

  public void setMemberName(String memberName) {
    this.memberName = memberName;
  }

  public MemberType getMemberType() {
    return memberType;
  }

  public void setMemberType(MemberType memberType) {
    this.memberType = memberType;
  }

  @Override
  public String toString() {
    return getContextId() + " - " + getRoleId() + " - " + getMemberName();
  }

  public boolean includes(UserPrincipal user) {
    return includes(user.getUsername(), user.getMembership());
  }

  public boolean includes(String username, Set<String> groups) {
    if (MemberType.USER.equals(getMemberType())) {
      return getMemberName().equalsIgnoreCase(username);
    }
    if (MemberType.GROUP.equals(getMemberType())) {
      return groups.contains(getMemberName());
    }
    return false;
  }
}

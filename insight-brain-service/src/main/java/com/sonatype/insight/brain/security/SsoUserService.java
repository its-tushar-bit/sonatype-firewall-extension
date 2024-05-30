/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.security.MemberType;

/**
 * This service orchestrate and manage the different SSO User implementations in the application.
 * <p>
 * It dynamically selects the SSO user implementation to use on the application based on the existing configuration. All
 * the business rules to select the proper SSO implementation should live here.
 */
@Named
@Singleton
public class SsoUserService
{
  final SamlSsoUserProvider samlSsoUserProvider;

  @Inject
  public SsoUserService(SamlSsoUserProvider samlSsoUserProvider) {
    this.samlSsoUserProvider = samlSsoUserProvider;
  }

  public boolean isSsoRealm(String realmId) {
    return samlSsoUserProvider.isSsoRealm(realmId);
  }

  public String normalizeRealmId(String realmId) {
    if (samlSsoUserProvider.isSsoRealm(realmId)) {
      return samlSsoUserProvider.getSsoRealm();
    }
    return realmId;
  }

  public boolean isSsoConfigured() {
    return samlSsoUserProvider.isSsoConfigured();
  }

  public void updateSsoUserAndGroups(SsoUser ssoUser, Set<String> newSamlGroupNames) {
    samlSsoUserProvider.updateSsoUserAndGroups(ssoUser, newSamlGroupNames);
  }

  public List<SsoUser> getSsoUsersByGroupName(String groupName) {
    return samlSsoUserProvider.getSsoUsersByGroupName(groupName);
  }

  public Set<String> filterExistingSsoGroupNames(Set<String> groupNames) {
    return samlSsoUserProvider.filterExistingSsoGroupNames(groupNames);
  }

  public List<Member> getSsoGroupMembers(Set<String> groupNames) {
    Set<String> existingSamlGroupNames = filterExistingSsoGroupNames(groupNames);
    return existingSamlGroupNames.stream()
        .map(group -> new Member(MemberType.GROUP, group, group, null, SamlRealm.ID))
        .collect(Collectors.toList());
  }

  public List<SsoUser> getSsoByUsernames(Set<String> usernames) {
    return samlSsoUserProvider.getSsoByUsernames(usernames);
  }

  public List<SsoUser> findSsoUsersByNameOrUsernameQuery(String nameQuery) {
    return samlSsoUserProvider.findSsoUsersByNameOrUsernameQuery(nameQuery);
  }

  public List<SsoGroup> findSsoGroupsByNameQuery(String nameQuery) {
    return samlSsoUserProvider.findSsoGroupsByNameQuery(nameQuery);
  }

  public List<Member> getSsoGroupMembersByNameQuery(String nameQuery) {
    return findSsoGroupsByNameQuery(nameQuery).stream()
        .map(group -> new Member(MemberType.GROUP, group.getName(), group.getName(), null, SamlRealm.ID))
        .collect(Collectors.toList());
  }

  public void deleteSsoUser(SsoUser ssoUser) {
    samlSsoUserProvider.deleteSsoUser(ssoUser);
  }

  public List<SsoUser> getAll() {
    return samlSsoUserProvider.getAll();
  }

  public SsoUser getByUsernameNotNull(final String username) {
    return samlSsoUserProvider.getByUsernameNotNull(username);
  }

  public SsoUser getByUsername(final String username) {
    return samlSsoUserProvider.getByUsername(username);
  }
}

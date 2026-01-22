/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2GroupDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserGroupDAO;
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.OAuth2UserGroup;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Sets;

@Named
public class OAuth2SsoUserProvider
    implements SsoUserProvider
{
  private final OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private final OAuth2UserDAO oAuth2UserDAO;

  private final OAuth2GroupDAO oAuth2GroupDAO;

  private final OAuth2UserGroupDAO oAuth2UserGroupDAO;

  @Inject
  public OAuth2SsoUserProvider(
      OAuth2ConfigurationDAO oAuth2ConfigurationDAO,
      OAuth2UserDAO oAuth2UserDAO,
      OAuth2GroupDAO oAuth2GroupDAO,
      OAuth2UserGroupDAO oAuth2UserGroupDAO)
  {
    this.oAuth2ConfigurationDAO = oAuth2ConfigurationDAO;
    this.oAuth2UserDAO = oAuth2UserDAO;
    this.oAuth2GroupDAO = oAuth2GroupDAO;
    this.oAuth2UserGroupDAO = oAuth2UserGroupDAO;
  }

  @Override
  public String getSsoRealm() {
    return OAuth2Realm.ID;
  }

  @Override
  public boolean isSsoRealm(String realmId) {
    return OAuth2Realm.ID.equalsIgnoreCase(realmId);
  }

  @Override
  public boolean isSsoConfigured() {
    return !oAuth2ConfigurationDAO.getAll().isEmpty();
  }

  @Override
  public void updateSsoUserAndGroups(SsoUser ssoUser, Set<String> newOAuth2GroupNames) {
    try (TransactionContext tx = oAuth2UserDAO.createTransactionContext()) {
      tx.begin();

      // Create/update user
      OAuth2User user = SsoUser.toOAuth2User(ssoUser);
      oAuth2UserDAO.upsertByUsername(tx, user);

      // Get user-group mappings if any
      List<OAuth2UserGroup> oAuth2UserGroups = oAuth2UserGroupDAO.getByOAuth2UserId(tx, user.getId());

      // Get groups if any
      List<OAuth2Group> oAuth2Groups =
          oAuth2GroupDAO.getByIds(tx,
              oAuth2UserGroups.stream().map(OAuth2UserGroup::getOAuth2GroupId).collect(Collectors.toSet()));
      Set<String> existingOAuth2GroupNames =
          oAuth2Groups.stream().map(OAuth2Group::getName).collect(Collectors.toSet());

      // Remove user-group mappings for groups the user no longer belongs to
      Set<String> oAuth2GroupNamesRemoved = Sets.difference(existingOAuth2GroupNames, newOAuth2GroupNames);
      Set<String> oAuth2GroupIdsRemoved = oAuth2Groups.stream()
          .filter(oAuth2Group -> oAuth2GroupNamesRemoved.contains(oAuth2Group.getName()))
          .map(OAuth2Group::getId)
          .collect(Collectors.toSet());
      oAuth2UserGroupDAO.deleteByOAuth2UserIdAndGroupIds(tx, user.getId(), oAuth2GroupIdsRemoved);

      // Remove groups if they no longer have any members
      Set<OAuth2Group> oAuth2GroupsRemoved = oAuth2Groups.stream()
          .filter(oAuth2Group -> oAuth2GroupIdsRemoved.contains(oAuth2Group.getId()))
          .collect(Collectors.toSet());
      for (OAuth2Group oAuth2Group : oAuth2GroupsRemoved) {
        if (oAuth2UserGroupDAO.getByOAuth2GroupId(tx, oAuth2Group.getId()).isEmpty()) {
          oAuth2GroupDAO.delete(tx, oAuth2Group);
        }
      }

      // Add new groups if needed
      Set<String> oAuth2GroupNamesAdded = Sets.difference(newOAuth2GroupNames, existingOAuth2GroupNames);
      Set<OAuth2Group> oAuth2GroupsAdded =
          oAuth2GroupNamesAdded.stream().map(OAuth2Group::new).collect(Collectors.toSet());
      oAuth2GroupsAdded.forEach(oAuth2Group -> oAuth2GroupDAO.upsertByName(tx, oAuth2Group));

      // Add new user-group mappings
      Set<OAuth2UserGroup> oAuth2UserGroupsAdded = oAuth2GroupsAdded.stream()
          .map(oAuth2GroupAdded -> new OAuth2UserGroup(user.getId(), oAuth2GroupAdded.getId()))
          .collect(Collectors.toSet());
      oAuth2UserGroupsAdded.forEach(
          oAuth2UserGroupAdded -> oAuth2UserGroupDAO.upsertByOAuth2UserIdAndOAuth2GroupId(tx, oAuth2UserGroupAdded));

      tx.commit();
    }
  }

  @Override
  public List<SsoUser> getSsoUsersByGroupName(String groupName) {
    try (TransactionContext tx = oAuth2UserDAO.createTransactionContext()) {
      OAuth2Group oAuth2Group = oAuth2GroupDAO.getByName(tx, groupName);
      if (oAuth2Group == null) {
        return Collections.emptyList();
      }
      List<OAuth2UserGroup> oAuth2UserGroups = oAuth2UserGroupDAO.getByOAuth2GroupId(tx, oAuth2Group.getId());

      List<OAuth2User> users = oAuth2UserDAO.getByIds(tx,
          oAuth2UserGroups.stream().map(OAuth2UserGroup::getOAuth2UserId)
              .collect(Collectors.toCollection(LinkedHashSet::new)));

      return users.stream().map(SsoUser::fromOAuth2User).collect(Collectors.toList());
    }
  }

  @Override
  public Set<String> filterExistingSsoGroupNames(Set<String> groupNames) {
    return oAuth2GroupDAO.getByNames(groupNames).stream().map(OAuth2Group::getName).collect(Collectors.toCollection(
        LinkedHashSet::new));
  }

  @Override
  public List<SsoUser> getSsoUsersByUsernames(Set<String> usernames) {
    List<OAuth2User> users = oAuth2UserDAO.getByUsernames(usernames);
    return users.stream().map(SsoUser::fromOAuth2User).collect(Collectors.toList());
  }

  @Override
  public List<SsoUser> getSsoUsersByEmails(Set<String> emails) {
    List<OAuth2User> users = oAuth2UserDAO.getByEmails(emails);
    return users.stream().map(SsoUser::fromOAuth2User).collect(Collectors.toList());
  }

  @Override
  // A realName is firstName + " " + lastName.
  public List<SsoUser> getSsoUsersByRealNames(Set<String> realNames) {
    List<OAuth2User> users = oAuth2UserDAO.getByRealNames(realNames);
    return users.stream().map(SsoUser::fromOAuth2User).collect(Collectors.toList());
  }

  @Override
  public List<SsoUser> findSsoUsersByNameOrUsernameQuery(String nameQuery) {
    List<OAuth2User> users = oAuth2UserDAO.findUsersByNameOrUsernameQuery(nameQuery);
    return users.stream().map(SsoUser::fromOAuth2User).collect(Collectors.toList());
  }

  @Override
  public List<SsoGroup> findSsoGroupsByNameQuery(String nameQuery) {
    List<OAuth2Group> groups = oAuth2GroupDAO.findGroupsByNameQuery(nameQuery);
    return groups.stream().map(SsoGroup::fromOAuth2Group).collect(Collectors.toList());
  }

  @Override
  public void deleteSsoUser(final SsoUser ssoUser) {
    oAuth2UserDAO.delete(SsoUser.toOAuth2User(ssoUser));
  }

  @Override
  public void upsertByUsername(final SsoUser ssoUser) {
    oAuth2UserDAO.upsertByUsername(SsoUser.toOAuth2User(ssoUser));
  }

  @Override
  public List<SsoUser> getAll() {
    List<OAuth2User> users = oAuth2UserDAO.getAll();
    return users.stream().map(SsoUser::fromOAuth2User).collect(Collectors.toList());
  }

  @Override
  public SsoUser getByUsername(final String username) {
    OAuth2User user = oAuth2UserDAO.getByUsername(username);
    return SsoUser.fromOAuth2User(user);
  }

  @Override
  public SsoUser getByUsernameNotNull(final String username) {
    OAuth2User user = oAuth2UserDAO.getByUsernameNotNull(username);
    return SsoUser.fromOAuth2User(user);
  }
}

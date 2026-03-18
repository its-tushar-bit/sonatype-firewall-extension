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

import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.security.SamlGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserGroupDAO;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Sets;

@Named
public class SamlSsoUserProvider
    implements SsoUserProvider
{
  private final SamlConfigurationService samlConfigurationService;

  private final SamlUserDAO samlUserDAO;

  private final SamlGroupDAO samlGroupDAO;

  private final SamlUserGroupDAO samlUserGroupDAO;

  @Inject
  public SamlSsoUserProvider(
      SamlConfigurationService samlConfigurationService,
      SamlUserDAO samlUserDAO,
      SamlGroupDAO samlGroupDAO,
      SamlUserGroupDAO samlUserGroupDAO)
  {
    this.samlConfigurationService = samlConfigurationService;
    this.samlUserDAO = samlUserDAO;
    this.samlGroupDAO = samlGroupDAO;
    this.samlUserGroupDAO = samlUserGroupDAO;
  }

  @Override
  public String getSsoRealm() {
    return SamlRealm.ID;
  }

  @Override
  public boolean isSsoRealm(String realmId) {
    return SamlRealm.ID.equalsIgnoreCase(realmId);
  }

  @Override
  public boolean isSsoConfigured() {
    return samlConfigurationService.get() != null;
  }

  @Override
  public void updateSsoUserAndGroups(SsoUser ssoUser, Set<String> newSamlGroupNames) {
    try (TransactionContext tx = samlUserDAO.createTransactionContext()) {
      tx.begin();

      // Create/update user
      SamlUser user = SsoUser.toSamlUser(ssoUser);
      samlUserDAO.upsertByUsername(tx, user);

      // Get user-group mappings if any
      List<SamlUserGroup> samlUserGroups = samlUserGroupDAO.getBySamlUserId(tx, user.getId());
      // Get groups if any
      List<SamlGroup> samlGroups =
          samlGroupDAO.getByIds(tx,
              samlUserGroups.stream().map(SamlUserGroup::getSamlGroupId).collect(Collectors.toSet()));
      Set<String> existingSamlGroupNames = samlGroups.stream().map(SamlGroup::getName).collect(Collectors.toSet());

      // Remove user-group mappings for groups the user no longer belongs to
      Set<String> samlGroupNamesRemoved = Sets.difference(existingSamlGroupNames, newSamlGroupNames);
      Set<String> samlGroupIdsRemoved = samlGroups.stream()
          .filter(samlGroup -> samlGroupNamesRemoved.contains(samlGroup.getName()))
          .map(SamlGroup::getId)
          .collect(Collectors.toSet());
      samlUserGroupDAO.deleteBySamlUserIdAndGroupIds(tx, user.getId(), samlGroupIdsRemoved);

      // Remove groups if they no longer have any members
      Set<SamlGroup> samlGroupsRemoved = samlGroups.stream()
          .filter(samlGroup -> samlGroupIdsRemoved.contains(samlGroup.getId()))
          .collect(Collectors.toSet());
      for (SamlGroup samlGroup : samlGroupsRemoved) {
        if (samlUserGroupDAO.getBySamlGroupId(tx, samlGroup.getId()).isEmpty()) {
          samlGroupDAO.delete(tx, samlGroup);
        }
      }

      // Add new groups if needed
      Set<String> samlGroupNamesAdded = Sets.difference(newSamlGroupNames, existingSamlGroupNames);
      Set<SamlGroup> samlGroupsAdded = samlGroupNamesAdded.stream().map(SamlGroup::new).collect(Collectors.toSet());
      samlGroupsAdded.forEach(samlGroup -> samlGroupDAO.upsertByName(tx, samlGroup));

      // Add new user-group mappings
      Set<SamlUserGroup> samlUserGroupsAdded = samlGroupsAdded.stream()
          .map(samlGroupAdded -> new SamlUserGroup(user.getId(), samlGroupAdded.getId()))
          .collect(Collectors.toSet());
      samlUserGroupsAdded.forEach(
          samlUserGroupAdded -> samlUserGroupDAO.upsertBySamlUserIdAndSamlGroupId(tx, samlUserGroupAdded));

      tx.commit();
    }
  }

  @Override
  public List<SsoUser> getSsoUsersByGroupName(String groupName) {
    try (TransactionContext tx = samlUserDAO.createTransactionContext()) {
      SamlGroup samlGroup = samlGroupDAO.getByName(tx, groupName);
      if (samlGroup == null) {
        return Collections.emptyList();
      }
      List<SamlUserGroup> samlUserGroups = samlUserGroupDAO.getBySamlGroupId(tx, samlGroup.getId());

      List<SamlUser> users = samlUserDAO.getByIds(tx,
          samlUserGroups.stream()
              .map(SamlUserGroup::getSamlUserId)
              .collect(Collectors.toCollection(LinkedHashSet::new)));

      return users.stream().map(SsoUser::fromSamlUser).collect(Collectors.toList());
    }
  }

  @Override
  public Set<String> filterExistingSsoGroupNames(Set<String> groupNames) {
    return samlGroupDAO.getByNames(groupNames)
        .stream()
        .map(SamlGroup::getName)
        .collect(Collectors.toCollection(
            LinkedHashSet::new));
  }

  @Override
  public List<SsoUser> getSsoUsersByUsernames(Set<String> usernames) {
    List<SamlUser> users = samlUserDAO.getByUsernames(usernames);
    return users.stream().map(SsoUser::fromSamlUser).collect(Collectors.toList());
  }

  @Override
  public List<SsoUser> getSsoUsersByEmails(Set<String> emails) {
    List<SamlUser> users = samlUserDAO.getByEmails(emails);
    return users.stream().map(SsoUser::fromSamlUser).collect(Collectors.toList());
  }

  @Override
  // A realName is firstName + " " + lastName.
  public List<SsoUser> getSsoUsersByRealNames(Set<String> realNames) {
    List<SamlUser> users = samlUserDAO.getByRealNames(realNames);
    return users.stream().map(SsoUser::fromSamlUser).collect(Collectors.toList());
  }

  @Override
  public List<SsoUser> findSsoUsersByNameOrUsernameQuery(String nameQuery) {
    List<SamlUser> users = samlUserDAO.findUsersByNameOrUsernameQuery(nameQuery);
    return users.stream().map(SsoUser::fromSamlUser).collect(Collectors.toList());
  }

  @Override
  public List<SsoGroup> findSsoGroupsByNameQuery(String nameQuery) {
    List<SamlGroup> groups = samlGroupDAO.findGroupsByNameQuery(nameQuery);
    return groups.stream().map(SsoGroup::fromSamlGroup).collect(Collectors.toList());
  }

  @Override
  public void deleteSsoUser(final SsoUser ssoUser) {
    samlUserDAO.delete(SsoUser.toSamlUser(ssoUser));
  }

  @Override
  public void upsertByUsername(final SsoUser ssoUser) {
    samlUserDAO.upsertByUsername(SsoUser.toSamlUser(ssoUser));
  }

  @Override
  public List<SsoUser> getAll() {
    List<SamlUser> users = samlUserDAO.getAll();
    return users.stream().map(SsoUser::fromSamlUser).collect(Collectors.toList());
  }

  @Override
  public SsoUser getByUsername(final String username) {
    SamlUser user = samlUserDAO.getByUsername(username);
    return SsoUser.fromSamlUser(user);
  }

  @Override
  public SsoUser getByUsernameNotNull(final String username) {
    SamlUser user = samlUserDAO.getByUsernameNotNull(username);
    return SsoUser.fromSamlUser(user);
  }
}

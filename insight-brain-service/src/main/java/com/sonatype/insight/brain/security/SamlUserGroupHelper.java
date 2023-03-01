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

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserGroupDAO;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Sets;

@Named
public class SamlUserGroupHelper
{
  private final SamlConfigurationDAO samlConfigurationDAO;

  private final SamlUserDAO samlUserDAO;

  private final SamlGroupDAO samlGroupDAO;

  private final SamlUserGroupDAO samlUserGroupDAO;

  @Inject
  public SamlUserGroupHelper(
      SamlConfigurationDAO samlConfigurationDAO,
      SamlUserDAO samlUserDAO,
      SamlGroupDAO samlGroupDAO,
      SamlUserGroupDAO samlUserGroupDAO)
  {
    this.samlConfigurationDAO = samlConfigurationDAO;
    this.samlUserDAO = samlUserDAO;
    this.samlGroupDAO = samlGroupDAO;
    this.samlUserGroupDAO = samlUserGroupDAO;
  }

  public boolean isSamlConfigured() {
    return samlConfigurationDAO.get() != null;
  }

  public void updateSamlUserAndGroups(SamlUser samlUser, Set<String> newSamlGroupNames) {
    try (TransactionContext tx = samlUserDAO.createTransactionContext()) {
      tx.begin();

      // Create/update user
      samlUserDAO.upsertByUsername(tx, samlUser);

      // Get user-group mappings if any
      List<SamlUserGroup> samlUserGroups = samlUserGroupDAO.getBySamlUserId(tx, samlUser.getId());
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
      samlUserGroupDAO.deleteBySamlUserIdAndGroupIds(tx, samlUser.getId(), samlGroupIdsRemoved);

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
          .map(samlGroupAdded -> new SamlUserGroup(samlUser.getId(), samlGroupAdded.getId()))
          .collect(Collectors.toSet());
      samlUserGroupsAdded.forEach(
          samlUserGroupAdded -> samlUserGroupDAO.upsertBySamlUserIdAndSamlGroupId(tx, samlUserGroupAdded));

      tx.commit();
    }
  }

  public List<SamlUser> getSamlUsersByGroupName(String groupName) {
    try (TransactionContext tx = samlUserDAO.createTransactionContext()) {
      SamlGroup samlGroup = samlGroupDAO.getByName(tx, groupName);
      if (samlGroup == null) {
        return Collections.emptyList();
      }
      List<SamlUserGroup> samlUserGroups = samlUserGroupDAO.getBySamlGroupId(tx, samlGroup.getId());
      return samlUserDAO.getByIds(tx,
          samlUserGroups.stream().map(SamlUserGroup::getSamlUserId)
              .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
  }

  public Set<String> filterExistingSamlGroupNames(Set<String> groupNames) {
    return samlGroupDAO.getByNames(groupNames).stream().map(SamlGroup::getName).collect(Collectors.toCollection(
        LinkedHashSet::new));
  }

  public List<SamlUser> getSamlUsersByUsernames(Set<String> usernames) {
    return samlUserDAO.getByUsernames(usernames);
  }

  public List<SamlUser> findSamlUsersByNameQuery(String nameQuery) {
    return samlUserDAO.findUsersByNameQuery(nameQuery);
  }

  public List<SamlGroup> findSamlGroupsByNameQuery(String nameQuery) {
    return samlGroupDAO.findGroupsByNameQuery(nameQuery);
  }
}

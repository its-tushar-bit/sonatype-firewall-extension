/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;

@Named
public class UserFilterService
{
  private final CurrentUser currentUser;

  private final UserFilterDAO userFilterDAO;

  private final UserFilterPruner userFilterPruner;

  @Inject
  public UserFilterService(CurrentUser currentUser, UserFilterDAO userFilterDAO, UserFilterPruner userFilterPruner) {
    this.currentUser = currentUser;
    this.userFilterDAO = userFilterDAO;
    this.userFilterPruner = userFilterPruner;
  }

  public UserFilterDTO createOrUpdateUserFilterForCurrentUser(UserFilterDTO userFilterDTO) {
    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();

    if (!ACTIVE_FILTER_NAME.equals(userFilterDTO.getName())) {
      UserFilter userFilter = new UserFilter();
      userFilter.setUsername(username);
      userFilter.setRealmId(realmId);
      userFilter.setFilter(JsonUtils.format(userFilterDTO.getFilter()));
      userFilter.setName(userFilterDTO.getName());
      userFilter.setType(userFilterDTO.getType());

      UserFilter existingUserFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(username, realmId,
          userFilterDTO.getName(), userFilterDTO.getType());
      if (existingUserFilter == null) {
        userFilterDAO.insert(userFilter);
      }
      else {
        userFilter.setId(existingUserFilter.getId());
        userFilterDAO.update(userFilter);
      }

      auditUserFilter(userFilter);
    }

    createOrUpdateActiveFilter(userFilterDTO, username, realmId);
    return userFilterDTO;
  }

  public UserFilterDTO getActiveUserFilterForCurrentUser(UserFilterType type) {
    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();

    UserFilter activeFilter =
        userFilterDAO.getByUsernameAndRealmIdAndNameAndType(username, realmId, ACTIVE_FILTER_NAME, type);

    if (activeFilter == null) {
      activeFilter = new UserFilter(username, realmId, ACTIVE_FILTER_NAME, type);
    }

    UserFilterDTO dto = new UserFilterDTO(activeFilter);
    userFilterPruner.process(dto);

    return dto;
  }

  public List<UserFilterDTO> getNamedFiltersForCurrentUser(UserFilterType type) {
    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();

    List<UserFilter> filters = userFilterDAO.getNamedFiltersByUsernameAndRealmIdAndType(username, realmId, type);
    List<UserFilterDTO> result = new ArrayList<>();

    for (UserFilter userFilter : filters) {
      UserFilterDTO dto = new UserFilterDTO(userFilter);
      userFilterPruner.process(dto);
      result.add(dto);
    }

    return result;
  }

  public void deleteFilterForCurrentUserByNameAndType(String name, UserFilterType type) {
    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();

    try (TransactionContext tx = userFilterDAO.createTransactionContext()) {
      tx.begin();
      UserFilter filter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(tx, username, realmId, name, type);
      if (filter == null) {
        throw new NotFoundException(
            "Cannot find a filter with name " + name + " and type " + type + " for user " + username + ".");
      }

      UserFilter activeFilter =
          userFilterDAO.getByUsernameAndRealmIdAndNameAndType(tx, username, realmId, ACTIVE_FILTER_NAME, type);

      if (activeFilter != null && filter.getName().equals(activeFilter.getBasedOnFilterName())) {
        activeFilter.setBasedOnFilterName(null);
        userFilterDAO.update(tx, activeFilter);
      }

      userFilterDAO.delete(tx, filter);
      tx.commit();
      auditUserFilter(filter);
    }
  }

  private void createOrUpdateActiveFilter(UserFilterDTO userFilterDTO, String username, String realmId) {
    UserFilter newActiveFilter = new UserFilter();
    newActiveFilter.setUsername(username);
    newActiveFilter.setRealmId(realmId);
    newActiveFilter.setFilter(JsonUtils.format(userFilterDTO.getFilter()));
    newActiveFilter.setName(ACTIVE_FILTER_NAME);
    newActiveFilter.setType(userFilterDTO.getType());

    if (userFilterDTO.getBasedOnFilterName() != null) {
      newActiveFilter.setBasedOnFilterName(userFilterDTO.getBasedOnFilterName());
    }
    else if (!userFilterDTO.getName().equals(ACTIVE_FILTER_NAME)) {
      newActiveFilter.setBasedOnFilterName(userFilterDTO.getName());
    }

    UserFilter existingActiveFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(username, realmId,
        ACTIVE_FILTER_NAME, userFilterDTO.getType());
    if (existingActiveFilter != null) {
      newActiveFilter.setId(existingActiveFilter.getId());
      userFilterDAO.update(newActiveFilter);
    }
    else {
      userFilterDAO.insert(newActiveFilter);
    }
    if (ACTIVE_FILTER_NAME.equals(userFilterDTO.getName())) {
      auditUserFilter(newActiveFilter);
    }
  }

  private void auditUserFilter(UserFilter userFilter) {
    AuditData.get()
        .setData("filterId", userFilter.getId())
        .setData("filterName", userFilter.getName().equals(ACTIVE_FILTER_NAME) ? "(active)" : userFilter.getName());
  }
}

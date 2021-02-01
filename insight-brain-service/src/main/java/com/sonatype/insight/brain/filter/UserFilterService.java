/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

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

  @Inject
  public UserFilterService(CurrentUser currentUser, UserFilterDAO userFilterDAO) {
    this.currentUser = currentUser;
    this.userFilterDAO = userFilterDAO;
  }

  public UserFilterDTO createOrUpdateUserFilterForCurrentUser(UserFilterDTO userFilterDTO) {
    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();

    if (!ACTIVE_FILTER_NAME.equals(userFilterDTO.name)) {
      UserFilter userFilter = new UserFilter();
      userFilter.setUsername(username);
      userFilter.setRealmId(realmId);
      userFilter.setFilter(JsonUtils.format(userFilterDTO.filter));
      userFilter.setName(userFilterDTO.name);
      userFilter.setType(userFilterDTO.type);

      UserFilter existingUserFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(username, realmId,
          userFilterDTO.name, userFilterDTO.type);
      if (existingUserFilter == null) {
        userFilterDAO.insert(userFilter);
      }
      else {
        userFilter.setId(existingUserFilter.getId());
        userFilterDAO.update(userFilter);
      }
    }

    createOrUpdateActiveFilter(userFilterDTO, username, realmId);
    return userFilterDTO;
  }

  public UserFilterDTO getActiveUserFilterForCurrentUser(UserFilterType type) throws IOException {
    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();

    UserFilter activeFilter =
        userFilterDAO.getByUsernameAndRealmIdAndNameAndType(username, realmId, ACTIVE_FILTER_NAME, type);

    if (activeFilter == null) {
      activeFilter = new UserFilter(username, realmId, ACTIVE_FILTER_NAME, type);
      activeFilter.setFilter(JsonUtils.format(new HashMap<>()));
    }

    return newUserFilterDTO(activeFilter);
  }

  public List<UserFilterDTO> getNamedFiltersForCurrentUser(UserFilterType type) throws IOException {
    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();

    List<UserFilter> filters = userFilterDAO.getNamedFiltersByUsernameAndRealmIdAndType(username, realmId, type);
    List<UserFilterDTO> result = new ArrayList<>();

    for (UserFilter userFilter : filters) {
      result.add(newUserFilterDTO(userFilter));
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
    }
  }

  private void createOrUpdateActiveFilter(UserFilterDTO userFilterDTO, String username, String realmId) {
    UserFilter newActiveFilter = new UserFilter();
    newActiveFilter.setUsername(username);
    newActiveFilter.setRealmId(realmId);
    newActiveFilter.setFilter(JsonUtils.format(userFilterDTO.filter));
    newActiveFilter.setName(ACTIVE_FILTER_NAME);
    newActiveFilter.setType(userFilterDTO.type);

    if (userFilterDTO.basedOnFilterName != null) {
      newActiveFilter.setBasedOnFilterName(userFilterDTO.basedOnFilterName);
    }
    else if (!userFilterDTO.name.equals(ACTIVE_FILTER_NAME)) {
      newActiveFilter.setBasedOnFilterName(userFilterDTO.name);
    }

    UserFilter existingActiveFilter =
        userFilterDAO.getByUsernameAndRealmIdAndNameAndType(username, realmId, ACTIVE_FILTER_NAME, userFilterDTO.type);
    if (existingActiveFilter != null) {
      newActiveFilter.setId(existingActiveFilter.getId());
      userFilterDAO.update(newActiveFilter);
    }
    else {
      userFilterDAO.insert(newActiveFilter);
    }
  }

  @SuppressWarnings("unchecked")
  private UserFilterDTO newUserFilterDTO(UserFilter userFilter) throws IOException {
    UserFilterDTO userFilterDTO = new UserFilterDTO();
    userFilterDTO.basedOnFilterName = userFilter.getBasedOnFilterName();
    userFilterDTO.filter = JsonUtils.parse(userFilter.getFilter(), Map.class);
    userFilterDTO.name = userFilter.getName();
    userFilterDTO.type = userFilter.getType();
    return userFilterDTO;
  }
}

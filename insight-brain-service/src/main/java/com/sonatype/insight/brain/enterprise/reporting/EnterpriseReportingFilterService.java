/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingFilterDAO;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingDefaultFilterDAO;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingFilter;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingDefaultFilter;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class EnterpriseReportingFilterService
{
  public static final int MAX_NAME_SIZE = 60;

  public static final String DEFAULT_FILTER_NAME = "Sonatype Default";

  public static final String INVALID_CHAR_MESSAGE = "Filter name contains an invalid character: '%c'.";

  private final EnterpriseReportingFilterDAO enterpriseReportingFilterDAO;

  private final EnterpriseReportingDefaultFilterDAO enterpriseReportingDefaultFilterDAO;

  private final CurrentUser currentUser;

  private final UserDirectory userDirectory;

  @Inject
  public EnterpriseReportingFilterService(EnterpriseReportingFilterDAO enterpriseReportingFilterDAO,
                                          EnterpriseReportingDefaultFilterDAO enterpriseReportingDefaultFilterDAO,
                                          CurrentUser currentUser,
                                          UserDirectory userDirectory)
  {
    this.enterpriseReportingFilterDAO = enterpriseReportingFilterDAO;
    this.enterpriseReportingDefaultFilterDAO = enterpriseReportingDefaultFilterDAO;
    this.currentUser = currentUser;
    this.userDirectory = userDirectory;
  }

  /**
   * Get all saved filters for the current user.
   *
   * @return list of filters owned by the current user, or an empty list if none exist
   * @throws NotFoundException if user doesn't exist in DB
   */
  public List<EnterpriseReportingDashboardFilterDTO> getFiltersForCurrentUser() {
    String userId = getCurrentUserId();
    return enterpriseReportingFilterDAO.getFiltersByUserId(userId).stream().map(f -> {
      var dto = new EnterpriseReportingDashboardFilterDTO();
      dto.name = f.getFilterName();
      dto.filter = f.getFilter();
      dto.id = f.getId();
      return dto;
    }).toList();
  }

  /**
   * Creates or updates a filter for the current user.
   * <p>
   * If dto.id matches an existing user-owned filter, that filter is updated; otherwise a new filter is created.
   * <p>
   * Creating / updating a filter runs basic validation, checks the name is unique per user and the name is not equal
   * to the reserved name "Sonatype Default" (case-insensitive).
   * <p>
   * When dto.isDefault is true, the filter is also set as the user's default in the same transaction.
   *
   * @param dto the EnterpriseReportingDashboardFilterDTO containing name, filter, isDefault, and optional id
   * @return the persisted filter DTO (id generated for new filters)
   * @throws InvalidNameException if the name is invalid or already in use by the user
   * @throws NotFoundException if user doesn't exist in DB
   */
  public EnterpriseReportingDashboardFilterDTO upsertFilterForCurrentUser(
      EnterpriseReportingDashboardFilterDTO dto)
  {
    String userId = getCurrentUserId();

    try (TransactionContext tx = enterpriseReportingFilterDAO.createTransactionContext()) {
      tx.begin();

      if (StringUtils.isBlank(dto.name)) {
        throw new InvalidNameException("Filter name is required.");
      }

      String trimmedName = dto.name.trim();
      validateInput(trimmedName);

      var existingFilter = findExistingFilter(tx, userId, dto.id);
      String existingFilterId = existingFilter != null ? existingFilter.getId() : null;
      validateUnique(tx, userId, trimmedName, existingFilterId);

      if (existingFilter == null) {
        EnterpriseReportingFilter enterpriseFilter = createFilter(dto, userId);
        enterpriseReportingFilterDAO.insert(tx, enterpriseFilter);
        dto.id = enterpriseFilter.getId();
      }
      else {
        existingFilter.setFilterName(trimmedName);
        existingFilter.setFilter(dto.filter);
        enterpriseReportingFilterDAO.update(tx, existingFilter);
      }

      if (dto.isDefault) {
        setDefaultFilterForCurrentUser(tx, dto.id);
      }

      tx.commit();
    }
    return dto;
  }

  /**
   * Deletes a filter owned by the current user.
   * <p>
   * If the filter is set as the user's default, the corresponding row is removed via ON DELETE CASCADE.
   *
   * @param filterId id of the filter to delete (must not be null)
   * @throws BadRequestException if filterId is null, "null", or "undefined" (attached as template literal to url)
   * @throws NotFoundException if the filter does not exist for the user, or if the suer doesn't exist in DB
   */
  public void deleteFilterForCurrentUser(String filterId) {
    if ("undefined".equals(filterId) || "null".equals(filterId) || StringUtils.isBlank(filterId)) {
      throw new BadRequestException("Filter ID cannot be null.");
    }
    String userId = getCurrentUserId();
    
    EnterpriseReportingFilter existingFilter =
        enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, filterId);
    if (existingFilter == null) {
      throw new NotFoundException("Cannot find filter to delete. It may have already been removed or does not exist.");
    }
    enterpriseReportingFilterDAO.delete(existingFilter);
  }

  /**
   * Returns the current user's default filter id, or null if none is set.
   *
   * @return the default filter id, or null if no default exists
   * @throws NotFoundException if user doesn't exist in DB
   */
  public String getDefaultFilterForCurrentUser() {
    String userId = getCurrentUserId();
    EnterpriseReportingDefaultFilter existingDefaultFilter =
        enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId);

    return existingDefaultFilter != null ? existingDefaultFilter.getFilterId() : null;
  }

  /**
   * Sets the current user's default filter.
   * <p>
   * Creates its own transaction so it can be called independently of upsert operations.
   *
   * @param filterId id of the filter to set as default for the current user
   * @return the filter id that was set as default
   * @throws BadRequestException if the filter does not exist for the user
   */
  public String setDefaultFilterForCurrentUser(String filterId) {
    String defaultFilter;
    try (TransactionContext tx = enterpriseReportingFilterDAO.createTransactionContext()) {
      tx.begin();
      defaultFilter = setDefaultFilterForCurrentUser(tx, filterId);
      tx.commit();
    }
    return defaultFilter;
  }

  /**
   * Sets the current user's default filter within the provided transaction.
   * <p>
   * If no default exists, it is created; otherwise it is updated with the given filter id.
   * It is commonly used alongside upsert operations so changes occur in a single transaction.
   *
   * @param tx transaction context to use
   * @param filterId id of the filter to set as default
   * @return the filter id that was set as default
   * @throws BadRequestException if the filter does not exist for the user
   */
  private String setDefaultFilterForCurrentUser(TransactionContext tx, String filterId) {
    String userId = getCurrentUserId();
    EnterpriseReportingFilter enterpriseFilter =
        enterpriseReportingFilterDAO.getFilterByUserAndFilterId(tx, userId, filterId);
    if (enterpriseFilter == null) {
      throw new BadRequestException("Filter does not already exist to mark as default.");
    }

    EnterpriseReportingDefaultFilter existingDefaultFilter =
        enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(tx, userId);

    if (existingDefaultFilter == null) {
      EnterpriseReportingDefaultFilter defaultFilter = createDefaultFilter(filterId, userId);
      enterpriseReportingDefaultFilterDAO.insert(tx, defaultFilter);
    }
    else {
      existingDefaultFilter.setFilterId(filterId);
      enterpriseReportingDefaultFilterDAO.update(tx, existingDefaultFilter);
    }
    return filterId;
  }

  /**
   * Deletes a default filter owned by the current user.
   * <p>
   * The corresponding filter in the enterprise_reporting_filter table will not be removed, only its
   * assignment as the user's default. The purpose of this service is to reset the user's default to
   * a "Sonatype Default" filter defined in the FE.
   * @throws NotFoundException if the filter does not exist for the user, or if the user doesn't exist in DB
   */
  public void deleteDefaultFilterForCurrentUser() {
    String userId = getCurrentUserId();
    
    EnterpriseReportingDefaultFilter existingFilter =
        enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId);
    if (existingFilter != null) {
      enterpriseReportingDefaultFilterDAO.delete(existingFilter);
    }
  }
  
  private String getCurrentUserId() {
    String username = currentUser.getUsername();
    UserDirectory.QueryResult result = userDirectory.getUsersByNames(Set.of(username));

    // If no user found
    if (result.get().isEmpty()) {
      throw new NotFoundException("User not found: " + username);
    }

    return result.get().get(0).getUserId();
  }

  private EnterpriseReportingFilter createFilter(EnterpriseReportingDashboardFilterDTO dto, String userId) {
    EnterpriseReportingFilter enterpriseFilter = new EnterpriseReportingFilter();
    enterpriseFilter.setId(IdUtil.newUUID());
    enterpriseFilter.setUserId(userId);
    enterpriseFilter.setFilterName(dto.name.trim());
    enterpriseFilter.setFilter(dto.filter);
    return enterpriseFilter;
  }

  private EnterpriseReportingDefaultFilter createDefaultFilter(String filterId, String userId) {
    EnterpriseReportingDefaultFilter defaultFilter = new EnterpriseReportingDefaultFilter();
    defaultFilter.setId(userId);
    defaultFilter.setFilterId(filterId);
    return defaultFilter;
  }

  private EnterpriseReportingFilter findExistingFilter(TransactionContext tx, String userId, String filterId) {
    boolean hasId = StringUtils.isNotBlank(filterId);
    return hasId ? enterpriseReportingFilterDAO.getFilterByUserAndFilterId(tx, userId, filterId) : null;
  }

  private static void validateInput(String trimmedName) {
    for (char c : trimmedName.toCharArray()) {
      if (!Character.isLetterOrDigit(c) && "-'._ ".indexOf(c) < 0) {
        throw new InvalidNameException(String.format(INVALID_CHAR_MESSAGE, c));
      }
    }
    if (trimmedName.indexOf("  ") > 0) {
      throw new InvalidNameException("Filter name must not have two spaces in a row.");
    }
    if (trimmedName.length() > MAX_NAME_SIZE) {
      throw new InvalidNameException("Filter name must be " + MAX_NAME_SIZE + " characters or less.");
    }
  }

  private void validateUnique(TransactionContext tx, String userId, String filterName, String existingFilterId) {
    if (DEFAULT_FILTER_NAME.equalsIgnoreCase(filterName)) {
      throw new InvalidNameException("The name " + DEFAULT_FILTER_NAME + " is reserved and cannot be used.");
    }

    EnterpriseReportingFilter sameNameFilter =
        enterpriseReportingFilterDAO.getFilterByUserIdAndName(tx, userId, filterName);

    if (sameNameFilter != null) {
      // If a filter with the same name already exists, and the user is either creating a new filter
      // (existingFilterId == null) or updating a different filter (filterIds differ), throw an error
      if (!Objects.equals(existingFilterId, sameNameFilter.getId())) {
        throw new InvalidNameException(filterName + " is already used as a name.");
      }
    }    
  }
}

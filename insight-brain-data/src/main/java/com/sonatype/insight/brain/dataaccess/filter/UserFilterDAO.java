/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.filter;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.UserFilter.USER_FILTER;
import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;

/**
 * @since 1.105
 */
@Named
@Singleton
public class UserFilterDAO
    extends AbstractOperationalSqlDAO<UserFilter>
{
  private static final Logger log = LoggerFactory.getLogger(UserFilterDAO.class);

  @Inject
  public UserFilterDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  protected UpdatableRecord<?> fromEntity(final UpdatableRecord<?> record, final UserFilter entity) {
    super.fromEntity(record, entity);
    record.set(USER_FILTER.USERNAME_LOWERCASE, User.normalizeUsername(entity.getUsername()));
    record.set(USER_FILTER.NAME_LOWERCASE_NO_WHITESPACE, NameHelper.normalize(entity.getName()));
    return record;
  }

  @Override
  public int insert(TransactionContext tx, UserFilter userFilter) {
    validate(tx, userFilter);
    UserFilter existingFilter = getByUsernameAndRealmIdAndNameAndType(tx, userFilter.getUsername(),
        userFilter.getRealmId(), userFilter.getName(), userFilter.getType());
    if (existingFilter != null) {
      throw new InvalidNameException(
          userFilter.getName() + " is already used as a name for type " + userFilter.getType());
    }
    return super.insert(tx, userFilter);
  }

  @Override
  public void update(TransactionContext tx, UserFilter userFilter) {
    validate(tx, userFilter);
    UserFilter existingFilter = getByUsernameAndRealmIdAndNameAndType(tx, userFilter.getUsername(),
        userFilter.getRealmId(), userFilter.getName(), userFilter.getType());
    if (existingFilter != null && !existingFilter.getId().equals(userFilter.getId())) {
      throw new InvalidNameException(
          userFilter.getName() + " is already used as a name for type " + userFilter.getType());
    }
    super.update(tx, userFilter);
  }

  public void deleteByRealmId(TransactionContext tx, String realmId) {
    getByRealmId(tx, realmId).forEach(userFilter -> delete(tx, userFilter));
  }

  public void deleteByUsernameAndRealmId(TransactionContext tx, String username, String realmId) {
    List<UserFilter> userFilters = getByUsernameAndRealmId(tx, username, realmId);
    for (UserFilter userFilter : userFilters) {
      delete(tx, userFilter);
    }
  }

  public UserFilter getByUsernameAndRealmIdAndNameAndType(
      String username,
      String realmId,
      String name,
      UserFilterType type)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndRealmIdAndNameAndType(tx, username, realmId, name, type);
    }
  }

  public UserFilter getByUsernameAndRealmIdAndNameAndType(
      TransactionContext tx,
      String username,
      String realmId,
      String name,
      UserFilterType type)
  {
    username = User.normalizeUsername(username);
    name = NameHelper.normalize(name);
    return toEntity(tx.dsl()
        .selectFrom(USER_FILTER)
        .where(USER_FILTER.USERNAME_LOWERCASE.eq(username))
        .and(USER_FILTER.REALM_ID.eq(realmId))
        .and(USER_FILTER.NAME_LOWERCASE_NO_WHITESPACE.eq(name))
        .and(USER_FILTER.FILTER_TYPE.eq(type.name()))
        .fetchOne());
  }

  public List<UserFilter> getNamedFiltersByUsernameAndRealmIdAndType(
      String username,
      String realmId,
      UserFilterType type)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getNamedFiltersByUsernameAndRealmIdAndType(tx, username, realmId, type);
    }
  }

  private List<UserFilter> getNamedFiltersByUsernameAndRealmIdAndType(
      TransactionContext tx,
      String username,
      String realmId,
      UserFilterType type)
  {
    username = User.normalizeUsername(username);
    return tx.dsl()
        .selectFrom(USER_FILTER)
        .where(USER_FILTER.USERNAME_LOWERCASE.eq(username))
        .and(USER_FILTER.REALM_ID.eq(realmId))
        .and(USER_FILTER.NAME_LOWERCASE_NO_WHITESPACE.ne(""))
        .and(USER_FILTER.FILTER_TYPE.eq(type.name()))
        .fetch()
        .map(this::toEntity);
  }

  private void validate(TransactionContext tx, UserFilter userFilter) {
    if (StringUtils.isBlank(userFilter.getRealmId())) {
      throw new BadRequestException("The realm ID is required.");
    }
    if (userFilter.getType() == null) {
      throw new BadRequestException("The type is required.");
    }

    String name = userFilter.getName();
    if (!ACTIVE_FILTER_NAME.equals(name)) {
      NameHelper.validate(name);
      if (userFilter.getBasedOnFilterName() != null) {
        throw new BadRequestException("Only the active filter can be based on another filter.");
      }
    }
    else if (userFilter.getBasedOnFilterName() != null) {
      UserFilter basedOnFilter = getByUsernameAndRealmIdAndNameAndType(tx, userFilter.getUsername(),
          userFilter.getRealmId(), userFilter.getBasedOnFilterName(), userFilter.getType());
      if (basedOnFilter == null) {
        log.debug("Attempted to persist active filter based on non-existing saved filter named {}.",
            userFilter.getBasedOnFilterName());
        userFilter.setBasedOnFilterName(null);
      }
    }
  }

  private List<UserFilter> getByRealmId(TransactionContext tx, String realmId) {
    return tx.dsl()
        .selectFrom(USER_FILTER)
        .where(USER_FILTER.REALM_ID.eq(realmId))
        .fetch()
        .map(this::toEntity);
  }

  private List<UserFilter> getByUsernameAndRealmId(TransactionContext tx, String username, String realmId) {
    username = User.normalizeUsername(username);
    return tx.dsl()
        .selectFrom(USER_FILTER)
        .where(USER_FILTER.USERNAME_LOWERCASE.eq(username))
        .and(USER_FILTER.REALM_ID.eq(realmId))
        .orderBy(USER_FILTER.NAME)
        .fetch()
        .map(this::toEntity);
  }

  public UserFilter getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  private UserFilter getByName(TransactionContext tx, String name) {
    return toEntity(tx.dsl()
        .selectFrom(USER_FILTER)
        .where(USER_FILTER.NAME_LOWERCASE_NO_WHITESPACE.eq(NameHelper.normalize(name)))
        .fetchOne());
  }

  @Override
  public Table<?> getJooqTable() {
    return USER_FILTER;
  }

  @Override
  public Class<UserFilter> getEntityClass() {
    return UserFilter.class;
  }
}

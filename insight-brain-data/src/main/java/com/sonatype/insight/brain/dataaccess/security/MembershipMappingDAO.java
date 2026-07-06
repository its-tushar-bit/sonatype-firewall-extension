/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.MembershipMapping.MEMBERSHIP_MAPPING;

/**
 * @since 1.7
 */
@Named
@Singleton
public class MembershipMappingDAO
    extends AbstractOperationalSqlDAO<MembershipMapping>
{
  private final OperationalDataStore operationalDataStore;

  @Inject
  public MembershipMappingDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
    this.operationalDataStore = operationalDataStore;
  }

  /**
   * Gets the membership mappings for a given context.
   */
  public List<MembershipMapping> getByContextId(String contextId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByContextId(tx, contextId);
    }
  }

  /**
   * Gets the membership mappings for a given context.
   */
  public List<MembershipMapping> getByContextId(TransactionContext tx, String contextId) {
    return tx.dsl()
        .selectFrom(MEMBERSHIP_MAPPING)
        .where(MEMBERSHIP_MAPPING.CONTEXT_ID.eq(contextId))
        .orderBy(MEMBERSHIP_MAPPING.ROLE_ID, MEMBERSHIP_MAPPING.MEMBER_NAME)
        .fetchInto(MembershipMapping.class);
  }

  List<MembershipMapping> getByUser(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUser(tx, username);
    }
  }

  List<MembershipMapping> getByUser(TransactionContext tx, String username) {
    return tx.dsl()
        .selectFrom(MEMBERSHIP_MAPPING)
        .where(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username)
            .and(MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.USER.name())))
        .orderBy(MEMBERSHIP_MAPPING.CONTEXT_ID, MEMBERSHIP_MAPPING.ROLE_ID)
        .fetchInto(MembershipMapping.class);
  }

  public List<MembershipMapping> getByUserCaseInsensitiveAndGroups(String username, Set<String> groupNames) {
    try (TransactionContext tx = createTransactionContext()) {
      // Match user by case-insensitive username comparison
      var userCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.USER.name())
          .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username)
              .or(DSL.lower(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toLowerCase()))
              .or(DSL.upper(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toUpperCase())));

      // Match groups by exact group name
      var groupCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.GROUP.name())
          .and(MEMBERSHIP_MAPPING.MEMBER_NAME.in(groupNames));

      return tx.dsl()
          .selectFrom(MEMBERSHIP_MAPPING)
          .where(userCondition.or(groupCondition))
          .orderBy(MEMBERSHIP_MAPPING.CONTEXT_ID, MEMBERSHIP_MAPPING.ROLE_ID)
          .fetchInto(MembershipMapping.class);
    }
  }

  public List<MembershipMapping> getByUserCaseInsensitiveAndGroupsAndRoles(
      String username,
      Set<String> groupNames,
      Set<String> roleIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      // Match user by case-insensitive username comparison
      var userCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.USER.name())
          .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username)
              .or(DSL.lower(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toLowerCase()))
              .or(DSL.upper(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toUpperCase())));

      // Match groups by exact group name
      var groupCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.GROUP.name())
          .and(MEMBERSHIP_MAPPING.MEMBER_NAME.in(groupNames));

      // Combine user or group condition, then restrict to specific roles
      var memberCondition = userCondition.or(groupCondition);

      return tx.dsl()
          .selectFrom(MEMBERSHIP_MAPPING)
          .where(memberCondition.and(MEMBERSHIP_MAPPING.ROLE_ID.in(roleIds)))
          .orderBy(MEMBERSHIP_MAPPING.CONTEXT_ID, MEMBERSHIP_MAPPING.ROLE_ID)
          .fetchInto(MembershipMapping.class);
    }
  }

  public List<String> getContextIdsByUserCaseInsensitiveAndGroupsAndRoles(
      String username,
      Set<String> groupNames,
      Set<String> roleIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      // Match user by case-insensitive username comparison
      var userCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.USER.name())
          .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username)
              .or(DSL.lower(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toLowerCase()))
              .or(DSL.upper(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toUpperCase())));

      // Match groups by exact group name
      var groupCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.GROUP.name())
          .and(MEMBERSHIP_MAPPING.MEMBER_NAME.in(groupNames));

      // Combine user or group condition, then restrict to specific roles
      var memberCondition = userCondition.or(groupCondition);

      return tx.dsl()
          .select(MEMBERSHIP_MAPPING.CONTEXT_ID)
          .from(MEMBERSHIP_MAPPING)
          .where(memberCondition.and(MEMBERSHIP_MAPPING.ROLE_ID.in(roleIds)))
          .fetch(MEMBERSHIP_MAPPING.CONTEXT_ID);
    }
  }

  /**
   * Gets the distinct member names for a given context and member type that are mapped to any of the
   * given roles. Filtering is performed in SQL rather than in memory.
   */
  public List<String> getDistinctMemberNamesByContextIdAndMemberTypeAndRoleIds(
      String contextId,
      MemberType memberType,
      Set<String> roleIds)
  {
    if (roleIds.isEmpty()) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectDistinct(MEMBERSHIP_MAPPING.MEMBER_NAME)
          .from(MEMBERSHIP_MAPPING)
          .where(MEMBERSHIP_MAPPING.CONTEXT_ID.eq(contextId)
              .and(MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(memberType.name()))
              .and(MEMBERSHIP_MAPPING.ROLE_ID.in(roleIds)))
          .fetch(MEMBERSHIP_MAPPING.MEMBER_NAME);
    }
  }

  List<MembershipMapping> getByContextIdAndUser(String contextId, String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(MEMBERSHIP_MAPPING)
          .where(MEMBERSHIP_MAPPING.CONTEXT_ID.eq(contextId)
              .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username))
              .and(MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.USER.name())))
          .orderBy(MEMBERSHIP_MAPPING.ROLE_ID)
          .fetchInto(MembershipMapping.class);
    }
  }

  public List<MembershipMapping> getByContextIdAndRoleId(TransactionContext tx, String contextId, String roleId) {
    return tx.dsl()
        .selectFrom(MEMBERSHIP_MAPPING)
        .where(MEMBERSHIP_MAPPING.CONTEXT_ID.eq(contextId)
            .and(MEMBERSHIP_MAPPING.ROLE_ID.eq(roleId)))
        .orderBy(MEMBERSHIP_MAPPING.MEMBER_NAME)
        .fetchInto(MembershipMapping.class);
  }

  public List<MembershipMapping> getByContextIdAndRoleId(String contextId, String roleId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByContextIdAndRoleId(tx, contextId, roleId);
    }
  }

  /**
   * @since 1.70
   */
  public MembershipMapping getByContextIdAndRoleIdAndMemberNameAndMemberType(
      String contextId,
      String roleId,
      String memberName,
      MemberType memberType)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(MEMBERSHIP_MAPPING)
          .where(MEMBERSHIP_MAPPING.CONTEXT_ID.eq(contextId)
              .and(MEMBERSHIP_MAPPING.ROLE_ID.eq(roleId))
              .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(memberName))
              .and(MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(memberType.name())))
          .fetchOneInto(MembershipMapping.class);
    }
  }

  /**
   * Sets the membership mappings for a given context and role.
   */
  public void setMembershipMappingsForContextAndRole(
      String contextId,
      String roleId,
      List<MembershipMapping> mappings)
  {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      setMembershipMappingsForContextAndRole(tx, contextId, roleId, mappings);

      tx.commit();
    }
  }

  /**
   * Sets the membership mappings for a given context and role.
   * Allows caller to provide the entity manager.
   *
   * @since 1.11.0
   */
  public void setMembershipMappingsForContextAndRole(
      TransactionContext tx,
      String contextId,
      String roleId,
      List<MembershipMapping> mappings)
  {

    Map<String, MembershipMapping> mappingsByMember = new HashMap<>();
    for (MembershipMapping existingMapping : getByContextIdAndRoleId(tx, contextId, roleId)) {
      mappingsByMember.put(getMemberKey(existingMapping), existingMapping);
    }

    // Create new values
    for (MembershipMapping newMapping : mappings) {
      newMapping.setContextId(contextId);
      newMapping.setRoleId(roleId);

      String memberKey = getMemberKey(newMapping);
      if (!mappingsByMember.containsKey(memberKey)) {
        newMapping.setId(null);
        insert(tx, newMapping);
      }
      mappingsByMember.put(memberKey, null);
    }

    // Delete old values
    for (MembershipMapping oldMapping : mappingsByMember.values()) {
      if (oldMapping != null) {
        delete(tx, oldMapping);
      }
    }
  }

  private String getMemberKey(MembershipMapping mapping) {
    return mapping.getMemberType() + ":" + mapping.getMemberName();
  }

  @Override
  public int update(TransactionContext tx, MembershipMapping entity) {
    throw new UnsupportedOperationException("Use setMembershipMappingsForContextAndRole() instead");
  }

  public void insertAll(final List<MembershipMapping> membershipMappings) {
    if (null == membershipMappings || membershipMappings.isEmpty()) {
      return;
    }
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      // Insert one row at a time, skipping if a record with same unique key exists
      // jOOQ's onDuplicateKeyIgnore() generates PostgreSQL MERGE syntax which H2 doesn't support
      for (MembershipMapping membershipMapping : membershipMappings) {
        // Check if record already exists (unique constraint on context_id, role_id, member_name, member_type)
        boolean exists = tx.dsl()
            .fetchExists(
                tx.dsl()
                    .selectFrom(MEMBERSHIP_MAPPING)
                    .where(MEMBERSHIP_MAPPING.CONTEXT_ID.eq(membershipMapping.getContextId()))
                    .and(MEMBERSHIP_MAPPING.ROLE_ID.eq(membershipMapping.getRoleId()))
                    .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(membershipMapping.getMemberName()))
                    .and(MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(membershipMapping.getMemberType().toString())));

        if (!exists) {
          tx.dsl()
              .insertInto(MEMBERSHIP_MAPPING)
              .set(MEMBERSHIP_MAPPING.MEMBERSHIP_MAPPING_ID, IdUtil.newUUID())
              .set(MEMBERSHIP_MAPPING.CONTEXT_ID, membershipMapping.getContextId())
              .set(MEMBERSHIP_MAPPING.ROLE_ID, membershipMapping.getRoleId())
              .set(MEMBERSHIP_MAPPING.MEMBER_NAME, membershipMapping.getMemberName())
              .set(MEMBERSHIP_MAPPING.MEMBER_TYPE, membershipMapping.getMemberType().toString())
              .execute();
        }
      }
      tx.commit();
    }
  }

  /**
   * This method may load tons (even millions) of records in production.
   * It is ok to use it for tests where there isn't a lot of data, but not in production, where it may consume a lot of
   * memory and have very poor performance.
   */
  List<MembershipMapping> getByRoleIdsForTestsOnly(Set<String> roleIds) {
    if (roleIds.isEmpty()) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(MEMBERSHIP_MAPPING)
          .where(MEMBERSHIP_MAPPING.ROLE_ID.in(roleIds))
          .fetchInto(MembershipMapping.class);
    }
  }

  public int getCountByRoleIdAndMemberType(String roleId, MemberType memberType) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.countDistinct(MEMBERSHIP_MAPPING.MEMBER_NAME))
          .from(MEMBERSHIP_MAPPING)
          .where(MEMBERSHIP_MAPPING.ROLE_ID.eq(roleId)
              .and(MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(memberType.name())))
          .fetchOneInto(Integer.class);
    }
  }

  public boolean isUserHavingRolesInAnyContext(
      Set<String> roleIds,
      String userName,
      Set<String> groupNames)
  {
    if (roleIds.isEmpty()) {
      return false;
    }
    try (TransactionContext tx = createTransactionContext()) {
      // Match user by case-insensitive username comparison
      var userCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.USER.name())
          .and(DSL.lower(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(userName.toLowerCase()));

      // Match groups by exact group name (if groups are provided)
      var memberCondition = userCondition;
      if (!CollectionUtils.isEmpty(groupNames)) {
        var groupCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.GROUP.name())
            .and(MEMBERSHIP_MAPPING.MEMBER_NAME.in(groupNames));
        memberCondition = userCondition.or(groupCondition);
      }

      // Apply role ID constraint to all matches
      var finalCondition = MEMBERSHIP_MAPPING.ROLE_ID.in(roleIds).and(memberCondition);

      return tx.dsl()
          .selectCount()
          .from(MEMBERSHIP_MAPPING)
          .where(finalCondition)
          .fetchOneInto(Integer.class) > 0;
    }
  }

  /**
   * @since 1.15.0
   */
  public List<MembershipMapping> getByRoleId(TransactionContext tx, String roleId) {
    return tx.dsl()
        .selectFrom(MEMBERSHIP_MAPPING)
        .where(MEMBERSHIP_MAPPING.ROLE_ID.eq(roleId))
        .fetchInto(MembershipMapping.class);
  }

  public boolean isSystemAdmin(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(MEMBERSHIP_MAPPING)
          .where(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username)
              .and(MEMBERSHIP_MAPPING.ROLE_ID.eq(Role.SYSTEM_ADMIN_ROLE_ID)))
          .fetchOneInto(Integer.class) > 0;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return MEMBERSHIP_MAPPING;
  }

  @Override
  public Class<MembershipMapping> getEntityClass() {
    return MembershipMapping.class;
  }
}

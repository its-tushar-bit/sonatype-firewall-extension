/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.DescriptionHelper;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.MembershipMapping.MEMBERSHIP_MAPPING;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Role.ROLE;

/**
 * @since 1.7
 */
@Named
@Singleton
public class RoleDAO
    extends AbstractOperationalSqlDAO<Role>
{
  private final boolean testing;

  private final RolePermissionDAO rolePermissionDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final PolicyDAO policyDAO;

  public static final String DEVELOPER = "Developer";

  @Inject
  public RoleDAO(
      final OperationalDataStore operationalDataStore,
      final RolePermissionDAO rolePermissionDAO,
      final MembershipMappingDAO membershipMappingDAO,
      final PolicyDAO policyDAO)
  {
    this(false, operationalDataStore, rolePermissionDAO, membershipMappingDAO, policyDAO);
  }

  public RoleDAO(
      final boolean testing,
      final OperationalDataStore operationalDataStore,
      final RolePermissionDAO rolePermissionDAO,
      final MembershipMappingDAO membershipMappingDAO,
      final PolicyDAO policyDAO)
  {
    super(operationalDataStore);
    this.testing = testing;
    this.rolePermissionDAO = rolePermissionDAO;
    this.membershipMappingDAO = membershipMappingDAO;
    this.policyDAO = policyDAO;
  }

  @Override
  public int insert(TransactionContext tx, Role entity) {
    if (entity.isBuiltIn()) {
      throw new BadRequestException("Cannot add built-in role '" + entity.getName() + "'.");
    }
    if (entity.isGlobal() && !testing) {
      throw new BadRequestException("Cannot add custom role '" + entity.getName() + "' at global scope.");
    }

    NameHelper.validate(entity.getName());
    if (getByName(tx, entity.getName()) != null) {
      throw new InvalidNameException("A role with the same name already exists.");
    }
    DescriptionHelper.validate(entity.getDescription());

    return super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, Role entity) {
    // NOTE: We can't trust the caller-supplied built-in flag
    Role current = getByIdNotNull(tx, entity.getId());
    if (current.isBuiltIn()) {
      throw new BadRequestException("Cannot change built-in role '" + current.getName() + "'.");
    }
    if (entity.isGlobal()) {
      throw new BadRequestException("Cannot change custom role '" + current.getName() + "' to global scope.");
    }
    if (entity.isBuiltIn()) {
      throw new BadRequestException("Cannot change custom role '" + current.getName() + "' to built-in.");
    }

    NameHelper.validate(entity.getName());
    Role existing = getByName(tx, entity.getName());
    if (existing != null && !existing.getId().equals(entity.getId())) {
      throw new InvalidNameException("A role with the same name already exists.");
    }
    DescriptionHelper.validate(entity.getDescription());

    super.update(tx, entity);
  }

  @Override
  public void delete(TransactionContext tx, Role entity) {
    // NOTE: We can't trust the caller-supplied built-in flag
    Role current = getByIdNotNull(tx, entity.getId());
    if (current.isBuiltIn()) {
      throw new BadRequestException("Cannot delete built-in role '" + current.getName() + "'.");
    }

    // Cascade to permissions
    for (RolePermission rolePermission : rolePermissionDAO.getByRoleId(tx, entity.getId())) {
      rolePermissionDAO.delete(tx, rolePermission);
    }

    // Cascade to membership mappings
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByRoleId(tx, entity.getId())) {
      membershipMappingDAO.delete(tx, membershipMapping);
    }

    // Cascade to policy notify actions
    for (Policy policy : policyDAO.getAll(tx)) {
      boolean policyWasChanged = removeRoleNotificationsIfNeeded(entity, policy.getNotifications());
      Map<String, Notifications> notificationsOverrides = policy.getPolicyNotificationsOverrides();
      if (notificationsOverrides != null) {
        for (Notifications notificationsOverride : notificationsOverrides.values()) {
          policyWasChanged = removeRoleNotificationsIfNeeded(entity, notificationsOverride) || policyWasChanged;
        }
      }
      if (policyWasChanged) {
        policyDAO.update(tx, policy);
      }
    }

    super.delete(tx, entity);
  }

  private boolean removeRoleNotificationsIfNeeded(Role roleToRemove, Notifications notifications) {
    for (Iterator<RoleNotification> it = notifications.getRoleNotifications().iterator(); it.hasNext();) {
      RoleNotification notification = it.next();
      if (roleToRemove.getId().equals(notification.getRoleId())) {
        it.remove();
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the role with the given name.
   */
  public Role getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  private Role getByName(TransactionContext tx, String name) {
    return toEntity(tx.dsl()
        .selectFrom(ROLE)
        .where(ROLE.NAME_LOWERCASE_NO_WHITESPACE.eq(NameHelper.normalize(name)))
        .fetchOne());
  }

  /**
   * Gets all roles applicable to the entire system.
   */
  public List<Role> getGlobalRoles() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(ROLE)
          .where(ROLE.GLOBAL.eq(true))
          .orderBy(ROLE.NAME)
          .fetch()
          .map(this::toEntity);
    }
  }

  /**
   * Gets all roles applicable to an organization or application (non-global roles).
   */
  public List<Role> getApplicationRoles() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(ROLE)
          .where(ROLE.GLOBAL.eq(false))
          .orderBy(ROLE.NAME)
          .fetch()
          .map(this::toEntity);
    }
  }

  public Set<String> getObfuscatedRolesByUserCaseInsensitiveAndGroups(String username, Set<String> groups) {
    try (TransactionContext tx = createTransactionContext()) {
      // Build CASE expression: WHEN built_in THEN name ELSE 'CUSTOM'
      Field<String> obfuscatedRoleName = DSL.when(ROLE.BUILT_IN.eq(true), ROLE.NAME)
          .otherwise(DSL.inline("CUSTOM"));

      // User condition: case-insensitive username match with member_type = 'USER'
      var userCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq("USER")
          .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username)
              .or(DSL.lower(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toLowerCase()))
              .or(DSL.upper(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toUpperCase())));

      // Group condition: member_name IN groups with member_type = 'GROUP'
      var groupCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq("GROUP")
          .and(MEMBERSHIP_MAPPING.MEMBER_NAME.in(groups));

      // Combined condition
      var memberCondition = userCondition.or(groupCondition);

      return new HashSet<>(tx.dsl()
          .selectDistinct(obfuscatedRoleName)
          .from(ROLE)
          .innerJoin(MEMBERSHIP_MAPPING)
          .on(ROLE.ROLE_ID.eq(MEMBERSHIP_MAPPING.ROLE_ID))
          .where(memberCondition)
          .fetch(obfuscatedRoleName));
    }
  }

  /**
   * Gets all roles sorted by 'sortOrder'
   */
  @Override
  public List<Role> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(ROLE)
          .orderBy(ROLE.SORT_ORDER)
          .fetch()
          .map(this::toEntity);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return ROLE;
  }

  @Override
  public Class<Role> getEntityClass() {
    return Role.class;
  }
}

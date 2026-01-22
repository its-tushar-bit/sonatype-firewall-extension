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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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
  public void insert(TransactionContext tx, Role entity) {
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

    super.insert(tx, entity);
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
      membershipMappingDAO.delete(membershipMapping);
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
    for (Iterator<RoleNotification> it = notifications.getRoleNotifications().iterator(); it.hasNext(); ) {
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
    String sQuery = "SELECT entity FROM Role entity WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(tx, sQuery, NameHelper.normalize(name));
  }

  /**
   * Gets all roles applicable to the entire system.
   */
  public List<Role> getGlobalRoles() {
    String sQuery = "SELECT entity FROM Role entity WHERE entity.global=TRUE ORDER BY entity.name";
    return getList(sQuery);
  }

  /**
   * Gets all roles applicable to an organization or application.
   */
  public List<Role> getApplicationRoles() {
    String sQuery = "SELECT entity FROM Role entity WHERE entity.global=FALSE ORDER BY entity.name";
    return getList(sQuery);
  }

  public Set<String> getObfuscatedRolesByUserCaseInsensitiveAndGroups(String username, Set<String> groups) {
    String schm = getDatabaseSchema();

    return new HashSet<>(getListWithSqlInClause(groups, groupPartition -> {
      // This SQL query needs to work in H2 as well so we can't use array syntax
      String inParamString = createInParamString(groupPartition.size(), 2);
      Object[] params = new Object[1 + groupPartition.size()];
      params[0] = username;
      System.arraycopy(groupPartition.toArray(), 0, params, 1, groupPartition.size());
      String sQuery = "SELECT DISTINCT (CASE WHEN r.built_in THEN r.name ELSE 'CUSTOM' END) " +
          "FROM " + schm + ".role r INNER JOIN " + schm + ".membership_mapping mm ON r.role_id = mm.role_id " +
          "WHERE (" +
          "  (LOWER(mm.member_name) = LOWER(?1) OR UPPER(mm.member_name) = UPPER(?1) OR mm.member_name = ?1) " +
          "  AND mm.member_type = 'USER'" +
          ") OR (" +
          "  mm.member_name IN " + inParamString + " AND mm.member_type = 'GROUP'" +
          ")";

      return getScalarsNative(String.class, sQuery, params);
    }));
  }

  /**
   * Create a string like (?1, ?2, ?3) for use with a SQL IN clause. In raw SQL, the individual items in the IN
   * clause must be separate bound parameters.
   * @param size The number of items in the IN clause
   * @param initialArgIndex The index of the first bound parameter
   */
  private String createInParamString(int size, int initialArgIndex) {
    StringBuilder sb = new StringBuilder("(");
    for (int i = 0; i < size; i++) {
      sb.append("?").append(i + initialArgIndex);
      if (i < size - 1) {
        sb.append(",");
      }
    }

    sb.append(")");
    return sb.toString();
  }

  /**
   * Gets all roles sorted by 'nameLowercaseNoWhitespace'
   */
  @Override
  public List<Role> getAll() {
    String sQuery = "SELECT entity FROM Role entity ORDER BY entity.sortOrder";
    return getList(sQuery);
  }
}

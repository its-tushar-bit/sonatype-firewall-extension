/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Iterator;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class RoleDAO
    extends AbstractOperationalSqlDAO<Role>
{
  @Override
  protected Role getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM Role entity WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public Role getByIdNotNull(String id) {
    Role role = getById(id);
    if (role == null) {
      throw new NotFoundException("Cannot find a role with ID " + id + ".");
    }
    return role;
  }

  @Override
  public void delete(TransactionContext tx, Role entity) {
    // Cascade to permissions
    RolePermissionDAO rolePermissionDAO = new RolePermissionDAO();
    for (RolePermission rolePermission : rolePermissionDAO.getByRoleId(tx, entity.getId())) {
      rolePermissionDAO.delete(tx, rolePermission);
    }

    // Cascade to policy notify actions
    PolicyDAO policyDAO = new PolicyDAO();
    for (Policy policy : policyDAO.getAll(tx)) {
      boolean policyWasChanged = false;
      if (policy.getActions() != null) {
        for (List<Action> actions : policy.getActions().values()) {
          Iterator<Action> iterAction = actions.iterator();
          while (iterAction.hasNext()) {
            Action action = iterAction.next();
            if (NotifyActionType.ID.equals(action.getActionTypeId())
                && NotifyActionType.TARGET_TYPE_ROLE.equals(action.getTargetType())
                && entity.getId().equals(action.getTarget())) {
              iterAction.remove();
              policyWasChanged = true;
            }
          }
        }
      }
      if (policy.getMonitorNotifyActions() != null) {
        Iterator<NotifyAction> iterAction = policy.getMonitorNotifyActions().iterator();
        while (iterAction.hasNext()) {
          NotifyAction action = iterAction.next();
          if (NotifyActionType.TARGET_TYPE_ROLE.equals(action.getTargetType())
              && entity.getId().equals(action.getTarget())) {
            iterAction.remove();
            policyWasChanged = true;
          }
        }
        if (policyWasChanged) {
          policyDAO.update(tx, policy);
        }
      }
    }

    super.delete(tx, entity);
  }

  /**
   * Gets the role with the given name.
   */
  public Role getByName(String name) {
    String sQuery = "SELECT entity FROM Role entity WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(sQuery, NameHelper.normalize(name));
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

  /**
   * Gets all roles sorted by 'nameLowercaseNoWhitespace'
   */
  public List<Role> getAll() {
    String sQuery = "SELECT entity FROM Role entity ORDER BY entity.nameLowercaseNoWhitespace";
    return getList(sQuery);
  }
}

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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.7
 */
@Named
@Singleton
public class MembershipMappingDAO
    extends AbstractOperationalSqlDAO<MembershipMapping>
{
  @Inject
  public MembershipMappingDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
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
    String sQuery = "SELECT entity FROM MembershipMapping entity WHERE entity.contextId=?1"
        + " ORDER BY entity.roleId, entity.memberName";
    return getList(tx, sQuery, contextId);
  }

  List<MembershipMapping> getByUser(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUser(tx, username);
    }
  }

  List<MembershipMapping> getByUser(TransactionContext tx, String username) {
    String sQuery = "SELECT entity FROM MembershipMapping entity"
        + " WHERE entity.memberName=?1 and entity.memberType=?2" + " ORDER BY entity.contextId, entity.roleId";
    return getList(tx, sQuery, username, MemberType.USER);
  }

  public List<MembershipMapping> getByUserCaseInsensitiveAndGroups(String username, Set<String> groupNames) {
    String sQuery = "SELECT entity FROM MembershipMapping entity" +
        " WHERE (" +
        "(entity.memberName=?1" +
        " OR LOWER(entity.memberName)=LOWER(?1)" +
        " OR UPPER(entity.memberName)=UPPER(?1))" +
        " AND entity.memberType=?2" +
        ")" +
        " OR (entity.memberName IN ?3 AND entity.memberType=?4)" +
        " ORDER BY entity.contextId, entity.roleId";

    return getList(sQuery, username, MemberType.USER, groupNames, MemberType.GROUP);
  }

  List<MembershipMapping> getByContextIdAndUser(String contextId, String username) {
    String sQuery = "SELECT entity FROM MembershipMapping entity"
        + " WHERE entity.contextId=?1 and entity.memberName=?2 and entity.memberType=?3" + " ORDER BY entity.roleId";
    return getList(sQuery, contextId, username, MemberType.USER);
  }

  public List<MembershipMapping> getByContextIdAndRoleId(TransactionContext tx, String contextId, String roleId) {
    String sQuery = "SELECT entity FROM MembershipMapping entity" + " WHERE entity.contextId=?1 and entity.roleId=?2"
        + " ORDER BY entity.memberName";
    return getList(tx, sQuery, contextId, roleId);
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
    String sQuery =
        "SELECT mm FROM MembershipMapping mm " +
            "WHERE mm.contextId=?1 " +
            "and mm.roleId=?2 " +
            "and mm.memberName=?3 " +
            "and mm.memberType=?4";
    return get(sQuery, contextId, roleId, memberName, memberType);
  }

  /**
   * Sets the membership mappings for a given context and role.
   */
  public void setMembershipMappingsForContextAndRole(String contextId,
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
  public void setMembershipMappingsForContextAndRole(TransactionContext tx,
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
  public void update(TransactionContext tx, MembershipMapping entity) {
    throw new UnsupportedOperationException("Use setMembershipMappingsForContextAndRole() instead");
  }

  /**
   * @since 1.15.0
   */
  public List<MembershipMapping> getByRoleIds(Set<String> roleIds) {
    if (roleIds.isEmpty()) {
      return Collections.emptyList();
    }
    String sQuery = "SELECT entity FROM MembershipMapping entity" //
        + " WHERE entity.roleId IN ?1";
    return getList(sQuery, roleIds);
  }

  /**
   * @since 1.15.0
   */
  public List<MembershipMapping> getByRoleId(TransactionContext tx, String roleId) {
    String sQuery = "SELECT entity FROM MembershipMapping entity" //
        + " WHERE entity.roleId=?1";
    return getList(tx, sQuery, roleId);
  }

  public boolean isSystemAdmin(String username) {
    String sQuery = "SELECT count(entity) FROM MembershipMapping entity" +
        " WHERE entity.memberName=?1 and entity.roleId=?2";
    return getSingle(Long.class, sQuery, username, Role.SYSTEM_ADMIN_ROLE_ID) > 0;
  }
}

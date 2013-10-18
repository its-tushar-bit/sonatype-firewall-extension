/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;

/**
 * @since 1.7
 */
public class MembershipMappingDAO
    extends AbstractOperationalSqlDAO<MembershipMapping>
{
  /**
   * Gets the membership mappings for a given context.
   */
  public List<MembershipMapping> getByContextId(String contextId) {
    EntityManager em = createEntityManager();
    try {
      return getByContextId(em, contextId);
    }
    finally {
      close(em);
    }
  }

  /**
   * Gets the membership mappings for a given context.
   */
  public List<MembershipMapping> getByContextId(EntityManager em, String contextId) {
    String sQuery = "SELECT entity FROM MembershipMapping entity WHERE entity.contextId=?1"
        + " ORDER BY entity.roleId, entity.memberName";
    return getList(em, sQuery, contextId);
  }

  List<MembershipMapping> getByUser(String username) {
    EntityManager em = createEntityManager();
    try {
      return getByUser(em, username);
    }
    finally {
      close(em);
    }
  }

  List<MembershipMapping> getByUser(EntityManager em, String username) {
    String sQuery = "SELECT entity FROM MembershipMapping entity"
        + " WHERE entity.memberName=?1 and entity.memberType=?2" + " ORDER BY entity.contextId, entity.roleId";
    return getList(em, sQuery, username, MemberType.USER);
  }

  List<MembershipMapping> getByContextIdAndUser(String contextId, String username) {
    String sQuery = "SELECT entity FROM MembershipMapping entity"
        + " WHERE entity.contextId=?1 and entity.memberName=?2 and entity.memberType=?3" + " ORDER BY entity.roleId";
    return getList(sQuery, contextId, username, MemberType.USER);
  }

  private List<MembershipMapping> getByContextIdAndRoleId(EntityManager em, String contextId, String roleId) {
    String sQuery = "SELECT entity FROM MembershipMapping entity" + " WHERE entity.contextId=?1 and entity.roleId=?2"
        + " ORDER BY entity.memberName";
    return getList(em, sQuery, contextId, roleId);
  }

  /**
   * Sets the membership mappings for a given context and role.
   */
  public void setMembershipMappingsForContextAndRole(String contextId, String roleId, List<MembershipMapping> mappings)
  {
    EntityManager em = createEntityManager();
    try {
      em.getTransaction().begin();

      Map<String, MembershipMapping> mappingsByMember = new HashMap<String, MembershipMapping>();
      for (MembershipMapping existingMapping : getByContextIdAndRoleId(em, contextId, roleId)) {
        mappingsByMember.put(getMemberKey(existingMapping), existingMapping);
      }

      // Create new values
      for (MembershipMapping newMapping : mappings) {
        newMapping.setContextId(contextId);
        newMapping.setRoleId(roleId);

        String memberKey = getMemberKey(newMapping);
        if (!mappingsByMember.containsKey(memberKey)) {
          newMapping.setId(null);
          insert(em, newMapping);
        }
        mappingsByMember.put(memberKey, null);
      }

      // Delete old values
      for (MembershipMapping oldMapping : mappingsByMember.values()) {
        if (oldMapping != null) {
          delete(em, oldMapping);
        }
      }

      em.getTransaction().commit();
    }
    finally {
      close(em);
    }
  }

  private String getMemberKey(MembershipMapping mapping) {
    return mapping.getMemberType() + ":" + mapping.getMemberName();
  }

  @Override
  public void update(EntityManager em, MembershipMapping entity) {
    throw new UnsupportedOperationException("Use setMembershipMappingsForContextAndRole() instead");
  }
}

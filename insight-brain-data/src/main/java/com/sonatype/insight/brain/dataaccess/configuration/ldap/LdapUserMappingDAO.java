/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.Collection;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class LdapUserMappingDAO
    extends AbstractOperationalSqlDAO<LdapUserMapping>
{
  /**
   * @since 1.7
   */
  @Override
  public LdapUserMapping getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LdapUserMapping entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  /**
   * @since 1.7
   */
  public LdapUserMapping getByIdNotNull(String id) {
    LdapUserMapping umap = getById(id);
    if (umap == null) {
      throw new NotFoundException("Cannot find LdapUserMapping with id " + id + ".");
    }
    return umap;
  }

  /**
   * @since 1.7
   */
  public LdapUserMapping getByServerId(String serverId) {
    EntityManager em = createEntityManager();
    try {
      return getByServerId(em, serverId);
    }
    finally {
      em.close();
    }
  }

  /**
   * @since 1.7
   */
  public LdapUserMapping getByServerId(EntityManager em, String serverId) {
    String sQuery = "SELECT entity FROM LdapUserMapping entity" + //
        " WHERE entity.serverId=?1";
    return get(em, sQuery, serverId);
  }

  /**
   * @since 1.7
   */
  public void deleteByServerId(EntityManager em, String id) {
    LdapUserMapping umap = getByServerId(em, id);
    if (umap != null) {
      delete(em, umap);
    }
  }

  /**
   * @since 1.7
   */
  public Collection<LdapUserMapping> getAll() {
    String sQuery = "SELECT entity FROM LdapUserMapping entity";
    return getList(sQuery);
  }

}

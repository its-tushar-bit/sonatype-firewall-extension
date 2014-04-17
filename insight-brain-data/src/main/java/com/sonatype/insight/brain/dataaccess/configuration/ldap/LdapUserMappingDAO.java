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

/**
 * @since 1.7
 */
public class LdapUserMappingDAO
    extends AbstractOperationalSqlDAO<LdapUserMapping>
{
  @Override
  public LdapUserMapping getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LdapUserMapping entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public LdapUserMapping getByServerId(String serverId) {
    EntityManager em = createEntityManager();
    try {
      return getByServerId(em, serverId);
    }
    finally {
      em.close();
    }
  }

  private LdapUserMapping getByServerId(EntityManager em, String serverId) {
    String sQuery = "SELECT entity FROM LdapUserMapping entity" + //
        " WHERE entity.serverId=?1";
    return get(em, sQuery, serverId);
  }

  public void deleteByServerId(EntityManager em, String id) {
    LdapUserMapping umap = getByServerId(em, id);
    if (umap != null) {
      delete(em, umap);
    }
  }

  public Collection<LdapUserMapping> getAll() {
    String sQuery = "SELECT entity FROM LdapUserMapping entity";
    return getList(sQuery);
  }

}

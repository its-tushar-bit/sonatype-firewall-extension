/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class LdapConnectionDAO
    extends AbstractOperationalSqlDAO<LdapConnection>
{

  @Override
  public LdapConnection getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LdapConnection entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public LdapConnection getByIdNotNull(String id) {
    LdapConnection conn = getById(id);
    if (conn == null) {
      throw new NotFoundException("Cannot find LdapConnection with id " + id + ".");
    }
    return conn;
  }

  public LdapConnection getByServerIdNotNull(String serverId) {
    EntityManager em = createEntityManager();
    try {
      LdapConnection conn = getByServerId(em, serverId);
      if (conn == null) {
        throw new NotFoundException("Cannot find LdapConnection for LdapServer with id " + serverId + ".");
      }
      return conn;
    }
    finally {
      em.close();
    }
  }

  public LdapConnection getByServerId(EntityManager em, String serverId) {
    String sQuery = "SELECT entity FROM LdapConnection entity" + //
        " WHERE entity.serverId=?1";
    return get(em, sQuery, serverId);
  }

  public void deleteByServerId(EntityManager em, String id) {
    LdapConnection conn = getByServerId(em, id);
    if (conn != null) {
      delete(em, conn);
    }
  }

}

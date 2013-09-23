/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class LdapServerDAO
    extends AbstractOperationalSqlDAO<LdapServer>
{

  @Override
  public LdapServer getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LdapServer entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public LdapServer getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }

  public LdapServer getByIdNotNull(EntityManager em, String id) {
    LdapServer config = getById(em, id);
    if (config == null) {
      throw new NotFoundException("Cannot find LdapServer with id " + id + ".");
    }
    return config;
  }

  private LdapServer getByName(EntityManager em, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The LdapServer name cannot be null or empty.");
    }
    // LdapConfiguration Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM LdapServer entity" + //
        " WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(em, sQuery, name);
  }

  public LdapServer getByName(String name) {
    EntityManager em = createEntityManager();
    try {
      return getByName(em, name);
    }
    finally {
      close(em);
    }
  }

  public List<LdapServer> getAll() {
    String sQuery = "SELECT entity FROM LdapServer entity" + //
        " ORDER BY entity.name";
    return getList(sQuery);
  }

  @Override
  public void insert(EntityManager em, LdapServer config) {
    NameHelper.validate(config.getName());

    if (getByName(em, config.getName()) != null) {
      throw new InvalidNameException(config.getName() + " is already used as a name.");
    }

    super.insert(em, config);
  }

  @Override
  public void update(EntityManager em, LdapServer config) {
    NameHelper.validate(config.getName());

    LdapServer existingConfig = getByName(em, config.getName());
    if (existingConfig != null && !existingConfig.getId().equals(config.getId())) {
      throw new InvalidNameException(config.getName() + " is already used as a name.");
    }

    super.update(em, config);
  }

  
  @Override
  public void delete(EntityManager em, LdapServer entity) {
    new LdapConnectionDAO().deleteByServerId(em, entity.getId());
    new LdapUserMappingDAO().deleteByServerId(em, entity.getId());
    super.delete(em, entity);
  }
}

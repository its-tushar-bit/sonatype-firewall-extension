/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.configuration.ldap.LdapConfiguration;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class LdapConfigurationDAO
    extends AbstractOperationalSqlDAO<LdapConfiguration>
{

  @Override
  public LdapConfiguration getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LdapConfiguration entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public LdapConfiguration getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }

  public LdapConfiguration getByIdNotNull(EntityManager em, String id) {
    LdapConfiguration config = getById(em, id);
    if (config == null) {
      throw new NotFoundException("Cannot find LdapConfiguration with id " + id + ".");
    }
    return config;
  }

  private LdapConfiguration getByName(EntityManager em, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The LdapConfiguration name cannot be null or empty.");
    }
    // LdapConfiguration Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM LdapConfiguration entity" + //
        " WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(em, sQuery, name);
  }

  public LdapConfiguration getByName(String name) {
    EntityManager em = createEntityManager();
    try {
      return getByName(em, name);
    }
    finally {
      close(em);
    }
  }

  public List<LdapConfiguration> getAll() {
    String sQuery = "SELECT entity FROM LdapConfiguration entity" + //
        " ORDER BY entity.name";
    return getList(sQuery);
  }

  @Override
  public void insert(EntityManager em, LdapConfiguration config) {
    NameHelper.validate(config.getName());

    if (getByName(em, config.getName()) != null) {
      throw new InvalidNameException(config.getName() + " is already used as a name.");
    }

    super.insert(em, config);
  }

  @Override
  public void update(EntityManager em, LdapConfiguration config) {
    NameHelper.validate(config.getName());

    LdapConfiguration existingConfig = getByName(em, config.getName());
    if (existingConfig != null && !existingConfig.getId().equals(config.getId())) {
      throw new InvalidNameException(config.getName() + " is already used as a name.");
    }

    super.update(em, config);
  }

}

/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.List;

import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class LdapServerDAO
    extends AbstractOperationalSqlDAO<LdapServer>
{

  @Override
  public LdapServer getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM LdapServer entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public LdapServer getByIdNotNull(String id) {
    LdapServer config = getById(id);
    if (config == null) {
      throw new NotFoundException("Cannot find LdapServer with ID " + id + ".");
    }
    return config;
  }

  private LdapServer getByName(TransactionContext tx, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The LdapServer name cannot be null or empty.");
    }
    // LdapConfiguration Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM LdapServer entity" + //
        " WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(tx, sQuery, name);
  }

  public LdapServer getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  public List<LdapServer> getAll() {
    String sQuery = "SELECT entity FROM LdapServer entity" + //
        " ORDER BY entity.name";
    return getList(sQuery);
  }

  @Override
  public void insert(TransactionContext tx, LdapServer config) {
    NameHelper.validate(config.getName());

    if (getByName(tx, config.getName()) != null) {
      throw new InvalidNameException(config.getName() + " is already used as a name.");
    }

    super.insert(tx, config);
  }

  @Override
  public void update(TransactionContext tx, LdapServer config) {
    NameHelper.validate(config.getName());

    LdapServer existingConfig = getByName(tx, config.getName());
    if (existingConfig != null && !existingConfig.getId().equals(config.getId())) {
      throw new InvalidNameException(config.getName() + " is already used as a name.");
    }

    super.update(tx, config);
  }

  
  @Override
  public void delete(TransactionContext tx, LdapServer entity) {
    new LdapConnectionDAO().deleteByServerId(tx, entity.getId());
    new LdapUserMappingDAO().deleteByServerId(tx, entity.getId());
    super.delete(tx, entity);
  }
}

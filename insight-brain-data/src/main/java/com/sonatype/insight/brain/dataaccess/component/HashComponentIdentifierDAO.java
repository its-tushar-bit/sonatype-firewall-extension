/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import javax.persistence.EntityManager;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.error.exception.BadRequestException;

public class HashComponentIdentifierDAO
    extends AbstractOperationalSqlDAO<HashComponentIdentifier>
{
  @Override
  protected HashComponentIdentifier getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM HashComponentIdentifier entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public HashComponentIdentifier getByHash(String hash) {
    EntityManager em = createEntityManager();
    try {
      return getByHash(em, hash);
    }
    finally {
      close(em);
    }
  }

  private HashComponentIdentifier getByHash(EntityManager em, String hash) {
    // Note that our truncated representation of the hash is what is stored, hence the transformation on the input
    // hash.
    String sQuery = "SELECT entity FROM HashComponentIdentifier entity" + //
        " WHERE entity.hash=?1";
    return get(em, sQuery, HashHelper.truncateHash(hash));
  }

  private HashComponentIdentifier getByComponentIdentifier(EntityManager em, ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM HashComponentIdentifier entity" + //
        " WHERE entity.componentIdFormat=?1 and entity.componentIdCoordinatesJson=?2";
    return get(em, sQuery, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  @Override
  public void insert(EntityManager em, HashComponentIdentifier entity) {
    HashComponentIdentifier other = getByHash(em, entity.getHash());
    if (other != null) {
      throw new BadRequestException("This component is already mapped to '"
          + ComponentDisplayNameUtil.fromIdentifier(other.getComponentIdentifier()) + "'.");
    }
    other = getByComponentIdentifier(em, entity.getComponentIdentifier());
    if (other != null) {
      throw new BadRequestException("Another component is already mapped to '"
          + ComponentDisplayNameUtil.fromIdentifier(other.getComponentIdentifier()) + "'.");
    }
    super.insert(em, entity);
  }

  public HashComponentIdentifier getByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    EntityManager em = createEntityManager();
    try {
      return getByComponentIdentifier(em, componentIdentifier);
    }
    finally {
      close(em);
    }
  }
}

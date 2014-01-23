/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.error.exception.BadRequestException;

public class HashGAVDAO
    extends AbstractOperationalSqlDAO<HashGAV>
{
  @Override
  protected HashGAV getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM HashGAV entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public HashGAV getByHash(String hash) {
    EntityManager em = createEntityManager();
    try {
      return getByHash(em, hash);
    }
    finally {
      close(em);
    }
  }

  private HashGAV getByHash(EntityManager em, String hash) {
    // Note that our truncated representation of the hash is what is stored, hence the transformation on the input
    // hash.
    String sQuery = "SELECT entity FROM HashGAV entity" + //
        " WHERE entity.hash=?1";
    return get(em, sQuery, HashHelper.truncateHash(hash));
  }

  private HashGAV getByGAVEC(EntityManager em, String groupId, String artifactId, String version, String extension,
      String classifier)
  {
    String sQuery = "SELECT entity FROM HashGAV entity" + //
        " WHERE entity.groupId=?1 AND entity.artifactId=?2 AND entity.version=?3 AND entity.extension=?4 AND entity.classifier=?5";
    return get(em, sQuery, groupId, artifactId, version, extension, classifier);
  }

  @Override
  public void insert(EntityManager em, HashGAV entity) {
    HashGAV other = getByHash(em, entity.getHash());
    if (other != null) {
      throw new BadRequestException("This component is already mapped to '" + other.getGAVECString() + "'");
    }
    other = getByGAVEC(em, entity.getGroupId(), entity.getArtifactId(), entity.getVersion(), entity.getExtension(),
        entity.getClassifier());
    if (other != null) {
      throw new BadRequestException("Another component is already mapped to '" + other.getGAVECString() + "'");
    }
    super.insert(em, entity);
  }

  @Override
  public void update(EntityManager em, HashGAV entity) {
    throw new UnsupportedOperationException();
  }

  public HashGAV getByGAV(String groupId, String artifactId, String version) {
    EntityManager em = createEntityManager();
    try {
      return getByGAV(em, groupId, artifactId, version);
    }
    finally {
      close(em);
    }
  }

  private HashGAV getByGAV(EntityManager em, String groupId, String artifactId, String version) {
    String sQuery = "SELECT entity FROM HashGAV entity" + //
        " WHERE entity.groupId=?1 AND entity.artifactId=?2 AND entity.version=?3" + //
        "       AND entity.extension IS NULL AND entity.classifier IS NULL";
    return get(em, sQuery, groupId, artifactId, version);
  }
}

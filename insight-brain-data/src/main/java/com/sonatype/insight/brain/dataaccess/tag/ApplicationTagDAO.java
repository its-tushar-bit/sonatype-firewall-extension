/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.tag.ApplicationTag;

/**
 * @since 1.9
 */
public class ApplicationTagDAO
    extends AbstractOperationalSqlDAO<ApplicationTag>
{
  @Override
  protected ApplicationTag getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  @Override
  public void update(EntityManager em, ApplicationTag appTag) {
    throw new UnsupportedOperationException("ApplicationTag table does not support update operations");
  }

  public List<ApplicationTag> getByApplicationId(String appId) {
    EntityManager em = createEntityManager();
    try {
      return getByApplicationId(em, appId);
    }
    finally {
      close(em);
    }
  }

  public List<ApplicationTag> getByApplicationId(EntityManager em, String appId) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.applicationId=?1";
    return getList(em, sQuery, appId);
  }

  public ApplicationTag getByApplicationIdAndTagId(String appId, String tagId) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.applicationId=?1 AND entity.tagId=?2";
    return get(sQuery, appId, tagId);
  }

  public List<ApplicationTag> getByTagId(String tagId) {
    EntityManager em = createEntityManager();
    try {
      return getByTagId(em, tagId);
    }
    finally {
      close(em);
    }
  }

  public List<ApplicationTag> getByTagId(EntityManager em, String tagId) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.tagId=?1";
    return getList(em, sQuery, tagId);
  }
}

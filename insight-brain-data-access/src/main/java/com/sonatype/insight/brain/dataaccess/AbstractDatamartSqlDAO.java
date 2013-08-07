/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractDatamartSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  private EntityManagerFactory entityManagerFactory = DatamartProvider.getJPAEntityManagerFactory();

  @Override
  public EntityManager createEntityManager() {
    return entityManagerFactory.createEntityManager();
  }
}

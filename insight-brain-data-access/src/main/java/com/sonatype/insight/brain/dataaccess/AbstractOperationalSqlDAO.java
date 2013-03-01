/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractOperationalSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
    private EntityManagerFactory entityManagerFactory = OperationalDataStoreProvider.getJPAEntityManagerFactory();

    @Override
    public EntityManager createEntityManager()
    {
        return entityManagerFactory.createEntityManager();
    }
}

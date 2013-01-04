/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractSqlDAO<T extends HasStringId>
    extends AbstractDAO<T>
{
    private EntityManagerFactory entityManagerFactory = OperationalEntityManagerFactoryProvider.get();

    @Override
    public EntityManager createEntityManager()
    {
        return entityManagerFactory.createEntityManager();
    }

    private String newUUID()
    {
        return UUID.randomUUID().toString().replace( "-", "" );
    }

    @Override
    public void insert( EntityManager em, T entity )
    {
        String id = entity.getId();
        if ( id == null || id.trim().isEmpty() )
        {
            entity.setId( newUUID() );
        }
        super.insert( em, entity );
    }
}

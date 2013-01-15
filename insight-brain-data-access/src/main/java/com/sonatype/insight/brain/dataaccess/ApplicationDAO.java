/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.error.exception.NotFoundException;

public class ApplicationDAO
    extends AbstractSqlDAO<Application>
{
    @Override
    protected Application getById( EntityManager em, String id )
    {
        String sQuery = "SELECT entity FROM Application entity" + //
            " WHERE entity.id=?1";
        return get( em, sQuery, id );
    }

    public Application getByIdNotNull( String id )
    {
        Application application = getById( id );
        if ( application == null )
        {
            throw new NotFoundException( "Cannot find application with id " + id );
        }
        return application;
    }

    public Application getOrInsertByPublicId( String publicId )
    {
        Application application = getByPublicId( publicId );
        if ( application == null )
        {
            application = new Application();
            application.setPublicId( publicId );
            insert( application );
        }
        return application;
    }

    public Application getByPublicId( String publicId )
    {
        if ( publicId == null || publicId.trim().isEmpty() )
        {
            throw new DataAccessException( "The application public ID cannot be null or empty." );
        }

        publicId = publicId.trim();
        String sQuery = "SELECT entity FROM Application entity" + //
            " WHERE entity.publicId=?1";
        return get( sQuery, publicId );
    }

    public Application getByPublicIdNotNull( String publicId )
    {
        Application application = getByPublicId( publicId );
        if ( application == null )
        {
            throw new NotFoundException( "Cannot find application with public id " + publicId );
        }
        return application;
    }
}

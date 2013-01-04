/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.Application;

public class ApplicationDAO
    extends AbstractSqlDAO<Application>
{
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

    private Application getByPublicId( String publicId )
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
}

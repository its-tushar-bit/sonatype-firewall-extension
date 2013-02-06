/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.error.exception.NotFoundException;

public class ApplicationDAO
    extends AbstractOperationalSqlDAO<Application>
{
    public static final int DEFAULT_LICENSE_THREAT_GROUP_COUNT = 4;

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
        EntityManager em = createEntityManager();
        try
        {
            em.getTransaction().begin();
            Application application = getByPublicId( em, publicId );
            if ( application == null )
            {
                application = new Application();
                application.setPublicId( publicId );
                insert( em, application );
            }
            em.getTransaction().commit();
            return application;
        }
        finally
        {
            close( em );
        }
    }

    private Application getByPublicId( EntityManager em, String publicId )
    {
        if ( publicId == null || publicId.trim().isEmpty() )
        {
            throw new DataAccessException( "The application public ID cannot be null or empty." );
        }

        publicId = publicId.trim();
        String sQuery = "SELECT entity FROM Application entity" + //
            " WHERE entity.publicId=?1";
        return get( em, sQuery, publicId );
    }

    public Application getByPublicId( String publicId )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByPublicId( em, publicId );
        }
        finally
        {
            close( em );
        }
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

    @Override
    public void insert( EntityManager em, Application application )
    {
        super.insert( em, application );

        new LicenseThreatGroupDAO().createDefaultGroups( em, application.getId() );
    }

    @Override
    public void delete( EntityManager em, Application application )
    {
        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByApplicationId( application.getId() );
        for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroups )
        {
            licenseThreatGroupDAO.delete( em, licenseThreatGroup );
        }
        super.delete( em, application );
    }
}

/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.error.exception.NotFoundException;

public class ApplicationDAO
    extends AbstractOperationalSqlDAO<Application>
{
    private static final Logger log = LoggerFactory.getLogger( ApplicationDAO.class );

    @Override
    public Application getById( EntityManager em, String id )
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
            throw new NotFoundException( "Cannot find application with id " + id + "." );
        }
        return application;
    }

    private Application getByPublicId( EntityManager em, String publicId )
    {
        if ( publicId == null || publicId.trim().isEmpty() )
        {
            throw new DataAccessException( "The application public ID cannot be null or empty." );
        }

        publicId = publicId.trim().toLowerCase( Locale.ENGLISH );
        String sQuery = "SELECT entity FROM Application entity" + //
            " WHERE entity.publicIdLowercase=?1";
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
            throw new NotFoundException( "Cannot find application with public id " + publicId + "." );
        }
        return application;
    }

    private Application getByName( EntityManager em, String name )
    {
        if ( name == null || name.trim().isEmpty() )
        {
            throw new DataAccessException( "The application name cannot be null or empty." );
        }
        // Application Name is whitespace and case insensitive
        name = NameHelper.normalize( name );
        String sQuery = "SELECT entity FROM Application entity WHERE entity.nameLowercaseNoWhitespace=?1";
        return get( em, sQuery, name );
    }

    public Application getByName( String name )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByName( em, name );
        }
        finally
        {
            close( em );
        }
    }

    public List<Application> getAll( EntityManager em )
    {
        String sQuery = "SELECT entity FROM Application entity" + //
            " ORDER BY entity.publicIdLowercase";
        return getList( em, sQuery );
    }

    public List<Application> getAll()
    {
        EntityManager em = createEntityManager();
        try
        {
            return getAll( em );
        }
        finally
        {
            close( em );
        }
    }

    public List<Application> getByOrganizationId( EntityManager em, String organizationId )
    {
        String sQuery = "SELECT entity FROM Application entity" + //
            " WHERE entity.organizationId=?1" + //
            " ORDER BY entity.publicIdLowercase";
        return getList( em, sQuery, organizationId );
    }

    @Override
    public void insert( EntityManager em, Application application )
    {
        validate( application );

        if ( getByName( em, application.getName() ) != null )
        {
            throw new InvalidNameException( application.getName() + " is already used as a name." );
        }
        if ( getByPublicId( em, application.getPublicId() ) != null )
        {
            throw new InvalidApplicationException( application.getPublicId() + " is already used as an ID." );
        }

        super.insert( em, application );
    }

    @Override
    public void update( EntityManager em, Application application )
    {
        validate( application );

        Application existingApplication = getById( em, application.getId() );
        if ( existingApplication == null )
        {
            throw new InvalidApplicationException( "Attempting to edit an application that doesn't exist. ID "
                + application.getPublicId() );
        }
        if ( !existingApplication.getPublicId().equals( application.getPublicId() ) )
        {
            throw new InvalidApplicationException( "Cannot change Public ID of existing application." );
        }
        if ( existingApplication.getOrganizationId() != null
            && !existingApplication.getOrganizationId().equals( application.getOrganizationId() ) )
        {
            throw new InvalidApplicationException( "Cannot change the parent organization of an application." );
        }
        existingApplication = getByName( em, application.getName() );
        if ( existingApplication != null && !existingApplication.getId().equals( application.getId() ) )
        {
            throw new InvalidNameException( application.getName() + " is already used as a name." );
        }
        existingApplication = getByPublicId( em, application.getPublicId() );
        if ( existingApplication != null && !existingApplication.getId().equals( application.getId() ) )
        {
            throw new InvalidApplicationException( application.getPublicId() + " is already used as an ID." );
        }

        super.update( em, application );
    }

    public void deleteWithIcon( Application application, File iconDirectory )
    {
        File applicationIconDirectory = new File( iconDirectory, application.getId() );
        try
        {
            FileUtils.deleteDirectory( applicationIconDirectory );
        }
        catch ( IOException e )
        {
            log.error( "Could not delete application icons: {}" + applicationIconDirectory, e );
        }

        delete( application );
    }

    @Override
    public void delete( EntityManager em, Application application )
    {
        // Cascade to license threat groups
        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        List<LicenseThreatGroup> licenseThreatGroups =
            licenseThreatGroupDAO.getByOwnerId( em, application.getId() );
        for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroups )
        {
            licenseThreatGroupDAO.delete( em, licenseThreatGroup );
        }

        // Cascade to labels
        LabelDAO labelDAO = new LabelDAO();
        List<Label> labels = labelDAO.getByApplicationId( em, application.getId() );
        for ( Label label : labels )
        {
            labelDAO.delete( em, label );
        }

        super.delete( em, application );
    }

    public void setIcon( String applicationId, File iconDirectory, InputStream imageStream )
        throws IOException
    {
        new IconDAO().setIcon( applicationId, iconDirectory, imageStream );
    }

    public byte[] getIcon( String applicationId, File iconDirectory )
        throws IOException
    {
        return new IconDAO().getIcon( applicationId, iconDirectory );
    }

    private void validate( Application application )
    {
        NameHelper.validate( application.getName() );

        final String applicationPublicId = application.getPublicId();
        if ( applicationPublicId == null || applicationPublicId.trim().isEmpty() )
        {
            throw new InvalidApplicationException( "ID is required." );
        }
    }
}

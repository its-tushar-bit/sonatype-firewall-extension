/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
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
            throw new NotFoundException( "Cannot find application with public id " + publicId );
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
        name = name.replaceAll( "\\s", "" ).toLowerCase();
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

    public List<Application> getAll()
    {
        String sQuery = "SELECT entity FROM Application entity" + //
            " ORDER BY entity.publicIdLowercase";
        return getList( sQuery );
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

    public void setIcon( String applicationId, File iconDirectory, InputStream imageStream )
        throws IOException, IllegalArgumentException
    {
        final int dimension = 36;
        Image image = ImageIO.read( imageStream );
        BufferedImage resizedImage = new BufferedImage( dimension, dimension, BufferedImage.TYPE_BYTE_INDEXED );
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage( image, 0, 0, dimension, dimension, null );
        g.dispose();

        File applicationIconDirectory = new File( iconDirectory, applicationId );
        if ( !applicationIconDirectory.exists() )
        {
            applicationIconDirectory.mkdirs();
        }

        File iconFile = new File( applicationIconDirectory, "icon36px.png" );
        if ( !iconFile.exists() )
        {
            iconFile.createNewFile();
        }

        ImageIO.write( resizedImage, "png", iconFile );
    }

    public byte[] getIcon( String applicationId, File iconDirectory )
        throws IOException
    {
        File applicationIconDirectory = new File( iconDirectory, applicationId );
        if ( !applicationIconDirectory.exists() )
        {
            return null;
        }
        File iconFile = new File( applicationIconDirectory, "icon36px.png" );
        if ( !iconFile.exists() )
        {
            return null;
        }

        BufferedImage image = ImageIO.read( iconFile );
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write( image, "png", byteArrayOutputStream );
        return byteArrayOutputStream.toByteArray();
    }
}

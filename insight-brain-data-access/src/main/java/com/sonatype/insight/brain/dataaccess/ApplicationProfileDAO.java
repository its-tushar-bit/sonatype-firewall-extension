/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.model.ApplicationProfile;
import com.sonatype.insight.error.exception.NotFoundException;

public class ApplicationProfileDAO
    extends AbstractOperationalSqlDAO<ApplicationProfile>
{
    public static final int DEFAULT_LICENSE_THREAT_GROUP_COUNT = 4;

    @Override
    protected ApplicationProfile getById( EntityManager em, String id )
    {
        String sQuery = "SELECT entity FROM ApplicationProfile entity" + //
            " WHERE entity.id=?1";
        return get( em, sQuery, id );
    }

    public ApplicationProfile getByIdNotNull( String id )
    {
        ApplicationProfile applicationProfile = getById( id );
        if ( applicationProfile == null )
        {
            throw new NotFoundException( "Cannot find an application profile with id " + id );
        }
        return applicationProfile;
    }

    public List<ApplicationProfile> getAll()
    {
        String sQuery = "SELECT entity FROM ApplicationProfile entity" + //
            " ORDER BY entity.nameLowercaseNoWhitespace";
        return getList( sQuery );
    }

    @Override
    public void insert( EntityManager em, ApplicationProfile applicationProfile )
    {
        validateName( applicationProfile.getName() );

        if ( getByName( em, applicationProfile.getName() ) != null )
        {
            throw new InvalidApplicationProfileException( "An application profile with the same name already exists." );
        }

        super.insert( em, applicationProfile );
    }

    @Override
    public void update( EntityManager em, ApplicationProfile applicationProfile )
    {
        validateName( applicationProfile.getName() );

        ApplicationProfile otherApplicationProfile = getByName( em, applicationProfile.getName() );
        if ( otherApplicationProfile != null && !otherApplicationProfile.getId().equals( applicationProfile.getId() ) )
        {
            throw new InvalidApplicationProfileException( "An application profile with the same name already exists." );
        }

        super.update( em, applicationProfile );
    }

    private void validateName( String name )
    {
        if ( name == null || name.trim().isEmpty() )
        {
            throw new InvalidApplicationProfileException( "Name is required." );
        }
        for ( char c : name.toCharArray() )
        {
            if ( !Character.isLetterOrDigit( c ) && c != '-' && c != ' ' )
            {
                throw new InvalidApplicationProfileException( "Name must be alpha numeric." );
            }
        }
        if ( name.startsWith( " " ) || name.endsWith( " " ) || name.indexOf( "  " ) > 0 )
        {
            throw new InvalidApplicationProfileException(
                                                          "Name must not have leading or trailing spaces, or have two spaces in a row." );
        }
    }

    private ApplicationProfile getByName( EntityManager em, String name )
    {
        if ( name == null || name.trim().isEmpty() )
        {
            throw new DataAccessException( "The application profile name cannot be null or empty." );
        }
        // The name is whitespace and case insensitive
        name = name.replaceAll( "\\s", "" ).toLowerCase( Locale.ENGLISH );
        String sQuery = "SELECT entity FROM ApplicationProfile entity WHERE entity.nameLowercaseNoWhitespace=?1";
        return get( em, sQuery, name );
    }

    public ApplicationProfile getByName( String name )
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
}

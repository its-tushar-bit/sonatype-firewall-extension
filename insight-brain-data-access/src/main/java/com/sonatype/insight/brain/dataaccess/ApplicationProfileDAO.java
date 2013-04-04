/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.model.ApplicationProfile;
import com.sonatype.insight.brain.model.ApplicationProfilePolicy;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

public class ApplicationProfileDAO
    extends AbstractOperationalSqlDAO<ApplicationProfile>
{
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

    public List<ApplicationProfile> getAll( EntityManager em )
    {
        String sQuery = "SELECT entity FROM ApplicationProfile entity" + //
            " ORDER BY entity.nameLowercaseNoWhitespace";
        return getList( em, sQuery );
    }

    @Override
    public void insert( EntityManager em, ApplicationProfile applicationProfile )
    {
        NameHelper.validate( applicationProfile.getName() );

        if ( getByName( em, applicationProfile.getName() ) != null )
        {
            throw new InvalidNameException( "An application profile with the same name already exists." );
        }

        super.insert( em, applicationProfile );
    }

    @Override
    public void update( EntityManager em, ApplicationProfile applicationProfile )
    {
        NameHelper.validate( applicationProfile.getName() );

        ApplicationProfile otherApplicationProfile = getByName( em, applicationProfile.getName() );
        if ( otherApplicationProfile != null && !otherApplicationProfile.getId().equals( applicationProfile.getId() ) )
        {
            throw new InvalidNameException( "An application profile with the same name already exists." );
        }

        super.update( em, applicationProfile );
    }

    private ApplicationProfile getByName( EntityManager em, String name )
    {
        if ( name == null || name.trim().isEmpty() )
        {
            throw new DataAccessException( "The application profile name cannot be null or empty." );
        }
        // The name is whitespace and case insensitive
        name = NameHelper.normalize( name );
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

    @Override
    public void delete( EntityManager em, ApplicationProfile entity )
    {
        if ( getAll( em ).size() <= 1 )
        {
            throw new BadRequestException( "Cannot delete the last application profile." );
        }

        if ( !new ApplicationDAO().getByApplicationProfileId( em, entity.getId() ).isEmpty() )
        {
            throw new BadRequestException( "Cannot delete an application profile that is used by applications." );
        }

        ApplicationProfilePolicyDAO applicationProfilePolicyDAO = new ApplicationProfilePolicyDAO();
        List<ApplicationProfilePolicy> applicationProfilePolicies =
            applicationProfilePolicyDAO.getByApplicationProfileId( em, entity.getId() );
        for ( ApplicationProfilePolicy applicationProfilePolicy : applicationProfilePolicies )
        {
            applicationProfilePolicyDAO.delete( em, applicationProfilePolicy );
        }
        super.delete( em, entity );
    }
}

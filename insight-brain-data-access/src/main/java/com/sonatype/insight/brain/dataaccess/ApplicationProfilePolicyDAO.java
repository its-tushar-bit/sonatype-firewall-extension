/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.model.ApplicationProfilePolicy;
import com.sonatype.insight.error.exception.ConflictException;

public class ApplicationProfilePolicyDAO
    extends AbstractOperationalSqlDAO<ApplicationProfilePolicy>
{
    @Override
    protected ApplicationProfilePolicy getById( EntityManager em, String id )
    {
        String sQuery = "SELECT entity FROM ApplicationProfilePolicy entity" + //
            " WHERE entity.id=?1";
        return get( em, sQuery, id );
    }

    private ApplicationProfilePolicy getByApplicationProfileIdAndPolicyId( EntityManager em,
                                                                           String applicationProfileId, String policyId )
    {
        String sQuery = "SELECT entity FROM ApplicationProfilePolicy entity" + //
            " WHERE entity.applicationProfileId=?1 AND entity.policyId=?2";
        return get( em, sQuery, applicationProfileId, policyId );
    }

    public List<ApplicationProfilePolicy> getByPolicyId( String policyId )
    {
        String sQuery = "SELECT entity FROM ApplicationProfilePolicy entity" + //
            " WHERE entity.policyId=?1" + //
            " ORDER BY entity.applicationProfileId";
        return getList( sQuery, policyId );
    }

    private List<ApplicationProfilePolicy> getByApplicationProfileId( EntityManager em, String applicationProfileId )
    {
        String sQuery = "SELECT entity FROM ApplicationProfilePolicy entity" + //
            " WHERE entity.applicationProfileId=?1" + //
            " ORDER BY entity.policyId";
        return getList( em, sQuery, applicationProfileId );
    }

    public List<ApplicationProfilePolicy> getByApplicationProfileId( String applicationProfileId )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByApplicationProfileId( em, applicationProfileId );
        }
        finally
        {
            close( em );
        }
    }

    @Override
    public void update( EntityManager em, ApplicationProfilePolicy entity )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert( EntityManager em, ApplicationProfilePolicy entity )
    {
        ApplicationProfilePolicy other =
            getByApplicationProfileIdAndPolicyId( em, entity.getApplicationProfileId(), entity.getPolicyId() );
        if ( other != null )
        {
            throw new ConflictException( "The policy is already associated with the application profile." );
        }
        super.insert( em, entity );
    }

    public void set( String applicationProfileId, Set<String> policyIds )
    {
        EntityManager em = createEntityManager();
        try
        {
            em.getTransaction().begin();

            List<ApplicationProfilePolicy> oldAssociations = new ArrayList<ApplicationProfilePolicy>();
            oldAssociations.addAll( getByApplicationProfileId( em, applicationProfileId ) );
            for ( String policyId : policyIds )
            {
                boolean alreadyInGroup = false;
                for ( ApplicationProfilePolicy oldAssociation : oldAssociations )
                {
                    if ( policyId.equals( oldAssociation.getPolicyId() ) )
                    {
                        alreadyInGroup = true;
                        oldAssociations.remove( oldAssociation );
                        break;
                    }
                }
                if ( alreadyInGroup )
                {
                    continue;
                }

                ApplicationProfilePolicy newAssociation = new ApplicationProfilePolicy();
                newAssociation.setApplicationProfileId( applicationProfileId );
                newAssociation.setPolicyId( policyId );
                insert( em, newAssociation );
            }

            for ( ApplicationProfilePolicy oldAssociation : oldAssociations )
            {
                delete( em, oldAssociation );
            }

            em.getTransaction().commit();
        }
        finally
        {
            close( em );
        }
    }
}

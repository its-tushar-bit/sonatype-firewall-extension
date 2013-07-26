/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.error.exception.BadRequestException;

public class PolicyWaiverDAO
    extends AbstractOperationalSqlDAO<PolicyWaiver>
{
    @Override
    protected PolicyWaiver getById( EntityManager em, String id )
    {
        String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
            " WHERE entity.id=?1";
        return get( em, sQuery, id );
    }

    public List<PolicyWaiver> getByOwnerId( String ownerId )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByOwnerId( em, ownerId );
        }
        finally
        {
            close( em );
        }
    }

    public List<PolicyWaiver> getByOwnerId( EntityManager em, String ownerId )
    {
        String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
            " WHERE entity.ownerId=?1";
        return getList( em, sQuery, ownerId );
    }

    public List<PolicyWaiver> getByPolicyId( String policyId )
    {
        String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
            " WHERE entity.policyId=?1";
        return getList( sQuery, policyId );
    }

    private PolicyWaiver getByHashAndPolicyIdAndConstraintIdAndOwnerId( EntityManager em, String hash, String policyId,
                                                                        String constraintId, String ownerId )
    {
        String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
            " WHERE entity.hash=?1 AND entity.policyId=?2 AND entity.constraintId=?3 AND entity.ownerId=?4";
        return get( em, sQuery, hash, policyId, constraintId, ownerId );
    }

    @Override
    public void insert( EntityManager em, PolicyWaiver entity )
    {
        PolicyWaiver other =
            getByHashAndPolicyIdAndConstraintIdAndOwnerId( em, entity.getHash(), entity.getPolicyId(),
                                                           entity.getConstraintId(), entity.getOwnerId() );
        if ( other != null )
        {
            throw new BadRequestException( "This policy waiver already exists" );
        }

        entity.setCreateTime( new Date() );

        super.insert( em, entity );
    }

    @Override
    public void update( EntityManager em, PolicyWaiver entity )
    {
        throw new UnsupportedOperationException();
    }
}

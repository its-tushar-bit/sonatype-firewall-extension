/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import com.sonatype.insight.brain.model.ApplicationProfile;
import com.sonatype.insight.brain.model.ApplicationProfilePolicy;

public class ApplicationProfilePolicyDAOTest
    extends AbstractDbDAOTest
{
    private ApplicationProfile applicationProfile;

    @After
    public void cleanUp()
    {
        if ( applicationProfile != null )
        {
            new ApplicationProfileDAO().delete( applicationProfile );
        }
    }

    @Test
    public void testGetSet()
        throws Exception
    {
        ApplicationProfileDAO applicationProfileDAO = new ApplicationProfileDAO();
        ApplicationProfile applicationProfile = new ApplicationProfile( "My app profile" );
        applicationProfileDAO.insert( applicationProfile );
        String applicationProfileId = applicationProfile.getId();

        ApplicationProfilePolicyDAO dao = new ApplicationProfilePolicyDAO();
        
        List<ApplicationProfilePolicy> applicationProfilePolicies =
            dao.getByApplicationProfileId( applicationProfileId );
        assertEquals( 0, applicationProfilePolicies.size() );

        // Set one policy id
        Set<String> policyIds = new LinkedHashSet<String>();
        policyIds.add( "policyId1" );
        dao.set( applicationProfileId, policyIds );

        // Get
        applicationProfilePolicies = dao.getByApplicationProfileId( applicationProfileId );
        assertEquals( 1, applicationProfilePolicies.size() );
        assertEquals( applicationProfileId, applicationProfilePolicies.get( 0 ).getApplicationProfileId() );
        assertEquals( "policyId1", applicationProfilePolicies.get( 0 ).getPolicyId() );

        // Set two policy ids
        policyIds.add( "policyId2" );
        dao.set( applicationProfileId, policyIds );

        // Get
        applicationProfilePolicies = dao.getByApplicationProfileId( applicationProfileId );
        assertEquals( 2, applicationProfilePolicies.size() );
        assertEquals( applicationProfileId, applicationProfilePolicies.get( 0 ).getApplicationProfileId() );
        assertEquals( "policyId1", applicationProfilePolicies.get( 0 ).getPolicyId() );
        assertEquals( applicationProfileId, applicationProfilePolicies.get( 1 ).getApplicationProfileId() );
        assertEquals( "policyId2", applicationProfilePolicies.get( 1 ).getPolicyId() );

        // Set one policy id again
        policyIds.remove( "policyId1" );
        dao.set( applicationProfileId, policyIds );

        // Get
        applicationProfilePolicies = dao.getByApplicationProfileId( applicationProfileId );
        assertEquals( 1, applicationProfilePolicies.size() );
        assertEquals( applicationProfileId, applicationProfilePolicies.get( 0 ).getApplicationProfileId() );
        assertEquals( "policyId2", applicationProfilePolicies.get( 0 ).getPolicyId() );

        // Set no policy ids
        policyIds.clear();
        dao.set( applicationProfileId, policyIds );

        // Get
        applicationProfilePolicies = dao.getByApplicationProfileId( applicationProfileId );
        assertEquals( 0, applicationProfilePolicies.size() );
    }
}

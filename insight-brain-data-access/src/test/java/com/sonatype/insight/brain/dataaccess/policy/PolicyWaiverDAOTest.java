/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.error.exception.BadRequestException;

public class PolicyWaiverDAOTest
    extends AbstractDbDAOTest
{
    @Test
    public void testCRUD()
        throws Exception
    {
        PolicyWaiverDAO dao = new PolicyWaiverDAO();

        String hash = "123456789012345678901";
        assertTrue( hash.length() > 20 );
        String truncatedHash = hash.substring( 0, 20 );
        String policyId = "MyPolicyId";
        String ownerId = "MyOwnerId";
        String comment = "My comment";

        // Create
        PolicyWaiver policyWaiver = new PolicyWaiver( hash, policyId, ownerId, comment );
        assertNull( policyWaiver.getId() );
        long beforeInsert = System.currentTimeMillis();
        dao.insert( policyWaiver );
        long afterInsert = System.currentTimeMillis();
        assertNotNull( policyWaiver.getId() );
        assertNotNull( policyWaiver.getCreateTime() );
        Date createTime = policyWaiver.getCreateTime();
        assertTrue( beforeInsert <= createTime.getTime() );
        assertTrue( createTime.getTime() <= afterInsert );

        // Read
        policyWaiver = dao.getById( policyWaiver.getId() );
        assertNotNull( policyWaiver );
        assertPolicyWaiver( truncatedHash, policyId, ownerId, comment, createTime, policyWaiver );

        // Update is not allowed
        try
        {
            dao.update( policyWaiver );
            fail( "Expected UnsupportedOperationException, updates to PolicyWaiver are not allowed" );
        }
        catch ( UnsupportedOperationException expected )
        {
        }

        // Delete
        dao.delete( policyWaiver );

        policyWaiver = dao.getById( policyWaiver.getId() );
        assertNull( policyWaiver );
    }

    private void assertPolicyWaiver( String hash, String policyId, String ownerId, String comment, Date createTime,
                                     PolicyWaiver actual )
    {
        assertEquals( hash, actual.getHash() );
        assertEquals( policyId, actual.getPolicyId() );
        assertEquals( ownerId, actual.getOwnerId() );
        assertEquals( comment, actual.getComment() );
        assertEquals( createTime, actual.getCreateTime() );
    }

    @Test
    public void testAddDuplicate()
        throws Exception
    {
        PolicyWaiverDAO dao = new PolicyWaiverDAO();

        String hash = "12345678901234567890";
        String policyId = "MyPolicyId";
        String ownerId = "MyOwnerId";
        String comment = "My comment";
        PolicyWaiver policyWaiver1 = new PolicyWaiver( hash, policyId, ownerId, comment );
        dao.insert( policyWaiver1 );

        PolicyWaiver policyWaiver2 = new PolicyWaiver( hash, policyId, ownerId, comment );
        try
        {
            dao.insert( policyWaiver2 );
            fail( "Expected BadRequestException" );
        }
        catch ( BadRequestException expected )
        {
            assertEquals( "This policy waiver already exists", expected.getMessage() );
        }

        dao.delete( policyWaiver1 );
    }
}

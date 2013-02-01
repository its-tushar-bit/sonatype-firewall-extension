/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;

public class LicenseThreatGroupDAOTest
    extends AbstractDbDAOTest
{
    @After
    public void cleanUp()
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
        List<LicenseThreatGroup> groups = dao.getByApplicationId( applicationId );
        for ( LicenseThreatGroup group : groups )
        {
            dao.delete( group );
        }
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        // Create
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        dao.insert( group );
        Assert.assertNotNull( group.getId() );

        group = dao.getById( group.getId() );
        Assert.assertNotNull( group );
        assertLicenseThreatGroup( applicationId, "My group", 4, group );

        // Update
        group.setName( "My updated name" );
        dao.update( group );

        group = dao.getById( group.getId() );
        Assert.assertNotNull( group );
        assertLicenseThreatGroup( applicationId, "My updated name", 4, group );

        // Delete
        dao.delete( group );

        group = dao.getById( group.getId() );
        Assert.assertNull( group );
    }

    @Test
    public void testCascadeDelete()
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

        // Create
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        dao.insert( group );
        Assert.assertNotNull( group.getId() );

        LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
        licenseThreatGroupLicense.setApplicationId( applicationId );
        licenseThreatGroupLicense.setLicenseThreatGroupId( group.getId() );
        licenseThreatGroupLicense.setMultiLicenseId( "UNSPECIFIED" );
        licenseThreatGroupLicenseDAO.insert( licenseThreatGroupLicense );

        // Delete
        dao.delete( group );

        group = dao.getById( group.getId() );
        Assert.assertNull( group );
    }

    @Test
    public void testAddDuplicateGroup()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        // Add a group
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        dao.insert( group );

        // Add another group with the same name
        group = new LicenseThreatGroup();
        group.setApplicationId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 5 );
        try
        {
            dao.insert( group );
            Assert.fail( "Expected InvalidLicenseThreatGroupException" );
        }
        catch ( InvalidLicenseThreatGroupException expected )
        {
            if ( !"A license threat group with the same name already exists".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testUpdateDuplicateName()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        // Add a group
        LicenseThreatGroup group1 = new LicenseThreatGroup();
        group1.setApplicationId( applicationId );
        group1.setName( "My group 1" );
        group1.setThreatLevel( 4 );
        dao.insert( group1 );

        // Add another group
        LicenseThreatGroup group2 = new LicenseThreatGroup();
        group2.setApplicationId( applicationId );
        group2.setName( "My group 2" );
        group2.setThreatLevel( 4 );
        dao.insert( group2 );

        // Update without changing the name
        group2.setThreatLevel( 6 );
        dao.update( group2 );
        assertLicenseThreatGroup( applicationId, "My group 2", 6, group2 );

        // Update with a conflicting name
        group2.setName( group1.getName() );
        try
        {
            dao.update( group2 );
            Assert.fail( "Expected InvalidLicenseThreatGroupException" );
        }
        catch ( InvalidLicenseThreatGroupException expected )
        {
            if ( !"A license threat group with the same name already exists".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testInsertInvalidThreatLevel()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( -1 );
        try
        {
            dao.insert( group );
            Assert.fail( "Expected InvalidLicenseThreatGroupException" );
        }
        catch ( InvalidLicenseThreatGroupException expected )
        {
            if ( !"The threat level must be a number between 0 and 10".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        group.setThreatLevel( 11 );
        try
        {
            dao.insert( group );
            Assert.fail( "Expected InvalidLicenseThreatGroupException" );
        }
        catch ( InvalidLicenseThreatGroupException expected )
        {
            if ( !"The threat level must be a number between 0 and 10".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testUpdateInvalidThreatLevel()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 1 );
        dao.insert( group );
        group.setThreatLevel( -1 );
        try
        {
            dao.update( group );
            Assert.fail( "Expected InvalidLicenseThreatGroupException" );
        }
        catch ( InvalidLicenseThreatGroupException expected )
        {
            if ( !"The threat level must be a number between 0 and 10".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        group.setThreatLevel( 11 );
        try
        {
            dao.update( group );
            Assert.fail( "Expected InvalidLicenseThreatGroupException" );
        }
        catch ( InvalidLicenseThreatGroupException expected )
        {
            if ( !"The threat level must be a number between 0 and 10".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    private void assertLicenseThreatGroup( String applicationId, String name, int threatLevel, LicenseThreatGroup actual )
    {
        Assert.assertEquals( applicationId, actual.getApplicationId() );
        Assert.assertEquals( name, actual.getName() );
        Assert.assertEquals( threatLevel, actual.getThreatLevel() );
    }
}

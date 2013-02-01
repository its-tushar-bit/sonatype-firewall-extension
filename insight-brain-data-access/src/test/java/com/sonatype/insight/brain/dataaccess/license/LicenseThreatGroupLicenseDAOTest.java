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
import com.sonatype.insight.error.exception.NotFoundException;

public class LicenseThreatGroupLicenseDAOTest
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
        LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        groupDAO.insert( group );

        LicenseThreatGroupLicenseDAO dao = new LicenseThreatGroupLicenseDAO();

        // Create
        LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
        licenseThreatGroupLicense.setApplicationId( applicationId );
        licenseThreatGroupLicense.setLicenseThreatGroupId( group.getId() );
        licenseThreatGroupLicense.setMultiLicenseId( "UNSPECIFIED" );
        dao.insert( licenseThreatGroupLicense );
        Assert.assertNotNull( licenseThreatGroupLicense.getId() );

        licenseThreatGroupLicense = dao.getById( licenseThreatGroupLicense.getId() );
        Assert.assertNotNull( licenseThreatGroupLicense );
        assertLicenseThreatGroupLicense( applicationId, group.getId(), "UNSPECIFIED", licenseThreatGroupLicense );

        // Update
        try
        {
            dao.update( licenseThreatGroupLicense );
            Assert.fail( "Expected UnsupportedOperationException" );
        }
        catch ( UnsupportedOperationException expected )
        {
        }

        // Delete
        dao.delete( licenseThreatGroupLicense );

        licenseThreatGroupLicense = dao.getById( licenseThreatGroupLicense.getId() );
        Assert.assertNull( licenseThreatGroupLicense );
    }

    @Test
    public void testAddSameLicenseToTwoGroups()
        throws Exception
    {
        LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();
        LicenseThreatGroup group1 = new LicenseThreatGroup();
        group1.setApplicationId( applicationId );
        group1.setName( "My group 1" );
        groupDAO.insert( group1 );
        LicenseThreatGroup group2 = new LicenseThreatGroup();
        group2.setApplicationId( applicationId );
        group2.setName( "My group 2" );
        groupDAO.insert( group2 );

        LicenseThreatGroupLicenseDAO dao = new LicenseThreatGroupLicenseDAO();
        LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
        licenseThreatGroupLicense.setApplicationId( applicationId );
        licenseThreatGroupLicense.setLicenseThreatGroupId( group1.getId() );
        licenseThreatGroupLicense.setMultiLicenseId( "UNSPECIFIED" );
        dao.insert( licenseThreatGroupLicense );

        licenseThreatGroupLicense = new LicenseThreatGroupLicense();
        licenseThreatGroupLicense.setApplicationId( applicationId );
        licenseThreatGroupLicense.setLicenseThreatGroupId( group2.getId() );
        licenseThreatGroupLicense.setMultiLicenseId( "UNSPECIFIED" );
        try
        {
            dao.insert( licenseThreatGroupLicense );
            Assert.fail( "Expected InvalidLicenseThreatGroupLicenseException" );
        }
        catch ( InvalidLicenseThreatGroupLicenseException expected )
        {
            if ( !"The license is already in the 'My group 1' license threat group".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testInsertInvalidMultiLicenseId()
        throws Exception
    {
        LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        groupDAO.insert( group );

        LicenseThreatGroupLicenseDAO dao = new LicenseThreatGroupLicenseDAO();

        // Create
        LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
        licenseThreatGroupLicense.setApplicationId( applicationId );
        licenseThreatGroupLicense.setLicenseThreatGroupId( group.getId() );
        licenseThreatGroupLicense.setMultiLicenseId( "BAZINGAAA" );
        try
        {
            dao.insert( licenseThreatGroupLicense );
            Assert.fail( "Expected NotFoundException" );
        }
        catch ( NotFoundException expected )
        {
            if ( !"A license with id 'BAZINGAAA' does not exist.".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    private void assertLicenseThreatGroupLicense( String applicationId, String licenseThreatGroupId,
                                                  String multiLicenseId, LicenseThreatGroupLicense actual )
    {
        Assert.assertEquals( applicationId, actual.getApplicationId() );
        Assert.assertEquals( licenseThreatGroupId, actual.getLicenseThreatGroupId() );
        Assert.assertEquals( multiLicenseId, actual.getMultiLicenseId() );
    }
}

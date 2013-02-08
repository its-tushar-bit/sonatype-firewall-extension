/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

public class ApplicationDAOTest
    extends AbstractDbDAOTest
{
    @Test
    public void testCRUD()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

        // Create
        // The super class creates an application by default
        List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByApplicationId( applicationId );
        Assert.assertEquals( 4, licenseThreatGroups.size() );
        Assert.assertEquals( "Copyleft", licenseThreatGroups.get( 0 ).getName() );
        Assert.assertEquals( 9, licenseThreatGroups.get( 0 ).getThreatLevel() );
        Assert.assertEquals( "Liberal", licenseThreatGroups.get( 1 ).getName() );
        Assert.assertEquals( 0, licenseThreatGroups.get( 1 ).getThreatLevel() );
        Assert.assertEquals( "Non Standard", licenseThreatGroups.get( 2 ).getName() );
        Assert.assertEquals( 6, licenseThreatGroups.get( 2 ).getThreatLevel() );
        Assert.assertEquals( "Weak Copyleft", licenseThreatGroups.get( 3 ).getName() );
        Assert.assertEquals( 2, licenseThreatGroups.get( 3 ).getThreatLevel() );
        for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroups )
        {
            Assert.assertTrue( licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId( licenseThreatGroup.getId() ).size() > 0 );
        }

        // Update
        Application application = applicationDAO.getById( applicationId );
        application.setPublicId( "ApplicationDAOTest New public id" );
        applicationDAO.update( application );
        application = applicationDAO.getById( applicationId );
        Assert.assertEquals( "ApplicationDAOTest New public id", application.getPublicId() );

        // Delete
        applicationDAO.delete( application );
        application = applicationDAO.getById( applicationId );
        Assert.assertNull( application );
    }

    @Test
    public void testPublicIdIsCaseInsensitive()
    {
        String appPublicId = "testPublicIdIsCaseInsensitive";

        Application application = new Application();
        application.setPublicId( appPublicId );
        ApplicationDAO applicationDAO = new ApplicationDAO();
        applicationDAO.insert( application );
        String applicationId = application.getId();

        Assert.assertEquals( appPublicId, application.getPublicId() );
        Assert.assertEquals( appPublicId.toLowerCase(), application.getPublicIdLowercase() );

        application = applicationDAO.getById( applicationId );
        Assert.assertNotNull( application );
        Assert.assertEquals( appPublicId, application.getPublicId() );
        Assert.assertEquals( appPublicId.toLowerCase(), application.getPublicIdLowercase() );

        application = applicationDAO.getByPublicId( appPublicId );
        Assert.assertNotNull( application );
        Assert.assertEquals( applicationId, application.getId() );

        application = applicationDAO.getByPublicId( appPublicId.toLowerCase() );
        Assert.assertNotNull( application );
        Assert.assertEquals( applicationId, application.getId() );

        application = applicationDAO.getByPublicId( appPublicId.toUpperCase() );
        Assert.assertNotNull( application );
        Assert.assertEquals( applicationId, application.getId() );
    }
}

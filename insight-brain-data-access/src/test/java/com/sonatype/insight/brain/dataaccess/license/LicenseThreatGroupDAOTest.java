/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;

public class LicenseThreatGroupDAOTest
    extends AbstractDbDAOTest
{
    private void testCRUD( String ownerId )
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        // Create
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setOwnerId( ownerId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        dao.insert( group );
        Assert.assertNotNull( group.getId() );

        group = dao.getById( group.getId() );
        Assert.assertNotNull( group );
        assertLicenseThreatGroup( ownerId, "My group", 4, group );

        // Update
        group.setName( "My updated name" );
        dao.update( group );

        group = dao.getById( group.getId() );
        Assert.assertNotNull( group );
        assertLicenseThreatGroup( ownerId, "My updated name", 4, group );

        // Delete
        dao.delete( group );

        group = dao.getById( group.getId() );
        Assert.assertNull( group );
    }

    @Test
    public void testCRUD_Application()
        throws Exception
    {
        testCRUD( applicationId );
    }

    @Test
    public void testCRUD_Organization()
        throws Exception
    {
        organization = new Organization();
        organization.setName( "testCRUD-Organization" );
        new OrganizationDAO().insert( organization );

        testCRUD( organization.getId() );
    }

    @Test
    public void testCascadeDelete()
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

        // Create
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setOwnerId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        dao.insert( group );
        Assert.assertNotNull( group.getId() );

        LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
        licenseThreatGroupLicense.setOwnerId( applicationId );
        licenseThreatGroupLicense.setLicenseThreatGroupId( group.getId() );
        licenseThreatGroupLicense.setLicenseId( "UNSPECIFIED" );
        licenseThreatGroupLicenseDAO.insert( licenseThreatGroupLicense );

        // Delete
        dao.delete( group );

        group = dao.getById( group.getId() );
        Assert.assertNull( group );
    }

    @Test
    public void testInsertDuplicateName_Application()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        // Add a group
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setOwnerId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        dao.insert( group );

        // Add another group with the same name
        group = new LicenseThreatGroup();
        group.setOwnerId( applicationId );
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
    public void testInsertDuplicateName_ApplicationOrganization()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
        
        organization = new Organization( "testInsertDuplicateName-ApplicationOrganization" );
        new OrganizationDAO().insert( organization );
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByIdNotNull( applicationId );
        application.setOrganizationId( organization.getId() );
        applicationDAO.update( application );

        // Add a group to the organization
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setOwnerId( organization.getId() );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        dao.insert( group );

        // Add another group with the same name to the application
        group = new LicenseThreatGroup();
        group.setOwnerId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 5 );
        try
        {
            dao.insert( group );
            Assert.fail( "Expected InvalidLicenseThreatGroupException" );
        }
        catch ( InvalidLicenseThreatGroupException expected )
        {
            if ( !"A license threat group with the same name already exists for the parent organization".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testInsertDuplicateName_OrganizationApplication()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        organization = new Organization( "testInsertDuplicateName-OrganizationApplication" );
        new OrganizationDAO().insert( organization );
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByIdNotNull( applicationId );
        application.setOrganizationId( organization.getId() );
        applicationDAO.update( application );

        // Add a group to the application
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setOwnerId( applicationId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        dao.insert( group );

        // Add another group with the same name to the organization
        group = new LicenseThreatGroup();
        group.setOwnerId( organization.getId() );
        group.setName( "My group" );
        group.setThreatLevel( 5 );
        try
        {
            dao.insert( group );
            Assert.fail( "Expected InvalidLicenseThreatGroupException" );
        }
        catch ( InvalidLicenseThreatGroupException expected )
        {
            if ( !"A license threat group with the same name already exists for application 'AbstractDbDAOTest'".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testUpdateDuplicateName_Application()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        // Add a group
        LicenseThreatGroup group1 = new LicenseThreatGroup();
        group1.setOwnerId( applicationId );
        group1.setName( "My group 1" );
        group1.setThreatLevel( 4 );
        dao.insert( group1 );

        // Add another group
        LicenseThreatGroup group2 = new LicenseThreatGroup();
        group2.setOwnerId( applicationId );
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
    public void testUpdateDuplicateName_ApplicationOrganization()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        organization = new Organization( "testUpdateDuplicateName-ApplicationOrganization" );
        new OrganizationDAO().insert( organization );
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByIdNotNull( applicationId );
        application.setOrganizationId( organization.getId() );
        applicationDAO.update( application );

        // Add a group to the organization
        LicenseThreatGroup group1 = new LicenseThreatGroup();
        group1.setOwnerId( organization.getId() );
        group1.setName( "My group 1" );
        group1.setThreatLevel( 4 );
        dao.insert( group1 );

        // Add another group to the application
        LicenseThreatGroup group2 = new LicenseThreatGroup();
        group2.setOwnerId( applicationId );
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
            if ( !"A license threat group with the same name already exists for the parent organization".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testUpdateDuplicateName_OrganizationApplication()
        throws Exception
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

        organization = new Organization( "testUpdateDuplicateName-OrganizationApplication" );
        new OrganizationDAO().insert( organization );
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByIdNotNull( applicationId );
        application.setOrganizationId( organization.getId() );
        applicationDAO.update( application );

        // Add a group to the organization
        LicenseThreatGroup group1 = new LicenseThreatGroup();
        group1.setOwnerId( organization.getId() );
        group1.setName( "My group 1" );
        group1.setThreatLevel( 4 );
        dao.insert( group1 );

        // Add another group to the application
        LicenseThreatGroup group2 = new LicenseThreatGroup();
        group2.setOwnerId( applicationId );
        group2.setName( "My group 2" );
        group2.setThreatLevel( 4 );
        dao.insert( group2 );

        // Update without changing the name
        group1.setThreatLevel( 6 );
        dao.update( group1 );
        assertLicenseThreatGroup( organization.getId(), "My group 1", 6, group1 );

        // Update with a conflicting name
        group1.setName( group2.getName() );
        try
        {
            dao.update( group1 );
            Assert.fail( "Expected InvalidLicenseThreatGroupException" );
        }
        catch ( InvalidLicenseThreatGroupException expected )
        {
            if ( !"A license threat group with the same name already exists for application 'AbstractDbDAOTest'".equals( expected.getMessage() ) )
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
        group.setOwnerId( applicationId );
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
        group.setOwnerId( applicationId );
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
        Assert.assertEquals( applicationId, actual.getOwnerId() );
        Assert.assertEquals( name, actual.getName() );
        Assert.assertEquals( threatLevel, actual.getThreatLevel() );
    }
}

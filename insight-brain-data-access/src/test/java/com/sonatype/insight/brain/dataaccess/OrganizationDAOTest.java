/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

public class OrganizationDAOTest
    extends AbstractDbDAOTest
{
    private OrganizationDAO dao = new OrganizationDAO();

    @Before
    public void before()
    {
        organization = new Organization();
    }

    @Test
    public void testCreateDefaultLicenseThreatGroups()
        throws Exception
    {
        organization.setName( "OrganizationDAOTest" );
        dao.insert( organization );
        List<LicenseThreatGroup> licenseThreatGroups = new LicenseThreatGroupDAO().getByOwnerId( organization.getId() );
        Assert.assertTrue( licenseThreatGroups.size() >= 4 );
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        // Create
        organization.setName( "OrganizationDAOTest" );
        dao.insert( organization );
        String organizationId = organization.getId();
        organization = dao.getById( organizationId );
        Assert.assertEquals( "OrganizationDAOTest", organization.getName() );

        // Update
        organization.setName( "OrganizationDAOTest New name" );
        dao.update( organization );
        organization = dao.getById( organizationId );
        Assert.assertEquals( "OrganizationDAOTest New name", organization.getName() );

        // Get All
        List<Organization> organizations = dao.getAll();
        Assert.assertEquals( 1, organizations.size() );
        Assert.assertEquals( organizationId, organizations.get( 0 ).getId() );

        // Delete
        dao.delete( organization );
        organization = dao.getById( organizationId );
        Assert.assertNull( organization );
    }

    @Test
    public void testValidateNullName_Insert()
    {
        organization.setName( null );
        try
        {
            dao.insert( organization );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "Name is required.", expected.getMessage() );
        }
    }

    @Test
    public void testValidateNullName_Update()
    {
        organization.setName( "testValidateNullName" );
        assertEquals( "testvalidatenullname", organization.getNameLowercaseNoWhitespace() );
        dao.insert( organization );

        organization.setName( null );
        assertNull( organization.getNameLowercaseNoWhitespace() );
        try
        {
            dao.update( organization );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "Name is required.", expected.getMessage() );
        }
    }

    @Test
    public void testValidateEmptyName_Insert()
    {
        organization.setName( " " );
        try
        {
            dao.insert( organization );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "Name is required.", expected.getMessage() );
        }
    }

    @Test
    public void testValidateEmptyName_Update()
    {
        organization.setName( "testValidateEmptyName" );
        assertEquals( "testvalidateemptyname", organization.getNameLowercaseNoWhitespace() );
        dao.insert( organization );

        organization.setName( " " );
        assertEquals( "", organization.getNameLowercaseNoWhitespace() );
        try
        {
            dao.update( organization );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "Name is required.", expected.getMessage() );
        }
    }

    @Test
    public void testValidateNameInvalidChars_Insert()
    {
        String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
        for ( String name : invalidAlphaNumericNames )
        {
            organization.setName( name );
            try
            {
                dao.insert( organization );
                fail( "Expected InvalidNameException" );
            }
            catch ( InvalidNameException expected )
            {
                assertEquals( "Name must be alpha numeric.", expected.getMessage() );
            }
        }
    }

    @Test
    public void testValidateNameInvalidChars_Update()
    {
        organization.setName( "testValidateNameInvalidChars" );
        dao.insert( organization );
        String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
        for ( String name : invalidAlphaNumericNames )
        {
            organization.setName( name );
            try
            {
                dao.update( organization );
                fail( "Expected InvalidNameException" );
            }
            catch ( InvalidNameException expected )
            {
                assertEquals( "Name must be alpha numeric.", expected.getMessage() );
            }
        }
    }

    @Test
    public void testValidateNameSpaces_Insert()
    {
        String[] invalidSpacingNames =
            { " leading space", "trailing space ", "double  space", "  starts with double space",
                "ends with double space  " };
        for ( String name : invalidSpacingNames )
        {
            organization.setName( name );
            try
            {
                dao.insert( organization );
                fail( "Expected InvalidNameException" );
            }
            catch ( InvalidNameException expected )
            {
                assertEquals( "Name must not have leading or trailing spaces, or have two spaces in a row.",
                              expected.getMessage() );
            }
        }
    }

    @Test
    public void testValidateNameSpaces_Update()
    {
        organization.setName( "testValidateNameSpaces" );
        dao.insert( organization );

        String[] invalidSpacingNames =
            { " leading space", "trailing space ", "double  space", "  starts with double space",
                "ends with double space  " };
        for ( String name : invalidSpacingNames )
        {
            organization.setName( name );
            try
            {
                dao.update( organization );
                fail( "Expected InvalidNameException" );
            }
            catch ( InvalidNameException expected )
            {
                assertEquals( "Name must not have leading or trailing spaces, or have two spaces in a row.",
                              expected.getMessage() );
            }
        }
    }

    @Test
    public void testNameIsCaseAndWhitespaceInsensitive()
    {
        String name = "test string With Case and Whitespace";

        organization.setName( name );
        dao.insert( organization );

        assertEquals( name, organization.getName() );
        assertEquals( "teststringwithcaseandwhitespace", organization.getNameLowercaseNoWhitespace() );

        String name1 = "TEST String      With    cASE and      whitespace";
        Organization organization1 = dao.getByName( name1 );
        assertNotNull( organization1 );
        assertEquals( organization.getId(), organization1.getId() );
    }

    @Test
    public void testDuplicateName_Insert()
    {
        organization.setName( "testDuplicateName" );
        dao.insert( organization );

        Organization organization1 = new Organization();
        organization1.setName( "Test Duplicate Name" );
        try
        {
            dao.insert( organization1 );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "Test Duplicate Name is already used as a name.", expected.getMessage() );
        }
    }

    @Test
    public void testDuplicateName_Update()
    {
        organization.setName( "testDuplicateName" );
        dao.insert( organization );

        Organization organization1 = new Organization();
        organization1.setName( "testDuplicateName1" );
        dao.insert( organization1 );

        organization1.setName( "Test Duplicate Name" );
        try
        {
            dao.update( organization1 );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "Test Duplicate Name is already used as a name.", expected.getMessage() );
        }
    }
}

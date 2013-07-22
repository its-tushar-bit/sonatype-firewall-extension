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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

public class OrganizationDAOTest
    extends AbstractDbDAOTest
{
    private OrganizationDAO dao = new OrganizationDAO();

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void testCreateDefaultLicenseThreatGroups()
        throws Exception
    {
        organization = createOrganization( "OrganizationDAOTest" );
        List<LicenseThreatGroup> licenseThreatGroups = new LicenseThreatGroupDAO().getByOwnerId( organization.getId() );
        Assert.assertEquals( LicenseThreatGroupDAO.DEFAULT_LICENSE_THREAT_GROUP_COUNT, licenseThreatGroups.size() );
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        // Create
        organization = createOrganization( "OrganizationDAOTest" );
        String organizationId = organization.getId();
        organization = dao.getById( organizationId );
        Assert.assertEquals( "OrganizationDAOTest", organization.getName() );

        // Set an icon for the organization
        BufferedImage image = new BufferedImage( 420, 420, BufferedImage.TYPE_INT_ARGB );
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write( image, "png", byteArrayOutputStream );
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( byteArrayOutputStream.toByteArray() );
        File iconDir = tmpDir.newFolder();
        File orgIconDir = new File( iconDir, organizationId );
        Assert.assertFalse( orgIconDir.exists() );
        new IconDAO().setIcon( organizationId, iconDir, byteArrayInputStream );
        Assert.assertTrue( orgIconDir.isDirectory() );

        // Get the icon
        byte[] iconBytes = new IconDAO().getIcon( organizationId, iconDir );
        Assert.assertNotNull( iconBytes );
        Assert.assertTrue( iconBytes.length > 0 );

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
        Organization organization = new Organization( null /* name */);
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
        organization = createOrganization( "testValidateNullName" );
        assertEquals( "testvalidatenullname", organization.getNameLowercaseNoWhitespace() );

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
        try
        {
            createOrganization( " " );
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
        organization = createOrganization( "testValidateEmptyName" );
        assertEquals( "testvalidateemptyname", organization.getNameLowercaseNoWhitespace() );

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
            Organization organization = new Organization( name );
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
        organization = createOrganization( "testValidateNameInvalidChars" );
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
            try
            {
                createOrganization( name );
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
        organization = createOrganization( "testValidateNameSpaces" );

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

        organization = createOrganization( name );

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
        createOrganization( "testDuplicateName" );

        try
        {
            createOrganization( "testDuplicateName" );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "testDuplicateName is already used as a name.", expected.getMessage() );
        }
    }

    @Test
    public void testDuplicateName_Update()
    {
        createOrganization( "testDuplicateName" );
        Organization organization1 = createOrganization( "testDuplicateName1" );

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

    @Test
    public void testValidateNameLength_Insert()
    {
        String name = StringUtils.repeat( "a", NameHelper.MAX_NAME_LENGTH );
        try
        {
            createOrganization( name + "a" );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "Name must be 60 characters or less.", expected.getMessage() );
        }

        createOrganization( name );
    }

    @Test
    public void testValidateNameLength_Update()
    {
        organization = createOrganization( "test name" );

        String name = StringUtils.repeat( "a", NameHelper.MAX_NAME_LENGTH );
        organization.setName( name + "a" );
        try
        {
            dao.update( organization );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "Name must be 60 characters or less.", expected.getMessage() );
        }

        organization.setName( name );
        dao.update( organization );
    }
}

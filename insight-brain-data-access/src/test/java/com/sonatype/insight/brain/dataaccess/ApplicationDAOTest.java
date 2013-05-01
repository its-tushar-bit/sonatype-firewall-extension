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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.Label;

public class ApplicationDAOTest
    extends AbstractDbDAOTest
{
    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private Application application;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Before
    public void setupApplication()
    {
        application = new Application();
        application.setName( "valid name" );
        application.setPublicId( "valid public id" );
    }

    @After
    public void cleanUp()
    {
        if ( applicationDAO.getById( application.getId() ) != null )
        {
            applicationDAO.delete( application );
        }
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        // Create
        // The super class creates an application by default
        File iconDir = tmpDir.newFolder();
        File appIconDir = new File( iconDir, applicationId );
        Assert.assertFalse( appIconDir.exists() );
        applicationDAO.setIcon( applicationId, iconDir, new ByteArrayInputStream( new byte[0] ) );
        Assert.assertTrue( appIconDir.isDirectory() );

        // Update
        Application application = applicationDAO.getById( applicationId );
        application.setName( "ApplicationDAOTest New name" );
        applicationDAO.update( application );
        application = applicationDAO.getById( applicationId );
        Assert.assertEquals( "ApplicationDAOTest New name", application.getName() );

        // Get All
        List<Application> applications = applicationDAO.getAll();
        Assert.assertEquals( 1, applications.size() );
        Assert.assertEquals( applicationId, applications.get( 0 ).getId() );

        // Delete
        applicationDAO.deleteWithIcon( application, iconDir );
        application = applicationDAO.getById( applicationId );
        Assert.assertNull( application );
        Assert.assertFalse( appIconDir.getAbsolutePath(), appIconDir.exists() );
    }

    @Test
    public void testValidateNullPublicId_Insert()
    {
        application.setPublicId( null );
        try
        {
            applicationDAO.insert( application );
            fail( "Expected InvalidApplicationException" );
        }
        catch ( InvalidApplicationException expected )
        {
            assertEquals( "ID is required.", expected.getMessage() );
        }
    }

    @Test
    public void testValidateNullPublicId_Update()
    {
        applicationDAO.insert( application );
        application.setPublicId( " " );
        application.setName( application.getName() + "1" );
        try
        {
            applicationDAO.update( application );
            fail( "Expected InvalidApplicationException" );
        }
        catch ( InvalidApplicationException expected )
        {
            assertEquals( "ID is required.", expected.getMessage() );
        }
    }

    @Test
    public void testPublicIdIsCaseInsensitive()
    {
        String appPublicId = "testPublicIdIsCaseInsensitive";

        Application application = new Application();
        application.setName( "test" );
        application.setPublicId( appPublicId );
        ApplicationDAO applicationDAO = new ApplicationDAO();
        applicationDAO.insert( application );
        String applicationId = application.getId();

        Assert.assertEquals( appPublicId, application.getPublicId() );
        Assert.assertEquals( appPublicId.toLowerCase( Locale.ENGLISH ), application.getPublicIdLowercase() );

        application = applicationDAO.getById( applicationId );
        Assert.assertNotNull( application );
        Assert.assertEquals( appPublicId, application.getPublicId() );
        Assert.assertEquals( appPublicId.toLowerCase( Locale.ENGLISH ), application.getPublicIdLowercase() );

        application = applicationDAO.getByPublicId( appPublicId );
        Assert.assertNotNull( application );
        Assert.assertEquals( applicationId, application.getId() );

        application = applicationDAO.getByPublicId( appPublicId.toLowerCase( Locale.ENGLISH ) );
        Assert.assertNotNull( application );
        Assert.assertEquals( applicationId, application.getId() );

        application = applicationDAO.getByPublicId( appPublicId.toUpperCase( Locale.ENGLISH ) );
        Assert.assertNotNull( application );
        Assert.assertEquals( applicationId, application.getId() );
    }

    @Test
    public void testValidateNullName_Insert()
    {
        application.setName( null );
        try
        {
            applicationDAO.insert( application );
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
        application.setName( "testValidateNullName" );
        assertEquals( "testvalidatenullname", application.getNameLowercaseNoWhitespace() );
        applicationDAO.insert( application );

        application.setName( null );
        assertNull( application.getNameLowercaseNoWhitespace() );
        try
        {
            applicationDAO.update( application );
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
        application.setName( " " );
        try
        {
            applicationDAO.insert( application );
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
        application.setName( "testValidateEmptyName" );
        assertEquals( "testvalidateemptyname", application.getNameLowercaseNoWhitespace() );
        applicationDAO.insert( application );

        application.setName( " " );
        assertEquals( "", application.getNameLowercaseNoWhitespace() );
        try
        {
            applicationDAO.update( application );
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
            application.setName( name );
            try
            {
                applicationDAO.insert( application );
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
        application.setName( "testValidateNameInvalidChars" );
        applicationDAO.insert( application );
        String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
        for ( String name : invalidAlphaNumericNames )
        {
            application.setName( name );
            try
            {
                applicationDAO.update( application );
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
            application.setName( name );
            try
            {
                applicationDAO.insert( application );
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
        application.setName( "testValidateNameSpaces" );
        applicationDAO.insert( application );

        String[] invalidSpacingNames =
            { " leading space", "trailing space ", "double  space", "  starts with double space",
                "ends with double space  " };
        for ( String name : invalidSpacingNames )
        {
            application.setName( name );
            try
            {
                applicationDAO.update( application );
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

        application.setName( name );
        applicationDAO.insert( application );

        assertEquals( name, application.getName() );
        assertEquals( "teststringwithcaseandwhitespace", application.getNameLowercaseNoWhitespace() );

        String name1 = "TEST String      With    cASE and      whitespace";
        Application application1 = applicationDAO.getByName( name1 );
        assertNotNull( application1 );
        assertEquals( application.getId(), application1.getId() );
    }

    @Test
    public void testDuplicateName_Insert()
    {
        application.setName( "testDuplicateName" );
        applicationDAO.insert( application );

        Application application1 = new Application();
        application1.setPublicId( "testDuplicateName1" );
        application1.setName( "Test Duplicate Name" );
        try
        {
            applicationDAO.insert( application1 );
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
        application.setName( "testDuplicateName" );
        applicationDAO.insert( application );

        Application application1 = new Application();
        application1.setPublicId( "testpublicid1" );
        application1.setName( "testDuplicateName1" );
        applicationDAO.insert( application1 );

        application1.setName( "Test Duplicate Name" );
        try
        {
            applicationDAO.update( application1 );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "Test Duplicate Name is already used as a name.", expected.getMessage() );
        }
    }

    @Test
    public void testCascadeDeleteToLabels()
    {
        application.setName( "testCascadeDeleteToLabels" );
        applicationDAO.insert( application );

        LabelDAO labelDAO = new LabelDAO();
        Label label = new Label( application.getId(), "testCascadeDeleteToLabels", Color.blue );
        labelDAO.insert( label );

        applicationDAO.delete( application );
    }
}

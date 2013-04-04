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

import org.junit.After;
import org.junit.Test;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationProfile;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.error.exception.BadRequestException;

public class ApplicationProfileDAOTest
    extends AbstractDbDAOTest
{
    private ApplicationProfileDAO dao = new ApplicationProfileDAO();

    private ApplicationProfile applicationProfile = new ApplicationProfile();

    @After
    public void cleanUp()
    {
        if ( applicationProfile != null && applicationProfile.getId() != null )
        {
            dao.delete( applicationProfile );
        }
    }

    @Test
    public void testCRUD()
    {
        // Insert
        applicationProfile.setName( "testCRUD" );
        dao.insert( applicationProfile );
        assertEquals( "testCRUD", applicationProfile.getName() );
        assertEquals( "testcrud", applicationProfile.getNameLowercaseNoWhitespace() );

        // Get
        applicationProfile = dao.getById( applicationProfile.getId() );
        assertNotNull( applicationProfile );
        assertEquals( "testCRUD", applicationProfile.getName() );
        assertEquals( "testcrud", applicationProfile.getNameLowercaseNoWhitespace() );

        // Update
        applicationProfile.setName( "testCRUD Updated" );
        dao.update( applicationProfile );
        applicationProfile = dao.getById( applicationProfile.getId() );
        assertEquals( "testCRUD Updated", applicationProfile.getName() );
        assertEquals( "testcrudupdated", applicationProfile.getNameLowercaseNoWhitespace() );

        // Get All
        List<ApplicationProfile> applicationProfiles = dao.getAll();
        assertEquals( 2, applicationProfiles.size() );

        // Delete
        dao.delete( applicationProfile );
        applicationProfile = dao.getById( applicationProfile.getId() );
        assertNull( applicationProfile );
    }

    @Test
    public void testValidateNullName_Insert()
    {
        try
        {
            dao.insert( applicationProfile );
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
        applicationProfile.setName( "testValidateNullName" );
        assertEquals( "testvalidatenullname", applicationProfile.getNameLowercaseNoWhitespace() );
        dao.insert( applicationProfile );

        applicationProfile.setName( null );
        assertNull( applicationProfile.getNameLowercaseNoWhitespace() );
        try
        {
            dao.update( applicationProfile );
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
        applicationProfile.setName( " " );
        try
        {
            dao.insert( applicationProfile );
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
        applicationProfile.setName( "testValidateEmptyName" );
        assertEquals( "testvalidateemptyname", applicationProfile.getNameLowercaseNoWhitespace() );
        dao.insert( applicationProfile );

        applicationProfile.setName( " " );
        assertEquals( "", applicationProfile.getNameLowercaseNoWhitespace() );
        try
        {
            dao.update( applicationProfile );
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
            applicationProfile.setName( name );
            try
            {
                dao.insert( applicationProfile );
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
        applicationProfile.setName( "testValidateNameInvalidChars" );
        dao.insert( applicationProfile );
        String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
        for ( String name : invalidAlphaNumericNames )
        {
            applicationProfile.setName( name );
            try
            {
                dao.update( applicationProfile );
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
            applicationProfile.setName( name );
            try
            {
                dao.insert( applicationProfile );
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
        applicationProfile.setName( "testValidateNameSpaces" );
        dao.insert( applicationProfile );

        String[] invalidSpacingNames =
            { " leading space", "trailing space ", "double  space", "  starts with double space",
                "ends with double space  " };
        for ( String name : invalidSpacingNames )
        {
            applicationProfile.setName( name );
            try
            {
                dao.update( applicationProfile );
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

        applicationProfile.setName( name );
        dao.insert( applicationProfile );

        assertEquals( name, applicationProfile.getName() );
        assertEquals( "teststringwithcaseandwhitespace", applicationProfile.getNameLowercaseNoWhitespace() );

        String name1 = "TEST String      With    cASE and      whitespace";
        ApplicationProfile applicationProfile1 = dao.getByName( name1 );
        assertNotNull( applicationProfile1 );
        assertEquals( applicationProfile.getId(), applicationProfile1.getId() );
    }

    @Test
    public void testDuplicateName_Insert()
    {
        applicationProfile.setName( "testDuplicateName" );
        dao.insert( applicationProfile );

        ApplicationProfile applicationProfile1 = new ApplicationProfile( "Test Duplicate Name" );
        try
        {
            dao.insert( applicationProfile1 );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "An application profile with the same name already exists.", expected.getMessage() );
        }
    }

    @Test
    public void testDuplicateName_Update()
    {
        applicationProfile.setName( "testDuplicateName" );
        dao.insert( applicationProfile );

        ApplicationProfile applicationProfile1 = new ApplicationProfile( "testDuplicateName1" );
        dao.insert( applicationProfile1 );

        applicationProfile1.setName( "Test Duplicate Name" );
        try
        {
            dao.update( applicationProfile1 );
            fail( "Expected InvalidNameException" );
        }
        catch ( InvalidNameException expected )
        {
            assertEquals( "An application profile with the same name already exists.", expected.getMessage() );
        }

        dao.delete( applicationProfile1 );
    }

    @Test
    public void testDeleteProfileUsedByApplication()
    {
        applicationProfile.setName( "testDeleteProfileUsedByApplication" );
        dao.insert( applicationProfile );

        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByIdNotNull( applicationId );
        application.setApplicationProfileId( applicationProfile.getId() );
        applicationDAO.update( application );

        try
        {
            dao.delete( applicationProfile );
            fail( "Expected BadRequestException" );
        }
        catch ( BadRequestException expected )
        {
            assertEquals( "Cannot delete an application profile that is used by applications.", expected.getMessage() );
        }

        application.setApplicationProfileId( null );
        applicationDAO.update( application );
    }

    @Test
    public void testDeleteLastApplicationProfile()
    {
        List<ApplicationProfile> applicationProfiles = dao.getAll();
        assertEquals( applicationProfiles.toString(), 1, applicationProfiles.size() );

        try
        {
            dao.delete( applicationProfiles.get( 0 ) );
            fail( "Expected BadRequestException" );
        }
        catch ( BadRequestException expected )
        {
            assertEquals( "Cannot delete the last application profile.", expected.getMessage() );
        }
    }
}

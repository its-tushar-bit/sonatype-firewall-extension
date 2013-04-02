/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sonatype.insight.brain.model.Application;

public class ApplicationDAOTest
    extends AbstractDbDAOTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testCRUD()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();

        // Create
        // The super class creates an application by default

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
        applicationDAO.delete( application );
        application = applicationDAO.getById( applicationId );
        Assert.assertNull( application );
    }

    @Test
    public void testApplicationRules()
    {
        final String appPublicId = "testApplicationRulesPublicID";
        final String appName = "añҘ長-AppName 34AppName";

        Application application = new Application();
        application.setPublicId( appPublicId );
        ApplicationDAO applicationDAO = new ApplicationDAO();

        // Name Rules
        exception.expect( InvalidApplicationException.class );
        exception.expectMessage( "Name is required." );
        applicationDAO.insert( application );

        final String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
        for ( String name : invalidAlphaNumericNames )
        {
            application.setName( name );
            exception.expect( InvalidApplicationException.class );
            exception.expectMessage( "Name must be alpha numeric." );
            applicationDAO.insert( application );
        }

        final String[] invalidSpacingNames =
            { " leading space", "trailing space ", "double  space", "  starts with double space",
                "ends with double space  " };
        for ( String name : invalidSpacingNames )
        {
            application.setName( name );
            exception.expect( InvalidApplicationException.class );
            exception.expectMessage( "Name must not have leading or trailing spaces, or have two spaces in a row" );
            applicationDAO.insert( application );
        }

        // Application with this name is created by super
        application.setName( ApplicationDAOTest.applicationName );
        exception.expect( InvalidApplicationException.class );
        exception.expectMessage( ApplicationDAOTest.applicationName + " is already used as a name." );
        applicationDAO.insert( application );

        application.setName( appName );
        application.setPublicId( "" );
        exception.expect( InvalidApplicationException.class );
        exception.expectMessage( "ID is required." );
        applicationDAO.insert( application );

        application.setPublicId( ApplicationDAOTest.applicationPublicId );
        exception.expect( InvalidApplicationException.class );
        exception.expectMessage( ApplicationDAOTest.applicationPublicId + " is already used as an ID." );
        applicationDAO.insert( application );

        application.setPublicId( appPublicId );
        applicationDAO.insert( application );

        application = applicationDAO.getByPublicId( appPublicId );
        Assert.assertNotNull( application );
        Assert.assertEquals( appName, application.getName() );

        application.setPublicId( "newPublicID" );
        exception.expect( InvalidApplicationException.class );
        exception.expectMessage( "Cannot change Public ID of existing application." );
        applicationDAO.update( application );

        application.setId( "newID" );
        exception.expect( InvalidApplicationException.class );
        exception.expectMessage( "Attempting to edit an application that doesn't exist. ID " + appPublicId );
        applicationDAO.update( application );
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
    public void testNameIsCaseAndWhitespaceInsensitive()
    {
        String appName = "test string With Case and Whitespace";

        Application application = new Application();
        application.setName( appName );
        application.setPublicId( "test public id" );
        ApplicationDAO applicationDAO = new ApplicationDAO();
        applicationDAO.insert( application );

        Assert.assertEquals( appName, application.getName() );
        Assert.assertEquals( appName.replaceAll( "\\s", "" ).toLowerCase( Locale.ENGLISH ),
                             application.getNameLowercaseNoWhitespace() );

        String appNameCaseAndWhiteSpace = "TEST String      With    cASE and      whitespace";
        application = applicationDAO.getByName( appNameCaseAndWhiteSpace );
        Assert.assertNotNull( application );
        Assert.assertEquals( appNameCaseAndWhiteSpace.replaceAll( "\\s", "" ).toLowerCase( Locale.ENGLISH ),
                             application.getNameLowercaseNoWhitespace() );
        Assert.assertEquals( appName, application.getName() );
    }
}

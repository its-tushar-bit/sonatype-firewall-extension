/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;

import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractLicenseTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

public class ValidationClientTest
    extends AbstractLicenseTest
{

    private void assertMatch( String pattern, String text )
    {
        assertTrue( text + " does not match pattern " + pattern, text != null && text.matches( pattern ) );
    }

    @Test
    public void testValidateConfiguration_AllGood()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        new ValidationClient( config ).validateConfiguration();
    }

    @Test
    public void testValidateConfiguration_BadContextRoot()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        config.setServerUrl( config.getServerUrl() + "/bad" );
        try
        {
            new ValidationClient( config ).validateConfiguration();
            fail( "Validation should have failed due to bad context root" );
        }
        catch ( HttpResponseException e )
        {
            assertEquals( 404, e.getStatusCode() );
            assertMatch( "(?i).*not found.*", e.getMessage() );
        }
    }

    @Test
    public void testValidateConfiguration_BadHost()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        config.setServerUrl( "http://bad.host/" );
        try
        {
            new ValidationClient( config ).validateConfiguration();
            fail( "Validation should have failed due to bad host" );
        }
        catch ( IOException e )
        {
            assertEquals( "Unknown host: bad.host", e.getMessage() );
        }
    }

    @Test
    public void testValidateConfiguration_BadPort()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        config.setServerUrl( "http://localhost:65535/" );
        try
        {
            new ValidationClient( config ).validateConfiguration();
            fail( "Validation should have failed due to bad port" );
        }
        catch ( IOException e )
        {
            assertMatch( "(?i).*Connection.* refused.*", e.getMessage() );
        }
    }

    @Test
    public void testValidateConfiguration_InvalidPort()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        config.setServerUrl( "http://localhost:NaN/" );
        try
        {
            new ValidationClient( config ).validateConfiguration();
            fail( "Validation should have failed due to invalid port" );
        }
        catch ( Exception e )
        {
            assertMatch( "(?i).*Invalid port.*", e.getMessage() );
        }
    }

    @Test
    public void testValidateConfiguration_BadProxyHost()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        config.setProxy( "bad.host" );
        try
        {
            new ValidationClient( config ).validateConfiguration();
            fail( "Validation should have failed due to bad proxy host" );
        }
        catch ( IOException e )
        {
            assertEquals( "Unknown host: bad.host", e.getMessage() );
        }
    }

    @Test
    public void testValidateConfiguration_BadProxyPort()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        config.setProxy( "localhost:65535" );
        try
        {
            new ValidationClient( config ).validateConfiguration();
            fail( "Validation should have failed due to bad proxy port" );
        }
        catch ( IOException e )
        {
            assertMatch( "(?i).*Connection.* refused.*", e.getMessage() );
        }
    }

    @Test
    public void testValidateApplicationId_AllGood()
        throws Exception
    {
        Application app = createApplication( "valid-id" );

        new ValidationClient( brain.getClientConfiguration() ).validateApplicationId( app.getPublicId() );
    }

    @Test
    public void testValidateApplicationId_UnknownId()
        throws Exception
    {
        try
        {
            new ValidationClient( brain.getClientConfiguration() ).validateApplicationId( "unknown-id" );
            fail( "Validation should have failed due to bad app id" );
        }
        catch ( IOException e )
        {
            Assert.assertEquals( "Invalid application id unknown-id", e.getMessage() );
        }
    }

    @Test
    public void testValidate_getApplicationIdNameMap()
        throws Exception
    {
        Application app = createApplication( "valid-id" );

        Map<String,String> map = new ValidationClient( brain.getClientConfiguration() ).getApplicationIdNameMap();

        assertEquals( 1, map.size() );
        assertTrue( map.containsKey( "valid-id" ) );
        assertEquals( app.getName(), map.get( "valid-id" ) );
    }
}

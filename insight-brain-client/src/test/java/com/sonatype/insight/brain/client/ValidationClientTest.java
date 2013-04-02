/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ValidationClientTest
    extends AbstractBrainServiceTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

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
        catch ( IOException e )
        {
            assertMatch( "(?i).*404.*not found.*", e.getMessage() );
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
        new ValidationClient( brain.getClientConfiguration() ).validateApplicationId( "valid-id" );
    }

    @Test
    public void testValidateApplicationId_SpecialCharacters()
        throws Exception
    {
        new ValidationClient( brain.getClientConfiguration() ).validateApplicationId( "id : % &" );
    }

    @Test
    public void testValidateApplicationId_UnknownId()
        throws Exception
    {
        invalidateAppId( "unknown-id", "Not Found" );
        try
        {
            new ValidationClient( brain.getClientConfiguration() ).validateApplicationId( "unknown-id" );
            fail( "Validation should have failed due to bad app id" );
        }
        catch ( IOException e )
        {
            assertMatch( "(?i).*invalid.*not found.*", e.getMessage() );
        }
    }

}

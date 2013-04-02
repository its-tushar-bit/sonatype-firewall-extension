/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;

import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.ApplicationProfile;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ApplicationProfileResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testCRUD()
        throws Exception
    {
        // Add
        ApplicationProfile applicationProfile = new ApplicationProfile();
        applicationProfile.setName( "My app profile" );
        Response response = RestAccess.post( getServiceURL(), JsonHelpers.asJson( applicationProfile ) );
        assertResponseStatus( 200, response );
        applicationProfile = JsonHelpers.fromJson( response.getResponseBody(), ApplicationProfile.class );
        String applicationProfileId = applicationProfile.getId();
        assertApplicationProfile( applicationProfileId, "My app profile", applicationProfile );

        // Get all
        response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );
        ApplicationProfile[] applicationProfiles =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationProfile[].class );
        assertNotNull( applicationProfiles );
        assertEquals( 2, applicationProfiles.length );
        assertApplicationProfile( applicationProfileId, "My app profile", applicationProfiles[1] );

        // Update
        applicationProfile.setName( "My updated app profile" );
        response = RestAccess.put( getServiceURL(), JsonHelpers.asJson( applicationProfile ) );
        assertResponseStatus( 200, response );
        applicationProfile = JsonHelpers.fromJson( response.getResponseBody(), ApplicationProfile.class );
        assertApplicationProfile( applicationProfileId, "My updated app profile", applicationProfile );

        // Get all
        response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );
        applicationProfiles = JsonHelpers.fromJson( response.getResponseBody(), ApplicationProfile[].class );
        assertNotNull( applicationProfiles );
        assertEquals( 2, applicationProfiles.length );
        assertApplicationProfile( applicationProfileId, "My updated app profile", applicationProfiles[1] );

        // Delete
        response = RestAccess.delete( getServiceURL() + "/" + applicationProfileId );
        assertResponseStatus( 204, response );

        // Get all
        response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );
        applicationProfiles = JsonHelpers.fromJson( response.getResponseBody(), ApplicationProfile[].class );
        assertNotNull( applicationProfiles );
        assertEquals( 1, applicationProfiles.length );
    }

    private void assertApplicationProfile( String id, String name, ApplicationProfile actual )
    {
        assertEquals( id, actual.getId() );
        assertEquals( name, actual.getName() );
        assertEquals( name.replace( " ", "" ).toLowerCase( Locale.ENGLISH ), actual.getNameLowercaseNoWhitespace() );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + ApplicationProfileResource.SERVICE_PATH;
    }
}

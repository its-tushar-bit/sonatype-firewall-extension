/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class LabelResourceTest
    extends AbstractResourceTest
{
    private ApplicationDAO applicationDAO = new ApplicationDAO();

    @Test
    public void testCRUD()
        throws Exception
    {
        // Create an application
        String appId = "LabelDAOTest_AppId";
        Application application = new Application();
        application.setPublicId( appId );
        applicationDAO.insert( application );

        // Get all labels
        Response response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        Label[] labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 0, labels.length );

        // Add a label
        Label label = new Label();
        label.setLabel( "My label" );
        response = RestAccess.post( getServiceURL( appId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "My label", null /* color */, label );

        // Get all labels
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 1, labels.length );
        assertLabel( application.getId(), "My label", null /* color */, labels[0] );

        // Update a label
        label.setLabel( "My updated label" );
        response = RestAccess.put( getServiceURL( appId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "My updated label", null /* color */, label );

        // Get all labels
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 1, labels.length );
        assertLabel( application.getId(), "My updated label", null /* color */, labels[0] );

        // Delete a label
        response = RestAccess.delete( getServiceURL( appId ) + "/" + label.getId() );
        assertResponseStatus( 204, response );

        // Get all labels
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 0, labels.length );
    }

    private void assertLabel( String applicationId, String label, Color color, Label actual )
    {
        Assert.assertEquals( applicationId, actual.getApplicationId() );
        Assert.assertEquals( label, actual.getLabel() );
        Assert.assertEquals( label.toLowerCase( Locale.ENGLISH ), actual.getLabelLowercase() );
        Assert.assertEquals( color, actual.getColor() );
    }

    private static String getServiceURL( final String appId )
    {
        return RestAccess.BASE_URL + LabelResource.SERVICE_PATH.replace( "{appId}", appId );
    }
}

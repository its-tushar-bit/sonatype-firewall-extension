/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class LabelResourceTest
    extends AbstractResourceTest
{
    private LabelDAO labelDAO = new LabelDAO();

    @Test
    public void testAddDuplicateLabel()
        throws Exception
    {
        String appPublicId = "LabelResourceTest_AppId";
        Application application = createApplication( appPublicId );

        // Add a label
        Label label = new Label();
        label.setColor( Color.blue );
        label.setLabel( "My label" );
        Response response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "My label", Color.blue, label );

        // Add another label with the same name
        label = new Label();
        label.setColor( Color.blue );
        label.setLabel( "My label" );
        response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 409, response );
        String message = response.getResponseBody();
        Assert.assertEquals( "A label with the same name already exists", message );
    }

    @Test
    public void testUpdateDuplicateLabel()
        throws Exception
    {
        String appPublicId = "LabelResourceTest_AppId";
        Application application = createApplication( appPublicId );

        Label label1 = new Label();
        label1.setColor( Color.blue );
        label1.setLabel( "My label 1" );
        Response response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label1 ) );
        assertResponseStatus( 200, response );
        label1 = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "My label 1", Color.blue, label1 );
        Label label2 = new Label();
        label2.setColor( Color.blue );
        label2.setLabel( "My label 2" );
        response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label2 ) );
        assertResponseStatus( 200, response );
        label2 = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "My label 2", Color.blue, label2 );

        label2.setLabel( label1.getLabel() );
        response = RestAccess.put( getServiceURL( appPublicId ), JsonHelpers.asJson( label2 ) );
        assertResponseStatus( 409, response );
        String message = response.getResponseBody();
        Assert.assertEquals( "A label with the same name already exists", message );
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        // Create an application
        String appPublicId = "LabelResourceTest_AppId";
        Application application = createApplication( appPublicId );

        // Get all labels
        Response response = RestAccess.get( getServiceURL( appPublicId ) );
        assertResponseStatus( 200, response );
        Label[] labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 0, labels.length );

        // Add a label
        Label label = new Label();
        label.setLabel( "My label" );
        response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "My label", null /* color */, label );

        // Get all labels
        response = RestAccess.get( getServiceURL( appPublicId ) );
        assertResponseStatus( 200, response );
        labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 1, labels.length );
        assertLabel( application.getId(), "My label", null /* color */, labels[0] );

        // Update a label
        label.setLabel( "My updated label" );
        response = RestAccess.put( getServiceURL( appPublicId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "My updated label", null /* color */, label );

        // Get all labels
        response = RestAccess.get( getServiceURL( appPublicId ) );
        assertResponseStatus( 200, response );
        labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 1, labels.length );
        assertLabel( application.getId(), "My updated label", null /* color */, labels[0] );

        // Delete a label
        response = RestAccess.delete( getServiceURL( appPublicId ) + "/" + label.getId() );
        assertResponseStatus( 204, response );

        // Get all labels
        response = RestAccess.get( getServiceURL( appPublicId ) );
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

    private String getServiceURL( final String appId )
    {
        return getRestBaseUrl() + LabelResource.SERVICE_PATH.replace( "{applicationPublicId}", appId );
    }

    @Override
    protected void cleanupApplication( Application application )
    {
        for ( Label label : labelDAO.getByApplicationId( application.getId() ) )
        {
            labelDAO.delete( label );
        }
        super.cleanupApplication( application );
    }
}

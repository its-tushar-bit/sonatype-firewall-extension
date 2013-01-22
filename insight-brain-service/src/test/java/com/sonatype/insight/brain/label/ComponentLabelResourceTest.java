/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ComponentLabelResourceTest
    extends AbstractResourceTest
{
    private LabelDAO labelDAO = new LabelDAO();

    @Override
    protected void cleanupApplication( Application application )
    {
        for ( Label label : labelDAO.getByApplicationId( application.getId() ) )
        {
            labelDAO.delete( label );
        }
        super.cleanupApplication( application );
    }

    @Test
    public void testSetGetComponentLabels()
        throws Exception
    {
        // Create an application
        String appPublicId = "ComponentLabelResourceTest_AppId";
        createApplication( appPublicId );

        String hash = "bababababa";
        Set<String> labels = toLabelSet( "LabelY", "LabelX" );
        ComponentLabelState state = new ComponentLabelState();
        state.setLabels( labels );
        Response response = RestAccess.put( getServiceURL( appPublicId, hash ), JsonHelpers.asJson( state ) );
        assertResponseStatus( 200, response );
        Label[] componentLabels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertEquals( 2, componentLabels.length );
        Assert.assertEquals( "LabelX", componentLabels[0].getLabel() );
        Assert.assertEquals( "LabelY", componentLabels[1].getLabel() );

        response = RestAccess.get( getServiceURL( appPublicId, hash ) );
        assertResponseStatus( 200, response );
        componentLabels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertEquals( 2, componentLabels.length );
        Assert.assertEquals( "LabelX", componentLabels[0].getLabel() );
        Assert.assertEquals( "LabelY", componentLabels[1].getLabel() );
    }

    @Test
    public void testSetComponentLabels_TooLong()
        throws Exception
    {
        String appPublicId = "ComponentLabelResourceTest_AppId";
        createApplication( appPublicId );

        String hash = "bababababa";
        Set<String> labels = toLabelSet( "A_very_long_label_that_exceeds_our_maximum_label_length_and_cannot_be_added" );
        ComponentLabelState state = new ComponentLabelState();
        state.setLabels( labels );
        Response response = RestAccess.put( getServiceURL( appPublicId, hash ), JsonHelpers.asJson( state ) );
        assertResponseStatus( 400, response );
        String msg = response.getResponseBody();
        Assert.assertNotNull( msg );
        Assert.assertTrue( msg, msg.contains( "exceeds the maximum length" ) );
    }

    private Set<String> toLabelSet( String... labels )
    {
        Set<String> labelSet = new LinkedHashSet<String>();
        Collections.addAll( labelSet, labels );
        return labelSet;
    }

    private String getServiceURL( String applicationPublicId, String hash )
    {
        return getRestBaseUrl()
            + ComponentLabelResource.SERVICE_PATH.replace( "{applicationPublicId}", applicationPublicId ).replace( "{hash}",
                                                                                                                   hash );
    }
}

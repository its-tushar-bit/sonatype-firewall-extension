/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

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
        Response response = RestAccess.put( getServiceURL( appPublicId, hash ), JsonHelpers.asJson( labels ) );
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

    private Set<String> toLabelSet( String... labels )
    {
        Set<String> labelSet = new LinkedHashSet<String>();
        for ( String label : labels )
        {
            labelSet.add( label );
        }
        return labelSet;
    }

    private String getServiceURL( String applicationPublicId, String hash )
    {
        return getRestBaseUrl()
            + ComponentLabelResource.SERVICE_PATH.replace( "{applicationPublicId}", applicationPublicId ).replace( "{hash}",
                                                                                                                   hash );
    }
}
